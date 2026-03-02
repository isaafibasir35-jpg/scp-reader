import requests
import json
import time
import re
from collections import defaultdict

def get_category(name):
    match = re.search(r'(\d+)', name)
    if match:
        num = int(match.group(1))
        if 1 <= num <= 999: return "Серия I"
        elif 1000 <= num <= 1999: return "Серия II"
        elif 2000 <= num <= 2999: return "Серия III"
        elif 3000 <= num <= 3999: return "Серия IV"
        elif 4000 <= num <= 4999: return "Серия V"
        elif 5000 <= num <= 5999: return "Серия VI"
        elif 6000 <= num <= 6999: return "Серия VII"
        elif 7000 <= num <= 7999: return "Серия VIII"
        elif 8000 <= num <= 8999: return "Серия IX"
        elif 9000 <= num <= 9999: return "Серия X"
    
    if "-RU" in name.upper():
        return "Филиал RU"
    return "Прочие"

def clean_title(name, title):
    clean = title.replace(name, '').strip()
    clean = clean.strip(' -—–:[]
	')
    if not clean:
        return f"Объект {name}"
    return clean

def main():
    base_url = "https://scpper.com/api/v2/find-pages"
    params = {
        "site": "ru",
        "kind": "scp",
        "limit": 50,
        "offset": 0
    }
    
    db = defaultdict(list)
    total_count = 0
    max_pages = 20  # Ограничим для GitHub Actions (1000 объектов), можно увеличить
    
    print("Starting automated SCP database update...")
    
    while params["offset"] < (max_pages * 50):
        try:
            response = requests.get(base_url, params=params, timeout=30)
            response.raise_for_status()
            data = response.json()
            
            pages = data.get("pages", [])
            if not pages:
                break
                
            for p in pages:
                name = str(p.get("name", "")).upper()
                title = str(p.get("title", ""))
                
                if name.startswith("SCP-"):
                    cat = get_category(name)
                    cleaned = clean_title(name, title)
                    db[cat].append(f"{name}|||{cleaned}")
                    total_count += 1
            
            print(f"Collected {total_count} objects...")
            params["offset"] += 50
            time.sleep(0.5)
            
        except Exception as e:
            print(f"Error occurred: {e}. Retrying...")
            time.sleep(5)
            continue

    if not db:
        print("No data collected.")
        return

    # Sort data
    sorted_db = {k: sorted(v) for k, v in db.items()}
    
    output_path = "app/src/main/assets/database.json"
    import os
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(sorted_db, f, ensure_ascii=False, indent=2)
    
    print(f"Success! Database saved to {output_path}")
    print(f"Total objects: {total_count}")

if __name__ == "__main__":
    main()

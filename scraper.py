import requests
import time
import json
import re
from collections import defaultdict

def get_category(name):
    # Пытаемся найти число в названии объекта (например, SCP-1234 -> 1234)
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
    # Очистка заголовка от лишних символов и повторения номера
    clean = title.replace(name, '').strip()
    clean = clean.strip(' -—–:[]\n\t')
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
    
    print("=== ЗАПУСК ПОЛНОГО СБОРА БАЗЫ SCP ===")
    
    while True:
        try:
            response = requests.get(base_url, params=params, timeout=15)
            response.raise_for_status()
            data = response.json()
            
            pages = data.get("pages", [])
            if not pages:
                break
                
            for p in pages:
                name = str(p.get("name", "")).upper()
                title = str(p.get("title", ""))
                
                # Фильтруем только основные объекты SCP
                if name.startswith("SCP-"):
                    cat = get_category(name)
                    cleaned = clean_title(name, title)
                    db[cat].append(f"{name}|||{cleaned}")
                    total_count += 1
            
            print(f"Собрано {total_count} объектов... Следующий блок...")
            
            # Переход к следующему блоку
            params["offset"] += 50
            time.sleep(0.2) # Небольшая пауза, чтобы не спамить сервер
            
        except (requests.exceptions.RequestException, json.JSONDecodeError) as e:
            print(f"Ошибка связи: {e}. Пробую еще раз через 5 секунд...")
            time.sleep(5)
            continue

    if not db:
        print("Ошибка: данные не были получены.")
        return

    # Сортировка для порядка внутри категорий
    sorted_db = {k: sorted(v) for k, v in db.items()}
    
    output_path = "app/src/main/assets/database.json"
    try:
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(sorted_db, f, ensure_ascii=False, indent=2)
        print(f"=== УСПЕХ: База сохранена в {output_path} ===")
        print(f"Всего объектов: {total_count}")
    except Exception as e:
        print(f"Ошибка при сохранении файла: {e}")

if __name__ == "__main__":
    main()

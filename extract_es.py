import email
import json
import re
from bs4 import BeautifulSoup

def extract_scp_es():
    with open('raw_es.txt', 'rb') as f:
        msg = email.message_from_binary_file(f)
    
    html_content = ""
    for part in msg.walk():
        if part.get_content_type() == 'text/html':
            payload = part.get_payload(decode=True)
            charset = part.get_content_charset() or 'utf-8'
            html_content = payload.decode(charset, errors='replace')
            break
    
    if not html_content:
        with open('raw_es.txt', 'r', encoding='utf-8', errors='replace') as f:
            html_content = f.read()

    soup = BeautifulSoup(html_content, 'html.parser')
    text_content = soup.get_text()
    
    scps = []
    
    # 1. Поиск по ссылкам (основной формат в файле)
    for a in soup.find_all('a'):
        text = a.get_text().strip()
        if re.match(r'^SCP-ES-\d+$', text):
            next_sibling = a.next_sibling
            if next_sibling and isinstance(next_sibling, str):
                title = next_sibling.strip(' -–—')
                if title:
                    scps.append(f"{text}|||{title}")

    # 2. Поиск по шаблону (ES) SCP-XXX, который вы упомянули
    # В MHTML данные могут быть "битыми" или склеенными, ищем гибко
    matches_es_pref = re.findall(r'\(ES\)\s*(SCP-ES-\d+|SCP-\d+)\s*[-–—]\s*([^\n\r<]+)', text_content)
    for m in matches_es_pref:
        number = m[0]
        title = m[1].strip()
        # Добавляем ES если его нет, но есть префикс (ES)
        if not number.startswith('SCP-ES-'):
            number = number.replace('SCP-', 'SCP-ES-')
        
        entry = f"{number}|||{title}"
        if entry not in scps:
            scps.append(entry)

    # 3. Дополнительный поиск для SCP-ES-XXX если они не в ссылках
    matches_es_raw = re.findall(r'(SCP-ES-\d+)\s*[-–—]\s*([^\n\r<]+)', text_content)
    for m in matches_es_raw:
        entry = f"{m[0]}|||{m[1].strip()}"
        if not any(entry.startswith(m[0] + "|||") for entry in scps):
            scps.append(entry)

    # Убираем дубликаты и пустые записи
    unique_scps = []
    seen_numbers = set()
    for item in scps:
        num = item.split('|||')[0]
        if num not in seen_numbers:
            unique_scps.append(item)
            seen_numbers.add(num)

    return unique_scps

def update_db(scps):
    with open('database.json', 'r', encoding='utf-8') as f:
        db = json.load(f)
    
    db["Испанский филиал"] = scps
    
    with open('database.json', 'w', encoding='utf-8') as f:
        json.dump(db, f, ensure_ascii=False, indent=4)
    
    return len(scps)

if __name__ == "__main__":
    found_scps = extract_scp_es()
    count = update_db(found_scps)
    print(f"Extraction complete. Found {count} objects.")

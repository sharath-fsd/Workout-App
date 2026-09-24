from pathlib import Path
from datetime import datetime
import json
import zipfile
from pypdf import PdfReader

root = Path(__file__).resolve().parents[1]
raw = (root / 'output/Saran-Routine.ics').read_bytes()
assert b'\r\n' in raw
assert all(len(line) <= 75 for line in raw.split(b'\r\n'))
lines = raw.decode().replace('\r\n ', '').split('\r\n')
stack = []
events = []
current = None
for line in lines:
    if line.startswith('BEGIN:'):
        component = line[6:]
        stack.append(component)
        if component == 'VEVENT':
            current = {}
        elif component == 'VALARM' and current is not None:
            current['alarm'] = True
    elif line.startswith('END:'):
        component = line[4:]
        assert stack.pop() == component
        if component == 'VEVENT':
            events.append(current)
            current = None
    elif current is not None and stack[-1] == 'VEVENT':
        key, value = line.split(':', 1)
        current[key] = value
assert not stack and len(events) == 18
assert len({event['UID'] for event in events}) == 18
weekdays = ['MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU']
for event in events:
    start = datetime.strptime(event['DTSTART;TZID=Asia/Kolkata'], '%Y%m%dT%H%M%S')
    end = datetime.strptime(event['DTEND;TZID=Asia/Kolkata'], '%Y%m%dT%H%M%S')
    assert end > start and event['alarm']
    assert event['RRULE'].startswith(('FREQ=DAILY', 'FREQ=WEEKLY'))
    if 'BYDAY=' in event['RRULE']:
        assert weekdays[start.weekday()] in event['RRULE'].split('BYDAY=')[1].split(',')

pdf = PdfReader(root / 'output/pdf/Saran-12-Week-Home-Routine.pdf')
assert len(pdf.pages) == 12
assert all(len(page.extract_text()) > 800 for page in pdf.pages)
assert sum(len(page.get('/Annots', [])) for page in pdf.pages) >= 6
with zipfile.ZipFile(root / 'output/Rise-Saran.apk') as apk:
    assert {'assets/guide.pdf', 'assets/content.json', 'classes.dex', 'AndroidManifest.xml'} <= set(apk.namelist())
    content = json.loads(apk.read('assets/content.json'))
    assert len(content['workouts']) == 3
    assert all(len(workout['exercises']) == 6 for workout in content['workouts'])
    assert apk.read('assets/guide.pdf') == (root / 'output/pdf/Saran-12-Week-Home-Routine.pdf').read_bytes()
    media = json.loads(apk.read('assets/media.json'))
    import struct
    with open(root / 'output/Rise-Saran.apk', 'rb') as stream:
        for entry in apk.infolist():
            stream.seek(entry.header_offset + 26)
            name_length, extra_length = struct.unpack('<HH', stream.read(4))
            local_name = stream.read(name_length).decode('utf-8')
            assert '\\' not in local_name and local_name == entry.filename, 'Android ZIP header path mismatch'
    from PIL import Image
    from io import BytesIO
    import re
    count = 0
    for workout in content['workouts']:
        for exercise in workout['exercises']:
            item = media['exercises'][exercise[0]]
            assert re.fullmatch(r'[A-Za-z0-9_-]{11}', item['video'])
            assert item['channel'] and item['title'] and item['alt']
            for image in item['images']:
                with Image.open(BytesIO(apk.read('assets/rise_visuals/' + image))) as picture:
                    picture.verify()
            count += 1
    assert count == 18
    assert len(media['food']) == 5
    for food in media['food']:
        with Image.open(BytesIO(apk.read('assets/rise_visuals/' + food['image']))) as picture:
            picture.verify()
    assert not any('clipboard' in name.lower() for name in apk.namelist())

print('PASS: 18 calendar alarms, 12 PDF pages, 18 illustrated exercise entries, 14 tutorial IDs, 5 food images and offline assets')

#!/usr/bin/env python3
"""공중화장실 CSV → toilets 테이블 bulk import"""

import csv
import os
import sys
import mysql.connector

CSV_PATH = os.environ.get("CSV_PATH", "공중 화장실 정보.csv")

DB = dict(
    host=os.environ.get("DB_HOST", "127.0.0.1"),
    port=int(os.environ.get("DB_PORT", "3306")),
    user=os.environ.get("DB_USER", "root"),
    password=os.environ["DB_PASSWORD"],
    database=os.environ.get("DB_NAME", "geuphalttaen"),
    charset="utf8mb4",
)

INSERT_SQL = """
INSERT INTO toilets
  (name, address, lat, lng, is_public, male, female, disabled, family_room,
   reported_by, status, created_at, updated_at)
VALUES
  (%s, %s, %s, %s, %s, %s, %s, %s, %s,
   NULL, 'ACTIVE', NOW(), NOW())
"""

def to_bool(val: str) -> bool:
    return str(val).strip() == 'Y'

def to_int(val: str) -> int:
    try:
        return int(val.strip())
    except (ValueError, AttributeError):
        return 0

def read_csv(path: str):
    encodings = ['euc-kr', 'cp949', 'utf-8-sig', 'utf-8']
    for enc in encodings:
        try:
            with open(path, encoding=enc, errors='replace') as f:
                rows = list(csv.DictReader(f))
            print(f"  인코딩: {enc}, 총 {len(rows)}행")
            return rows
        except Exception:
            continue
    raise RuntimeError("CSV 읽기 실패")

def main():
    rows = read_csv(CSV_PATH)

    records = []
    skipped = 0
    for row in rows:
        name    = (row.get('화장실명') or '').strip()
        address = (row.get('소재지도로명주소') or row.get('소재지지번주소') or '').strip()
        lat_s   = (row.get('WGS84위도') or '').strip()
        lng_s   = (row.get('WGS84경도') or '').strip()

        if not name or not address or not lat_s or not lng_s:
            skipped += 1
            continue
        try:
            lat = float(lat_s)
            lng = float(lng_s)
        except ValueError:
            skipped += 1
            continue

        if not (-90 <= lat <= 90 and -180 <= lng <= 180):
            skipped += 1
            continue

        male     = to_int(row.get('남성용-대변기수','0')) + to_int(row.get('남성용-소변기수','0')) > 0
        female   = to_int(row.get('여성용-대변기수','0')) > 0
        disabled = (to_int(row.get('남성용-장애인용대변기수','0'))
                  + to_int(row.get('여성용-장애인용대변기수','0'))) > 0
        family   = to_bool(row.get('기저귀교환대유무','N'))

        records.append((name, address, lat, lng, True, male, female, disabled, family))

    print(f"  유효: {len(records)}행, 스킵: {skipped}행")

    conn = mysql.connector.connect(**DB)
    cursor = conn.cursor()

    cursor.execute("DELETE FROM toilets WHERE is_public = TRUE AND reported_by IS NULL")
    deleted = cursor.rowcount
    print(f"  기존 공공데이터 {deleted}행 삭제")

    batch = 500
    for i in range(0, len(records), batch):
        cursor.executemany(INSERT_SQL, records[i:i+batch])
        print(f"  insert {min(i+batch, len(records))}/{len(records)}", end='\r')

    conn.commit()
    cursor.close()
    conn.close()
    print(f"\n완료! {len(records)}개 화장실 import 됨")

if __name__ == '__main__':
    main()

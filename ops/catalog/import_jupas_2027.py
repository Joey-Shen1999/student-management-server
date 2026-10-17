"""Refresh the checked-in Hong Kong programme snapshot from JUPAS.

Run explicitly when reviewing the catalogue; startup uses only the CSV snapshot.
The official English programme title is used where it fits the database limit.
For longer titles the JUPAS short name and code keep the choice identifiable.
"""

import csv
import html
import re
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "src/main/resources/university-programs.seed.csv"
INSTITUTIONS = {
    "cityuhk": "City University of Hong Kong",
    "hkbu": "Hong Kong Baptist University",
    "lingnanu": "Lingnan University",
    "cuhk": "The Chinese University of Hong Kong",
    "eduhk": "The Education University of Hong Kong",
    "polyu": "The Hong Kong Polytechnic University",
    "hkust": "The Hong Kong University of Science and Technology",
    "hku": "The University of Hong Kong",
    "hkmu": "Hong Kong Metropolitan University",
}


def plain(value):
    value = re.sub(r"<br\s*/?>.*", "", value, flags=re.I | re.S)
    value = re.sub(r"<[^>]+>", "", value)
    value = html.unescape(value)
    value = re.sub(r"\s+", " ", value).strip()
    return re.sub(r"(?:\s*\*?\s*(?:NEW|Updated))+$", "", value, flags=re.I).strip()


def programmes(slug, university):
    url = f"https://www.jupas.edu.hk/en/programme/{slug}/"
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        page = response.read().decode("utf-8")
    rows = re.findall(r'<tr>\s*<td class="c-is">.*?</tr>', page, flags=re.S)
    if len(rows) < 10:
        raise ValueError(f"Unexpected JUPAS programme count for {slug}: {len(rows)}")
    seen = set()
    for row in rows:
        cells = re.findall(r'<td class="[^"]+">(.*?)</td>', row, flags=re.S)
        if len(cells) != 5:
            raise ValueError(f"Unexpected JUPAS row: {row[:120]}")
        code = plain(cells[1])
        title = plain(cells[4])
        short = plain(cells[3])
        if not re.fullmatch(r"JS\d{4}", code):
            raise ValueError(f"Unexpected programme code: {code}")
        title = f"{title} ({code})"
        if len(title) > 180:
            title = f"{short} ({code})"
        if not title or len(title) > 180:
            raise ValueError(f"Invalid programme title for {code}: {title}")
        key = (university, code)
        if key in seen:
            raise ValueError(f"Duplicate JUPAS code: {key}")
        seen.add(key)
        yield [university, title, "", ""]


def main():
    with SEED.open(newline="", encoding="utf-8") as source:
        existing = list(csv.reader(source))
    names = set(INSTITUTIONS.values())
    preserved = [row for row in existing[1:] if row and row[0] not in names]
    imported = []
    for slug, university in INSTITUTIONS.items():
        imported.extend(programmes(slug, university))
    with SEED.open("w", newline="", encoding="utf-8") as target:
        writer = csv.writer(target, lineterminator="\n")
        writer.writerow(existing[0])
        writer.writerows(preserved)
        writer.writerows(imported)
    print(f"Imported {len(imported)} JUPAS programmes for {len(names)} Hong Kong universities")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Generate Ontario high school seed data from Ontario open data.

Source dataset:
https://data.ontario.ca/dataset/ontario-public-school-contact-information
"""

import csv
import io
import json
import re
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Dict, Iterable, List, Optional

PACKAGE_ID = "ontario-public-school-contact-information"
PACKAGE_SHOW_URL = "https://data.ontario.ca/api/3/action/package_show"
TARGET_HIGH_SCHOOL_OUTPUT = Path("src/main/resources/canadian-high-schools.seed.csv")
TARGET_EXTERNAL_COURSE_PROVIDER_OUTPUT = Path("src/main/resources/ontario-course-providers.seed.csv")

PROVIDER_SPECIAL_CONDITIONS = {
    "continuing education",
    "summer",
    "adult",
    "online school",
    "temporary remote learning school",
}
PROVIDER_NAME_KEYWORD_PATTERN = re.compile(
    r"\b(summer|night|e learning|elearning|online|virtual|adult|continuing|credit)\b"
)


def request_json(url: str, params: Dict[str, str]) -> Dict[str, object]:
    query_string = urllib.parse.urlencode(params)
    req = urllib.request.Request(
        f"{url}?{query_string}",
        headers={"User-Agent": "student-management-platform/1.0"},
    )
    with urllib.request.urlopen(req, timeout=60) as response:
        return json.loads(response.read().decode("utf-8"))


def request_text(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "student-management-platform/1.0"})
    with urllib.request.urlopen(req, timeout=120) as response:
        return response.read().decode("utf-8-sig", errors="replace")


def normalize_text(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", (value or "").strip().lower()).strip()


def normalize_postal(postal_code: str) -> str:
    compact = re.sub(r"[^A-Za-z0-9]", "", (postal_code or "").upper())
    if len(compact) == 6 and compact.isalnum():
        return f"{compact[:3]} {compact[3:]}"
    return compact


def build_street_address(row: Dict[str, str]) -> str:
    suite = (row.get("Suite") or "").strip()
    po_box = (row.get("PO Box") or "").strip()
    street = (row.get("Street") or "").strip()

    parts: List[str] = []
    if suite:
        parts.append(f"Suite {suite}")
    if po_box:
        parts.append(f"PO Box {po_box}")
    if street:
        parts.append(street)
    return ", ".join(parts)


def pick_english_txt_resource(resources: Iterable[Dict[str, object]]) -> Optional[Dict[str, object]]:
    strict_choices = []
    fallback_choices = []
    for resource in resources:
        name = str(resource.get("name") or "")
        fmt = str(resource.get("format") or "").strip().upper()
        url = str(resource.get("url") or "")
        if fmt != "TXT":
            continue
        lower_url = url.lower()
        lower_name = name.lower()

        if "_en.txt" in lower_url:
            strict_choices.append(resource)
            continue

        if "_fr.txt" in lower_url:
            continue

        if "english" in lower_name and "french" not in lower_name:
            fallback_choices.append(resource)

    choices = strict_choices if strict_choices else fallback_choices
    if not choices:
        return None

    def sort_key(item: Dict[str, object]) -> str:
        return str(item.get("last_modified") or item.get("created") or "")

    choices.sort(key=sort_key, reverse=True)
    return choices[0]


def parse_source_rows(raw_text: str) -> List[Dict[str, str]]:
    reader = csv.DictReader(io.StringIO(raw_text), delimiter="|")
    return [dict(row) for row in reader]

def generate_high_school_rows(source_rows: List[Dict[str, str]]) -> List[Dict[str, str]]:
    rows: List[Dict[str, str]] = []
    seen = set()

    for source_row in source_rows:
        province = (source_row.get("Province") or "").strip()
        if normalize_text(province) != "ontario":
            continue

        school_name = (source_row.get("School Name") or "").strip()
        if not school_name:
            continue
        normalized_school_name = normalize_text(school_name)
        if re.search(r"\b(adult|program|e learning|elearning|virtual)\b", normalized_school_name):
            continue

        school_level = normalize_text(source_row.get("School Level") or "")
        if "secondary" not in school_level:
            continue

        school_type = normalize_text(source_row.get("School Type") or "")
        if school_type not in ("public", "catholic"):
            continue

        special_condition = normalize_text(source_row.get("School Special Conditions") or "")
        if special_condition not in ("", "not applicable"):
            continue

        school_number = (source_row.get("School Number") or "").strip()
        board_number = (source_row.get("Board Number") or "").strip()
        school_id = f"on-public:{board_number}:{school_number}" if school_number else f"on-public:{board_number}:{school_name}"

        street_address = build_street_address(source_row)
        city = (source_row.get("City") or "").strip()
        postal = normalize_postal(source_row.get("Postal Code") or "")

        row = {
            "id": school_id,
            "name": school_name,
            "streetAddress": street_address,
            "city": city,
            "state": "Ontario",
            "country": "Canada",
            "postal": postal,
        }

        dedupe_key = "|".join(
            [
                normalize_text(row["name"]),
                normalize_text(row["streetAddress"]),
                normalize_text(row["city"]),
                normalize_text(row["postal"]),
            ]
        )
        if dedupe_key in seen:
            continue
        seen.add(dedupe_key)
        rows.append(row)

    rows.sort(
        key=lambda item: (
            normalize_text(item["name"]),
            normalize_text(item["city"]),
            normalize_text(item["streetAddress"]),
        )
    )
    return rows

def generate_external_course_provider_rows(source_rows: List[Dict[str, str]]) -> List[Dict[str, str]]:
    rows: List[Dict[str, str]] = []
    seen = set()
    seen_school_ids = set()

    for source_row in source_rows:
        province = (source_row.get("Province") or "").strip()
        if normalize_text(province) != "ontario":
            continue

        school_name = (source_row.get("School Name") or "").strip()
        if not school_name:
            continue
        normalized_school_name = normalize_text(school_name)

        school_level = normalize_text(source_row.get("School Level") or "")
        if "secondary" not in school_level:
            continue

        school_type = normalize_text(source_row.get("School Type") or "")
        if school_type not in ("public", "catholic"):
            continue

        special_condition_raw = (source_row.get("School Special Conditions") or "").strip()
        special_condition = normalize_text(special_condition_raw)
        matches_special_condition = special_condition in PROVIDER_SPECIAL_CONDITIONS
        matches_name_keywords = bool(PROVIDER_NAME_KEYWORD_PATTERN.search(normalized_school_name))
        if not matches_special_condition and not matches_name_keywords:
            continue

        school_number = (source_row.get("School Number") or "").strip()
        board_number = (source_row.get("Board Number") or "").strip()
        school_id = f"on-provider:{board_number}:{school_number}" if school_number else f"on-provider:{board_number}:{school_name}"
        if school_id in seen_school_ids:
            continue

        board_name = (source_row.get("Board Name") or "").strip()
        street_address = build_street_address(source_row)
        city = (source_row.get("City") or "").strip()
        postal = normalize_postal(source_row.get("Postal Code") or "")

        row = {
            "id": school_id,
            "name": school_name,
            "boardName": board_name,
            "schoolSpecialConditions": special_condition_raw,
            "streetAddress": street_address,
            "city": city,
            "state": "Ontario",
            "country": "Canada",
            "postal": postal,
        }

        dedupe_key = "|".join(
            [
                normalize_text(row["name"]),
                normalize_text(row["streetAddress"]),
                normalize_text(row["city"]),
                normalize_text(row["postal"]),
            ]
        )
        if dedupe_key in seen:
            continue
        seen.add(dedupe_key)
        seen_school_ids.add(school_id)
        rows.append(row)

    rows.sort(
        key=lambda item: (
            normalize_text(item["name"]),
            normalize_text(item["city"]),
            normalize_text(item["streetAddress"]),
        )
    )
    return rows


def write_rows(path: Path, rows: List[Dict[str, str]], fieldnames: List[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=fieldnames,
            quoting=csv.QUOTE_ALL,
        )
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    package = request_json(PACKAGE_SHOW_URL, {"id": PACKAGE_ID})
    if not package.get("success"):
        raise RuntimeError("Failed to query Ontario open data package metadata.")

    result = package.get("result") or {}
    resources = result.get("resources") or []
    if not isinstance(resources, list):
        raise RuntimeError("Unexpected package metadata format: resources is not a list.")

    resource = pick_english_txt_resource(resources)
    if resource is None:
        raise RuntimeError("Could not find an English TXT resource in the Ontario package.")

    source_url = str(resource.get("url") or "")
    if not source_url:
        raise RuntimeError("Ontario package resource is missing download URL.")

    raw_text = request_text(source_url)
    source_rows = parse_source_rows(raw_text)

    high_school_rows = generate_high_school_rows(source_rows)
    write_rows(
        TARGET_HIGH_SCHOOL_OUTPUT,
        high_school_rows,
        ["id", "name", "streetAddress", "city", "state", "country", "postal"],
    )

    provider_rows = generate_external_course_provider_rows(source_rows)
    write_rows(
        TARGET_EXTERNAL_COURSE_PROVIDER_OUTPUT,
        provider_rows,
        ["id", "name", "boardName", "schoolSpecialConditions", "streetAddress", "city", "state", "country", "postal"],
    )

    print(f"Source URL: {source_url}")
    print(f"Wrote {len(high_school_rows)} Ontario high school rows to {TARGET_HIGH_SCHOOL_OUTPUT}")
    print(f"Wrote {len(provider_rows)} Ontario external-course provider rows to {TARGET_EXTERNAL_COURSE_PROVIDER_OUTPUT}")


if __name__ == "__main__":
    main()

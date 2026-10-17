# Hong Kong university catalogue (2027 entry)

The catalogue snapshot was checked on 2026-10-15 against the official
[JUPAS programme listings](https://www.jupas.edu.hk/en/search/) for the nine
Hong Kong participating universities. It adds 372 undergraduate programme
choices. Each programme includes its JUPAS catalogue number in the displayed
name, so staff can find the official entry by code. Titles longer than the database's
180-character limit use the JUPAS short name plus code.

Sources: [CityUHK](https://www.jupas.edu.hk/en/programme/cityuhk/),
[HKBU](https://www.jupas.edu.hk/en/programme/hkbu/),
[LingnanU](https://www.jupas.edu.hk/en/programme/lingnanu/),
[CUHK](https://www.jupas.edu.hk/en/programme/cuhk/),
[EdUHK](https://www.jupas.edu.hk/en/programme/eduhk/),
[PolyU](https://www.jupas.edu.hk/en/programme/polyu/),
[HKUST](https://www.jupas.edu.hk/en/programme/hkust/),
[HKU](https://www.jupas.edu.hk/en/programme/hku/), and
[HKMU](https://www.jupas.edu.hk/en/programme/hkmu/).

JUPAS is the HKDSE route. Students applying with other qualifications should
verify the exact programme and application route on the university website.
The "Other / 其他" controls allow staff to enter universities and programmes
that are not in this snapshot. The CSV snapshot is imported at application
startup; `ops/catalog/import_jupas_2027.py` refreshes it only when run
explicitly during a future catalogue review.

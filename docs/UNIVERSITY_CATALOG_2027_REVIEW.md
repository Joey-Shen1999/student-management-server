# University program catalog review (2026-09-28)

The existing seed covers 49 Canadian universities and was last updated in May 2026. This pass checks specific Ontario changes published by OUInfo for the 2027–2028 application cycle. It is a targeted update, not a full re-import of every university's programs.

| University | Catalog change | Official source |
| --- | --- | --- |
| Carleton | Add Systems Security Engineering | https://ouinfo.ca/universities/carleton/ |
| Guelph | Add Electrical Engineering (Co-op) | https://ouinfo.ca/universities/guelph/ |
| Toronto Mississauga | Replace the former Commerce and Management admission categories with Commerce & Management | https://ouinfo.ca/universities/toronto-mississauga/ |
| Toronto Scarborough | Add Climate Change Studies | https://ouinfo.ca/universities/toronto-scarborough/ |
| Waterloo | Add Biomedical Sciences as an entry program and Social Development Studies and Bachelor of Social Work Double Degree | https://ouinfo.ca/universities/waterloo/ and https://ouinfo.ca/universities/waterloo-renison/ |

The seed inserts missing programs without deleting existing records. Only the two superseded Toronto Mississauga admission categories are explicitly marked inactive on startup, so historical applications can retain their program references. User-entered Other programs are created through the authenticated catalog API and become available to other users of the same university.

OUInfo notes that program information can change; application decisions should be checked against the university's current admissions page. Programs awaiting university approval were not added in this pass.

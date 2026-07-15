# ADR 0003: ใช้ Same-origin Session Authentication

- สถานะ: Accepted
- วันที่: 2026-07-15

## บริบท

React และ Spring Boot เป็นระบบของร้านเดียวกัน ผู้ใช้เป็นพนักงานที่ทำงานผ่าน browser และไม่มี mobile/public API client ใน Phase 1

## การตัดสินใจ

Production จะรวม React static files เข้า Spring Boot และใช้ HttpOnly session cookie ร่วมกับ CSRF protection

## เหตุผล

- Browser ไม่ต้องเก็บ access token ใน localStorage
- Spring Security ดูแล session fixation, logout และ authorization ได้โดยตรง
- Same-origin ลด CORS และ cookie configuration ที่ไม่จำเป็น

## ผลกระทบ

- Unsafe requests ต้องส่ง CSRF token
- หากแยก frontend domain หรือเพิ่ม mobile client ภายหลัง ต้องทบทวนแนวทาง authentication ใหม่
- Session storage แบบกระจายยังไม่จำเป็นจนกว่าจะมีหลาย application instances

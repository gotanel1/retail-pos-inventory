# ADR 0001: ใช้ Modular Monolith

- สถานะ: Accepted
- วันที่: 2026-07-15

## บริบท

ระบบมีหลายโดเมน เช่น catalog, inventory, sales และ payments แต่มีผู้พัฒนาหลักคนเดียว ร้านหนึ่งสาขา และกำหนดส่ง MVP ภายในหกสัปดาห์

## การตัดสินใจ

ใช้ Spring Boot application หนึ่งตัวและ PostgreSQL หนึ่งฐานข้อมูล แบ่ง package ตาม business module แต่ละ module เป็นเจ้าของ domain, service, repository และ controller ของตัวเอง

## เหตุผล

- Transaction ระหว่างการขายกับสต็อกทำได้ในฐานข้อมูลเดียว
- Debug, test และ deploy ง่ายกว่า distributed system
- ยังแสดงการแยก bounded context และ dependency direction ใน Portfolio ได้
- สามารถแยกบริการภายหลังได้หากมีหลักฐานด้าน scaling หรือ ownership จริง

## ผลกระทบ

- ต้องมี architecture tests ป้องกันการข้าม module โดยตรง
- ห้ามเรียก repository ของ module อื่น
- การ deploy module ใด module หนึ่งแยกกันยังทำไม่ได้ ซึ่งยอมรับได้สำหรับ Phase 1

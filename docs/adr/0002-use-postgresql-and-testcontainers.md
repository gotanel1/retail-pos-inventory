# ADR 0002: ใช้ PostgreSQL และ Testcontainers

- สถานะ: Accepted
- วันที่: 2026-07-15

## บริบท

กฎสำคัญของระบบขึ้นกับ transaction, row locking, unique constraints และ numeric precision การทดสอบด้วยฐานข้อมูล in-memory อาจให้ผลต่างจาก production

## การตัดสินใจ

ใช้ PostgreSQL 18.4 ทั้ง local, test และ production โดย integration tests สร้างฐานข้อมูลชั่วคราวผ่าน Testcontainers

## เหตุผล

- ทดสอบ Flyway migrations และ PostgreSQL behavior จริง
- รองรับ row-level locking สำหรับป้องกัน overselling
- ลดความเสี่ยงที่ tests ผ่านบน H2 แต่ล้มบน production

## ผลกระทบ

- ผู้พัฒนาต้องเปิด Docker Desktop ก่อนรัน integration tests
- Test รอบแรกช้ากว่าเพราะต้องดาวน์โหลด image
- CI runner ต้องรองรับ Docker ซึ่ง GitHub-hosted Ubuntu runner รองรับอยู่แล้ว

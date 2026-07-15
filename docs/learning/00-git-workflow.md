# บทเรียน 00: GitHub Flow ของโปรเจกต์

## GitHub Flow คืออะไร

GitHub Flow เป็นวิธีทำงานที่มี `main` เป็น branch หลักเพียงตัวเดียว งานแต่ละชิ้นถูกแยกไปทำบน branch ของตัวเอง แล้วจึงนำกลับผ่าน Pull Request หลังการตรวจสอบ

## ทำงานอย่างไร

1. สร้าง GitHub Issue เพื่อระบุปัญหา ขอบเขต และเกณฑ์สำเร็จ
2. อัปเดต `main` และตรวจว่า working tree สะอาด
3. สร้าง branch เช่น `feature/12-inventory-ledger`
4. เขียนโค้ดและ tests เป็น commits เล็กที่อ่านเป็นเรื่องราว
5. Push branch และเปิด Pull Request ที่เชื่อมกับ Issue
6. ตรวจ diff, tests, migration, security และเอกสาร
7. Merge เมื่อ CI ผ่าน แล้วลบ branch ที่จบงาน

## ทำไมเลือก GitHub Flow

- Portfolio แสดงให้ recruiter เห็นวิธีคิดและวินัยการทำงาน ไม่ใช่เฉพาะโค้ดสุดท้าย
- Feature แยกจากกัน ทำให้ review และ rollback ง่าย
- เหมาะกับทีมขนาดเล็กและ continuous delivery มากกว่า GitFlow ที่มีหลาย long-lived branches

## รูปแบบ Commit

หัวข้อใช้ Conventional Commits แต่คำอธิบายเป็นภาษาไทย:

```text
feat(inventory): เพิ่ม Stock Ledger และยอดคงเหลือ
```

Body ต้องบอกเหตุผล การเปลี่ยนแปลง และผลทดสอบ เพื่อให้คนที่ไม่ได้อยู่ตอนเขียนโค้ดเข้าใจที่มาที่ไปได้

## Merge policy

ใช้ merge commit เพื่อรักษา commits รายย่อยและเชื่อมประวัติ Pull Request ห้าม force push หรือเขียนลง `main` โดยตรงหลัง bootstrap

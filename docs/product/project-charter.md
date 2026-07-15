# Project Charter: Retail POS & Inventory

## เป้าหมาย

สร้างระบบ POS และ Inventory สำหรับร้านค้าปลีกสินค้าทั่วไปหนึ่งสาขา โดยแก้ปัญหายอดสต็อกไม่ตรงและตรวจสอบย้อนหลังไม่ได้ พร้อมใช้เป็น Portfolio สำหรับตำแหน่ง Java Backend Developer

## ลูกค้าและโมเดลธุรกิจ

- ลูกค้าเป้าหมาย: ร้านสินค้าทั่วไปหรืออุปกรณ์หนึ่งสาขา
- ค่าติดตั้งมาตรฐาน: 6,900 บาท
- ค่าดูแล: 590 บาทต่อเดือน ไม่รวม Cloud Hosting
- ค่าดูแลรวม backup, ตรวจระบบ และ remote support ไม่เกิน 2 ชั่วโมงต่อเดือน
- Design Partner 3 ร้านแรกทดลองฟรี 30 วันแบบทยอยทีละร้าน และยกเว้นค่าติดตั้งหากใช้ต่อ

## Phase 1

- ผู้ใช้ 4 บทบาท: Owner, Manager, Cashier และ Inventory Staff
- Catalog, barcode และ CSV import แบบ preview
- Supplier, Goods Receipt และต้นทุนถัวเฉลี่ยเคลื่อนที่
- Immutable Stock Ledger, stock balance, reservation และการห้ามสต็อกติดลบ
- Stock Count และ Manager-approved adjustment
- Customer profile และ purchase history ขั้นต่ำ
- POS เงินสด, VAT, Manager-approved discount และใบเสร็จ 80mm/A4
- Stripe PromptPay Test Mode พร้อม reservation 10 นาทีและ idempotent webhook
- Dashboard และรายงานยอดขาย กำไร สต็อก และ movement

## ไม่รวมใน Phase 1

- Purchase Order, return/refund และการเปิดปิดกะ
- Loyalty, production payment และ e-Tax
- Multi-branch, multi-tenant และ offline mode
- ระบบบัญชีและการเชื่อมต่อ hardware โดยตรง

## เกณฑ์สำเร็จ

- ธุรกรรมรับเข้า ขาย และ adjustment ตรวจสอบย้อนหลังได้ครบ
- ไม่สามารถทำให้ available stock ติดลบได้
- การขายสินค้าชิ้นสุดท้ายพร้อมกันสำเร็จเพียงหนึ่งรายการ
- Pilot บันทึกธุรกรรมอย่างน้อย 90% และยอดนับจริงคลาดเคลื่อนไม่เกิน 2%
- Backend domain/service packages มี line coverage อย่างน้อย 80%
- CI, production build และ Docker build ผ่านก่อน merge ทุก Pull Request

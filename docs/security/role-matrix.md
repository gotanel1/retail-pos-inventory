# ตารางสิทธิ์ Phase 1

| ความสามารถ | OWNER | MANAGER | CASHIER | INVENTORY_STAFF |
|---|:---:|:---:|:---:|:---:|
| ดูรายชื่อผู้ใช้ | ✓ | ✓ | – | – |
| สร้างและกำหนดบทบาทผู้ใช้ | ✓ | – | – | – |
| จัดการสินค้าและหมวดหมู่ | ✓ | ✓ | – | ✓ |
| รับสินค้าเข้า | ✓ | ✓ | – | ✓ |
| บันทึกยอดตรวจนับ | ✓ | ✓ | – | ✓ |
| อนุมัติ Stock Adjustment | ✓ | ✓ | – | – |
| เปิดบิลและรับชำระเงิน | ✓ | ✓ | ✓ | – |
| อนุมัติส่วนลดด้วย Manager PIN | ✓ | ✓ | – | – |
| ดู Dashboard และ Reports | ✓ | ✓ | – | – |

เครื่องหมายในตารางคือ policy เป้าหมายของ Phase 1 ปัจจุบัน branch Authentication บังคับสองแถวแรกแล้ว แต่ละ feature branch ต้องเพิ่ม security tests ของแถวที่เกี่ยวข้องก่อนถือว่าเสร็จ

สิทธิ์ถูกตรวจสองชั้น:

1. `SecurityFilterChain` ตรวจว่าผู้เรียกเข้าสู่ระบบแล้ว
2. `@PreAuthorize` ตรวจบทบาทที่ method ของ use case หรือ controller

ฐานข้อมูลไม่ได้เชื่อ role ที่ frontend ส่งมา แต่ใช้ role จาก authenticated session เท่านั้น

# Project collaboration rules

- ตอบและอธิบายกับเจ้าของโปรเจกต์เป็นภาษาไทยเป็นหลัก
- ก่อนเริ่มแต่ละ feature ให้สรุปว่า feature คืออะไร ทำงานอย่างไร และทำไมเลือกแนวทางนั้น
- ห้าม commit ลง `main` โดยตรงหลัง repository bootstrap
- ทุกงานต้องมี GitHub Issue, feature branch, Pull Request และผลทดสอบก่อน merge
- ใช้ Conventional Commits โดยอธิบายหัวข้อ เหตุผล การเปลี่ยนแปลง และผลทดสอบเป็นภาษาไทย
- ตรวจ `git diff --cached` และรัน test ที่เกี่ยวข้องก่อน commit ทุกครั้ง
- เก็บบทเรียนที่ทบทวนภายหลังได้ใน `docs/learning`
- เก็บการตัดสินใจเชิงสถาปัตยกรรมใน `docs/adr`
- ห้าม commit secrets, production data หรือข้อมูลส่วนบุคคลของลูกค้า
- รักษา MVP scope; ฟีเจอร์ Phase 2 ต้องไม่แทรกเข้ามาโดยไม่มีการแก้ Project Charter

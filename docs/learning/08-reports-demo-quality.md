# บทเรียน 08: Reporting, Demo Data และ Quality Gates

## ฟีเจอร์นี้คืออะไร

branch นี้ปิดวงจร Portfolio ให้ดูได้จริงสามด้านพร้อมกัน:

1. Dashboard ช่วย OWNER/MANAGER ตอบคำถามเรื่องยอดขาย กำไรขั้นต้น วิธีชำระ มูลค่าสต็อก สินค้าใกล้หมด และประวัติ movement
2. Demo data ทำให้ recruiter ทดลองระบบได้โดยไม่ใช้ข้อมูลลูกค้าจริง
3. Quality gates พิสูจน์ว่า backend, frontend, browser flow และ Docker artifact ยังทำงานร่วมกัน

## ทำงานอย่างไร

### Dashboard read model

เส้นทางข้อมูลคือ:

```text
GET /api/v1/reports/dashboard
  -> ReportingController ตรวจ role OWNER/MANAGER
  -> ReportingService แปลงวันที่ด้วย business timezone
  -> SQL อ่าน sales + sale_items + payments + inventory
  -> DashboardReport
  -> React แสดง summary cards และตาราง
```

งานรายงานต้องรวมข้อมูลหลายโมดูลและไม่มีการเปลี่ยน state จึงใช้ `NamedParameterJdbcTemplate` เป็น read model โดยตรง ส่วน use case ที่เขียนข้อมูลยังคงใช้ domain service และ JPA transaction ตามเดิม วิธีนี้ทำให้ SQL aggregation ชัดและไม่สร้าง object graph จำนวนมากเกินจำเป็น

กำไรขั้นต้นคำนวณจาก:

```text
ยอดขายไม่รวม VAT - ต้นทุนสินค้าที่ขาย = กำไรขั้นต้น
```

ต้นทุนสินค้าที่ขายใช้ `unit_cost_snapshot` ซึ่งถูกล็อกไว้ตอน completed sale ไม่ใช้ต้นทุนปัจจุบัน เพราะต้นทุนเฉลี่ยอาจเปลี่ยนหลังรับสินค้าเที่ยวใหม่ หากใช้ค่าปัจจุบัน รายงานอดีตจะเปลี่ยนตามและตรวจสอบย้อนหลังไม่ได้

`inventoryValue` ใช้ `onHand × averageCost` ของยอดปัจจุบัน จึงเป็น operational estimate ไม่ใช่งบการเงินที่ผ่านการรับรอง

### Demo data แยกจากข้อมูลจริง

`DemoDataInitializer` ทำงานเมื่อ `APP_DEMO_DATA=true` เท่านั้น และบังคับรหัสผ่านแยกอย่างน้อย 12 ตัวอักษร ข้อมูลลูกค้าใช้ชื่อและเบอร์สมมติ ตัว seed ตรวจข้อมูลเดิมก่อนสร้าง จึงรันซ้ำได้โดยไม่เพิ่มชุดเดิมไม่รู้จบ

เหตุผลที่ไม่เปิด public signup คือระบบนี้เป็น single-store demo การเปิดให้คนทั่วไปสร้างบัญชีจะเพิ่ม attack surface และทำให้ข้อมูล demo ควบคุมยากโดยไม่สร้างคุณค่าต่อ Portfolio

### Render และ same-origin artifact

Docker build React ก่อน แล้วคัดลอก `dist` เข้า Spring Boot JAR ดังนั้นหน้าเว็บและ API อยู่ origin เดียวกัน ช่วยลดการตั้งค่า CORS และใช้ session cookie + CSRF ตามรูปแบบ browser application ได้ตรงไปตรงมา

Render ส่ง database URL เป็น `postgresql://user:password@host/database` แต่ pgJDBC ต้องการ `jdbc:postgresql://host/database` และรับ credentials แยก `RenderDatabaseEnvironmentPostProcessor` จึงแปลง URL ก่อน DataSource auto-configuration พร้อมตัด user information ไม่ให้ซ้ำอยู่ใน JDBC URL ขณะที่ Docker Compose ยังส่ง JDBC URL เต็มผ่าน `SPRING_DATASOURCE_URL` และมี priority สูงกว่า

### Quality gates

- `mvn verify` รัน unit/integration tests, Flyway บน PostgreSQL Testcontainers และ JaCoCo check
- JaCoCo บังคับ line coverage อย่างน้อย 80% เฉพาะ `domain` และ `application` ซึ่งเป็นจุดรวม business rules
- Frontend gate รัน ESLint, Vitest และ production build
- Playwright เปิด browser จริงและทดสอบสองเส้นทาง: import → receive → cash sale → receipt → stock count และ PromptPay → payment confirmation → receipt
- Docker build พิสูจน์ว่า React และ Spring Boot รวมเป็น production artifact ได้จริง

Coverage ไม่ได้แปลว่า logic ถูกทั้งหมด มันบอกเพียงว่า test วิ่งผ่านบรรทัดใดบ้าง จึงต้องใช้คู่กับกรณี business rule, integration, concurrency, security และ E2E

## บั๊กที่ E2E ช่วยจับ

ระหว่างทดสอบ flow จริง หน้ารายการตรวจนับเจอ `LazyInitializationException` เพราะ transaction อ่านจบก่อน serializer เข้าถึง count items การแก้คือโหลด items ภายใน read transaction และเพิ่ม regression test ที่เรียก service นอก transaction

บทเรียนสำคัญคือ unit test ของแต่ละชั้นอาจเขียว แต่ source-to-effect จริงตั้งแต่ browser ถึง JSON serialization ยังพังได้ E2E จึงมีหน้าที่ตรวจรอยต่อ ไม่ใช่แทน unit/integration tests ทั้งหมด

อีกปัญหาหนึ่งเกิดที่ test boundary: Vitest เคยค้นไฟล์ `e2e/*.spec.ts` แล้วพยายามรัน Playwright spec ด้วย runner ผิดตัว จึงกำหนด glob ให้ Vitest อ่านเฉพาะ `src/**/*.test.{ts,tsx}` ส่วน Playwright อ่านเฉพาะโฟลเดอร์ `e2e` การแยกนี้ทำให้คำสั่ง local และ CI ให้ผลเหมือนกัน

full backend suite ยังช่วยเจอ assertion ที่ผูกกับ `lowStock[0]` ทั้งที่ API contract ระบุเพียงการ sort ตาม available และอาจมีสินค้าจากกรณีทดสอบอื่นอยู่ก่อนหน้า การแก้ให้ตรวจว่า list มี SKU ที่สร้างไว้ ทำให้ test ยืนยัน business outcome โดยไม่สมมติ global database state เกินจริง

## ความเสี่ยงและสิ่งที่ยังไม่ควรอ้าง

- Dashboard เป็นรายงานเพื่อการปฏิบัติงาน ไม่ใช่ระบบบัญชีหรือรายงานภาษี
- Demo ใช้ Stripe Test Mode ไม่ใช่ production payment
- Render free tier เหมาะกับ Portfolio ไม่ใช่ SLA สำหรับร้านจริง
- ระบบไม่ควรถูกนำข้อมูลลูกค้าจริงขึ้น public demo และยังไม่อ้างว่าผ่านการรับรอง PDPA
- E2E ใช้ fake provider ที่เปิดได้เฉพาะ profile `e2e`; การทดสอบ Stripe จริงยังต้องใช้ Test Mode credentials และ webhook endpoint จริง

## ไฟล์ที่ควรเปิดอ่าน

- `reporting/application/ReportingService.java`: SQL read model และสูตรรายงาน
- `reporting/web/ReportingController.java`: role gate และ API boundary
- `demo/DemoDataInitializer.java`: seed ที่เปิดด้วย environment flag
- `frontend/e2e/retail-flow.spec.ts`: user journey ที่ browser ทำจริง
- `.github/workflows/ci.yml`: gate ก่อน merge
- `render.yaml`: infrastructure declaration ของ public demo

## ลองอธิบายกลับ

1. ทำไมกำไรย้อนหลังต้องใช้ cost snapshot แทน moving-average cost ปัจจุบัน?
2. ทำไม read model ใช้ SQL ตรงได้ แต่ checkout ยังต้องผ่าน domain service และ transaction?
3. E2E จับปัญหาอะไรที่ unit test อาจไม่เห็น และทำไมเราไม่ควรเขียนทุกกรณีเป็น E2E?

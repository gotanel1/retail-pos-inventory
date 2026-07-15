# บทเรียน 01: วาง Foundation ของระบบ

## Foundation คืออะไร

Foundation คือโครงขั้นต่ำที่ทำให้ทีมเริ่มเพิ่ม feature ได้โดยไม่ต้องตัดสินใจเรื่องเครื่องมือและวิธีรันใหม่ทุกครั้ง ประกอบด้วย project structure, dependency management, database test environment, frontend shell, Docker และ CI

## Backend ทำงานอย่างไร

Spring Boot เริ่มจาก `RetailPosInventoryApplication` แล้วค้นหา configuration และ components ใต้ package `com.got.retailpos` Maven เป็นผู้จัดการ dependencies และ lifecycle เช่น `test`, `package` และ `verify`

Integration test ใช้ `TestcontainersConfiguration` สร้าง PostgreSQL 18.4 ชั่วคราว จากนั้น Spring Boot ส่ง connection details ให้ Flyway และ JPA โดยอัตโนมัติ เมื่อ test จบ container ถูกเก็บกวาดโดย Testcontainers

## Frontend ทำงานอย่างไร

Vite เริ่ม React จาก `src/main.tsx` แล้วประกอบ providers ที่ใช้ร่วมทั้งระบบ:

- `ThemeProvider` ควบคุมรูปแบบ Material UI
- `QueryClientProvider` จัดการ server state และ cache
- `BrowserRouter` เตรียม routing สำหรับหน้าจอในอนาคต

Component test ตรวจว่าหน้าจอ foundation แสดงชื่อและโมดูลหลัก ส่วน `npm run build` ตรวจ TypeScript และสร้าง production assets

## Docker multi-stage ทำงานอย่างไร

1. Node stage ติดตั้ง dependencies และ build React
2. Maven stage package Spring Boot และนำ React assets เข้า `static`
3. Runtime stage ใช้เฉพาะ Java JRE และรันด้วย non-root user

จึงไม่ต้องส่ง Node, Maven, source code หรือ test dependencies เข้า production image

## CI ทำหน้าที่อะไร

GitHub Actions แยก backend กับ frontend เป็นคนละ job เพื่อเห็นสาเหตุที่ล้มได้เร็ว เมื่อทั้งสองผ่านจึง build container เป็นด่านสุดท้าย PR ที่ build ไม่ผ่านจะ merge เข้า `main` ไม่ได้หลังเปิด branch protection

## สิ่งที่ได้เรียนรู้จากรอบนี้

- Generator สำเร็จไม่ได้แปลว่า output ถูกต้อง: npm 11 ตีความ template argument ผิดและสร้าง Vanilla TypeScript จึงต้องตรวจ `package.json`
- Testcontainers ล้มครั้งแรกเพราะ Docker daemon ไม่ทำงาน ไม่ใช่เพราะ application code
- TypeScript strict build ตรวจพบ Material UI 9 API change แม้ component test จะผ่าน จึงต้องรันทั้ง test และ production build
- Dependency และ container image ควร pin version เช่น `postgres:18.4` แทน `latest`

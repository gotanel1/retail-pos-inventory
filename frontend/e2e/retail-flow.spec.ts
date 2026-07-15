import { expect, test } from '@playwright/test'

const demoPassword = process.env.E2E_DEMO_PASSWORD ?? 'demo-password-for-e2e'

async function login(page: import('@playwright/test').Page, username: string) {
  await page.goto('/')
  await page.getByLabel('ชื่อผู้ใช้').fill(username)
  await page.getByLabel('รหัสผ่าน').fill(demoPassword)
  await page.getByRole('button', { name: 'เข้าสู่ระบบ' }).click()
  await expect(page.getByText(/ร้านตัวอย่าง/).first()).toBeVisible()
}

test('นำเข้า CSV รับสินค้า ขายเงินสด ออกใบเสร็จ และอนุมัติตรวจนับ', async ({ page }) => {
  const unique = Date.now().toString()
  const sku = `E2E-${unique}`
  const productName = `สินค้า E2E ${unique}`
  const reference = `E2E-GR-${unique}`
  const countReason = `ตรวจนับ E2E ${unique}`
  await login(page, 'demo-manager')

  await page.getByRole('link', { name: 'นำเข้า CSV' }).click()
  await page.getByLabel('เลือกไฟล์ CSV').setInputFiles({
    name: 'e2e-products.csv',
    mimeType: 'text/csv',
    buffer: Buffer.from(`sku,barcode,name,category,salePrice,lowStockThreshold\n${sku},,${productName},E2E Category,100.00,2\n`),
  })
  await page.getByRole('button', { name: 'ตรวจไฟล์' }).click()
  await expect(page.getByText('พร้อมนำเข้า')).toBeVisible()
  await page.getByRole('button', { name: 'ยืนยันนำเข้า 1 รายการ' }).click()
  await expect(page.getByText('สร้างสินค้าแล้ว 1 รายการ')).toBeVisible()

  await page.getByRole('link', { name: 'รับสินค้า' }).click()
  await page.getByRole('combobox', { name: 'Supplier' }).click()
  await page.getByRole('option', { name: 'Demo Supplier' }).click()
  await page.getByLabel('เลขอ้างอิง').fill(reference)
  await page.getByRole('combobox', { name: 'สินค้ารายการ 1' }).click()
  await page.getByRole('option', { name: new RegExp(sku) }).click()
  await page.getByLabel('จำนวนรายการ 1').fill('5')
  await page.getByLabel('ต้นทุนรายการ 1').fill('50.0000')
  await page.getByRole('button', { name: 'ยืนยันรับสินค้า' }).click()
  await expect(page.getByText('บันทึกรับสินค้าและอัปเดต Stock Ledger แล้ว')).toBeVisible()

  await page.getByRole('link', { name: 'ขายหน้าร้าน' }).click()
  await page.getByRole('combobox', { name: 'สินค้า' }).click()
  await page.getByRole('option', { name: new RegExp(sku) }).click()
  await page.getByRole('button', { name: 'เพิ่ม' }).click()
  await page.getByRole('button', { name: 'สร้างบิลและยืนยันราคา' }).click()
  await page.getByLabel('รับเงินสด').fill('100')
  await page.getByRole('button', { name: 'ชำระเงินสด' }).click()
  await expect(page.getByText(/เลขที่: R-/)).toBeVisible()
  await expect(page.getByText('ไม่ใช่ใบกำกับภาษี')).toBeVisible()

  await page.getByRole('button', { name: 'เริ่มบิลใหม่' }).click()
  await page.getByRole('link', { name: 'ตรวจนับ' }).click()
  await page.getByRole('combobox', { name: 'สินค้า' }).click()
  await page.getByRole('option', { name: new RegExp(sku) }).click()
  await page.getByLabel('ยอดนับจริง').fill('3')
  await page.getByLabel('เหตุผล').fill(countReason)
  await page.getByRole('button', { name: 'ส่งยอดนับ' }).click()
  const countRow = page.getByRole('row').filter({ hasText: countReason })
  await countRow.getByRole('button', { name: 'อนุมัติ' }).click()
  await expect(countRow.getByText('APPROVED')).toBeVisible()
})

test('PromptPay รอ provider ยืนยันก่อนออกใบเสร็จ', async ({ page }) => {
  await login(page, 'demo-cashier')
  await page.getByRole('link', { name: 'ขายหน้าร้าน' }).click()
  await page.getByRole('combobox', { name: 'สินค้า' }).click()
  await page.getByRole('option', { name: /DEMO-TAPE/ }).click()
  await page.getByRole('button', { name: 'เพิ่ม' }).click()
  await page.getByRole('button', { name: 'สร้างบิลและยืนยันราคา' }).click()

  const checkoutResponse = page.waitForResponse((response) => response.url().includes('/checkout/promptpay') && response.status() === 200)
  await page.getByRole('button', { name: 'ชำระด้วย PromptPay' }).click()
  const payment = await (await checkoutResponse).json() as { paymentIntentId: string }
  await expect(page.getByRole('img', { name: 'QR PromptPay' })).toBeVisible()
  await expect(page.getByText('กำลังรอ Stripe webhook ยืนยันการชำระเงิน')).toBeVisible()

  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json() as { headerName: string; token: string }
  const providerResult = await page.request.post(`/api/v1/test/payments/${payment.paymentIntentId}/succeed`, {
    headers: { [csrf.headerName]: csrf.token },
  })
  expect(providerResult.status()).toBe(204)
  await expect(page.getByText('ชำระโดย: PromptPay')).toBeVisible()
  await expect(page.getByRole('button', { name: 'พิมพ์ใบเสร็จ' })).toBeVisible()
})

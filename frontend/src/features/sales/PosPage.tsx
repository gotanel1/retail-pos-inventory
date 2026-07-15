import AddShoppingCartOutlinedIcon from '@mui/icons-material/AddShoppingCartOutlined'
import PrintOutlinedIcon from '@mui/icons-material/PrintOutlined'
import {
  Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, FormControl, GlobalStyles,
  InputLabel, MenuItem, Paper, Select, Stack, Table, TableBody, TableCell, TableHead, TableRow,
  TextField, Typography,
} from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../api/http'
import { getProductOptions } from '../catalog/catalogApi'
import { getCustomers } from '../customers/customerApi'
import { applyDiscount, checkoutCash, createSale, getStoreSettings } from './salesApi'
import type { DiscountType, Sale } from './salesApi'

interface CartLine { productId: string; quantity: number }

const money = (value: number | null) => {
  const amount = value ?? 0
  const formatted = Math.abs(amount).toLocaleString('th-TH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return amount < 0 ? `-฿${formatted}` : `฿${formatted}`
}

export function PosPage() {
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [customerId, setCustomerId] = useState('')
  const [cart, setCart] = useState<CartLine[]>([])
  const [sale, setSale] = useState<Sale | null>(null)
  const [cashReceived, setCashReceived] = useState('')
  const [discountOpen, setDiscountOpen] = useState(false)
  const products = useQuery({ queryKey: ['product-options'], queryFn: getProductOptions })
  const customers = useQuery({ queryKey: ['customers', 'pos'], queryFn: () => getCustomers() })
  const settings = useQuery({ queryKey: ['store-settings'], queryFn: getStoreSettings })
  const create = useMutation({ mutationFn: createSale, onSuccess: setSale })
  const discount = useMutation({
    mutationFn: ({ saleId, input }: { saleId: string; input: Parameters<typeof applyDiscount>[1] }) => applyDiscount(saleId, input),
    onSuccess: (result) => { setSale(result); setDiscountOpen(false) },
  })
  const checkout = useMutation({
    mutationFn: ({ saleId, cash }: { saleId: string; cash: string }) => checkoutCash(saleId, cash, crypto.randomUUID()),
    onSuccess: setSale,
  })

  function addItem() {
    if (!productId || Number(quantity) < 1) return
    setCart((current) => {
      const existing = current.find((line) => line.productId === productId)
      return existing
        ? current.map((line) => line.productId === productId ? { ...line, quantity: line.quantity + Number(quantity) } : line)
        : [...current, { productId, quantity: Number(quantity) }]
    })
    setProductId(''); setQuantity('1')
  }

  function reset() {
    setSale(null); setCart([]); setCashReceived(''); setCustomerId('')
  }

  const productMap = new Map((products.data?.content ?? []).map((product) => [product.id, product]))
  const error = [create.error, discount.error, checkout.error].find(Boolean)
  const errorMessage = error instanceof ApiError ? error.message : error ? 'ดำเนินรายการไม่สำเร็จ กรุณาลองใหม่' : null

  if (sale?.status === 'COMPLETED') {
    return <Receipt sale={sale} settings={settings.data} onNewSale={reset} />
  }

  return <Stack spacing={3}>
    <Box><Typography component="h1" variant="h4">ขายหน้าร้าน</Typography><Typography color="text.secondary">ยอดในบิลและ VAT ยืนยันจาก server ก่อนตัดสต็อกทุกครั้ง</Typography></Box>
    {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
    <Stack direction={{ xs: 'column', lg: 'row' }} spacing={3} sx={{ alignItems: 'flex-start' }}>
      <Paper variant="outlined" sx={{ borderRadius: 3, flex: 1, p: 3, width: '100%' }}>
        <Stack spacing={2}>
          <FormControl fullWidth disabled={Boolean(sale)}><InputLabel id="customer-label">ลูกค้า (ไม่บังคับ)</InputLabel><Select label="ลูกค้า (ไม่บังคับ)" labelId="customer-label" value={customerId} onChange={(event) => setCustomerId(event.target.value)}><MenuItem value="">ลูกค้าทั่วไป</MenuItem>{(customers.data?.content ?? []).map((customer) => <MenuItem key={customer.id} value={customer.id}>{customer.name}{customer.phone ? ` · ${customer.phone}` : ''}</MenuItem>)}</Select></FormControl>
          {!sale && <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <FormControl fullWidth><InputLabel id="product-label">สินค้า</InputLabel><Select label="สินค้า" labelId="product-label" value={productId} onChange={(event) => setProductId(event.target.value)}>{(products.data?.content ?? []).map((product) => <MenuItem key={product.id} value={product.id}>{product.sku} · {product.name} · {money(product.salePrice)}</MenuItem>)}</Select></FormControl>
            <TextField label="จำนวน" type="number" value={quantity} onChange={(event) => setQuantity(event.target.value)} slotProps={{ htmlInput: { min: 1, step: 1 } }} sx={{ width: { sm: 130 } }} />
            <Button disabled={!productId} onClick={addItem} startIcon={<AddShoppingCartOutlinedIcon />} variant="outlined">เพิ่ม</Button>
          </Stack>}
          <Table size="small"><TableHead><TableRow><TableCell>สินค้า</TableCell><TableCell align="right">จำนวน</TableCell><TableCell align="right">ราคา/หน่วย</TableCell><TableCell align="right">รวม</TableCell></TableRow></TableHead><TableBody>
            {(sale?.items ?? cart.map((line) => ({ ...line, sku: productMap.get(line.productId)?.sku ?? '', name: productMap.get(line.productId)?.name ?? '', unitPrice: productMap.get(line.productId)?.salePrice ?? 0, lineTotal: (productMap.get(line.productId)?.salePrice ?? 0) * line.quantity, unitCostSnapshot: null }))).map((line) => <TableRow key={line.productId}><TableCell>{line.sku} · {line.name}</TableCell><TableCell align="right">{line.quantity}</TableCell><TableCell align="right">{money(line.unitPrice)}</TableCell><TableCell align="right">{money(line.lineTotal)}</TableCell></TableRow>)}
          </TableBody></Table>
          {!sale && <Button disabled={!cart.length || create.isPending} onClick={() => create.mutate({ customerId: customerId || null, items: cart })} variant="contained">สร้างบิลและยืนยันราคา</Button>}
        </Stack>
      </Paper>
      <Paper variant="outlined" sx={{ borderRadius: 3, p: 3, width: { xs: '100%', lg: 360 } }}><Stack spacing={2}>
        <Typography variant="h6">สรุปยอด</Typography>
        <AmountRow label="ยอดสินค้า" value={sale?.subtotal ?? cart.reduce((sum, line) => sum + (productMap.get(line.productId)?.salePrice ?? 0) * line.quantity, 0)} />
        {sale && <><AmountRow label="ส่วนลด" value={-sale.discountAmount} /><AmountRow label={`VAT ${sale.vatEnabled ? `${sale.vatRate}% (รวมในราคา)` : 'ปิด'}`} value={sale.vatAmount} /><Divider /><AmountRow strong label="ยอดสุทธิ" value={sale.total} /></>}
        {sale && <><Button onClick={() => setDiscountOpen(true)} variant="outlined">ขออนุมัติส่วนลด</Button><TextField label="รับเงินสด" type="number" value={cashReceived} onChange={(event) => setCashReceived(event.target.value)} slotProps={{ htmlInput: { min: sale.total, step: '0.01' } }} /><Button disabled={!cashReceived || checkout.isPending} onClick={() => checkout.mutate({ saleId: sale.id, cash: cashReceived })} variant="contained">ชำระเงินสด</Button></>}
      </Stack></Paper>
    </Stack>
    {sale && <DiscountDialog error={discount.error} loading={discount.isPending} onClose={() => setDiscountOpen(false)} onSubmit={(input) => discount.mutate({ saleId: sale.id, input })} open={discountOpen} />}
  </Stack>
}

function AmountRow({ label, strong = false, value }: { label: string; strong?: boolean; value: number }) { return <Stack direction="row" sx={{ justifyContent: 'space-between' }}><Typography sx={{ fontWeight: strong ? 700 : 400 }}>{label}</Typography><Typography sx={{ fontWeight: strong ? 700 : 400 }}>{money(value)}</Typography></Stack> }

function DiscountDialog({ error, loading, onClose, onSubmit, open }: { error: Error | null; loading: boolean; onClose: () => void; onSubmit: (input: Parameters<typeof applyDiscount>[1]) => void; open: boolean }) {
  const [type, setType] = useState<DiscountType>('AMOUNT')
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const data = new FormData(event.currentTarget); onSubmit({ type, value: String(data.get('value')), managerUsername: String(data.get('managerUsername')), managerPin: String(data.get('managerPin')) }) }
  return <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}><Box component="form" onSubmit={submit}><DialogTitle>ผู้จัดการอนุมัติส่วนลด</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>{error && <Alert severity="error">อนุมัติไม่สำเร็จ กรุณาตรวจ username/PIN</Alert>}<FormControl><InputLabel id="discount-type">รูปแบบส่วนลด</InputLabel><Select label="รูปแบบส่วนลด" labelId="discount-type" value={type} onChange={(event) => setType(event.target.value as DiscountType)}><MenuItem value="AMOUNT">จำนวนเงิน (บาท)</MenuItem><MenuItem value="PERCENT">เปอร์เซ็นต์</MenuItem></Select></FormControl><TextField label="ค่าส่วนลด" name="value" required type="number" slotProps={{ htmlInput: { min: 0, step: '0.01' } }} /><TextField label="ชื่อผู้ใช้ผู้จัดการ" name="managerUsername" required /><TextField label="Manager PIN" name="managerPin" required type="password" slotProps={{ htmlInput: { inputMode: 'numeric', pattern: '[0-9]{4,6}' } }} /></Stack></DialogContent><DialogActions><Button onClick={onClose}>ยกเลิก</Button><Button disabled={loading} type="submit" variant="contained">อนุมัติ</Button></DialogActions></Box></Dialog>
}

function Receipt({ onNewSale, sale, settings }: { onNewSale: () => void; sale: Sale; settings?: { storeName: string; receiptFooter: string | null } }) {
  return <Stack spacing={3} sx={{ alignItems: 'center' }}>
    <GlobalStyles styles={{ '@media print': { 'body *': { visibility: 'hidden' }, '.print-receipt, .print-receipt *': { visibility: 'visible' }, '.no-print': { display: 'none !important' } } }} />
    <Paper className="print-receipt" elevation={0} sx={{ '@media print': { border: 0, left: 0, position: 'absolute', top: 0, width: '80mm' }, border: 1, borderColor: 'divider', maxWidth: 560, p: 4, width: '100%' }}>
      <Stack spacing={1}><Typography align="center" variant="h5">{settings?.storeName ?? 'Retail POS'}</Typography><Typography align="center">ใบเสร็จรับเงิน</Typography><Typography align="center" color="text.secondary" variant="caption">ไม่ใช่ใบกำกับภาษี</Typography><Divider /><Typography>เลขที่: {sale.receiptNumber}</Typography><Typography>วันที่: {sale.completedAt ? new Date(sale.completedAt).toLocaleString('th-TH') : '—'}</Typography>{sale.items.map((item) => <Stack direction="row" key={item.productId} sx={{ justifyContent: 'space-between' }}><Typography>{item.name} × {item.quantity}</Typography><Typography>{money(item.lineTotal)}</Typography></Stack>)}<Divider /><AmountRow label="ยอดสินค้า" value={sale.subtotal} /><AmountRow label="ส่วนลด" value={-sale.discountAmount} />{sale.vatEnabled && <AmountRow label={`VAT ${sale.vatRate}% (รวมในราคา)`} value={sale.vatAmount} />}<AmountRow strong label="ยอดสุทธิ" value={sale.total} /><AmountRow label="รับเงิน" value={sale.cashReceived ?? 0} /><AmountRow label="เงินทอน" value={sale.changeAmount ?? 0} />{settings?.receiptFooter && <Typography align="center" sx={{ pt: 2 }}>{settings.receiptFooter}</Typography>}</Stack>
    </Paper>
    <Stack className="no-print" direction="row" spacing={2}><Button onClick={() => window.print()} startIcon={<PrintOutlinedIcon />} variant="contained">พิมพ์ใบเสร็จ</Button><Button onClick={onNewSale} variant="outlined">เริ่มบิลใหม่</Button></Stack>
  </Stack>
}

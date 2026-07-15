import {
  Alert, Box, Card, CardContent, Chip, Paper, Skeleton, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { getDashboard } from './reportingApi'

const money = (value: number) => value.toLocaleString('th-TH', { style: 'currency', currency: 'THB' })
const movementLabels = { OPENING: 'ยอดตั้งต้น', RECEIVE: 'รับเข้า', SALE: 'ขาย', ADJUSTMENT_IN: 'ปรับเพิ่ม', ADJUSTMENT_OUT: 'ปรับลด' }

function localDate() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

export function ReportingPage() {
  const today = localDate()
  const [from, setFrom] = useState(today)
  const [to, setTo] = useState(today)
  const report = useQuery({ queryKey: ['dashboard-report', from, to], queryFn: () => getDashboard(from, to), enabled: Boolean(from && to && from <= to) })

  return <Stack spacing={3}>
    <Box><Typography component="h1" variant="h4">ภาพรวมร้าน</Typography><Typography color="text.secondary">กำไรใช้ต้นทุน snapshot ตอนขาย และยอดขายไม่รวม VAT</Typography></Box>
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
      <TextField label="ตั้งแต่วันที่" onChange={(event) => setFrom(event.target.value)} type="date" value={from} slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: to } }} />
      <TextField label="ถึงวันที่" onChange={(event) => setTo(event.target.value)} type="date" value={to} slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: from } }} />
    </Stack>
    {from > to && <Alert severity="warning">วันที่สิ้นสุดต้องไม่น้อยกว่าวันที่เริ่มต้น</Alert>}
    {report.isError && <Alert severity="error">โหลดรายงานไม่สำเร็จ กรุณาลองใหม่</Alert>}
    {report.isPending ? <Stack direction="row" spacing={2}>{[1, 2, 3].map((item) => <Skeleton height={120} key={item} variant="rounded" width="100%" />)}</Stack> : report.data && <>
      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(3, 1fr)' } }}>
        <Metric label="ยอดขายรวม" value={money(report.data.summary.totalSales)} helper={`${report.data.summary.salesCount.toLocaleString('th-TH')} บิล`} />
        <Metric label="กำไรขั้นต้น" value={money(report.data.summary.grossProfit)} helper={`ต้นทุนขาย ${money(report.data.summary.costOfGoodsSold)}`} />
        <Metric label="มูลค่าสต็อก" value={money(report.data.summary.inventoryValue)} helper="on hand × ต้นทุนเฉลี่ย" />
      </Box>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={3} sx={{ alignItems: 'flex-start' }}>
        <ReportTable title="วิธีชำระเงิน" headers={['วิธี', 'รายการ', 'ยอดรวม']} rows={report.data.payments.map((payment) => [payment.method === 'CASH' ? 'เงินสด' : 'PromptPay', payment.transactionCount.toLocaleString('th-TH'), money(payment.amount)])} empty="ยังไม่มีการชำระเงินในช่วงนี้" />
        <ReportTable title="สินค้าใกล้หมด" headers={['สินค้า', 'คงเหลือ', 'จอง', 'ขายได้']} rows={report.data.lowStock.map((item) => [`${item.sku} · ${item.name}`, item.onHand, item.reserved, item.available])} empty="ไม่มีสินค้าใกล้หมด" />
      </Stack>
      <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
        <Box sx={{ p: 2.5 }}><Typography variant="h6">ความเคลื่อนไหวสต็อกล่าสุด</Typography></Box>
        <TableContainer><Table size="small"><TableHead><TableRow><TableCell>เวลา</TableCell><TableCell>สินค้า</TableCell><TableCell>ประเภท</TableCell><TableCell align="right">เปลี่ยนแปลง</TableCell><TableCell align="right">คงเหลือ</TableCell></TableRow></TableHead><TableBody>
          {report.data.movements.map((movement) => <TableRow key={movement.id}><TableCell>{new Date(movement.occurredAt).toLocaleString('th-TH')}</TableCell><TableCell>{movement.sku} · {movement.productName}</TableCell><TableCell><Chip label={movementLabels[movement.movementType]} size="small" /></TableCell><TableCell align="right" sx={{ color: movement.quantityDelta < 0 ? 'error.main' : 'success.main' }}>{movement.quantityDelta > 0 ? '+' : ''}{movement.quantityDelta}</TableCell><TableCell align="right">{movement.onHandAfter}</TableCell></TableRow>)}
          {!report.data.movements.length && <TableRow><TableCell align="center" colSpan={5}>ยังไม่มี movement ในช่วงนี้</TableCell></TableRow>}
        </TableBody></Table></TableContainer>
      </Paper>
    </>}
  </Stack>
}

function Metric({ helper, label, value }: { helper: string; label: string; value: string }) {
  return <Card variant="outlined"><CardContent><Typography color="text.secondary">{label}</Typography><Typography sx={{ fontWeight: 800, my: 0.5 }} variant="h5">{value}</Typography><Typography color="text.secondary" variant="caption">{helper}</Typography></CardContent></Card>
}

function ReportTable({ empty, headers, rows, title }: { empty: string; headers: string[]; rows: (string | number)[][]; title: string }) {
  return <Paper variant="outlined" sx={{ borderRadius: 3, flex: 1, overflow: 'hidden', width: '100%' }}><Box sx={{ p: 2.5 }}><Typography variant="h6">{title}</Typography></Box><TableContainer><Table size="small"><TableHead><TableRow>{headers.map((header) => <TableCell key={header}>{header}</TableCell>)}</TableRow></TableHead><TableBody>{rows.map((row, index) => <TableRow key={`${row[0]}-${index}`}>{row.map((value, cell) => <TableCell key={`${value}-${cell}`}>{value}</TableCell>)}</TableRow>)}{!rows.length && <TableRow><TableCell align="center" colSpan={headers.length}>{empty}</TableCell></TableRow>}</TableBody></Table></TableContainer></Paper>
}

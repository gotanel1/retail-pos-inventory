import AddOutlinedIcon from '@mui/icons-material/AddOutlined'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined'
import {
  Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, IconButton,
  InputLabel, MenuItem, Paper, Select, Stack, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, TextField, Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../api/http'
import { getProductOptions } from '../catalog/catalogApi'
import { createGoodsReceipt, createSupplier, getGoodsReceipts, getSuppliers } from './inventoryApi'

interface ReceiptRow { productId: string; quantity: string; unitCost: string }
const emptyRow = (): ReceiptRow => ({ productId: '', quantity: '1', unitCost: '0.0000' })

export function GoodsReceiptPage() {
  const queryClient = useQueryClient()
  const [supplierId, setSupplierId] = useState('')
  const [referenceNumber, setReferenceNumber] = useState('')
  const [note, setNote] = useState('')
  const [rows, setRows] = useState<ReceiptRow[]>([emptyRow()])
  const [supplierDialogOpen, setSupplierDialogOpen] = useState(false)
  const suppliersQuery = useQuery({ queryKey: ['suppliers'], queryFn: getSuppliers })
  const productsQuery = useQuery({ queryKey: ['product-options'], queryFn: getProductOptions })
  const receiptsQuery = useQuery({ queryKey: ['goods-receipts'], queryFn: getGoodsReceipts })
  const receiptMutation = useMutation({
    mutationFn: createGoodsReceipt,
    onSuccess: async () => {
      setReferenceNumber(''); setNote(''); setRows([emptyRow()])
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['goods-receipts'] }),
        queryClient.invalidateQueries({ queryKey: ['inventory-balances'] }),
        queryClient.invalidateQueries({ queryKey: ['stock-movements'] }),
      ])
    },
  })
  const supplierMutation = useMutation({
    mutationFn: createSupplier,
    onSuccess: async (supplier) => {
      setSupplierId(supplier.id); setSupplierDialogOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['suppliers'] })
    },
  })

  function updateRow(index: number, field: keyof ReceiptRow, value: string) {
    setRows((current) => current.map((row, rowIndex) => rowIndex === index ? { ...row, [field]: value } : row))
  }

  function submitReceipt(event: FormEvent) {
    event.preventDefault()
    receiptMutation.mutate({
      supplierId, referenceNumber, note, receivedAt: new Date().toISOString(),
      items: rows.map((row) => ({ productId: row.productId, quantity: Number(row.quantity), unitCost: row.unitCost })),
    })
  }

  const error = receiptMutation.error instanceof ApiError ? receiptMutation.error.message : receiptMutation.error ? 'บันทึก Goods Receipt ไม่สำเร็จ' : null

  return (
    <Stack spacing={4}>
      <Box><Typography component="h1" variant="h4">รับสินค้าเข้า</Typography><Typography color="text.secondary">เมื่อยืนยันแล้ว ระบบจะเพิ่ม Stock Ledger และคำนวณต้นทุนเฉลี่ยภายใน transaction เดียว</Typography></Box>

      <Paper component="form" onSubmit={submitReceipt} variant="outlined" sx={{ borderRadius: 3, p: 3 }}>
        <Stack spacing={3}>
          {error && <Alert severity="error">{error}</Alert>}
          {receiptMutation.isSuccess && <Alert severity="success">บันทึกรับสินค้าและอัปเดต Stock Ledger แล้ว</Alert>}
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
            <FormControl required fullWidth>
              <InputLabel id="supplier-label">Supplier</InputLabel>
              <Select label="Supplier" labelId="supplier-label" onChange={(event) => setSupplierId(event.target.value)} value={supplierId}>
                {(suppliersQuery.data ?? []).map((supplier) => <MenuItem key={supplier.id} value={supplier.id}>{supplier.name}</MenuItem>)}
              </Select>
            </FormControl>
            <Button onClick={() => setSupplierDialogOpen(true)} startIcon={<AddOutlinedIcon />} variant="outlined" sx={{ minWidth: 170 }}>เพิ่ม Supplier</Button>
            <TextField fullWidth label="เลขอ้างอิง" onChange={(event) => setReferenceNumber(event.target.value)} value={referenceNumber} />
          </Stack>
          <TextField fullWidth label="หมายเหตุ" onChange={(event) => setNote(event.target.value)} value={note} />

          <TableContainer>
            <Table size="small">
              <TableHead><TableRow><TableCell>สินค้า</TableCell><TableCell width={150}>จำนวน</TableCell><TableCell width={180}>ต้นทุน/หน่วย</TableCell><TableCell width={60} /></TableRow></TableHead>
              <TableBody>
                {rows.map((row, index) => (
                  <TableRow key={index}>
                    <TableCell><FormControl required fullWidth size="small"><Select aria-label={`สินค้ารายการ ${index + 1}`} displayEmpty onChange={(event) => updateRow(index, 'productId', event.target.value)} value={row.productId}><MenuItem disabled value="">เลือกสินค้า</MenuItem>{(productsQuery.data?.content ?? []).map((product) => <MenuItem key={product.id} value={product.id}>{product.sku} · {product.name}</MenuItem>)}</Select></FormControl></TableCell>
                    <TableCell><TextField required size="small" type="number" value={row.quantity} onChange={(event) => updateRow(index, 'quantity', event.target.value)} slotProps={{ htmlInput: { min: 1, step: 1, 'aria-label': `จำนวนรายการ ${index + 1}` } }} /></TableCell>
                    <TableCell><TextField required size="small" type="number" value={row.unitCost} onChange={(event) => updateRow(index, 'unitCost', event.target.value)} slotProps={{ htmlInput: { min: 0, step: '0.0001', 'aria-label': `ต้นทุนรายการ ${index + 1}` } }} /></TableCell>
                    <TableCell><IconButton aria-label={`ลบรายการ ${index + 1}`} disabled={rows.length === 1} onClick={() => setRows((current) => current.filter((_, rowIndex) => rowIndex !== index))}><DeleteOutlineIcon /></IconButton></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <Stack direction="row" spacing={2} sx={{ justifyContent: 'space-between' }}>
            <Button onClick={() => setRows((current) => [...current, emptyRow()])} startIcon={<AddOutlinedIcon />}>เพิ่มรายการ</Button>
            <Button disabled={receiptMutation.isPending || !supplierId} type="submit" variant="contained">ยืนยันรับสินค้า</Button>
          </Stack>
        </Stack>
      </Paper>

      <Box><Typography variant="h5" sx={{ mb: 2 }}>รายการรับเข้าล่าสุด</Typography>
        <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 3 }}><Table size="small"><TableHead><TableRow><TableCell>เวลารับ</TableCell><TableCell>เลขอ้างอิง</TableCell><TableCell>Supplier</TableCell><TableCell>สถานะ</TableCell></TableRow></TableHead><TableBody>
          {(receiptsQuery.data?.content ?? []).map((receipt) => <TableRow key={receipt.id}><TableCell>{new Date(receipt.receivedAt).toLocaleString('th-TH')}</TableCell><TableCell>{receipt.referenceNumber || '—'}</TableCell><TableCell>{receipt.supplierName}</TableCell><TableCell>{receipt.status}</TableCell></TableRow>)}
        </TableBody></Table></TableContainer>
      </Box>

      <SupplierDialog error={supplierMutation.error} isPending={supplierMutation.isPending} onClose={() => setSupplierDialogOpen(false)} onSubmit={(input) => supplierMutation.mutate(input)} open={supplierDialogOpen} />
    </Stack>
  )
}

function SupplierDialog({ error, isPending, onClose, onSubmit, open }: { error: Error | null; isPending: boolean; onClose: () => void; onSubmit: (input: { name: string; phone: string; note: string }) => void; open: boolean }) {
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const data = new FormData(event.currentTarget); onSubmit({ name: String(data.get('name')), phone: String(data.get('phone')), note: String(data.get('note')) }) }
  return <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}><Box component="form" onSubmit={submit}><DialogTitle>เพิ่ม Supplier</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>{error && <Alert severity="error">เพิ่ม Supplier ไม่สำเร็จ</Alert>}<TextField label="ชื่อ Supplier" name="name" required /><TextField label="เบอร์โทร" name="phone" /><TextField label="หมายเหตุ" name="note" /></Stack></DialogContent><DialogActions><Button onClick={onClose}>ยกเลิก</Button><Button disabled={isPending} type="submit" variant="contained">บันทึก</Button></DialogActions></Box></Dialog>
}

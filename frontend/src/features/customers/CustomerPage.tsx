import AddOutlinedIcon from '@mui/icons-material/AddOutlined'
import { Alert, Box, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Role } from '../auth/authApi'
import { anonymizeCustomer, createCustomer, getCustomers, getCustomerSales } from './customerApi'
import type { Customer } from './customerApi'

export function CustomerPage({ role }: { role: Role }) {
  const queryClient = useQueryClient(); const [search, setSearch] = useState(''); const [open, setOpen] = useState(false); const [historyCustomer, setHistoryCustomer] = useState<Customer | null>(null)
  const query = useQuery({ queryKey: ['customers', search], queryFn: () => getCustomers(search) })
  const create = useMutation({ mutationFn: createCustomer, onSuccess: async () => { setOpen(false); await queryClient.invalidateQueries({ queryKey: ['customers'] }) } })
  const anonymize = useMutation({ mutationFn: anonymizeCustomer, onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['customers'] }) })
  return <Stack spacing={3}>
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between' }}><Box><Typography component="h1" variant="h4">ลูกค้า</Typography><Typography color="text.secondary">ข้อมูลติดต่อ ความยินยอม และประวัติซื้อจะเชื่อมกับบิลในขั้น POS</Typography></Box><Button onClick={() => setOpen(true)} startIcon={<AddOutlinedIcon />} variant="contained">เพิ่มลูกค้า</Button></Stack>
    <TextField label="ค้นหาชื่อหรือเบอร์โทร" onChange={(event) => setSearch(event.target.value)} value={search} />
    {query.isError && <Alert severity="error">โหลดข้อมูลลูกค้าไม่สำเร็จ</Alert>}
    <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 3 }}><Table><TableHead><TableRow><TableCell>ชื่อ</TableCell><TableCell>เบอร์โทร</TableCell><TableCell>หมายเหตุ</TableCell><TableCell>การตลาด</TableCell><TableCell /></TableRow></TableHead><TableBody>
      {(query.data?.content ?? []).map((customer) => <TableRow key={customer.id}><TableCell>{customer.name}</TableCell><TableCell>{customer.phone || '—'}</TableCell><TableCell>{customer.note || '—'}</TableCell><TableCell>{customer.marketingConsent ? 'ยินยอม' : 'ไม่ยินยอม'}</TableCell><TableCell><Button onClick={() => setHistoryCustomer(customer)}>ประวัติซื้อ</Button>{role !== 'CASHIER' && <Button color="error" disabled={anonymize.isPending} onClick={() => anonymize.mutate(customer.id)}>Anonymize</Button>}</TableCell></TableRow>)}
    </TableBody></Table></TableContainer>
    <CustomerDialog error={create.isError} onClose={() => setOpen(false)} onSubmit={(input) => create.mutate(input)} open={open} />
    <PurchaseHistoryDialog customer={historyCustomer} onClose={() => setHistoryCustomer(null)} />
  </Stack>
}

function PurchaseHistoryDialog({ customer, onClose }: { customer: Customer | null; onClose: () => void }) {
  const query = useQuery({ queryKey: ['customer-sales', customer?.id], queryFn: () => getCustomerSales(customer!.id), enabled: Boolean(customer) })
  return <Dialog fullWidth maxWidth="md" onClose={onClose} open={Boolean(customer)}><DialogTitle>ประวัติซื้อ · {customer?.name}</DialogTitle><DialogContent>{query.isError && <Alert severity="error">โหลดประวัติซื้อไม่สำเร็จ</Alert>}<Table size="small"><TableHead><TableRow><TableCell>เลขใบเสร็จ</TableCell><TableCell>วันที่</TableCell><TableCell align="right">ยอดสุทธิ</TableCell></TableRow></TableHead><TableBody>{(query.data?.content ?? []).map((sale) => <TableRow key={sale.id}><TableCell>{sale.receiptNumber}</TableCell><TableCell>{sale.completedAt ? new Date(sale.completedAt).toLocaleString('th-TH') : '—'}</TableCell><TableCell align="right">฿{sale.total.toFixed(2)}</TableCell></TableRow>)}</TableBody></Table></DialogContent><DialogActions><Button onClick={onClose}>ปิด</Button></DialogActions></Dialog>
}

function CustomerDialog({ error, onClose, onSubmit, open }: { error: boolean; onClose: () => void; onSubmit: (input: Parameters<typeof createCustomer>[0]) => void; open: boolean }) {
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const data = new FormData(event.currentTarget); onSubmit({ name: String(data.get('name')), phone: String(data.get('phone')), note: String(data.get('note')), marketingConsent: data.get('marketingConsent') === 'on' }) }
  return <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}><Box component="form" onSubmit={submit}><DialogTitle>เพิ่มลูกค้า</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>{error && <Alert severity="error">บันทึกลูกค้าไม่สำเร็จ</Alert>}<TextField label="ชื่อ" name="name" required /><TextField label="เบอร์โทร" name="phone" /><TextField label="หมายเหตุ" name="note" /><FormControlLabel control={<Checkbox name="marketingConsent" />} label="ลูกค้ายินยอมรับข้อมูลการตลาด" /></Stack></DialogContent><DialogActions><Button onClick={onClose}>ยกเลิก</Button><Button type="submit" variant="contained">บันทึก</Button></DialogActions></Box></Dialog>
}

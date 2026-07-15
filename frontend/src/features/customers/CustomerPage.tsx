import AddOutlinedIcon from '@mui/icons-material/AddOutlined'
import { Alert, Box, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Role } from '../auth/authApi'
import { anonymizeCustomer, createCustomer, getCustomers } from './customerApi'

export function CustomerPage({ role }: { role: Role }) {
  const queryClient = useQueryClient(); const [search, setSearch] = useState(''); const [open, setOpen] = useState(false)
  const query = useQuery({ queryKey: ['customers', search], queryFn: () => getCustomers(search) })
  const create = useMutation({ mutationFn: createCustomer, onSuccess: async () => { setOpen(false); await queryClient.invalidateQueries({ queryKey: ['customers'] }) } })
  const anonymize = useMutation({ mutationFn: anonymizeCustomer, onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['customers'] }) })
  return <Stack spacing={3}>
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between' }}><Box><Typography component="h1" variant="h4">ลูกค้า</Typography><Typography color="text.secondary">ข้อมูลติดต่อ ความยินยอม และประวัติซื้อจะเชื่อมกับบิลในขั้น POS</Typography></Box><Button onClick={() => setOpen(true)} startIcon={<AddOutlinedIcon />} variant="contained">เพิ่มลูกค้า</Button></Stack>
    <TextField label="ค้นหาชื่อหรือเบอร์โทร" onChange={(event) => setSearch(event.target.value)} value={search} />
    {query.isError && <Alert severity="error">โหลดข้อมูลลูกค้าไม่สำเร็จ</Alert>}
    <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 3 }}><Table><TableHead><TableRow><TableCell>ชื่อ</TableCell><TableCell>เบอร์โทร</TableCell><TableCell>หมายเหตุ</TableCell><TableCell>การตลาด</TableCell><TableCell /></TableRow></TableHead><TableBody>
      {(query.data?.content ?? []).map((customer) => <TableRow key={customer.id}><TableCell>{customer.name}</TableCell><TableCell>{customer.phone || '—'}</TableCell><TableCell>{customer.note || '—'}</TableCell><TableCell>{customer.marketingConsent ? 'ยินยอม' : 'ไม่ยินยอม'}</TableCell><TableCell>{role !== 'CASHIER' && <Button color="error" disabled={anonymize.isPending} onClick={() => anonymize.mutate(customer.id)}>Anonymize</Button>}</TableCell></TableRow>)}
    </TableBody></Table></TableContainer>
    <CustomerDialog error={create.isError} onClose={() => setOpen(false)} onSubmit={(input) => create.mutate(input)} open={open} />
  </Stack>
}

function CustomerDialog({ error, onClose, onSubmit, open }: { error: boolean; onClose: () => void; onSubmit: (input: Parameters<typeof createCustomer>[0]) => void; open: boolean }) {
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const data = new FormData(event.currentTarget); onSubmit({ name: String(data.get('name')), phone: String(data.get('phone')), note: String(data.get('note')), marketingConsent: data.get('marketingConsent') === 'on' }) }
  return <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}><Box component="form" onSubmit={submit}><DialogTitle>เพิ่มลูกค้า</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>{error && <Alert severity="error">บันทึกลูกค้าไม่สำเร็จ</Alert>}<TextField label="ชื่อ" name="name" required /><TextField label="เบอร์โทร" name="phone" /><TextField label="หมายเหตุ" name="note" /><FormControlLabel control={<Checkbox name="marketingConsent" />} label="ลูกค้ายินยอมรับข้อมูลการตลาด" /></Stack></DialogContent><DialogActions><Button onClick={onClose}>ยกเลิก</Button><Button type="submit" variant="contained">บันทึก</Button></DialogActions></Box></Dialog>
}

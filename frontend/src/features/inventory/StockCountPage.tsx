import { Alert, Box, Button, FormControl, InputLabel, MenuItem, Paper, Select, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Role } from '../auth/authApi'
import { getInventoryBalances } from './inventoryApi'
import { approveInventoryCount, getInventoryCounts, rejectInventoryCount, submitInventoryCount } from './stockCountApi'

export function StockCountPage({ role }: { role: Role }) {
  const queryClient = useQueryClient(); const [productId, setProductId] = useState(''); const [counted, setCounted] = useState('0'); const [reason, setReason] = useState('')
  const balances = useQuery({ queryKey: ['inventory-balances', '', false], queryFn: () => getInventoryBalances() })
  const counts = useQuery({ queryKey: ['inventory-counts'], queryFn: getInventoryCounts })
  const refresh = async () => { await Promise.all([queryClient.invalidateQueries({ queryKey: ['inventory-counts'] }), queryClient.invalidateQueries({ queryKey: ['inventory-balances'] }), queryClient.invalidateQueries({ queryKey: ['stock-movements'] })]) }
  const submit = useMutation({ mutationFn: submitInventoryCount, onSuccess: refresh }); const approve = useMutation({ mutationFn: approveInventoryCount, onSuccess: refresh }); const reject = useMutation({ mutationFn: rejectInventoryCount, onSuccess: refresh })
  const canApprove = role === 'OWNER' || role === 'MANAGER'
  function handleSubmit(event: FormEvent) { event.preventDefault(); submit.mutate({ countedAt: new Date().toISOString(), reason, items: [{ productId, countedQuantity: Number(counted) }] }) }
  return <Stack spacing={4}><Box><Typography component="h1" variant="h4">ตรวจนับสต็อก</Typography><Typography color="text.secondary">ผู้บันทึกส่งยอดจริง จากนั้น Manager ตรวจและอนุมัติ Adjustment</Typography></Box>
    <Paper component="form" onSubmit={handleSubmit} variant="outlined" sx={{ p: 3, borderRadius: 3 }}><Stack direction={{ xs: 'column', md: 'row' }} spacing={2}><FormControl required fullWidth><InputLabel id="count-product-label">สินค้า</InputLabel><Select label="สินค้า" labelId="count-product-label" value={productId} onChange={(event) => setProductId(event.target.value)}>{(balances.data?.content ?? []).map((item) => <MenuItem key={item.productId} value={item.productId}>{item.sku} · {item.productName} (ระบบ {item.onHand})</MenuItem>)}</Select></FormControl><TextField required label="ยอดนับจริง" type="number" value={counted} onChange={(event) => setCounted(event.target.value)} slotProps={{ htmlInput: { min: 0, step: 1 } }} /><TextField fullWidth required label="เหตุผล" value={reason} onChange={(event) => setReason(event.target.value)} /><Button disabled={!productId || submit.isPending} type="submit" variant="contained">ส่งยอดนับ</Button></Stack></Paper>
    {submit.isError && <Alert severity="error">ส่งยอดนับไม่สำเร็จ</Alert>}
    <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 3 }}><Table><TableHead><TableRow><TableCell>เวลา</TableCell><TableCell>เหตุผล</TableCell><TableCell>ผลต่าง</TableCell><TableCell>สถานะ</TableCell><TableCell /></TableRow></TableHead><TableBody>{(counts.data?.content ?? []).map((count) => <TableRow key={count.id}><TableCell>{new Date(count.countedAt).toLocaleString('th-TH')}</TableCell><TableCell>{count.reason}</TableCell><TableCell>{count.items.map((item) => `${item.sku}: ${item.difference > 0 ? '+' : ''}${item.difference}`).join(', ')}</TableCell><TableCell>{count.status}</TableCell><TableCell>{canApprove && count.status === 'SUBMITTED' && <Stack direction="row"><Button onClick={() => approve.mutate(count.id)}>อนุมัติ</Button><Button color="error" onClick={() => reject.mutate(count.id)}>ปฏิเสธ</Button></Stack>}</TableCell></TableRow>)}</TableBody></Table></TableContainer>
  </Stack>
}

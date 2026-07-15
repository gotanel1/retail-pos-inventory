import { Alert, Box, Button, Checkbox, FormControlLabel, Paper, Stack, TextField, Typography } from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { FormEvent } from 'react'
import type { Role } from '../auth/authApi'
import { configureManagerPin, getStoreSettings, updateStoreSettings } from './salesApi'

export function SettingsPage({ role }: { role: Role }) {
  const queryClient = useQueryClient()
  const settings = useQuery({ queryKey: ['store-settings'], queryFn: getStoreSettings })
  const update = useMutation({ mutationFn: updateStoreSettings, onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['store-settings'] }) })
  const pin = useMutation({ mutationFn: configureManagerPin })
  function submitSettings(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const data = new FormData(event.currentTarget); update.mutate({ storeName: String(data.get('storeName')), vatEnabled: data.get('vatEnabled') === 'on', vatRate: String(data.get('vatRate')), receiptFooter: String(data.get('receiptFooter')) }) }
  function submitPin(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const data = new FormData(event.currentTarget); pin.mutate({ currentPassword: String(data.get('currentPassword')), pin: String(data.get('pin')) }); event.currentTarget.reset() }
  return <Stack spacing={3}><Box><Typography component="h1" variant="h4">ตั้งค่าร้านและการอนุมัติ</Typography><Typography color="text.secondary">ค่า VAT จะถูก snapshot ลงบิลใหม่ ส่วน Manager PIN ใช้ยืนยันส่วนลด</Typography></Box>
    {role === 'OWNER' && settings.data && <Paper component="form" onSubmit={submitSettings} variant="outlined" sx={{ borderRadius: 3, p: 3 }}><Stack spacing={2}><Typography variant="h6">ข้อมูลร้านและ VAT</Typography>{update.isSuccess && <Alert severity="success">บันทึกการตั้งค่าแล้ว</Alert>}<TextField defaultValue={settings.data.storeName} label="ชื่อร้าน" name="storeName" required /><FormControlLabel control={<Checkbox defaultChecked={settings.data.vatEnabled} name="vatEnabled" />} label="เปิดใช้ VAT แบบรวมในราคา" /><TextField defaultValue={settings.data.vatRate} label="อัตรา VAT (%)" name="vatRate" required type="number" slotProps={{ htmlInput: { min: 0, max: 100, step: '0.01' } }} /><TextField defaultValue={settings.data.receiptFooter ?? ''} label="ข้อความท้ายใบเสร็จ" name="receiptFooter" /><Button disabled={update.isPending} type="submit" variant="contained">บันทึกการตั้งค่า</Button></Stack></Paper>}
    <Paper component="form" onSubmit={submitPin} variant="outlined" sx={{ borderRadius: 3, p: 3 }}><Stack spacing={2}><Typography variant="h6">ตั้ง Manager PIN ของฉัน</Typography>{pin.isSuccess && <Alert severity="success">ตั้ง PIN แล้ว</Alert>}{pin.isError && <Alert severity="error">ตั้ง PIN ไม่สำเร็จ กรุณาตรวจรหัสผ่าน</Alert>}<TextField label="รหัสผ่านปัจจุบัน" name="currentPassword" required type="password" /><TextField label="PIN ใหม่ 4–6 หลัก" name="pin" required type="password" slotProps={{ htmlInput: { inputMode: 'numeric', pattern: '[0-9]{4,6}' } }} /><Button disabled={pin.isPending} type="submit" variant="contained">ตั้ง PIN</Button></Stack></Paper>
  </Stack>
}

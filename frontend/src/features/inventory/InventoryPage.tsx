import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined'
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  FormControlLabel,
  InputAdornment,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { getInventoryBalances, getStockMovements } from './inventoryApi'

const movementLabels = {
  OPENING: 'ยอดตั้งต้น',
  RECEIVE: 'รับเข้า',
  SALE: 'ขาย',
  ADJUSTMENT_IN: 'ปรับเพิ่ม',
  ADJUSTMENT_OUT: 'ปรับลด',
}

export function InventoryPage() {
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [lowStock, setLowStock] = useState(false)
  const balancesQuery = useQuery({
    queryKey: ['inventory-balances', search, lowStock],
    queryFn: () => getInventoryBalances(search, lowStock),
  })
  const movementsQuery = useQuery({ queryKey: ['stock-movements'], queryFn: getStockMovements })

  function submitSearch(event: FormEvent) {
    event.preventDefault()
    setSearch(searchInput)
  }

  return (
    <Stack spacing={4}>
      <Box>
        <Typography component="h1" variant="h4">ยอดสต็อก</Typography>
        <Typography color="text.secondary">onHand คือยอดจริง, reserved คือยอดจอง และ available คือยอดที่ขายได้</Typography>
      </Box>

      <Stack component="form" direction={{ xs: 'column', sm: 'row' }} spacing={2} onSubmit={submitSearch}>
        <TextField
          fullWidth
          label="ค้นหาสินค้า"
          onChange={(event) => setSearchInput(event.target.value)}
          slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchOutlinedIcon /></InputAdornment> } }}
          value={searchInput}
        />
        <FormControlLabel
          control={<Switch checked={lowStock} onChange={(_, checked) => setLowStock(checked)} />}
          label="เฉพาะใกล้หมด"
          sx={{ minWidth: 160 }}
        />
      </Stack>

      {balancesQuery.isPending && <CircularProgress aria-label="กำลังโหลดยอดสต็อก" />}
      {balancesQuery.isError && <Alert severity="error">โหลดยอดสต็อกไม่สำเร็จ</Alert>}
      {balancesQuery.data && (
        <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 3 }}>
          <Table>
            <TableHead><TableRow>
              <TableCell>SKU</TableCell><TableCell>สินค้า</TableCell><TableCell align="right">onHand</TableCell>
              <TableCell align="right">reserved</TableCell><TableCell align="right">available</TableCell>
              <TableCell align="right">ต้นทุนเฉลี่ย</TableCell><TableCell>สถานะ</TableCell>
            </TableRow></TableHead>
            <TableBody>
              {balancesQuery.data.content.map((balance) => (
                <TableRow key={balance.productId} hover>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{balance.sku}</TableCell>
                  <TableCell>{balance.productName}</TableCell>
                  <TableCell align="right">{balance.onHand}</TableCell>
                  <TableCell align="right">{balance.reserved}</TableCell>
                  <TableCell align="right">{balance.available}</TableCell>
                  <TableCell align="right">฿{Number(balance.averageCost).toFixed(4)}</TableCell>
                  <TableCell>{balance.lowStock ? <Chip color="warning" label="ใกล้หมด" size="small" /> : <Chip color="success" label="ปกติ" size="small" />}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Box>
        <Typography variant="h5" sx={{ mb: 2 }}>Movement ล่าสุด</Typography>
        {movementsQuery.isPending && <CircularProgress aria-label="กำลังโหลด movement" />}
        {movementsQuery.isError && <Alert severity="error">โหลด movement ไม่สำเร็จ</Alert>}
        {movementsQuery.data && (
          <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 3 }}>
            <Table size="small">
              <TableHead><TableRow><TableCell>เวลา</TableCell><TableCell>สินค้า</TableCell><TableCell>ประเภท</TableCell><TableCell align="right">เปลี่ยน</TableCell><TableCell align="right">คงเหลือ</TableCell></TableRow></TableHead>
              <TableBody>
                {movementsQuery.data.content.map((movement) => (
                  <TableRow key={movement.id}>
                    <TableCell>{new Date(movement.occurredAt).toLocaleString('th-TH')}</TableCell>
                    <TableCell>{movement.sku} · {movement.productName}</TableCell>
                    <TableCell>{movementLabels[movement.movementType]}</TableCell>
                    <TableCell align="right" sx={{ color: movement.quantityDelta > 0 ? 'success.main' : 'error.main' }}>{movement.quantityDelta > 0 ? '+' : ''}{movement.quantityDelta}</TableCell>
                    <TableCell align="right">{movement.onHandAfter}</TableCell>
                  </TableRow>
                ))}
                {movementsQuery.data.content.length === 0 && <TableRow><TableCell align="center" colSpan={5}>ยังไม่มี movement</TableCell></TableRow>}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Box>
    </Stack>
  )
}

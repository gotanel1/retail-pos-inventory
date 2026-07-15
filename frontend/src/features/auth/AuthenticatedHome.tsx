import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined'
import PointOfSaleOutlinedIcon from '@mui/icons-material/PointOfSaleOutlined'
import { Box, Chip, Paper, Stack, Typography } from '@mui/material'

const plannedModules = [
  'Catalog และ Barcode',
  'Stock Ledger',
  'Goods Receipt',
  'POS และ VAT',
  'PromptPay Test Mode',
  'Dashboard และ Reports',
]

export function AuthenticatedHome() {
  return (
    <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', borderRadius: 4, p: { xs: 3, md: 6 } }}>
      <Stack spacing={4}>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <Box sx={{ display: 'grid', placeItems: 'center', width: 56, height: 56, borderRadius: 3, bgcolor: 'primary.main', color: 'primary.contrastText' }}>
            <PointOfSaleOutlinedIcon fontSize="large" />
          </Box>
          <Box>
            <Typography variant="overline" color="primary.main">Catalog Ready</Typography>
            <Typography component="h1" variant="h3">Retail POS & Inventory</Typography>
          </Box>
        </Stack>

        <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 400 }}>
          ระบบพร้อมจัดการสินค้า ค้นหาจาก SKU/Barcode และนำเข้า CSV แบบตรวจ Preview ก่อนบันทึก
        </Typography>

        <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
          {plannedModules.map((module) => (
            <Chip key={module} icon={<Inventory2OutlinedIcon />} label={module} variant="outlined" />
          ))}
        </Stack>
      </Stack>
    </Paper>
  )
}

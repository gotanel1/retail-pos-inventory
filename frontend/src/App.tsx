import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined'
import PointOfSaleOutlinedIcon from '@mui/icons-material/PointOfSaleOutlined'
import { Box, Chip, Container, Paper, Stack, Typography } from '@mui/material'

const plannedModules = [
  'Catalog และ Barcode',
  'Stock Ledger',
  'Goods Receipt',
  'POS และ VAT',
  'PromptPay Test Mode',
  'Dashboard และ Reports',
]

export function App() {
  return (
    <Box component="main" sx={{ minHeight: '100vh', py: { xs: 6, md: 10 } }}>
      <Container maxWidth="md">
        <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', borderRadius: 4, p: { xs: 3, md: 6 } }}>
          <Stack spacing={4}>
            <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
              <Box sx={{ display: 'grid', placeItems: 'center', width: 56, height: 56, borderRadius: 3, bgcolor: 'primary.main', color: 'primary.contrastText' }}>
                <PointOfSaleOutlinedIcon fontSize="large" />
              </Box>
              <Box>
                <Typography variant="overline" color="primary.main">Portfolio Foundation</Typography>
                <Typography component="h1" variant="h3">Retail POS & Inventory</Typography>
              </Box>
            </Stack>

            <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 400 }}>
              ระบบขายหน้าร้านและควบคุมสต็อกสำหรับร้านค้าปลีกหนึ่งสาขา เน้นความถูกต้องของธุรกรรมและการตรวจสอบย้อนหลัง
            </Typography>

            <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
              {plannedModules.map((module) => (
                <Chip key={module} icon={<Inventory2OutlinedIcon />} label={module} variant="outlined" />
              ))}
            </Stack>

            <Typography variant="body2" color="text.secondary">
              Foundation branch พร้อมสำหรับเพิ่ม authentication, catalog และ inventory modules ตามลำดับ Pull Request
            </Typography>
          </Stack>
        </Paper>
      </Container>
    </Box>
  )
}

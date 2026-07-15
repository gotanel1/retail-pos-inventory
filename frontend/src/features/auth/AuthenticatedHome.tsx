import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined'
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined'
import PointOfSaleOutlinedIcon from '@mui/icons-material/PointOfSaleOutlined'
import { Box, Button, Chip, Container, Paper, Stack, Typography } from '@mui/material'
import type { CurrentUser, Role } from './authApi'

const roleLabels: Record<Role, string> = {
  OWNER: 'เจ้าของร้าน',
  MANAGER: 'ผู้จัดการ',
  CASHIER: 'แคชเชียร์',
  INVENTORY_STAFF: 'พนักงานสต็อก',
}

const plannedModules = [
  'Catalog และ Barcode',
  'Stock Ledger',
  'Goods Receipt',
  'POS และ VAT',
  'PromptPay Test Mode',
  'Dashboard และ Reports',
]

interface AuthenticatedHomeProps {
  user: CurrentUser
  isLoggingOut: boolean
  onLogout: () => void
}

export function AuthenticatedHome({ user, isLoggingOut, onLogout }: AuthenticatedHomeProps) {
  return (
    <Box component="main" sx={{ minHeight: '100vh', py: { xs: 4, md: 8 } }}>
      <Container maxWidth="md">
        <Stack spacing={2}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' } }}>
            <Box>
              <Typography variant="body2" color="text.secondary">เข้าสู่ระบบในชื่อ</Typography>
              <Typography variant="h6">{user.displayName} · {roleLabels[user.role]}</Typography>
            </Box>
            <Button
              color="inherit"
              disabled={isLoggingOut}
              onClick={onLogout}
              startIcon={<LogoutOutlinedIcon />}
              variant="outlined"
            >
              ออกจากระบบ
            </Button>
          </Stack>

          <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', borderRadius: 4, p: { xs: 3, md: 6 } }}>
            <Stack spacing={4}>
              <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                <Box sx={{ display: 'grid', placeItems: 'center', width: 56, height: 56, borderRadius: 3, bgcolor: 'primary.main', color: 'primary.contrastText' }}>
                  <PointOfSaleOutlinedIcon fontSize="large" />
                </Box>
                <Box>
                  <Typography variant="overline" color="primary.main">Secure Foundation</Typography>
                  <Typography component="h1" variant="h3">Retail POS & Inventory</Typography>
                </Box>
              </Stack>

              <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 400 }}>
                ระบบพร้อมยืนยันตัวตนด้วย session และควบคุมสิทธิ์ตามบทบาท ก่อนเริ่มโมดูลสินค้าและสต็อก
              </Typography>

              <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                {plannedModules.map((module) => (
                  <Chip key={module} icon={<Inventory2OutlinedIcon />} label={module} variant="outlined" />
                ))}
              </Stack>
            </Stack>
          </Paper>
        </Stack>
      </Container>
    </Box>
  )
}

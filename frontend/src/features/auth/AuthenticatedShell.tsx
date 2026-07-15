import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined'
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined'
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined'
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined'
import {
  AppBar,
  Box,
  Button,
  Chip,
  Container,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material'
import { Link, useLocation } from 'react-router'
import type { CurrentUser, Role } from './authApi'

const roleLabels: Record<Role, string> = {
  OWNER: 'เจ้าของร้าน',
  MANAGER: 'ผู้จัดการ',
  CASHIER: 'แคชเชียร์',
  INVENTORY_STAFF: 'พนักงานสต็อก',
}

const catalogEditors: Role[] = ['OWNER', 'MANAGER', 'INVENTORY_STAFF']

interface AuthenticatedShellProps {
  children: React.ReactNode
  user: CurrentUser
  isLoggingOut: boolean
  onLogout: () => void
}

export function AuthenticatedShell({ children, user, isLoggingOut, onLogout }: AuthenticatedShellProps) {
  const location = useLocation()
  const links = [
    { label: 'หน้าหลัก', path: '/', icon: <HomeOutlinedIcon fontSize="small" /> },
    { label: 'สินค้า', path: '/products', icon: <Inventory2OutlinedIcon fontSize="small" /> },
    ...(catalogEditors.includes(user.role)
      ? [{ label: 'นำเข้า CSV', path: '/products/import', icon: <UploadFileOutlinedIcon fontSize="small" /> }]
      : []),
  ]

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar color="inherit" elevation={0} position="sticky" sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar>
          <Container maxWidth="lg" disableGutters>
            <Stack direction="row" spacing={2} sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
              <Box>
                <Typography color="primary.main" sx={{ fontWeight: 800 }}>Retail POS</Typography>
                <Typography color="text.secondary" variant="caption">{user.displayName} · {roleLabels[user.role]}</Typography>
              </Box>
              <Button color="inherit" disabled={isLoggingOut} onClick={onLogout} startIcon={<LogoutOutlinedIcon />}>
                ออกจากระบบ
              </Button>
            </Stack>
          </Container>
        </Toolbar>
      </AppBar>

      <Container component="nav" maxWidth="lg" sx={{ py: 2 }}>
        <Stack direction="row" spacing={1} useFlexGap sx={{ overflowX: 'auto' }}>
          {links.map((link) => (
            <Chip
              clickable
              color={location.pathname === link.path ? 'primary' : 'default'}
              component={Link}
              icon={link.icon}
              key={link.path}
              label={link.label}
              to={link.path}
              variant={location.pathname === link.path ? 'filled' : 'outlined'}
            />
          ))}
        </Stack>
      </Container>

      <Container component="main" maxWidth="lg" sx={{ pb: 8, pt: 2 }}>
        {children}
      </Container>
    </Box>
  )
}

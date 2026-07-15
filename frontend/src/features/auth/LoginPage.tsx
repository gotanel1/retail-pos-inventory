import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import { Alert, Avatar, Box, Button, Container, Paper, Stack, TextField, Typography } from '@mui/material'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../../api/http'
import type { LoginInput } from './authApi'

interface LoginPageProps {
  isPending: boolean
  onLogin: (input: LoginInput) => void
  error: Error | null
}

export function LoginPage({ isPending, onLogin, error }: LoginPageProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onLogin({ username, password })
  }

  const errorMessage = error instanceof ApiError
    ? error.problem?.detail ?? error.message
    : error?.message

  return (
    <Box component="main" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', py: 4 }}>
      <Container maxWidth="xs">
        <Paper elevation={0} sx={{ border: 1, borderColor: 'divider', borderRadius: 4, p: { xs: 3, sm: 5 } }}>
          <Stack component="form" spacing={3} onSubmit={handleSubmit}>
            <Stack spacing={1.5} sx={{ alignItems: 'center', textAlign: 'center' }}>
              <Avatar sx={{ bgcolor: 'primary.main', width: 52, height: 52 }}>
                <LockOutlinedIcon />
              </Avatar>
              <Typography component="h1" variant="h4">เข้าสู่ระบบ</Typography>
              <Typography color="text.secondary">Retail POS & Inventory</Typography>
            </Stack>

            {errorMessage && <Alert severity="error">{errorMessage}</Alert>}

            <TextField
              autoComplete="username"
              autoFocus
              disabled={isPending}
              label="ชื่อผู้ใช้"
              required
              value={username}
              onChange={(event) => setUsername(event.target.value)}
            />
            <TextField
              autoComplete="current-password"
              disabled={isPending}
              label="รหัสผ่าน"
              required
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
            <Button disabled={isPending} size="large" type="submit" variant="contained">
              {isPending ? 'กำลังเข้าสู่ระบบ…' : 'เข้าสู่ระบบ'}
            </Button>
          </Stack>
        </Paper>
      </Container>
    </Box>
  )
}

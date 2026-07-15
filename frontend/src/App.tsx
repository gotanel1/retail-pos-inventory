import { Alert, Box, CircularProgress, Container } from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { AuthenticatedHome } from './features/auth/AuthenticatedHome'
import { AuthenticatedShell } from './features/auth/AuthenticatedShell'
import { getCurrentUser, login, logout } from './features/auth/authApi'
import { LoginPage } from './features/auth/LoginPage'

const ProductPage = lazy(() => import('./features/catalog/ProductPage').then((module) => ({ default: module.ProductPage })))
const ProductImportPage = lazy(() => import('./features/catalog/ProductImportPage').then((module) => ({ default: module.ProductImportPage })))

const currentUserQueryKey = ['current-user'] as const

export function App() {
  const queryClient = useQueryClient()
  const currentUserQuery = useQuery({
    queryKey: currentUserQueryKey,
    queryFn: getCurrentUser,
    retry: false,
  })
  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (user) => queryClient.setQueryData(currentUserQueryKey, user),
  })
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => queryClient.setQueryData(currentUserQueryKey, null),
  })

  if (currentUserQuery.isPending) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <CircularProgress aria-label="กำลังตรวจสอบสถานะการเข้าสู่ระบบ" />
      </Box>
    )
  }

  if (currentUserQuery.isError) {
    return (
      <Container maxWidth="sm" sx={{ py: 8 }}>
        <Alert severity="error">ไม่สามารถตรวจสอบสถานะการเข้าสู่ระบบได้ กรุณาลองใหม่</Alert>
      </Container>
    )
  }

  if (!currentUserQuery.data) {
    return (
      <LoginPage
        error={loginMutation.error}
        isPending={loginMutation.isPending}
        onLogin={loginMutation.mutate}
      />
    )
  }

  const user = currentUserQuery.data
  const canImport = user.role !== 'CASHIER'

  return (
    <AuthenticatedShell
      isLoggingOut={logoutMutation.isPending}
      onLogout={logoutMutation.mutate}
      user={user}
    >
      <Suspense fallback={<CircularProgress aria-label="กำลังโหลดหน้า" />}>
        <Routes>
          <Route path="/" element={<AuthenticatedHome />} />
          <Route path="/products" element={<ProductPage role={user.role} />} />
          <Route path="/products/import" element={canImport ? <ProductImportPage /> : <Navigate replace to="/products" />} />
          <Route path="*" element={<Navigate replace to="/" />} />
        </Routes>
      </Suspense>
    </AuthenticatedShell>
  )
}

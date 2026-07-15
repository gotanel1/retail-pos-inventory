import AddOutlinedIcon from '@mui/icons-material/AddOutlined'
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputAdornment,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../api/http'
import type { Role } from '../auth/authApi'
import { createProduct, getCategories, getProducts } from './catalogApi'

interface ProductPageProps {
  role: Role
}

const catalogEditors: Role[] = ['OWNER', 'MANAGER', 'INVENTORY_STAFF']

export function ProductPage({ role }: ProductPageProps) {
  const queryClient = useQueryClient()
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const productsQuery = useQuery({
    queryKey: ['products', search, page],
    queryFn: () => getProducts(search, page),
  })
  const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: getCategories })
  const createMutation = useMutation({
    mutationFn: createProduct,
    onSuccess: async () => {
      setDialogOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['products'] })
    },
  })
  const canEdit = catalogEditors.includes(role)

  function submitSearch(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput)
  }

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' } }}>
        <Box>
          <Typography component="h1" variant="h4">สินค้า</Typography>
          <Typography color="text.secondary">ค้นหาจากชื่อ SKU หรือ Barcode และกำหนดจุดเตือนสต็อกต่ำ</Typography>
        </Box>
        {canEdit && (
          <Button onClick={() => setDialogOpen(true)} startIcon={<AddOutlinedIcon />} variant="contained">
            เพิ่มสินค้า
          </Button>
        )}
      </Stack>

      <Box component="form" onSubmit={submitSearch}>
        <TextField
          fullWidth
          label="ค้นหาสินค้า"
          onChange={(event) => setSearchInput(event.target.value)}
          placeholder="เช่น HAMMER-01 หรือ 885..."
          slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchOutlinedIcon /></InputAdornment> } }}
          value={searchInput}
        />
      </Box>

      {productsQuery.isPending && <CircularProgress aria-label="กำลังโหลดสินค้า" />}
      {productsQuery.isError && <Alert severity="error">โหลดรายการสินค้าไม่สำเร็จ กรุณาลองใหม่</Alert>}
      {productsQuery.data && (
        <>
          <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 3 }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>SKU</TableCell>
                  <TableCell>สินค้า</TableCell>
                  <TableCell>หมวดหมู่</TableCell>
                  <TableCell>Barcode</TableCell>
                  <TableCell align="right">ราคาขาย</TableCell>
                  <TableCell align="right">เตือนเมื่อ ≤</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {productsQuery.data.content.map((product) => (
                  <TableRow key={product.id} hover>
                    <TableCell sx={{ fontFamily: 'monospace' }}>{product.sku}</TableCell>
                    <TableCell>{product.name}</TableCell>
                    <TableCell>{product.categoryName}</TableCell>
                    <TableCell>{product.barcode || '—'}</TableCell>
                    <TableCell align="right">฿{Number(product.salePrice).toLocaleString('th-TH', { minimumFractionDigits: 2 })}</TableCell>
                    <TableCell align="right">{product.lowStockThreshold}</TableCell>
                  </TableRow>
                ))}
                {productsQuery.data.content.length === 0 && (
                  <TableRow><TableCell align="center" colSpan={6}>ยังไม่มีสินค้าที่ตรงกับการค้นหา</TableCell></TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography color="text.secondary">ทั้งหมด {productsQuery.data.totalElements} รายการ</Typography>
            {productsQuery.data.totalPages > 1 && (
              <Pagination
                count={productsQuery.data.totalPages}
                onChange={(_, value) => setPage(value - 1)}
                page={productsQuery.data.page + 1}
              />
            )}
          </Stack>
        </>
      )}

      <ProductDialog
        categories={categoriesQuery.data ?? []}
        error={createMutation.error}
        isPending={createMutation.isPending}
        onClose={() => setDialogOpen(false)}
        onSubmit={(input) => createMutation.mutate(input)}
        open={dialogOpen}
      />
    </Stack>
  )
}

interface ProductDialogProps {
  categories: Array<{ id: string; name: string }>
  error: Error | null
  isPending: boolean
  onClose: () => void
  onSubmit: (input: Parameters<typeof createProduct>[0]) => void
  open: boolean
}

function ProductDialog({ categories, error, isPending, onClose, onSubmit, open }: ProductDialogProps) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    onSubmit({
      categoryId: data.get('categoryId') as string,
      sku: data.get('sku') as string,
      barcode: data.get('barcode') as string,
      name: data.get('name') as string,
      salePrice: data.get('salePrice') as string,
      lowStockThreshold: Number(data.get('lowStockThreshold')),
    })
  }

  const errorMessage = error instanceof ApiError ? error.message : error ? 'บันทึกสินค้าไม่สำเร็จ' : null

  return (
    <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}>
      <Box component="form" onSubmit={handleSubmit}>
        <DialogTitle>เพิ่มสินค้า</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
            <FormControl required fullWidth>
              <InputLabel id="category-label">หมวดหมู่</InputLabel>
              <Select label="หมวดหมู่" labelId="category-label" name="categoryId" defaultValue="">
                {categories.map((category) => <MenuItem key={category.id} value={category.id}>{category.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField slotProps={{ htmlInput: { pattern: '[a-zA-Z0-9._-]+' } }} label="SKU" name="sku" required />
            <TextField slotProps={{ htmlInput: { pattern: '[a-zA-Z0-9._-]*' } }} label="Barcode (ไม่บังคับ)" name="barcode" />
            <TextField label="ชื่อสินค้า" name="name" required />
            <TextField slotProps={{ htmlInput: { min: 0, step: '0.01' } }} label="ราคาขาย" name="salePrice" required type="number" />
            <TextField defaultValue="0" slotProps={{ htmlInput: { min: 0, step: 1 } }} label="จุดเตือนสต็อกต่ำ" name="lowStockThreshold" required type="number" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button disabled={isPending} onClick={onClose}>ยกเลิก</Button>
          <Button disabled={isPending || categories.length === 0} type="submit" variant="contained">บันทึกสินค้า</Button>
        </DialogActions>
      </Box>
    </Dialog>
  )
}

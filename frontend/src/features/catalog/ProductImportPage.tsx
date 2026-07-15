import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlineOutlined'
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined'
import {
  Alert,
  Box,
  Button,
  Chip,
  LinearProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { ChangeEvent } from 'react'
import { ApiError } from '../../api/http'
import { commitProductImport, previewProductImport } from './catalogApi'

export function ProductImportPage() {
  const queryClient = useQueryClient()
  const [file, setFile] = useState<File | null>(null)
  const previewMutation = useMutation({ mutationFn: previewProductImport })
  const commitMutation = useMutation({
    mutationFn: commitProductImport,
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['products'] }),
  })
  const preview = previewMutation.data

  function chooseFile(event: ChangeEvent<HTMLInputElement>) {
    setFile(event.target.files?.[0] ?? null)
    previewMutation.reset()
    commitMutation.reset()
  }

  const error = previewMutation.error ?? commitMutation.error
  const errorMessage = error instanceof ApiError ? error.message : error ? 'นำเข้าไฟล์ไม่สำเร็จ' : null

  return (
    <Stack spacing={3}>
      <Box>
        <Typography component="h1" variant="h4">นำเข้าสินค้าจาก CSV</Typography>
        <Typography color="text.secondary">ตรวจตัวอย่างและข้อผิดพลาดก่อนยืนยัน ระบบจะไม่สร้างสินค้าระหว่าง preview</Typography>
      </Box>

      <Paper variant="outlined" sx={{ borderRadius: 3, p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">1. เลือกไฟล์ UTF-8</Typography>
          <Typography color="text.secondary">
            Header ที่ต้องมี: sku, barcode, name, category, salePrice, lowStockThreshold (สูงสุด 1,000 แถว / 2 MB)
          </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'center' } }}>
            <Button component="label" startIcon={<UploadFileOutlinedIcon />} variant="outlined">
              เลือกไฟล์ CSV
              <input accept=".csv,text/csv" hidden onChange={chooseFile} type="file" />
            </Button>
            <Typography>{file?.name ?? 'ยังไม่ได้เลือกไฟล์'}</Typography>
            <Button
              disabled={!file || previewMutation.isPending}
              onClick={() => file && previewMutation.mutate(file)}
              variant="contained"
            >
              ตรวจไฟล์
            </Button>
          </Stack>
          {previewMutation.isPending && <LinearProgress aria-label="กำลังตรวจไฟล์" />}
          {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
        </Stack>
      </Paper>

      {preview && (
        <Paper variant="outlined" sx={{ borderRadius: 3, p: 3 }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="h6">2. ตรวจ Preview</Typography>
              <Stack direction="row" spacing={1} useFlexGap sx={{ mt: 1, flexWrap: 'wrap' }}>
                <Chip label={`ทั้งหมด ${preview.totalRows}`} />
                <Chip color="success" label={`ผ่าน ${preview.validRows}`} />
                <Chip color={preview.invalidRows ? 'error' : 'default'} label={`ไม่ผ่าน ${preview.invalidRows}`} />
              </Stack>
            </Box>
            <TableContainer sx={{ maxHeight: 480 }}>
              <Table stickyHeader size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>แถว</TableCell>
                    <TableCell>SKU</TableCell>
                    <TableCell>สินค้า</TableCell>
                    <TableCell>หมวดหมู่</TableCell>
                    <TableCell align="right">ราคา</TableCell>
                    <TableCell>ผลตรวจ</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {preview.rows.map((row) => (
                    <TableRow key={row.rowNumber} sx={row.errors.length ? { bgcolor: 'error.50' } : undefined}>
                      <TableCell>{row.rowNumber}</TableCell>
                      <TableCell sx={{ fontFamily: 'monospace' }}>{row.sku || '—'}</TableCell>
                      <TableCell>{row.name || '—'}</TableCell>
                      <TableCell>{row.category || '—'}</TableCell>
                      <TableCell align="right">{row.salePrice == null ? '—' : `฿${Number(row.salePrice).toFixed(2)}`}</TableCell>
                      <TableCell>
                        {row.errors.length
                          ? row.errors.map((message) => <Typography color="error" key={message} variant="body2">{message}</Typography>)
                          : <Chip color="success" label="พร้อมนำเข้า" size="small" />}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            {commitMutation.isSuccess ? (
              <Alert icon={<CheckCircleOutlineIcon />} severity="success">
                สร้างสินค้าแล้ว {commitMutation.data.createdProducts} รายการ
              </Alert>
            ) : (
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' } }}>
                <Typography color="text.secondary">Preview หมดอายุ {new Date(preview.expiresAt).toLocaleString('th-TH')}</Typography>
                <Button
                  disabled={preview.invalidRows > 0 || commitMutation.isPending}
                  onClick={() => commitMutation.mutate(preview.importId)}
                  variant="contained"
                >
                  ยืนยันนำเข้า {preview.validRows} รายการ
                </Button>
              </Stack>
            )}
          </Stack>
        </Paper>
      )}
    </Stack>
  )
}

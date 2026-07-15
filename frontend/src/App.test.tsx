import '@testing-library/jest-dom/vitest'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { App } from './App'

describe('App', () => {
  it('แสดงชื่อระบบและขอบเขตของ foundation', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Retail POS & Inventory' })).toBeInTheDocument()
    expect(screen.getByText('Stock Ledger')).toBeInTheDocument()
  })
})

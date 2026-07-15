import { createTheme } from '@mui/material/styles'

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#166534',
    },
    background: {
      default: '#f6f7f3',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: '"Noto Sans Thai", "Segoe UI", sans-serif',
    h3: {
      fontWeight: 750,
      letterSpacing: '-0.03em',
    },
  },
})

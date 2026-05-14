const THEME_STORAGE_KEY = 'bid-system-theme'
const THEME_DARK = 'dark'
const THEME_LIGHT = 'light'
const VALID_THEMES = new Set([THEME_LIGHT, THEME_DARK])

const getSystemTheme = () => {
  if (typeof window === 'undefined' || !window.matchMedia) return THEME_LIGHT
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? THEME_DARK : THEME_LIGHT
}

export const normalizeTheme = (theme) => {
  if (VALID_THEMES.has(theme)) return theme
  return getSystemTheme()
}

export const readStoredTheme = () => {
  if (typeof localStorage === 'undefined') return getSystemTheme()
  return normalizeTheme(localStorage.getItem(THEME_STORAGE_KEY))
}

export const applyTheme = (theme) => {
  const nextTheme = normalizeTheme(theme)
  if (typeof document !== 'undefined') {
    document.documentElement.dataset.theme = nextTheme
    document.body?.setAttribute('arco-theme', nextTheme === THEME_DARK ? THEME_DARK : THEME_LIGHT)
  }
  return nextTheme
}

export const storeTheme = (theme) => {
  const nextTheme = normalizeTheme(theme)
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(THEME_STORAGE_KEY, nextTheme)
  }
  return applyTheme(nextTheme)
}

export const initTheme = () => applyTheme(readStoredTheme())

export const toggleTheme = (theme) => {
  return storeTheme(normalizeTheme(theme) === THEME_DARK ? THEME_LIGHT : THEME_DARK)
}

export { THEME_DARK, THEME_LIGHT, THEME_STORAGE_KEY }

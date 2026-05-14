export const USER_INFO_KEY = 'userInfo'
export const PERMISSIONS_KEY = 'permissions'
export const TOKEN_KEY = 'token'

export const readStoredToken = () => {
  return localStorage.getItem(TOKEN_KEY)
}

export const writeStoredToken = (token) => {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export const readStoredUserInfo = () => {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const readStoredPermissions = () => {
  try {
    const raw = localStorage.getItem(PERMISSIONS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const writeStoredUserContext = (userInfo, permissions) => {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  localStorage.setItem('username', userInfo.username || '用户')
  localStorage.setItem('role', userInfo.role || 'user')
  localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(permissions || []))
}

export const clearStoredUserContext = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('role')
  localStorage.removeItem('username')
  localStorage.removeItem(PERMISSIONS_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

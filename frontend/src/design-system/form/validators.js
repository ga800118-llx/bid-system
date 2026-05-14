const ID_CARD_WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
const ID_CARD_CHECK_CODES = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

export const DS_INPUT_TYPES = ['text', 'phone', 'email', 'idCard']

const hasMaskedValue = (value) => String(value || '').includes('*')

const isValidIdCard = (value) => {
  const text = String(value || '').trim().toUpperCase()
  if (!/^\d{17}[\dX]$/.test(text)) return false
  const sum = ID_CARD_WEIGHTS.reduce((total, weight, index) => total + Number(text[index]) * weight, 0)
  return ID_CARD_CHECK_CODES[sum % 11] === text[17]
}

export const validateDsInputValue = ({
  value,
  label = '该字段',
  type = 'text',
  required = false,
  maxLength
} = {}) => {
  const text = value === undefined || value === null ? '' : String(value).trim()
  if (!text) {
    return required ? `请输入${label}` : ''
  }

  if (hasMaskedValue(text)) return ''

  if (maxLength && String(value).length > maxLength) {
    return `${label}不能超过${maxLength}个字符`
  }

  if (type === 'phone' && !/^1[3-9]\d{9}$/.test(text)) {
    return '手机号格式不正确'
  }

  if (type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(text)) {
    return '邮箱格式不正确'
  }

  if (type === 'idCard' && !isValidIdCard(text)) {
    return '身份证格式不正确'
  }

  return ''
}

export const normalizeDsInputValue = (value, maxLength) => {
  const text = value === undefined || value === null ? '' : String(value)
  if (!maxLength || text.length <= maxLength) return value
  return text.slice(0, maxLength)
}

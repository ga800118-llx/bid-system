import { validateDsInputValue } from '@/design-system/form/validators'

export const validateUserFormFields = ({
  form,
  isEdit = false,
  validateUsername = true,
  validateRealName = true,
  canWriteField = () => true
} = {}) => {
  if (validateUsername && !isEdit) {
    const usernameError = validateDsInputValue({
      value: form.username,
      label: '账号',
      type: 'text',
      required: true,
      maxLength: 100
    })
    if (usernameError) return usernameError
  }

  if (validateRealName) {
    const realNameError = validateDsInputValue({
      value: form.realName,
      label: '姓名',
      type: 'text',
      required: true,
      maxLength: 50
    })
    if (realNameError) return realNameError
  }

  if (canWriteField('phone')) {
    const phoneError = validateDsInputValue({
      value: form.phone,
      label: '手机号',
      type: 'phone',
      maxLength: 11
    })
    if (phoneError) return phoneError
  }

  if (canWriteField('email')) {
    const emailError = validateDsInputValue({
      value: form.email,
      label: '邮箱',
      type: 'email',
      maxLength: 100
    })
    if (emailError) return emailError
  }

  if (canWriteField('idCard')) {
    const idCardError = validateDsInputValue({
      value: form.idCard,
      label: '身份证号',
      type: 'idCard',
      maxLength: 18
    })
    if (idCardError) return idCardError
  }

  return ''
}

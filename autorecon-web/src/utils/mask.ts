export function maskPhone(phone: string): string {
  if (!phone || phone.length < 7) return phone || ''
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

export function maskIdNo(idNo: string): string {
  if (!idNo || idNo.length < 8) return idNo || ''
  return idNo.substring(0, 4) + '****' + idNo.substring(idNo.length - 4)
}

export function maskEmail(email: string): string {
  if (!email) return ''
  const [local, domain] = email.split('@')
  if (!domain) return email
  const maskedLocal = local.length > 2 ? local[0] + '***' + local[local.length - 1] : '***'
  return maskedLocal + '@' + domain
}

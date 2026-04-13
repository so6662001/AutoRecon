import type { DeviceInfo } from '../types'

const DEVICE_ID_KEY = 'analytics_device_id'

function generateUUID(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

function generateDeviceId(): string {
  const stored = localStorage.getItem(DEVICE_ID_KEY)
  if (stored) return stored

  const components = [
    screen.width,
    screen.height,
    Intl.DateTimeFormat().resolvedOptions().timeZone,
    navigator.language,
    navigator.platform,
  ].join('|')

  let hash = 0
  for (let i = 0; i < components.length; i++) {
    const char = components.charCodeAt(i)
    hash = ((hash << 5) - hash + char) | 0
  }

  const deviceId = `dev_${Math.abs(hash).toString(36)}_${Date.now().toString(36)}`
  localStorage.setItem(DEVICE_ID_KEY, deviceId)
  return deviceId
}

function parseBrowser(ua: string): string {
  if (ua.includes('Edg/')) return 'Edge'
  if (ua.includes('OPR/') || ua.includes('Opera')) return 'Opera'
  if (ua.includes('Firefox/')) return 'Firefox'
  if (ua.includes('Chrome/')) return 'Chrome'
  if (ua.includes('Safari/') && !ua.includes('Chrome')) return 'Safari'
  if (ua.includes('MSIE') || ua.includes('Trident/')) return 'IE'
  return 'Unknown'
}

function parseOS(ua: string): string {
  if (ua.includes('Windows')) return 'Windows'
  if (ua.includes('Mac OS')) return 'macOS'
  if (ua.includes('Android')) return 'Android'
  if (ua.includes('iPhone') || ua.includes('iPad')) return 'iOS'
  if (ua.includes('Linux')) return 'Linux'
  return 'Unknown'
}

function parsePlatform(ua: string): string {
  if (ua.includes('iPhone') || ua.includes('Android') && ua.includes('Mobile')) return 'mobile'
  if (ua.includes('iPad') || ua.includes('Tablet')) return 'tablet'
  return 'desktop'
}

export function getDeviceInfo(): DeviceInfo {
  const ua = navigator.userAgent
  const platform = parsePlatform(ua)

  return {
    deviceId: generateDeviceId(),
    platform,
    os: parseOS(ua),
    browser: parseBrowser(ua),
    screenWidth: screen.width,
    screenHeight: screen.height,
    isMobile: platform === 'mobile' || platform === 'tablet',
    userAgent: ua,
  }
}

export { generateUUID }

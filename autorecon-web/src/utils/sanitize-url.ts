/**
 * Sanitize URL for safe use in href/src attributes.
 * Only allows http/https or relative paths.
 */
export function sanitizeUrl(url: string): string {
  if (!url) return ''
  try {
    const parsed = new URL(url, window.location.origin)
    if (['http:', 'https:'].includes(parsed.protocol)) {
      return url
    }
  } catch {
    if (url.startsWith('/')) return url
  }
  return ''
}

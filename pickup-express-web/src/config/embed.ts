export interface EmbedConfig {
  // Whether running in embedded mode (inside host platform)
  embedded: boolean
  // Base path for routes (e.g., '/pickup' when embedded under host's /pickup)
  basePath: string
  // API base URL (can be overridden by host platform)
  apiBaseUrl: string
  // Auth token (injected by host platform)
  authToken?: string
  // Whether to show the sidebar (false when host has its own nav)
  showSidebar: boolean
  // Whether to show the header (false when host has its own header)
  showHeader: boolean
  // Whether to show login page (false when host handles auth)
  showLogin: boolean
  // Host platform callback functions
  onNavigate?: (path: string) => void
  onLogout?: () => void
  onError?: (error: unknown) => void
}

const defaultConfig: EmbedConfig = {
  embedded: false,
  basePath: '/',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
  showSidebar: true,
  showHeader: true,
  showLogin: true,
}

let currentConfig: EmbedConfig = { ...defaultConfig }

// Host platform calls this to configure the module
export function configureEmbed(config: Partial<EmbedConfig>) {
  currentConfig = { ...defaultConfig, ...config }
  // If host provides auth token, save it
  if (config.authToken) {
    localStorage.setItem('token', config.authToken)
  }
}

export function getEmbedConfig(): EmbedConfig {
  return currentConfig
}

export function isEmbedded(): boolean {
  return currentConfig.embedded
}

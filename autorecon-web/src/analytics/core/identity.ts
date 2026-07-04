import type { UserInfo } from '../types'

export class IdentityManager {
  private user: UserInfo | null = null

  identify(user: UserInfo): void {
    this.user = { ...user }
  }

  getUser(): UserInfo | null {
    return this.user ? { ...this.user } : null
  }

  clear(): void {
    this.user = null
  }
}

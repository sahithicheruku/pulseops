import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'

type AuthContextValue = { token: string | null; signIn: (username: string, password: string) => Promise<void>; signInDemo: () => Promise<void>; signUp: (name: string, email: string, password: string, confirmPassword: string) => Promise<void>; signOut: () => void }
const AuthContext = createContext<AuthContextValue | null>(null)
const storageKey = 'pulseops.jwt'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState(() => sessionStorage.getItem(storageKey))
  const signIn = async (username: string, password: string) => {
    try {
      const response = await fetch('/api/v1/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password }) })
      if (!response.ok) throw new Error(response.status === 401 ? 'Invalid username or password.' : 'Could not sign in.')
      const result = await response.json() as { token: string }
      sessionStorage.setItem(storageKey, result.token)
      setToken(result.token)
    } catch (error) {
      if (error instanceof TypeError && error.message.toLowerCase().includes('fetch')) throw new Error('We could not reach the transaction service. Check your connection and try again.')
      throw error
    }
  }
  const signInDemo = async () => {
    const response = await fetch('/api/v1/auth/demo', { method: 'POST' })
    if (!response.ok) throw new Error(response.status === 404 ? 'Public demo access is not enabled.' : 'Could not open the demo.')
    const result = await response.json() as { token: string }
    sessionStorage.setItem(storageKey, result.token)
    setToken(result.token)
  }
  const signUp = async (name: string, email: string, password: string, confirmPassword: string) => {
    const response = await fetch('/api/v1/auth/signup', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name, email, password, confirmPassword }) })
    if (!response.ok) {
      const body = await response.json().catch(() => null) as { message?: string } | null
      throw new Error(body?.message || (response.status === 409 ? 'That email is already registered.' : 'Could not create account.'))
    }
    const result = await response.json() as { token: string }
    sessionStorage.setItem(storageKey, result.token)
    setToken(result.token)
  }
  const signOut = () => { sessionStorage.removeItem(storageKey); setToken(null) }
  return <AuthContext.Provider value={{ token, signIn, signInDemo, signUp, signOut }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}

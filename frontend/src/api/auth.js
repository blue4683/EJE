import { client, unwrap } from './client'
import { useSessionStore } from '@/stores/session'

export const signUp = (payload) => unwrap(client.post('/auth/signup', payload))
export const logIn = (payload) => unwrap(client.post('/auth/login', payload))
export const reissue = () => unwrap(client.post('/auth/reissue'))
export const logOut = () => unwrap(client.post('/auth/logout'))

export async function restoreSession() {
  const session = useSessionStore()
  try {
    const { accessToken } = await unwrap(client.post('/auth/reissue', null, { timeout: 3000 }))
    session.setAccessToken(accessToken)
    session.setUser(await unwrap(client.get('/users/me')))
  } catch {
    session.clear()
  } finally {
    session.markBootstrapped()
  }
}

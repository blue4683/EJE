import { client, unwrap } from './client'

export const fetchMe = () => unwrap(client.get('/users/me'))
export const withdraw = (password) =>
  unwrap(client.delete('/users/me', { data: { password } }))

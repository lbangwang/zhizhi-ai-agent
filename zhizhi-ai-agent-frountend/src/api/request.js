import axios from 'axios'
import { resolveApiUrl } from './config.js'

const request = axios.create({
  baseURL: resolveApiUrl('/api'),
  timeout: 60000,
})

export default request

import axios from 'axios'

const client = axios.create({ 
  baseURL: '/api',
  timeout: 35000  // 35秒,略大于后端超时时间
})

// initialize token from localStorage if present
const saved = localStorage.getItem('token')
if(saved){ client.defaults.headers.common['Authorization'] = 'Bearer ' + saved }

export default {
  setAuthToken(token){
    if(token){
      client.defaults.headers.common['Authorization'] = 'Bearer ' + token
      localStorage.setItem('token', token)
    } else {
      delete client.defaults.headers.common['Authorization']
      localStorage.removeItem('token')
    }
  },
  async post(path, data){
    const res = await client.post(path, data)
    // backend uses wrapper { code, message, data }
    return res.data && res.data.data ? res.data.data : res.data
  },
  async get(path){
    const res = await client.get(path)
    return res.data && res.data.data ? res.data.data : res.data
  }
}


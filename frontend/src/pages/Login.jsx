import React, {useState} from 'react'
import api from '../api'

export default function Login(){
  const [username,setUsername]=useState('')
  const [password,setPassword]=useState('')
  const [msg,setMsg]=useState('')

  const submit = async (e)=>{
    e.preventDefault()
    try{
      const res = await api.post('/auth/login',{username,password})
      if(res && res.token){
        api.setAuthToken(res.token)
        setMsg('登录成功')
      } else {
        setMsg('登录失败')
      }
    }catch(err){
      setMsg(err?.response?.data?.message || '请求失败')
    }
  }

  return (
    <div className="card" style={{maxWidth:420,margin:'0 auto'}}>
      <h2 style={{marginTop:0}}>登录</h2>
      <form onSubmit={submit}>
        <div style={{marginBottom:8}}>
          <label className="muted">用户名</label>
          <input placeholder="用户名" value={username} onChange={e=>setUsername(e.target.value)} />
        </div>
        <div style={{marginBottom:12}}>
          <label className="muted">密码</label>
          <input type="password" placeholder="密码" value={password} onChange={e=>setPassword(e.target.value)} />
        </div>
        <div style={{display:'flex',gap:8}}>
          <button className="btn btn-primary" type="submit">登录</button>
          <button type="button" className="btn btn-ghost" onClick={()=>{setUsername('');setPassword('');setMsg('')}}>重置</button>
        </div>
      </form>
      <div style={{marginTop:12}} className="muted">{msg}</div>
    </div>
  )
}

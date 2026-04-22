import React, {useEffect, useState} from 'react'
import api from '../api'

export default function UserManagement(){
  const [users,setUsers]=useState([])
  const [loading,setLoading]=useState(true)

  const load = async ()=>{
    setLoading(true)
    try{
      const res = await api.get('/user/page?pageNo=1&pageSize=50')
      setUsers(res?.records || [])
    }catch(e){
      console.error(e)
    }finally{setLoading(false)}
  }

  useEffect(()=>{load()},[])

  return (
    <div className="card">
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
        <h2 style={{margin:0}}>用户管理</h2>
        <div>
          <button className="btn btn-ghost" onClick={load}>{loading? '加载中...':'刷新'}</button>
        </div>
      </div>

      <div style={{marginTop:12}}>
        {loading ? <div className="muted">正在加载...</div> : (
          users.length === 0 ? <div className="muted">无用户</div> : (
            <table className="table">
              <thead>
                <tr><th>姓名</th><th>年龄</th><th>邮箱</th><th>操作</th></tr>
              </thead>
              <tbody>
                {users.map(u=> (
                  <tr key={u.id}>
                    <td>{u.name}</td>
                    <td>{u.age}</td>
                    <td>{u.email}</td>
                    <td><button className="btn btn-ghost" onClick={()=>navigator.clipboard?.writeText(u.id)}>复制ID</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        )}
      </div>
    </div>
  )
}

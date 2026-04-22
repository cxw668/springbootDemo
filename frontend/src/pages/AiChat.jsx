import React, {useState, useRef} from 'react'
import api from '../api'

export default function AiChat(){
  const [message,setMessage]=useState('')
  const [model,setModel]=useState('Qwen/Qwen3.5-4B')
  const [reply,setReply]=useState('')
  const [loading,setLoading]=useState(false)
  const replyRef = useRef()

  const send = async ()=>{
    if(!message.trim()) return
    setLoading(true)
    setReply('') // 清空上次回复
    try{
      const res = await api.post('/ai/chat',{model, message})
      setReply(res?.reply || res?.content || JSON.stringify(res))
      // scroll into view
      setTimeout(()=>replyRef.current?.scrollIntoView({behavior:'smooth'}),100)
    }catch(err){
      const errorMsg = err?.response?.data?.message || '调用失败'
      setReply(`❌ ${errorMsg}`)
    }finally{setLoading(false)}
  }

  return (
    <div className="card">
      <h2>AI 聊天</h2>

      <div style={{marginBottom:12}}>
        <label className="muted">模型</label>
        <select value={model} onChange={e=>setModel(e.target.value)}>
          <option>Qwen/Qwen3.5-4B</option>
          <option>Pro/zai-org/GLM-4.7</option>
        </select>
      </div>

      <div style={{marginBottom:12}}>
        <label className="muted">输入消息</label>
        <textarea value={message} onChange={e=>setMessage(e.target.value)} placeholder="请输入要发送给模型的内容" />
      </div>

      <div style={{display:'flex',gap:8,marginBottom:12}}>
        <button className="btn btn-primary" onClick={send} disabled={loading || !message.trim()}>{loading? '发送中...':'发送'}</button>
        <button className="btn btn-ghost" onClick={()=>setMessage('')}>清空</button>
      </div>

      <div>
        <strong>回复</strong>
        <div className="reply" ref={replyRef} style={{marginTop:8, whiteSpace:'pre-wrap'}}>
          {loading ? (
            <span className="muted">⏳ AI 正在思考中，请稍候...</span>
          ) : (
            reply || <span className="muted">尚无回复</span>
          )}
        </div>
      </div>
    </div>
  )
}

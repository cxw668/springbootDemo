import React, {useState, useRef} from 'react'

export default function AiChat(){
  const [message,setMessage]=useState('')
  const [model,setModel]=useState('Qwen3-8B')
  const [reply,setReply]=useState('')
  const [thinking,setThinking]=useState('')
  const [loading,setLoading]=useState(false)
  const replyRef = useRef()

  // 判断是否为思考模型（会输出 <think> 标签）
  const isThinkingModel = (modelName) => {
    return modelName.includes('DeepSeek-R1')
  }

  // 解析回复内容，分离思考过程和最终答案
  const parseReply = (content) => {
    if (!content) return { thinking: '', answer: content }
    
    // 非思考模型（如 THUDM/GLM）直接返回内容
    if (!isThinkingModel(model)) {
      return { thinking: '', answer: content }
    }
    
    // 思考模型：匹配所有 ...</think> 标签（可能有多个）
    const thinkMatches = [...content.matchAll(/([\s\S]*?)<\/think>/g)]

    if (thinkMatches.length > 0) {
      // 提取所有思考内容，保留原始格式（包括标点符号和空行）
      const thinking = thinkMatches.map((match) => match[1]).join('\n\n')
      // 移除所有 <think> 标签及其内容，获取最终答案
      const answer = content.replace(/[\s\S]*?<\/think>/g, '').trim()
      return { thinking, answer }
    }

    return { thinking: '', answer: content }
  }

  const send = async ()=>{
    if(!message.trim()) return
    setLoading(true)
    setReply('') // 清空上次回复
    setThinking('') // 清空上次思考内容
    
    try{
      // 使用 fetch API 进行流式请求
      const response = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify({model, message})
      })
      
      // 处理错误响应
      if (!response.ok) {
        const errorMsg = `HTTP ${response.status}: ${response.statusText}`
        setReply(`❌ ${errorMsg}`)
        setLoading(false)
        return
      }

      setLoading(false)
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let fullContent = ''
      let pendingChunk = ''

      const appendContent = (eventChunk) => {
        const content = eventChunk
          .split(/\r?\n/)
          .filter((line) => line.startsWith('data:'))
          .map((line) => line.slice(5))
          .join('\n')

        if (!content || content === '[DONE]') {
          return
        }

        fullContent += content
        const { thinking, answer } = parseReply(fullContent)
        setThinking(thinking)
        // 去除回复开头的空白字符
        setReply(answer ? answer.trimStart() : answer)
      }
      
      // 读取流式数据
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          pendingChunk += decoder.decode()
          break
        }
        
        pendingChunk += decoder.decode(value, { stream: true })
        
        // 解析 SSE 格式数据
        // SSE 格式：事件之间以空行分隔，单个事件可包含多行 data:
        const events = pendingChunk.split(/\r?\n\r?\n/)
        pendingChunk = events.pop() ?? ''

        for (const eventChunk of events) {
          appendContent(eventChunk)
        }
      }

      if (pendingChunk.trim()) {
        appendContent(pendingChunk)
      }
      
      // scroll into view
      setTimeout(()=>replyRef.current?.scrollIntoView({behavior:'smooth'}),100)
    }catch(err){
      const errorMsg = err.message || '调用失败'
      setReply(`❌ ${errorMsg}`)
    }finally{
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <h2>AI 聊天</h2>

      <div style={{marginBottom:12}}>
        <label className="muted">模型</label>
        <select value={model} onChange={e=>setModel(e.target.value)}>
          <option>THUDM/GLM-Z1-9B-0414</option>
          <option>deepseek-ai/DeepSeek-R1-Distill-Qwen-7B</option>
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
        <div className="reply" ref={replyRef} style={{marginTop:8}}>
          {loading ? (
            <span className="muted">⏳ AI 正在思考中，请稍候...</span>
          ) : (
            <>
              {thinking && (
                <details style={{marginBottom: 12, padding: 12, background: '#f5f5f5', borderRadius: 6}}>
                  <summary style={{cursor: 'pointer', fontWeight: 500, color: '#666'}}>💭 思考过程（点击展开/收起）</summary>
                  <div style={{marginTop: 8, whiteSpace: 'pre-wrap', fontSize: 14, color: '#555'}}>
                    {thinking}
                  </div>
                </details>
              )}
              {reply ? (
                <div style={{whiteSpace: 'pre-wrap'}}>{reply}</div>
              ) : (
                thinking
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}

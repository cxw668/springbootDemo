import React from 'react'
import { Routes, Route, Link } from 'react-router-dom'
import Login from './pages/Login'
import AiChat from './pages/AiChat'
import UserManagement from './pages/UserManagement'
import './styles.css'

export default function App() {
  return (
    <div className="container">
      <header className="header">
        <h1 style={{margin:0}}>项目后台面板</h1>
        <nav>
          <Link className="nav-link" to="/login">登录</Link>
          <Link className="nav-link" to="/ai">AI 聊天</Link>
          <Link className="nav-link" to="/users">用户管理</Link>
        </nav>
      </header>

      <main style={{marginTop:12}}>
        <Routes>
          <Route path="/login" element={<Login/>} />
          <Route path="/ai" element={<AiChat/>} />
          <Route path="/users" element={<UserManagement/>} />
          <Route path="/" element={<Login/>} />
        </Routes>
      </main>
    </div>
  )
}

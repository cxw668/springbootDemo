Frontend quickstart

1. cd frontend
2. npm install
3. npm run dev

Notes:
- The frontend expects backend at same origin or configure a proxy in vite.config.js
- Set Authorization header after login; login calls /auth/login and stores token via api.setAuthToken
- Configure SILICONFLOW_API_KEY in backend environment before running backend

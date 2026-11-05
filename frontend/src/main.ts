import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { setSessionProblemHandler } from './api/http'
import { clearSession } from './session'

// Sessão terminada ou palavra-passe temporária em qualquer pedido (docs/ui/README.md, secção 10).
setSessionProblemHandler((problem) => {
  if (problem.code === 'PASSWORD_CHANGE_REQUIRED') {
    void router.push({ name: 'set-password' })
    return
  }
  clearSession()
  void router.push({ name: 'login', query: { sessao: 'terminou' } })
})

const app = createApp(App)

app.use(router)

app.mount('#app')

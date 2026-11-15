import { createRouter, createWebHistory } from 'vue-router'
import type { Role } from '@/api/types'
import { consumeSessionExpired, loadSession } from '@/session'
import LoginView from '@/views/LoginView.vue'
import SetPasswordView from '@/views/SetPasswordView.vue'
import DashboardView from '@/views/DashboardView.vue'
import ComingSoonView from '@/views/ComingSoonView.vue'
import ProjectsView from '@/views/ProjectsView.vue'
import ProjectDetailView from '@/views/ProjectDetailView.vue'
import ProjectFormView from '@/views/ProjectFormView.vue'
import TaskDetailView from '@/views/TaskDetailView.vue'
import TaskFormView from '@/views/TaskFormView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    /** Página que não exige sessão (só a entrada, SEG01). */
    public?: boolean
    /** Papéis que veem a página no menu (DA21). Sem valor: todos. */
    roles?: Role[]
    title?: string
    /** Item do menu que fica ativo nesta página (por exemplo, o detalhe de um projeto fica em Projetos). */
    section?: string
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/entrar', name: 'login', component: LoginView, meta: { public: true, title: 'Entrar' } },
    {
      path: '/definir-palavra-passe',
      name: 'set-password',
      component: SetPasswordView,
      meta: { title: 'Definir palavra-passe nova' },
    },
    { path: '/', name: 'home', component: DashboardView, meta: { title: 'Painel' } },
    { path: '/projetos', name: 'projects', component: ProjectsView, meta: { title: 'Projetos' } },
    {
      path: '/projetos/novo',
      name: 'project-new',
      component: ProjectFormView,
      meta: { title: 'Novo projeto', section: 'projects', roles: ['ADMINISTRATOR', 'MANAGER'] },
    },
    {
      path: '/projetos/:id(\\d+)',
      name: 'project',
      component: ProjectDetailView,
      meta: { title: 'Projeto', section: 'projects' },
    },
    {
      path: '/projetos/:id(\\d+)/editar',
      name: 'project-edit',
      component: ProjectFormView,
      meta: { title: 'Editar projeto', section: 'projects' },
    },
    {
      path: '/tarefas/:id(\\d+)',
      name: 'task',
      component: TaskDetailView,
      meta: { title: 'Tarefa', section: 'projects' },
    },
    {
      path: '/tarefas/:id(\\d+)/editar',
      name: 'task-edit',
      component: TaskFormView,
      meta: { title: 'Editar tarefa', section: 'projects' },
    },
    {
      path: '/minhas-tarefas',
      name: 'my-tasks',
      component: ComingSoonView,
      meta: { title: 'As minhas tarefas', roles: ['MANAGER', 'MEMBER'] },
    },
    { path: '/equipas', name: 'teams', component: ComingSoonView, meta: { title: 'Equipas' } },
    {
      path: '/contas',
      name: 'accounts',
      component: ComingSoonView,
      meta: { title: 'Contas', roles: ['ADMINISTRATOR'] },
    },
    { path: '/:pathMatch(.*)*', redirect: { name: 'home' } },
  ],
})

// Guarda de rotas: sem sessão vai para a entrada; com palavra-passe temporária,
// só a página de definir a nova (SEG01, DA25). A proteção que conta é a do backend (SEG02).
router.beforeEach(async (to) => {
  const user = await loadSession()
  if (to.meta.public) {
    if (!user) {
      return true
    }
    return user.mustChangePassword ? { name: 'set-password' } : { name: 'home' }
  }
  if (!user) {
    return consumeSessionExpired() ? { name: 'login', query: { sessao: 'terminou' } } : { name: 'login' }
  }
  if (user.mustChangePassword && to.name !== 'set-password') {
    return { name: 'set-password' }
  }
  if (!user.mustChangePassword && to.name === 'set-password') {
    return { name: 'home' }
  }
  if (to.meta.roles && !to.meta.roles.includes(user.role)) {
    return { name: 'home' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · Matriz` : 'Matriz'
})

export default router

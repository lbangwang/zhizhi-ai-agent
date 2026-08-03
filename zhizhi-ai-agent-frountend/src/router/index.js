import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import LoveMaster from '../views/LoveMaster.vue'
import SuperAgent from '../views/SuperAgent.vue'
import Login from '../views/Login.vue'
import Knowledge from '../views/Knowledge.vue'
import { isLoggedIn } from '../utils/auth.js'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { guestOnly: true },
  },
  {
    path: '/love-master',
    name: 'LoveMaster',
    component: LoveMaster,
    meta: { requiresAuth: true },
  },
  {
    path: '/super-agent',
    name: 'SuperAgent',
    component: SuperAgent,
    meta: { requiresAuth: true },
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: Knowledge,
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const loggedIn = isLoggedIn()
  if (to.meta.requiresAuth && !loggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }
  if (to.meta.guestOnly && loggedIn) {
    return { path: '/' }
  }
  return true
})

export default router

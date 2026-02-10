import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../views/LoginPage.vue'
import HomePage from '../views/HomePage.vue'
import UserManagementPage from '../views/UserManagementPage.vue'
import ProfileSettingsPage from '../views/ProfileSettingsPage.vue'
import { getAccessToken, getUser } from '../utils/auth'
import ChannelManagementPage from '../views/ChannelManagementPage.vue'
import ChannelConfigPage from '../views/ChannelConfigPage.vue'
import { hasPermission, CHANNEL_PERMISSIONS } from '../utils/permissions'

const routes = [
  {
    path: '/login',
    component: LoginPage,
    meta: { public: true }
  },
  {
    path: '/',
    component: HomePage
  },
  {
    path: '/users',
    component: UserManagementPage,
    meta: { requiresAdmin: true }
  },
  {
    path: '/settings',
    component: ProfileSettingsPage
  },
  {
    path: '/channels',
    component: ChannelManagementPage,
    meta: { requiresPermission: CHANNEL_PERMISSIONS.view }
  },
  {
    path: '/channels/new',
    component: ChannelConfigPage,
    meta: { requiresPermission: CHANNEL_PERMISSIONS.create }
  },
  {
    path: '/channels/:id',
    component: ChannelConfigPage,
    meta: { requiresPermission: CHANNEL_PERMISSIONS.edit }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  const token = getAccessToken()
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin) {
    const user = getUser()
    if (!user || user.role !== 'ADMIN') {
      return { path: '/' }
    }
  }
  if (to.meta.requiresPermission) {
    const user = getUser()
    if (!hasPermission(user, to.meta.requiresPermission)) {
      return { path: '/', query: { denied: 'channel' } }
    }
  }
  return true
})

export default router

import { createRouter, createWebHashHistory } from 'vue-router'

// 路由与后端 sys_menu.path 一一对应；菜单由角色动态返回，前端只做兜底
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'intake/desk', component: () => import('../views/IntakeDesk.vue') },
      { path: 'intake/new', component: () => import('../views/IntakeNew.vue') },
      { path: 'batch/list', component: () => import('../views/BatchList.vue') },
      { path: 'batch/:id', component: () => import('../views/BatchDetail.vue') },
      { path: 'shelf/map', component: () => import('../views/ShelfMap.vue') },
      { path: 'parcel/list', component: () => import('../views/ParcelList.vue') },
      { path: 'parcel/:id', component: () => import('../views/ParcelDetail.vue') },
      { path: 'stocktake', component: () => import('../views/Stocktake.vue') },
      { path: 'notify', component: () => import('../views/Notify.vue') },
      { path: 'pickup', component: () => import('../views/Pickup.vue') },
      { path: 'retention', component: () => import('../views/Retention.vue') },
      { path: 'return', component: () => import('../views/ReturnList.vue') },
      { path: 'exception', component: () => import('../views/ExceptionList.vue') },
      { path: 'rule', component: () => import('../views/RuleConfig.vue') },
      { path: 'system/user', component: () => import('../views/system/UserList.vue') },
      { path: 'system/role', component: () => import('../views/system/RoleList.vue') },
      { path: 'system/dict', component: () => import('../views/system/DictList.vue') },
      { path: 'system/log', component: () => import('../views/system/LogList.vue') },
      { path: 'message', component: () => import('../views/Message.vue') },
      { path: 'profile', component: () => import('../views/Profile.vue') },
      { path: 'portal', component: () => import('../views/Portal.vue') },
      { path: 'courier', component: () => import('../views/CourierDesk.vue') },
      { path: 'review', component: () => import('../views/ReviewList.vue') },
      { path: 'mobile', component: () => import('../views/Mobile.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/dashboard'
  return true
})

export default router

<template>
  <div class="shell">
    <aside class="side" :class="{ collapsed }">
      <div class="brand">
        <span class="logo">驿</span>
        <div v-show="!collapsed" class="brand-text">
          <b>驿站作业平台</b>
          <i>PARCEL STATION</i>
        </div>
      </div>
      <el-scrollbar class="menus">
        <el-menu :default-active="$route.path" :collapse="collapsed" :collapse-transition="false"
                 background-color="#0F3B5D" text-color="#C7D8E6" active-text-color="#fff" router unique-opened>
          <template v-for="m in menus" :key="m.id">
            <el-sub-menu v-if="m.children && m.children.length" :index="'g' + m.id">
              <template #title>
                <el-icon><component :is="iconOf(m.icon)" /></el-icon>
                <span>{{ m.name }}</span>
              </template>
              <el-menu-item v-for="c in m.children" :key="c.id" :index="c.path">
                <el-icon><component :is="iconOf(c.icon)" /></el-icon>
                <span>{{ c.name }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="m.path">
              <el-icon><component :is="iconOf(m.icon)" /></el-icon>
              <span>{{ m.name }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ roleName }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="right">
          <el-tooltip content="站内消息" placement="bottom">
            <el-badge :value="msgCount" :hidden="!msgCount" class="bell" @click="$router.push('/message')">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </el-tooltip>
          <el-dropdown @command="onCommand">
            <span class="user">
              <img class="avatar-dot" :src="user.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
              <span class="uname">{{ user.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="mobile">移动端视图</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <section class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { me, logout, unread } from '../api'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const menus = ref([])
const user = ref({})
const msgCount = ref(0)

// 后端返回英文图标名，映射到 Element Plus 图标组件
const ICONS = {
  grid: 'Grid', truck: 'Van', box: 'Box', file: 'Document', check: 'Select',
  bell: 'Bell', alert: 'Warning', settings: 'Setting', users: 'UserFilled',
  shield: 'Lock', user: 'User', clock: 'Timer', clipboard: 'Tickets', plus: 'Plus'
}
const iconOf = (n) => ICONS[n] || 'Menu'

const roleName = computed(() => user.value.roleName || '驿站员工')
const currentTitle = computed(() => {
  const all = menus.value.flatMap((m) => (m.children && m.children.length ? m.children : [m]))
  return all.find((m) => m.path === route.path)?.name || '工作台'
})

function pickUser() {
  try {
    const cached = localStorage.getItem('user')
    if (cached && cached !== 'undefined') return JSON.parse(cached)
  } catch (e) { /* 登录态损坏时回落到接口 */ }
  return {}
}

onMounted(async () => {
  user.value = pickUser()
  try {
    const d = await me()
    if (d) {
      // 接口直接返回用户对象（含 menus），不是 {user, menus}
      user.value = d
      menus.value = d.menus || []
      localStorage.setItem('user', JSON.stringify({ ...d, menus: undefined }))
    }
  } catch (e) { /* 401 已由拦截器处理 */ }
  try {
    const r = await unread()
    msgCount.value = r?.count || 0
  } catch (e) { /* 忽略角标失败 */ }
})

async function onCommand(cmd) {
  if (cmd === 'logout') {
    try { await logout() } catch (e) { /* 忽略 */ }
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'mobile') {
    router.push('/mobile')
  }
}
</script>

<style scoped>
.shell { display: flex; height: 100vh; overflow: hidden; background: var(--bg); }
.side { width: 220px; background: #0F3B5D; display: flex; flex-direction: column; transition: width .18s; flex: 0 0 auto; }
.side.collapsed { width: 64px; }
.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 16px; background: #0B2F4B; flex: 0 0 auto; }
.logo { width: 28px; height: 28px; border-radius: 6px; background: #2878B5; color: #fff; display: grid; place-items: center; font-weight: 700; flex: 0 0 auto; }
.brand-text b { display: block; color: #fff; font-size: 13px; letter-spacing: .5px; }
.brand-text i { color: #7FA3BE; font-style: normal; font-size: 9px; letter-spacing: 1.2px; }
.menus { flex: 1 1 auto; overflow: hidden; }
.menus :deep(.el-menu) { border-right: none; }
.menus :deep(.el-menu-item), .menus :deep(.el-sub-menu__title) { height: 40px; line-height: 40px; font-size: 13px; }
.menus :deep(.el-menu-item.is-active) { background: #2878B5 !important; }
.main { flex: 1 1 auto; display: flex; flex-direction: column; min-width: 0; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid var(--line); display: flex; align-items: center; justify-content: space-between; padding: 0 18px; flex: 0 0 auto; }
.topbar .left { display: flex; align-items: center; gap: 14px; }
.collapse-btn { cursor: pointer; color: #4A6274; }
.topbar .right { display: flex; align-items: center; gap: 22px; }
.bell { cursor: pointer; color: #4A6274; display: flex; }
.user { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.uname { font-size: 13px; color: #22384A; }
.content { flex: 1 1 auto; overflow-y: auto; min-height: 0; }

@media (max-width: 767px) {
  .side { position: fixed; z-index: 2000; height: 100%; transform: translateX(-100%); transition: transform .2s; }
  .side.collapsed { transform: translateX(0); width: 220px; }
  .topbar :deep(.el-breadcrumb) { display: none; }
  .uname { display: none; }
}
</style>

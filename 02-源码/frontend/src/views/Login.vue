<template>
  <div class="login-wrap">
    <!-- 左侧实景视觉区：驿站库房照片 + 深色遮罩，避免模板化渐变 -->
    <div class="login-visual">
      <div class="login-brand">
        <span class="brand-mark">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M3.5 8.5 12 4l8.5 4.5v7L12 20l-8.5-4.5z" stroke-linejoin="round" />
            <path d="M3.5 8.5 12 13l8.5-4.5M12 13v7" stroke-linejoin="round" />
          </svg>
        </span>
        <span class="brand-text">
          驿通 · 智能驿站作业平台
          <em>PARCEL STATION OPERATION</em>
        </span>
      </div>

      <div class="login-copy">
        <h1>从到站交接<br />到取件核验的<br />一条完整链路</h1>
        <p>批次交接、入库上架、取件码通知、本人与代取核验、滞留催取、退件交接、异常与盘点，统一在一张站内台账上完成，每一次状态流转都留痕可查。</p>
        <div class="login-tags">
          <span>批次交接</span><span>货位推荐</span><span>取件核验</span><span>滞留预警</span>
        </div>
      </div>

      <div class="login-metrics">
        <div><b>433</b><span>在库台账快件</span></div>
        <div><b>20</b><span>在用货架</span></div>
        <div><b>6</b><span>滞留预警因子</span></div>
      </div>
    </div>

    <!-- 右侧登录面板 -->
    <div class="login-panel">
      <div class="login-form">
        <div class="form-head">
          <div class="form-dot"></div>
          <h2>登录作业台</h2>
        </div>
        <div class="tip">请使用站点分配的账号登录，连续失败 5 次将锁定 10 分钟</div>
        <el-form :model="form" @submit.prevent="submit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                      show-password :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button class="submit" type="primary" :loading="loading" @click="submit">登 录</el-button>
        </el-form>
        <div class="login-accounts">
          <div class="acc-title">演示账号</div>
          <div class="acc-grid">
            <span><b>admin</b> / admin123 <em>系统管理员</em></span>
            <span><b>manager01</b> / 123456 <em>站点负责人</em></span>
            <span><b>staff01</b> / 123456 <em>驿站员工</em></span>
            <span><b>courier01</b> / 123456 <em>快递员</em></span>
            <span><b>cust01</b> / 123456 <em>收件人</em></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../api'

const router = useRouter()
const loading = ref(false)
const form = ref({ username: 'admin', password: 'admin123' })

async function submit() {
  if (!form.value.username || !form.value.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    // 接口返回 { token, user:{...}, menus:[...] }，用户信息在 user 里，不要按扁平字段取
    const d = await login(form.value)
    const u = d.user || {}
    localStorage.setItem('token', d.token)
    localStorage.setItem('user', JSON.stringify({
      id: u.id, username: u.username, realName: u.realName,
      avatar: u.avatar, roleCode: u.roleCode, roleName: u.roleName,
      orgId: u.orgId, orgName: u.orgName
    }))
    ElMessage.success('登录成功')
    // 按角色分流：收件人进门户、快递员进交接台、其余进看板
    const rc = u.roleCode
    router.push(rc === 'CUSTOMER' ? '/portal' : rc === 'COURIER' ? '/courier' : '/dashboard')
  } catch (e) {
    // 拦截器已提示，这里保留表单状态
  } finally {
    loading.value = false
  }
}
</script>

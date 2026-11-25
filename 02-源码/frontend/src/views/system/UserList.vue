<template>
  <div class="page">
    <div class="page-head">
      <div><h2>用户管理</h2><div class="sub">账号、角色与所属站点；密码使用 BCrypt 保存，任何接口不返回密码</div></div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="头像" width="70">
          <template #default="{ row }"><img class="avatar-dot" :src="row.avatar || '/uploads/seed/avatar/01.jpg'" alt="" /></template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="90" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="roleName" label="角色" min-width="110" />
        <el-table-column prop="orgName" label="所属组织" min-width="150" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="row.status === 1 ? 'tag-ok' : 'tag-muted'">{{ row.status === 1 ? '启用' : '停用' }}</span></template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="150"><template #default="{ row }">{{ fmtTime(row.lastLoginTime) }}</template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="q.page" :page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { staffPage } from '../../api'
import { fmtTime } from '../../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ page: 1, size: 10 })
async function load() { loading.value = true; try { const d = await staffPage(q); rows.value = d.records; total.value = d.total } finally { loading.value = false } }
onMounted(load)
</script>

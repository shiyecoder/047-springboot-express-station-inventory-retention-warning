<template>
  <div class="page">
    <div class="page-head">
      <div><h2>角色管理</h2><div class="sub">角色决定菜单可见范围与数据范围（全部 / 本站 / 本人）</div></div>
    </div>
    <div class="card">
      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="roleCode" label="角色编码" min-width="110" />
        <el-table-column prop="roleName" label="角色名称" min-width="130" />
        <el-table-column label="数据范围" min-width="130">
          <template #default="{ row }">
            <span class="tag tag-info">{{ scopeText(row.dataScope) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="260" show-overflow-tooltip />
      </el-table>
    </div>
    <div class="card">
      <h3 class="card-title">菜单权限（按角色动态下发）</h3>
      <div class="menus">
        <div v-for="m in menus" :key="m.id" class="mgroup">
          <b>{{ m.name }}</b>
          <span v-for="c in m.children || []" :key="c.id" class="mtag">{{ c.name }}</span>
          <span v-if="!(m.children || []).length" class="mtag">{{ m.path }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { roleList, menuList } from '../../api'

const roles = ref([]); const menus = ref([]); const loading = ref(false)
function scopeText(s) { return s === 'ALL' ? '全部数据' : s === 'ORG' ? '本站数据' : '本人数据' }
onMounted(async () => {
  loading.value = true
  try { roles.value = await roleList(); menus.value = await menuList() } finally { loading.value = false }
})
</script>

<style scoped>
.menus { display: flex; flex-direction: column; gap: 10px; }
.mgroup { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; font-size: 12px; }
.mgroup b { width: 90px; color: #33475B; }
.mtag { padding: 2px 9px; border-radius: 10px; background: #EBF3F8; color: #245A7A; border: 1px solid #CBE0ED; }
</style>

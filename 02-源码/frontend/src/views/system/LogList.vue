<template>
  <div class="page">
    <div class="page-head">
      <div><h2>操作日志</h2><div class="sub">快件状态变化、导出与审批均留痕，日志不可由普通用户修改</div></div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="userName" label="操作人" min-width="100" />
        <el-table-column prop="module" label="模块" min-width="110" />
        <el-table-column prop="action" label="动作" min-width="100" />
        <el-table-column prop="method" label="接口" min-width="220" show-overflow-tooltip />
        <el-table-column prop="params" label="参数" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" min-width="120" />
        <el-table-column label="耗时" min-width="80"><template #default="{ row }"><span class="num">{{ row.costMs }}ms</span></template></el-table-column>
        <el-table-column label="时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.createTime) }}</template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="q.page" :page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import http from '../../api'
import { fmtTime } from '../../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ page: 1, size: 10 })
async function load() { loading.value = true; try { const d = await http.get('/system/log/page', { params: q }); rows.value = d.records; total.value = d.total } finally { loading.value = false } }
onMounted(load)
</script>

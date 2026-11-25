<template>
  <div class="page">
    <div class="page-head">
      <div><h2>字典管理</h2><div class="sub">状态、件型、异常类型等取值统一维护，前端下拉只允许提交已定义值</div></div>
    </div>
    <div class="card">
      <el-table :data="rows" stripe>
        <el-table-column prop="dictType" label="字典类型" min-width="140" />
        <el-table-column prop="dictLabel" label="显示名" min-width="140" />
        <el-table-column prop="dictValue" label="取值" min-width="120" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { options } from '../../api'

const rows = ref([])
onMounted(async () => {
  const o = await options()
  rows.value = o.dicts || []
})
</script>

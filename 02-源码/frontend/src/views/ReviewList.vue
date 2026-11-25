<template>
  <div class="page">
    <div class="page-head">
      <div><h2>服务评价</h2><div class="sub">收件人对驿站服务的反馈，回复后状态置为已回复</div></div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column prop="receiverName" label="评价人" min-width="90" />
        <el-table-column label="评分" min-width="130">
          <template #default="{ row }"><el-rate :model-value="row.score" disabled size="small" /></template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="reply" label="回复" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="90">
          <template #default="{ row }"><el-button v-if="row.status !== '已回复'" link type="primary" @click="reply(row)">回复</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="q.page" :page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewPage, replyReview } from '../api'
import { tagType } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ page: 1, size: 10 })
async function load() { loading.value = true; try { const d = await reviewPage(q); rows.value = d.records; total.value = d.total } finally { loading.value = false } }
async function reply(row) {
  const { value } = await ElMessageBox.prompt('回复内容', '回复评价')
  await replyReview(row.id, { reply: value })
  ElMessage.success('已回复')
  load()
}
onMounted(load)
</script>

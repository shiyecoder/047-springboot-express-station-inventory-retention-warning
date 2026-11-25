<template>
  <div class="page">
    <div class="page-head">
      <div><h2>取件通知</h2><div class="sub">记录电话、站内消息等通知结果，不强制集成短信服务商</div></div>
    </div>
    <div class="card">
      <div class="filter-row" style="margin-bottom: 12px">
        <el-select v-model="q.status" placeholder="通知结果" clearable>
          <el-option label="已通知" value="已通知" /><el-option label="未接通" value="未接通" /><el-option label="已催取" value="已催取" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column prop="receiverName" label="收件人" min-width="90" />
        <el-table-column prop="notifyType" label="通知方式" min-width="100" />
        <el-table-column prop="content" label="通知内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="result" label="结果" min-width="90" />
        <el-table-column prop="operatorName" label="经办人" min-width="90" />
        <el-table-column label="通知时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.notifyTime) }}</template></el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]"
                       layout="total, sizes, prev, pager, next" @current-change="load" @size-change="search" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { notifyPage } from '../api'
import { fmtTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ status: '', page: 1, size: 10 })
async function load() { loading.value = true; try { const d = await notifyPage(q); rows.value = d.records; total.value = d.total } finally { loading.value = false } }
function search() { q.page = 1; load() }
onMounted(load)
</script>

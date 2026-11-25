<template>
  <div class="page">
    <div class="page-head">
      <div><h2>我的交接批次</h2><div class="sub">快递员仅可查看本人批次与退件接收，不接受跨人查询</div></div>
    </div>
    <div class="kpi-row" style="grid-template-columns: repeat(3, 1fr); flex: none; margin-bottom: 14px">
      <div class="kpi"><span class="label">本人在交接批次</span><span class="value num">{{ rows.length }}</span></div>
      <div class="kpi"><span class="label">累计交接件数</span><span class="value num">{{ totalParcels }}</span></div>
      <div class="kpi"><span class="label">待接收退件</span><span class="value num" style="color:#96601A">{{ pendingReturn }}</span></div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="companyName" label="快递公司" min-width="110" />
        <el-table-column label="到站时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.arriveTime) }}</template></el-table-column>
        <el-table-column prop="totalCount" label="交接总数" min-width="90" />
        <el-table-column prop="receivedCount" label="接收" min-width="80" />
        <el-table-column prop="rejectedCount" label="拒收" min-width="80" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="90">
          <template #default="{ row }"><el-button link type="primary" @click="$router.push('/batch/' + row.id)">明细</el-button></template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card">
      <h3 class="card-title">待接收退件</h3>
      <el-table :data="returns" size="small">
        <el-table-column prop="applyNo" label="申请单号" min-width="150" />
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column prop="reasonType" label="原因" min-width="100" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { batchPage, returnPage } from '../api'
import { fmtTime, tagType } from '../utils/format'

const rows = ref([]); const returns = ref([]); const loading = ref(false)
const totalParcels = computed(() => rows.value.reduce((s, r) => s + (r.receivedCount || 0), 0))
const pendingReturn = computed(() => returns.value.filter((r) => r.status === '待交接').length)

onMounted(async () => {
  loading.value = true
  try {
    const d = await batchPage({ page: 1, size: 20 })
    rows.value = d.records
    const r = await returnPage({ status: '待交接', page: 1, size: 10 })
    returns.value = r.records
  } finally { loading.value = false }
})
</script>

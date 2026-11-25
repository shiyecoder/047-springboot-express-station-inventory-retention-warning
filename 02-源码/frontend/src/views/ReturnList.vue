<template>
  <div class="page">
    <div class="page-head">
      <div><h2>退件与交接</h2><div class="sub">负责人审批后进入待交接，员工与快递员完成数量核对并上传凭证</div></div>
    </div>
    <div class="card">
      <div class="filter-row" style="margin-bottom: 12px">
        <el-select v-model="q.status" placeholder="申请状态" clearable>
          <el-option v-for="s in ['待审批', '待交接', '已交接', '已驳回']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="applyNo" label="申请单号" min-width="150" />
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column prop="reasonType" label="退件原因" min-width="100" />
        <el-table-column prop="reason" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="applyUserName" label="申请人" min-width="90" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待审批'" link type="primary" @click="approve(row, true)">通过</el-button>
            <el-button v-if="row.status === '待审批'" link type="danger" @click="approve(row, false)">驳回</el-button>
            <el-button v-if="row.status === '待交接'" link type="primary" @click="handover(row)">登记交接</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="search" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { returnPage, approveReturn, handoverReturn, options } from '../api'
import { fmtTime, tagType } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ status: '', page: 1, size: 10 })
const opts = ref({ couriers: [] })

async function load() { loading.value = true; try { const d = await returnPage(q); rows.value = d.records; total.value = d.total } finally { loading.value = false } }
function search() { q.page = 1; load() }

async function approve(row, pass) {
  let reason = ''
  if (!pass) {
    const r = await ElMessageBox.prompt('驳回原因（不少于 5 个字）', '驳回退件申请', {
      inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字'
    })
    reason = r.value
  } else {
    await ElMessageBox.confirm('通过后快件将置为「待退回」，确认？', '审批通过', { type: 'warning' })
  }
  await approveReturn(row.id, { pass, reason })
  ElMessage.success(pass ? '已通过，快件进入待退回' : '已驳回')
  load()
}
async function handover(row) {
  const { value } = await ElMessageBox.prompt('实际交接数量', '登记退件交接 · ' + row.applyNo, { inputValue: '1' })
  await handoverReturn({ applyId: row.id, courierId: opts.value.couriers?.[0]?.id, planCount: 1, actualCount: Number(value), receiverName: opts.value.couriers?.[0]?.courierName, remark: '' })
  ElMessage.success('交接已登记，快件置为已退件')
  load()
}
onMounted(async () => { opts.value = await options(); load() })
</script>

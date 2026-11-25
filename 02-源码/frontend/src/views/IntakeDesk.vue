<template>
  <div class="page">
    <div class="page-head">
      <div><h2>扫码入库台</h2><div class="sub">待上架快件优先处理，确认货架后生成取件码</div></div>
      <el-button type="primary" :icon="Plus" @click="$router.push('/intake/new')">单件快速入库</el-button>
    </div>
    <div class="kpi-row" style="grid-template-columns: repeat(4, 1fr); flex: none; margin-bottom: 14px">
      <div class="kpi"><span class="label">今日入库</span><span class="value num">{{ d.todayCount }}<small>件</small></span></div>
      <div class="kpi"><span class="label">待上架</span><span class="value num" style="color:#96601A">{{ d.waitShelf }}<small>件</small></span></div>
      <div class="kpi"><span class="label">可用货位余量</span><span class="value num">{{ d.shelfFree }}<small>位</small></span></div>
      <div class="kpi"><span class="label">今日交接批次</span><span class="value num">{{ d.batchToday }}<small>批</small></span></div>
    </div>
    <div class="card">
      <h3 class="card-title">待上架队列</h3>
      <el-table :data="d.pending" stripe v-loading="loading">
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column prop="companyName" label="快递公司" min-width="100" />
        <el-table-column prop="receiverName" label="收件人" min-width="90" />
        <el-table-column prop="itemType" label="件型" min-width="80" />
        <el-table-column label="到站时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.arriveTime) }}</template></el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/parcel/' + row.id)">查看</el-button>
            <el-button link type="primary" @click="shelve(row)">确认上架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !(d.pending || []).length" description="当前没有待上架快件，可去单件快速入库新建" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { intakeDesk, transition } from '../api'
import { fmtTime } from '../utils/format'

const d = ref({ pending: [] })
const loading = ref(false)
async function load() { loading.value = true; try { d.value = await intakeDesk() } finally { loading.value = false } }
async function shelve(row) {
  await transition(row.id, { target: '在库', reason: '入库员确认货架，完成上架作业' })
  ElMessage.success('已上架并生成取件码')
  load()
}
onMounted(load)
</script>

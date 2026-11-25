<template>
  <div class="page">
    <div class="page-head">
      <div><h2>盘点任务</h2><div class="sub">差异不能直接删除快件，须经异常复核后调整状态或位置</div></div>
      <div>
        <el-button @click="tab = 'move'">移库记录</el-button>
        <el-button type="primary" :icon="Plus" @click="create">新建盘点任务</el-button>
      </div>
    </div>

    <div class="card" v-if="tab === 'task'">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="taskNo" label="任务号" min-width="150" />
        <el-table-column label="范围" min-width="90"><template #default="{ row }">{{ row.shelfIds === 'ALL' ? '全站' : '货架 ' + row.shelfIds }}</template></el-table-column>
        <el-table-column prop="planCount" label="账面件数" min-width="90" />
        <el-table-column prop="actualCount" label="实盘件数" min-width="90" />
        <el-table-column prop="profitCount" label="盘盈" min-width="70" />
        <el-table-column prop="lossCount" label="盘亏" min-width="70" />
        <el-table-column prop="mismatchCount" label="错位" min-width="70" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button v-if="row.status === '进行中'" link type="primary" @click="finish(row)">完成盘点</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="q.page" :page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" /></div>
    </div>

    <div class="card" v-else>
      <el-table :data="moves" stripe>
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column label="原货架" min-width="90"><template #default="{ row }">{{ row.fromShelfNo || '—' }}</template></el-table-column>
        <el-table-column prop="toShelfNo" label="目标货架" min-width="90" />
        <el-table-column prop="reason" label="移库原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="经办人" min-width="90" />
        <el-table-column label="时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.createTime) }}</template></el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="drawer" :title="'盘点明细 · ' + (cur.taskNo || '')" size="720px">
      <el-table :data="cur.items" size="small">
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column prop="shelfNo" label="货架" width="80" />
        <el-table-column prop="systemCount" label="账面" width="70" />
        <el-table-column prop="actualCount" label="实盘" width="70" />
        <el-table-column label="差异类型" width="100">
          <template #default="{ row }"><span class="tag" :class="row.diffType === '无差异' ? 'tag-ok' : 'tag-warn'">{{ row.diffType }}</span></template>
        </el-table-column>
        <el-table-column prop="remark" label="复核说明" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="90">
          <template #default="{ row }"><el-button link type="primary" @click="check(row)">核对</el-button></template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { stocktakePage, stocktakeDetail, createStocktake, checkStocktakeItem, finishStocktake, movePage } from '../api'
import { fmtTime, tagType } from '../utils/format'

const tab = ref('task')
const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ page: 1, size: 10 })
const moves = ref([])
const drawer = ref(false); const cur = ref({})

async function load() {
  loading.value = true
  try { const d = await stocktakePage(q); rows.value = d.records; total.value = d.total } finally { loading.value = false }
  const m = await movePage({ page: 1, size: 10 }); moves.value = m.records
}
async function create() {
  const d = await createStocktake({ stationId: 1 })
  ElMessage.success('盘点任务已创建，共 ' + 0 + ' 条明细，请进入明细核对')
  load()
  openDetail({ id: d })
}
async function openDetail(row) { cur.value = await stocktakeDetail(row.id); drawer.value = true }
async function check(row) {
  const { value } = await ElMessageBox.prompt('实盘数量', '核对 · ' + row.waybillNo, { inputValue: String(row.actualCount ?? 1) })
  const actual = Number(value)
  const diff = actual === row.systemCount ? '无差异' : actual > row.systemCount ? '盘盈' : '盘亏'
  const remark = diff === '无差异' ? '' : '现场复核：' + (await ElMessageBox.prompt('复核说明', '差异说明')).value
  await checkStocktakeItem(row.id, { actualCount: actual, diffType: diff, remark })
  ElMessage.success('已记录核对结果')
  openDetail({ id: cur.value.id })
}
async function finish(row) { await finishStocktake(row.id); ElMessage.success('盘点已完成'); load() }
onMounted(load)
</script>

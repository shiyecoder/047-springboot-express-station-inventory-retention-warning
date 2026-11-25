<template>
  <div class="page">
    <div class="page-head">
      <div><h2>快件台账</h2><div class="sub">全部到站快件的主档与流转状态，改动均留操作日志</div></div>
      <div>
        <el-button :icon="Download" @click="doExport">导出当前筛选</el-button>
        <el-button type="primary" :icon="Plus" @click="$router.push('/intake/new')">新建入库</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filter-row">
        <el-input v-model="q.keyword" placeholder="运单号/收件人/取件码" clearable @keyup.enter="search" />
        <el-select v-model="q.companyId" placeholder="快递公司" clearable>
          <el-option v-for="c in opts.companies" :key="c.id" :label="c.companyName" :value="c.id" />
        </el-select>
        <el-select v-model="q.status" placeholder="快件状态" clearable>
          <el-option v-for="s in opts.parcelStatus" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="q.shelfNo" placeholder="货架" clearable filterable>
          <el-option v-for="s in opts.shelves" :key="s.id" :label="s.shelfNo" :value="s.shelfNo" />
        </el-select>
        <el-date-picker v-model="range" type="daterange" start-placeholder="入库起" end-placeholder="入库止"
                        value-format="YYYY-MM-DD" style="width: 240px" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <div class="desktop-table">
        <el-table :data="rows" v-loading="loading" stripe style="width: 100%">
          <el-table-column label="包裹照片" width="86">
            <template #default="{ row }">
              <el-image class="thumb" :src="row.coverImage" :preview-src-list="[row.coverImage]"
                        preview-teleported fit="cover" lazy>
                <template #error><div class="thumb-empty">无图</div></template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="waybillNo" label="运单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="companyName" label="快递公司" min-width="100" />
          <el-table-column prop="receiverName" label="收件人" min-width="118">
            <template #default="{ row }">
              <div>{{ row.receiverName }}</div>
              <div class="num sub-line">{{ maskPhone(row.receiverPhone) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="itemType" label="件型" min-width="80" />
          <el-table-column prop="shelfNo" label="货架" min-width="80" />
          <el-table-column prop="pickupCode" label="取件码" min-width="90">
            <template #default="{ row }"><span class="num">{{ row.pickupCode }}</span></template>
          </el-table-column>
          <el-table-column label="状态" min-width="90">
            <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
          </el-table-column>
          <el-table-column label="入库时间" min-width="150">
            <template #default="{ row }">{{ fmtTime(row.arriveTime) }}</template>
          </el-table-column>
          <el-table-column label="在库天数" min-width="90">
            <template #default="{ row }">
              <span class="num" :style="row.retentionDays >= 5 ? 'color:#B22B31;font-weight:600' : ''">{{ row.retentionDays }} 天</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push('/parcel/' + row.id)">查看</el-button>
              <el-button v-if="row.status === '在库'" link type="primary" @click="notify(row)">通知</el-button>
              <el-button v-if="['在库', '已通知'].includes(row.status)" link type="danger" @click="askReturn(row)">退件</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="mobile-cards">
        <div v-for="row in rows" :key="row.id" class="mcard" @click="$router.push('/parcel/' + row.id)">
          <div class="mcard-body">
            <el-image class="mthumb" :src="row.coverImage" fit="cover" lazy>
              <template #error><div class="thumb-empty">无图</div></template>
            </el-image>
            <div class="mcard-main">
              <div class="row1"><span class="num">{{ row.waybillNo }}</span><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></div>
              <div class="row2"><span>{{ row.receiverName }} · {{ row.shelfNo }}</span><span>{{ fmtTime(row.arriveTime).slice(5, 16) }}</span></div>
            </div>
          </div>
        </div>
      </div>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
                       @size-change="search" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import { parcelPage, options, notifyParcel, exportParcels, createReturn } from '../api'
import { fmtTime, maskPhone, tagType } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false); const range = ref([])
const opts = ref({ companies: [], parcelsStatus: [], shelves: [], parcelStatus: [] })
const q = reactive({ keyword: '', companyId: null, status: '', shelfNo: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const d = await parcelPage({ ...q, dateFrom: range.value?.[0], dateTo: range.value?.[1] })
    rows.value = d.records; total.value = d.total
  } finally { loading.value = false }
}
function search() { q.page = 1; load() }
function reset() { q.keyword = ''; q.companyId = null; q.status = ''; q.shelfNo = ''; range.value = []; search() }

async function notify(row) {
  await notifyParcel(row.id, { notifyType: '电话通知', result: '已通知' })
  ElMessage.success('已记录一次通知，通知次数 +1')
  load()
}
async function askReturn(row) {
  const { value } = await ElMessageBox.prompt('退件原因（不少于 5 个字）', '发起退件申请 · ' + row.waybillNo, {
    inputPlaceholder: '例如：超期未取，多次通知无人领取',
    inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字'
  })
  await createReturn({ parcelId: row.id, reasonType: '超期未取', reason: value })
  ElMessage.success('退件申请已提交，等待负责人审批')
  load()
}
async function doExport() {
  const d = await exportParcels({ status: q.status, dateFrom: range.value?.[0], dateTo: range.value?.[1] })
  const header = Object.keys(d.rows[0] || {})
  const csv = [header.join(',')].concat(d.rows.map((r) => header.map((h) => '"' + (r[h] ?? '') + '"').join(','))).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob); a.download = d.fileName; a.click()
  ElMessage.success('已导出 ' + d.count + ' 条，任务已记入操作日志')
}

watch(() => q.size, () => search())
onMounted(async () => { opts.value = await options(); load() })
</script>

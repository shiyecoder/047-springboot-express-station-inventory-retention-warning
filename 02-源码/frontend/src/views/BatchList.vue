<template>
  <div class="page">
    <div class="page-head">
      <div><h2>批次交接</h2><div class="sub">按快递公司与快递员建立交接批次，差异必须写说明，完成后不可随意删除</div></div>
      <el-button type="primary" :icon="Plus" @click="open">新建交接批次</el-button>
    </div>
    <div class="card">
      <div class="filter-row" style="margin-bottom: 12px">
        <el-select v-model="q.status" placeholder="交接状态" clearable>
          <el-option label="已确认" value="已确认" /><el-option label="待确认" value="待确认" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="companyName" label="快递公司" min-width="100" />
        <el-table-column prop="courierName" label="快递员" min-width="90" />
        <el-table-column label="到站时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.arriveTime) }}</template></el-table-column>
        <el-table-column prop="totalCount" label="交接总数" min-width="90" />
        <el-table-column prop="receivedCount" label="接收数" min-width="80" />
        <el-table-column prop="rejectedCount" label="拒收数" min-width="80" />
        <el-table-column label="差异" min-width="80">
          <template #default="{ row }"><span class="num" :style="row.diffCount ? 'color:#B22B31;font-weight:600' : ''">{{ row.diffCount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="90" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="$router.push('/batch/' + row.id)">查看</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total"
                       layout="total, prev, pager, next" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新建交接批次" width="520px">
      <el-form :model="f" label-width="100px">
        <el-form-item label="快递公司"><el-select v-model="f.companyId" style="width: 100%">
          <el-option v-for="c in opts.companies" :key="c.id" :label="c.companyName" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="快递员"><el-select v-model="f.courierId" style="width: 100%">
          <el-option v-for="c in opts.couriers" :key="c.id" :label="c.courierName" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="交接总数"><el-input-number v-model="f.totalCount" :min="0" /></el-form-item>
        <el-form-item label="实际接收"><el-input-number v-model="f.receivedCount" :min="0" /></el-form-item>
        <el-form-item label="拒收数"><el-input-number v-model="f.rejectedCount" :min="0" /></el-form-item>
        <el-form-item label="差异说明"><el-input v-model="f.diffNote" type="textarea" :rows="2" placeholder="存在差异时必填" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">确认交接</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { batchPage, saveBatch, options } from '../api'
import { fmtTime, tagType } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false); const dlg = ref(false)
const q = reactive({ status: '', page: 1, size: 10 })
const opts = ref({ companies: [], couriers: [] })
const f = ref({ companyId: null, courierId: null, totalCount: 0, receivedCount: 0, rejectedCount: 0, diffNote: '' })

async function load() { loading.value = true; try { const d = await batchPage(q); rows.value = d.records; total.value = d.total } finally { loading.value = false } }
function search() { q.page = 1; load() }
function open() { dlg.value = true }
async function save() {
  await saveBatch(f.value)
  ElMessage.success('批次已建立')
  dlg.value = false; load()
}
onMounted(async () => { opts.value = await options(); load() })
</script>

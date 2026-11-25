<template>
  <div class="page">
    <div class="page-head">
      <div><h2>异常件处理</h2><div class="sub">破损、渗漏、错件、丢失、信息不符与疑似重复件；丢失与重大破损需负责人审核关闭</div></div>
      <el-button type="primary" :icon="Plus" @click="dlg = true">登记异常件</el-button>
    </div>
    <div class="card">
      <div class="filter-row" style="margin-bottom: 12px">
        <el-select v-model="q.status" placeholder="处理状态" clearable>
          <el-option label="待处理" value="待处理" /><el-option label="处理中" value="处理中" /><el-option label="已关闭" value="已关闭" />
        </el-select>
        <el-select v-model="q.exType" placeholder="异常类型" clearable>
          <el-option v-for="t in ['破损', '渗漏', '错件', '丢失', '信息不符', '疑似重复']" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="exceptionNo" label="异常单号" min-width="145" />
        <el-table-column prop="waybillNo" label="运单号" min-width="170" show-overflow-tooltip />
        <el-table-column label="现场照片" width="92">
          <template #default="{ row }">
            <el-image class="thumb" :src="imgList(row)[0]" :preview-src-list="imgList(row)"
                      preview-teleported fit="cover" lazy>
              <template #error><div class="thumb-empty">无图</div></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="exType" label="类型" min-width="90" />
        <el-table-column prop="findStage" label="发现环节" min-width="100" />
        <el-table-column prop="dutySide" label="责任方" min-width="100" />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="登记时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" min-width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== '已关闭'" link type="primary" @click="audit(row)">审核关闭</el-button>
            <span v-else class="tag tag-ok">已闭环</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total"
                       layout="total, sizes, prev, pager, next" @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="登记异常件" width="520px">
      <el-form :model="f" label-width="100px">
        <el-form-item label="运单号"><el-input v-model="f.waybillNo" placeholder="可留空，仅登记现场异常" /></el-form-item>
        <el-form-item label="异常类型"><el-select v-model="f.exType" style="width: 100%">
          <el-option v-for="t in ['破损', '渗漏', '错件', '丢失', '信息不符', '疑似重复']" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="发现环节"><el-select v-model="f.findStage" style="width: 100%">
          <el-option v-for="t in ['交接', '入库', '上架', '取件', '盘点']" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="责任方"><el-select v-model="f.dutySide" style="width: 100%">
          <el-option v-for="t in ['快递公司', '驿站', '收件人', '待判定']" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="现场描述"><el-input v-model="f.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">登记</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { exceptionPage, createException, auditException } from '../api'
import { fmtTime, tagType } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false); const dlg = ref(false)
const q = reactive({ status: '', exType: '', page: 1, size: 10 })
const f = ref({ waybillNo: '', exType: '破损', findStage: '入库', dutySide: '待判定', description: '' })

async function load() { loading.value = true; try { const d = await exceptionPage(q); rows.value = d.records; total.value = d.total } finally { loading.value = false } }

/** images 字段是逗号分隔的相对路径串，拆成数组供 el-image 预览 */
function imgList(row) {
  if (!row.images) return []
  return String(row.images).split(',').map((s) => s.trim()).filter(Boolean)
}
function search() { q.page = 1; load() }
async function save() {
  await createException(f.value)
  ElMessage.success('异常件已登记')
  dlg.value = false; load()
}
async function audit(row) {
  const { value: result } = await ElMessageBox.prompt('处理结果（如：快递公司赔付并补发）', '审核关闭 · ' + row.exceptionNo)
  const { value: process } = await ElMessageBox.prompt('处理过程（完整链路）', '处理过程')
  await auditException(row.id, { result, handleProcess: process })
  ElMessage.success('已审核关闭，处理链路已留痕')
  load()
}
onMounted(load)
</script>

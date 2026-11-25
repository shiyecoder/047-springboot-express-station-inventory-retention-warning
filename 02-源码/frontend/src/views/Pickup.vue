<template>
  <div class="page">
    <div class="page-head">
      <div><h2>取件核验台</h2><div class="sub">运单号 / 手机号后四位 / 取件码任一项查询；代取必须登记代取人信息</div></div>
    </div>

    <div class="card">
      <div class="filter-row">
        <el-input v-model="key" placeholder="运单号 / 手机尾号 / 取件码" clearable style="width: 320px" @keyup.enter="query" />
        <el-button type="primary" :icon="Search" @click="query">查询待取件</el-button>
      </div>
    </div>

    <div class="card">
      <h3 class="card-title">待取件（{{ items.length }}）</h3>
      <el-table :data="items" v-loading="loading" stripe @selection-change="(v) => (sel = v)">
        <el-table-column type="selection" width="44" />
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column prop="receiverName" label="收件人" min-width="90" />
        <el-table-column label="手机号" min-width="120"><template #default="{ row }"><span class="num">{{ maskPhone(row.receiverPhone) }}</span></template></el-table-column>
        <el-table-column prop="shelfNo" label="货架" min-width="80" />
        <el-table-column prop="pickupCode" label="取件码" min-width="90" />
        <el-table-column label="在库天数" min-width="90"><template #default="{ row }"><span class="num">{{ row.retentionDays }} 天</span></template></el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !items.length" description="输入运单号、手机尾号或取件码查询；同一收件人可一次勾选多件" />

      <div class="confirm-row" v-if="items.length">
        <el-radio-group v-model="pickupType">
          <el-radio-button value="本人取件">本人取件</el-radio-button>
          <el-radio-button value="代取">代取</el-radio-button>
        </el-radio-group>
        <el-select v-model="verifyType" style="width: 200px">
          <el-option label="取件码核验" value="取件码核验" /><el-option label="身份证核验" value="身份证核验" /><el-option label="手机尾号核验" value="手机尾号核验" />
        </el-select>
        <template v-if="pickupType === '代取'">
          <el-input v-model="person" placeholder="代取人姓名" style="width: 150px" />
          <el-input v-model="phone" placeholder="代取人电话" style="width: 160px" />
        </template>
        <el-button type="primary" :disabled="!sel.length" @click="confirm">确认出库（{{ sel.length }} 件）</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { pickupQuery, confirmPickup, parcelPage } from '../api'
import { maskPhone, tagType } from '../utils/format'

const key = ref('')
const items = ref([]); const sel = ref([]); const loading = ref(false)
const pickupType = ref('本人取件'); const verifyType = ref('取件码核验')
const person = ref(''); const phone = ref('')

async function query() {
  loading.value = true
  try { items.value = await pickupQuery(key.value) } finally { loading.value = false }
}
async function confirm() {
  await ElMessageBox.confirm('取件与退件均为终态操作，确认逐件核对无误？', '确认出库', { type: 'warning' })
  const d = await confirmPickup({
    parcelIds: sel.value.map((r) => r.id), pickupType: pickupType.value,
    verifyType: verifyType.value, pickupPerson: person.value, pickupPhone: phone.value
  })
  ElMessage.success('出库成功 ' + d.success + ' 件' + (d.failed.length ? '，失败：' + d.failed.join('；') : ''))
  key.value = ''; items.value = []
}
onMounted(async () => {
  // 默认展示在库件，便于演示查询与批量取件
  const d = await parcelPage({ status: '在库', page: 1, size: 10 })
  items.value = d.records
})
</script>

<style scoped>
.confirm-row { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-top: 14px; padding-top: 14px; border-top: 1px dashed #E3EBF2; }
</style>

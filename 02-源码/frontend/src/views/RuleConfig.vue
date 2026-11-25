<template>
  <div class="page">
    <div class="page-head">
      <div><h2>规则与权重配置</h2><div class="sub">六因子权重合计必须等于 100%，保存后旧版本自动归档并记录生效时间</div></div>
      <el-button type="primary" :icon="Check" @click="save">保存为新版本</el-button>
    </div>

    <div class="detail-grid">
      <div class="card">
        <h3 class="card-title">当前编辑（生效版本 {{ active.ruleVersion }}）</h3>
        <div v-for="f in FACTORS" :key="f.key" class="wrow">
          <div class="wname">{{ f.label }}<i>{{ f.tip }}</i></div>
          <el-slider v-model="w[f.key]" :min="0" :max="60" :step="5" show-input :show-input-controls="false" style="flex:1" />
        </div>
        <div class="wtot" :class="{ bad: total !== 100 }">
          权重合计：<b class="num">{{ total }}%</b>
          <span v-if="total !== 100">（必须等于 100%）</span>
        </div>
        <el-alert type="info" :closable="false" style="margin-top:12px"
                  title="修改权重只影响新计算或主动重新计算的数据，历史评分记录保留当时的规则版本与输入快照。" />
      </div>

      <div class="card">
        <h3 class="card-title">版本记录</h3>
        <el-table :data="versions" size="small">
          <el-table-column prop="ruleVersion" label="版本" width="70" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><span class="tag" :class="row.status === '生效中' ? 'tag-ok' : 'tag-muted'">{{ row.status }}</span></template>
          </el-table-column>
          <el-table-column label="生效时间" min-width="140"><template #default="{ row }">{{ fmtTime(row.effectiveTime) }}</template></el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="90" />
          <el-table-column label="权重" min-width="150">
            <template #default="{ row }"><span class="num" style="font-size:11px">{{ brief(row.weightsJson) }}</span></template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { rulePage, saveRule } from '../api'
import { fmtTime } from '../utils/format'

const FACTORS = [
  { key: 'size', label: '包裹尺寸', tip: '件型与货架规格是否匹配' },
  { key: 'capacity', label: '货架容量', tip: '目标货架剩余容量占比' },
  { key: 'area', label: '快递公司分区', tip: '公司约定分区与货架分区一致性' },
  { key: 'duration', label: '入库时长', tip: '在库天数越长催取优先级越高' },
  { key: 'notify', label: '通知次数', tip: '历史通知次数，多次未取需升级' },
  { key: 'special', label: '特殊件标记', tip: '贵重/生鲜/易碎件优先处理' }
]
const w = reactive({ size: 20, capacity: 25, area: 15, duration: 25, notify: 10, special: 5 })
const active = ref({})
const versions = ref([])
const total = computed(() => FACTORS.reduce((s, f) => s + (Number(w[f.key]) || 0), 0))

function brief(json) { try { const o = JSON.parse(json); return FACTORS.map((f) => o[f.key]).join('/') } catch (e) { return '—' } }

async function load() {
  const d = await rulePage({ page: 1, size: 20 })
  versions.value = d.records
  active.value = d.records.find((r) => r.status === '生效中') || d.records[0] || {}
  if (active.value.weightsJson) {
    try { const o = JSON.parse(active.value.weightsJson); FACTORS.forEach((f) => (w[f.key] = o[f.key])) } catch (e) { /* 保留默认 */ }
  }
}
async function save() {
  if (total.value !== 100) return ElMessage.error('权重合计为 ' + total.value + '%，必须等于 100%')
  await saveRule({ ruleName: '滞留风险与货架位置推荐', weightsJson: JSON.stringify(w), thresholdJson: JSON.stringify({ normal: 3, soon: 5, overdue: 9, return: 10 }) })
  ElMessage.success('已保存为新版本，旧版本自动归档')
  load()
}
onMounted(load)
</script>

<style scoped>
.wrow { display: flex; align-items: center; gap: 18px; padding: 6px 0; }
.wname { width: 200px; font-size: 13px; color: #33475B; }
.wname i { display: block; font-style: normal; font-size: 11px; color: #8A9AA8; margin-top: 2px; }
.wtot { margin-top: 14px; padding-top: 12px; border-top: 1px dashed #E3EBF2; font-size: 13px; color: #1F7653; }
.wtot.bad { color: #B22B31; }
.wtot b { font-size: 18px; }
</style>

<template>
  <div class="page">
    <div class="page-head">
      <div><h2>滞留处理中心</h2><div class="sub">按在库天数分级催取，风险等级由生效规则版本计算，每次计算留输入快照</div></div>
      <div>
        <el-button :icon="Refresh" @click="gen">生成今日滞留清单</el-button>
        <el-button type="primary" :icon="Bell" :disabled="!sel.length" @click="batchNotify">批量二次通知（{{ sel.length }}）</el-button>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="tab">
        <el-tab-pane label="滞留清单" name="list">
          <div class="filter-row" style="margin-bottom: 12px">
            <el-select v-model="q.level" placeholder="预警等级" clearable>
              <el-option label="即将滞留" value="即将滞留" /><el-option label="已滞留" value="已滞留" />
              <el-option label="待退回" value="待退回" /><el-option label="正常" value="正常" />
            </el-select>
            <el-select v-model="q.handleStatus" placeholder="处理状态" clearable>
              <el-option label="待处理" value="待处理" /><el-option label="已通知" value="已通知" />
              <el-option label="已处理" value="已处理" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          </div>
          <el-table :data="rows" v-loading="loading" stripe @selection-change="(v) => (sel = v)">
            <el-table-column type="selection" width="44" />
            <el-table-column prop="waybillNo" label="运单号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="receiverName" label="收件人" min-width="90" />
            <el-table-column label="手机号" min-width="120"><template #default="{ row }"><span class="num">{{ maskPhone(row.receiverPhone) }}</span></template></el-table-column>
            <el-table-column prop="shelfNo" label="货架" min-width="80" />
            <el-table-column label="在库天数" min-width="100">
              <template #default="{ row }"><span class="num" style="color:#B22B31;font-weight:600">{{ row.retentionDays }} 天</span></template>
            </el-table-column>
            <el-table-column label="等级" min-width="100">
              <template #default="{ row }"><span class="tag" :class="'tag-' + (row.level === '已滞留' ? 'danger' : row.level === '即将滞留' ? 'warn' : 'info')">{{ row.level }}</span></template>
            </el-table-column>
            <el-table-column prop="notifyCount" label="通知次数" min-width="90" />
            <el-table-column label="处理状态" min-width="100">
              <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.handleStatus)">{{ row.handleStatus }}</span></template>
            </el-table-column>
            <el-table-column label="操作" min-width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="$router.push('/parcel/' + row.parcelId)">详情</el-button>
                <el-button link type="primary" @click="handle(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total"
                           :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="search" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="推荐计算结果" name="score">
          <div class="filter-row" style="margin-bottom: 12px">
            <el-select v-model="sq.grade" placeholder="结果等级" clearable>
              <el-option label="优选" value="优选" /><el-option label="可用" value="可用" /><el-option label="受限" value="受限" />
            </el-select>
            <el-input v-model="sq.waybill" placeholder="运单号" clearable />
            <el-button type="primary" :icon="Search" @click="loadScore">查询</el-button>
          </div>
          <el-table :data="scores" stripe>
            <el-table-column prop="waybillNo" label="运单号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="itemType" label="件型" min-width="80" />
            <el-table-column label="总分" min-width="80"><template #default="{ row }"><b class="num">{{ row.totalScore }}</b></template></el-table-column>
            <el-table-column label="结果" min-width="90">
              <template #default="{ row }"><span class="tag" :class="'tag-' + gradeTag(row.grade)">{{ row.grade }}</span></template>
            </el-table-column>
            <el-table-column prop="recommendShelf" label="推荐货位" min-width="90" />
            <el-table-column prop="ruleVersion" label="规则版本" min-width="90" />
            <el-table-column label="人工修正" min-width="110">
              <template #default="{ row }">
                <span v-if="row.manualAdjust" class="tag tag-warn">{{ row.manualAdjust }}</span>
                <span v-else style="color:#8A9AA8">未调整</span>
              </template>
            </el-table-column>
            <el-table-column label="计算时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.createTime) }}</template></el-table-column>
            <el-table-column label="操作" min-width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="showFactor(row)">依据</el-button>
                <el-button link type="primary" @click="adjust(row)">人工调整</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="sq.page" v-model:page-size="sq.size" :total="sTotal"
                           layout="total, prev, pager, next" @current-change="loadScore" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-drawer v-model="factorDrawer" title="计算依据与人工调整记录" size="520px">
      <div v-if="cur">
        <div class="score-head">
          <div><div class="score-num num">{{ cur.totalScore }}</div>
            <span class="tag" :class="'tag-' + gradeTag(cur.grade)">{{ cur.grade }}</span></div>
          <div class="score-shelf">推荐 {{ cur.recommendShelf }}<i>规则 {{ cur.ruleVersion }}</i></div>
        </div>
        <p class="explain">{{ cur.recommendText }}</p>
        <div v-for="f in cur.factors" :key="f.factor" class="factor">
          <div class="fline"><span>{{ f.label }}（权重 {{ f.weight }}%）</span><span class="num">{{ f.score }} → {{ f.weighted }}</span></div>
          <el-progress :percentage="f.score" :stroke-width="6" :show-text="false"
                       :color="f.score >= 80 ? '#1F7653' : f.score >= 60 ? '#2878B5' : '#B22B31'" />
          <div class="fbasis">{{ f.basis }}</div>
        </div>
        <div class="card" style="margin-top:14px">
          <h3 class="card-title">输入快照</h3>
          <pre class="snap">{{ pretty(cur.inputJson) }}</pre>
        </div>
        <div v-if="cur.adjustReason" class="card">
          <h3 class="card-title">人工调整记录</h3>
          <div class="fbasis">调整为 {{ cur.manualAdjust }}：{{ cur.adjustReason }}（{{ fmtTime(cur.adjustTime) }}）</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Bell } from '@element-plus/icons-vue'
import { retentionPage, generateRetention, retentionNotify, handleRetention, scorePage, adjustScore } from '../api'
import { fmtTime, maskPhone, tagType, gradeTag } from '../utils/format'

const tab = ref('list')
const rows = ref([]); const total = ref(0); const loading = ref(false); const sel = ref([])
const q = reactive({ level: '', handleStatus: '', page: 1, size: 10 })
const scores = ref([]); const sTotal = ref(0)
const sq = reactive({ grade: '', waybill: '', page: 1, size: 10 })
const factorDrawer = ref(false); const cur = ref(null)

async function load() {
  loading.value = true
  try { const d = await retentionPage(q); rows.value = d.records; total.value = d.total } finally { loading.value = false }
}
function search() { q.page = 1; load() }
async function loadScore() { const d = await scorePage(sq); scores.value = d.records; sTotal.value = d.total }

async function gen() {
  const d = await generateRetention()
  ElMessage.success('扫描 ' + d.scanned + ' 条，新增 ' + d.created + ' 条滞留预警')
  load()
}
async function batchNotify() {
  const ids = sel.value.map((r) => r.id)
  const { value } = await ElMessageBox.prompt('二次通知内容', '批量催取（' + ids.length + ' 件）', { inputValue: '您的快件在库时间较长，请尽快到站取件' })
  const d = await retentionNotify({ ids, note: value })
  ElMessage.success('成功 ' + d.success + ' 条' + (d.failed.length ? '，失败 ' + d.failed.length + ' 条：' + d.failed.join('；') : ''))
  load()
}
async function handle(row) {
  const { value } = await ElMessageBox.prompt('处理备注', '处理滞留 · ' + row.waybillNo, { inputPlaceholder: '例如：已电话联系，约定两日内取件' })
  await handleRetention(row.id, { handleStatus: '已处理', note: value })
  ElMessage.success('已记录处理结果')
  load()
}
function showFactor(row) {
  try {
    cur.value = { ...row, factors: JSON.parse(row.scoresJson || '[]') }
  } catch (e) { cur.value = { ...row, factors: [] } }
  factorDrawer.value = true
}
function pretty(s) { try { return JSON.stringify(JSON.parse(s), null, 2) } catch (e) { return s || '—' } }
async function adjust(row) {
  const { value } = await ElMessageBox.prompt('人工调整后的等级（优选/可用/受限）', '人工调整 · ' + row.waybillNo, {
    inputValue: row.manualAdjust || row.grade,
    inputValidator: (v) => ['优选', '可用', '受限'].includes(v) || '只能填优选/可用/受限'
  })
  const { value: reason } = await ElMessageBox.prompt('调整原因（不少于 5 个字，原始计算结果不会被覆盖）', '调整原因', {
    inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字'
  })
  await adjustScore(row.id, { grade: value, reason })
  ElMessage.success('已保存人工调整，原始结果保留')
  loadScore()
}
onMounted(() => { load(); loadScore() })
</script>

<style scoped>
.score-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 10px; }
.score-num { font-size: 30px; font-weight: 600; color: #0F3B5D; line-height: 1; margin-bottom: 6px; }
.score-shelf { font-size: 12px; color: #6B7C8C; text-align: right; }
.score-shelf i { display: block; font-style: normal; margin-top: 4px; }
.explain { font-size: 12px; color: #4A6274; line-height: 1.75; background: #F4F8FB; border-radius: 6px; padding: 9px 10px; }
.factor { margin-bottom: 12px; }
.fline { display: flex; justify-content: space-between; font-size: 12px; color: #33475B; margin-bottom: 4px; }
.fbasis { font-size: 11px; color: #8A9AA8; margin-top: 3px; }
.snap { font-size: 11px; color: #4A6274; background: #F4F8FB; padding: 10px; border-radius: 6px; overflow: auto; margin: 0; }
</style>

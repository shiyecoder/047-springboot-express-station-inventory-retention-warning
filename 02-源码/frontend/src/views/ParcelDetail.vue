<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>快件详情 · <span class="num">{{ p.waybillNo }}</span></h2>
        <div class="sub">当前状态：{{ p.status }} · 经办人 {{ p.handlerName || '—' }} · 最近更新 {{ fmtTime(p.updateTime) }}</div>
      </div>
      <div>
        <el-button @click="$router.back()">返回</el-button>
        <el-button type="primary" :icon="DataAnalysis" :loading="scoring" @click="doEvaluate">执行推荐计算</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <div>
        <div class="card">
          <h3 class="card-title">入库信息</h3>
          <!-- 包裹照片：本地种子图（/uploads/seed/parcel/NN.jpg），点击可放大 -->
          <el-image class="detail-photo" :src="p.coverImage" :preview-src-list="p.coverImage ? [p.coverImage] : []"
                    preview-teleported fit="cover">
            <template #error><div class="detail-photo-empty">暂无包裹照片</div></template>
          </el-image>
          <div class="kv">
            <span class="k">运单号</span><span class="v num">{{ p.waybillNo }}</span>
            <span class="k">快递公司</span><span class="v">{{ p.companyName }}</span>
            <span class="k">收件人</span><span class="v">{{ p.receiverName }}</span>
            <span class="k">手机号</span><span class="v num">{{ maskPhone(p.receiverPhone) }}</span>
            <span class="k">件型 / 特殊件</span><span class="v">{{ p.itemType }} · {{ p.specialFlag }}</span>
            <span class="k">货架位</span><span class="v">{{ p.shelfNo || '未上架' }}（{{ p.areaCode || '—' }} 区）</span>
            <span class="k">取件码</span><span class="v num">{{ p.pickupCode }}</span>
            <span class="k">到站时间</span><span class="v">{{ fmtTime(p.arriveTime) }}</span>
            <span class="k">上架时间</span><span class="v">{{ fmtTime(p.shelfTime) }}</span>
            <span class="k">在库天数</span><span class="v num">{{ p.retentionDays }} 天 · 已通知 {{ p.notifyCount }} 次</span>
            <span class="k">备注</span><span class="v">{{ p.remark || '—' }}</span>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">关联单据</h3>
          <el-tabs>
            <el-tab-pane :label="'通知记录 ' + (p.notifies || []).length">
              <el-table :data="p.notifies" size="small">
                <el-table-column prop="notifyType" label="方式" width="110" />
                <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
                <el-table-column prop="result" label="结果" width="100" />
                <el-table-column label="时间" width="160"><template #default="{ row }">{{ fmtTime(row.notifyTime) }}</template></el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane :label="'上架与流转 ' + (p.shelfRecords || []).length">
              <el-table :data="p.shelfRecords" size="small">
                <el-table-column prop="actionType" label="动作" width="110" />
                <el-table-column prop="shelfNo" label="货架" width="90" />
                <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
                <el-table-column label="时间" width="160"><template #default="{ row }">{{ fmtTime(row.createTime) }}</template></el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane :label="'取件记录 ' + (p.pickup || []).length">
              <el-table :data="p.pickup" size="small">
                <el-table-column prop="pickupType" label="方式" width="100" />
                <el-table-column prop="verifyType" label="核验" width="110" />
                <el-table-column prop="pickupPerson" label="取件人" width="110" />
                <el-table-column label="时间" width="160"><template #default="{ row }">{{ fmtTime(row.pickupTime) }}</template></el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <div>
        <div class="card">
          <h3 class="card-title">流转时间轴</h3>
          <ul class="timeline">
            <li v-for="(e, i) in p.timeline || []" :key="i">
              <div class="t">{{ e.title }}</div>
              <div class="d">{{ fmtTime(e.time) }} · {{ e.desc }}</div>
            </li>
          </ul>
        </div>

        <div class="card">
          <h3 class="card-title">滞留风险与货位推荐</h3>
          <div v-if="!score" class="empty">尚未计算，点击右上角「执行推荐计算」</div>
          <div v-else>
            <div class="score-head">
              <div>
                <div class="score-num num">{{ score.totalScore }}</div>
                <span class="tag" :class="'tag-' + (score.grade === '优选' ? 'ok' : score.grade === '可用' ? 'info' : 'danger')">{{ score.grade }}</span>
              </div>
              <div class="score-shelf">推荐 {{ score.recommendShelf }}<i>规则 {{ score.ruleVersion }}</i></div>
            </div>
            <p class="explain">{{ score.recommendText }}</p>
            <div v-for="f in score.factors" :key="f.factor" class="factor">
              <div class="fline"><span>{{ f.label }}</span><span class="num">{{ f.score }} × {{ f.weight }}% = {{ f.weighted }}</span></div>
              <el-progress :percentage="f.score" :stroke-width="6" :show-text="false"
                           :color="f.score >= 80 ? '#1F7653' : f.score >= 60 ? '#2878B5' : '#B22B31'" />
              <div class="fbasis">{{ f.basis }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis } from '@element-plus/icons-vue'
import { parcelDetail, evaluate } from '../api'
import { fmtTime, maskPhone } from '../utils/format'

const route = useRoute()
const p = ref({})
const score = ref(null)
const scoring = ref(false)

async function load() {
  p.value = await parcelDetail(route.params.id)
  if (p.value.scores && p.value.scores.length) score.value = normalize(p.value.scores[0])
}
// 历史评分记录里的 scores_json 是字符串，转成明细数组
function normalize(s) {
  try {
    return { totalScore: s.totalScore, grade: s.adjust || s.grade, recommendShelf: s.recommendShelf,
      recommendText: s.recommendText, ruleVersion: s.ruleVersion, factors: JSON.parse(s.scoresJson || '[]') }
  } catch (e) { return null }
}
async function doEvaluate() {
  scoring.value = true
  try {
    score.value = await evaluate(route.params.id)
    ElMessage.success('计算完成：' + score.value.grade + ' / ' + score.value.totalScore + ' 分')
    load()
  } finally { scoring.value = false }
}
onMounted(load)
</script>

<style scoped>
.empty { color: #8A9AA8; font-size: 13px; padding: 18px 0; text-align: center; }
.score-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 10px; }
.score-num { font-size: 30px; font-weight: 600; color: #0F3B5D; line-height: 1; margin-bottom: 6px; }
.score-shelf { font-size: 12px; color: #6B7C8C; text-align: right; }
.score-shelf i { display: block; font-style: normal; margin-top: 4px; }
.explain { font-size: 12px; color: #4A6274; line-height: 1.75; background: #F4F8FB; border-radius: 6px; padding: 9px 10px; margin: 0 0 12px; }
.factor { margin-bottom: 10px; }
.fline { display: flex; justify-content: space-between; font-size: 12px; color: #33475B; margin-bottom: 4px; }
.fbasis { font-size: 11px; color: #8A9AA8; margin-top: 3px; }
</style>

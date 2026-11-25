<template>
  <div class="portal">
    <div class="hero">
      <div>
        <h1>您好，{{ user.realName || '收件人' }}</h1>
        <p>这里是您的快件中心：到站、待取、已取全程可查，代取需先在站内提交授权。</p>
      </div>
      <div class="code-box">
        <span>我的待取件</span><b class="num">{{ pending.length }}</b>
      </div>
    </div>

    <div class="quick">
      <div class="qcard"><b>{{ pending.length }}</b><span>待取件</span></div>
      <div class="qcard"><b>{{ all.length }}</b><span>我的全部快件</span></div>
      <div class="qcard"><b>{{ overdue }}</b><span>即将滞留</span></div>
      <div class="qcard"><b>{{ picked }}</b><span>已取件</span></div>
    </div>

    <div class="card">
      <h3 class="card-title">待取快件</h3>
      <div class="plist">
        <div v-for="p in pending" :key="p.id" class="pitem">
          <div class="pleft">
            <div class="pno num">{{ p.waybillNo }}</div>
            <div class="pmeta">{{ p.companyName }} · {{ p.itemType }} · 到站 {{ fmtTime(p.arriveTime) }}</div>
          </div>
          <div class="pright">
            <div class="pcode"><span>取件码</span><b class="num">{{ p.pickupCode }}</b></div>
            <div class="pdays" :class="{ warn: p.retentionDays >= 5 }">已存 {{ p.retentionDays }} 天</div>
          </div>
        </div>
      </div>
      <el-empty v-if="!pending.length" description="暂时没有待取快件，到站后会第一时间通知您" />
    </div>

    <div class="card">
      <h3 class="card-title">全部记录</h3>
      <el-table :data="all" size="small" stripe>
        <el-table-column prop="waybillNo" label="运单号" min-width="150" />
        <el-table-column prop="companyName" label="快递公司" min-width="100" />
        <el-table-column prop="shelfNo" label="货架" min-width="80" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="'tag-' + tagType(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="到站" min-width="150"><template #default="{ row }">{{ fmtTime(row.arriveTime) }}</template></el-table-column>
        <el-table-column label="取件" min-width="150"><template #default="{ row }">{{ fmtTime(row.pickupTime) }}</template></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { parcelPage } from '../api'
import { fmtTime, tagType } from '../utils/format'

const all = ref([])
const user = ref({})
const pending = computed(() => all.value.filter((p) => ['在库', '已通知'].includes(p.status)))
const picked = computed(() => all.value.filter((p) => p.status === '已取件').length)
const overdue = computed(() => all.value.filter((p) => p.retentionDays >= 5 && ['在库', '已通知'].includes(p.status)).length)

onMounted(async () => {
  try { user.value = JSON.parse(localStorage.getItem('user') || '{}') } catch (e) { user.value = {} }
  const d = await parcelPage({ page: 1, size: 50 })
  all.value = d.records
})
</script>

<style scoped>
.portal { padding: 20px 24px 28px; }
.hero { background: linear-gradient(120deg, #0F3B5D, #245A7A); color: #fff; border-radius: 10px; padding: 26px 30px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.hero h1 { font-size: 21px; margin: 0 0 8px; font-weight: 600; }
.hero p { margin: 0; font-size: 13px; color: rgba(255,255,255,.72); }
.code-box { text-align: right; }
.code-box span { font-size: 12px; opacity: .7; display: block; }
.code-box b { font-size: 34px; line-height: 1.1; }
.quick { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.qcard { background: #fff; border: 1px solid var(--line); border-radius: 8px; padding: 14px 16px; }
.qcard b { display: block; font-size: 24px; color: #0F3B5D; font-variant-numeric: tabular-nums; }
.qcard span { font-size: 12px; color: #6B7C8C; }
.plist { display: flex; flex-direction: column; gap: 10px; }
.pitem { display: flex; justify-content: space-between; align-items: center; border: 1px solid var(--line); border-radius: 8px; padding: 13px 16px; }
.pno { font-size: 14px; font-weight: 600; color: #22384A; }
.pmeta { font-size: 12px; color: #6B7C8C; margin-top: 4px; }
.pright { text-align: right; }
.pcode span { font-size: 11px; color: #6B7C8C; margin-right: 6px; }
.pcode b { font-size: 18px; color: #0F3B5D; letter-spacing: 1px; }
.pdays { font-size: 11px; color: #6B7C8C; margin-top: 3px; }
.pdays.warn { color: #B22B31; font-weight: 600; }
</style>

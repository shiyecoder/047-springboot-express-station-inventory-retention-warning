<template>
  <div class="page">
    <div class="page-head">
      <div><h2>批次详情 · {{ b.batchNo }}</h2><div class="sub">{{ b.companyName }} · {{ b.courierName }} · {{ fmtTime(b.arriveTime) }}</div></div>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <div class="detail-grid">
      <div class="card">
        <h3 class="card-title">批次明细（{{ (b.items || []).length }} 条）</h3>
        <el-table :data="b.items" size="small" stripe>
          <el-table-column prop="waybillNo" label="运单号" min-width="150" />
          <el-table-column prop="receiverName" label="收件人" min-width="90" />
          <el-table-column prop="itemType" label="件型" min-width="80" />
          <el-table-column prop="result" label="交接结果" min-width="100" />
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        </el-table>
      </div>
      <div class="card">
        <h3 class="card-title">交接结果</h3>
        <div class="kv">
          <span class="k">交接总数</span><span class="v num">{{ b.totalCount }}</span>
          <span class="k">实际接收</span><span class="v num">{{ b.receivedCount }}</span>
          <span class="k">拒收数</span><span class="v num">{{ b.rejectedCount }}</span>
          <span class="k">差异数</span><span class="v num" :style="b.diffCount ? 'color:#B22B31;font-weight:600' : ''">{{ b.diffCount }}</span>
          <span class="k">差异说明</span><span class="v">{{ b.diffNote || '—' }}</span>
          <span class="k">经办人</span><span class="v">{{ b.handlerName }}</span>
          <span class="k">状态</span><span class="v"><span class="tag" :class="'tag-' + tagType(b.status)">{{ b.status }}</span></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { batchDetail } from '../api'
import { fmtTime, tagType } from '../utils/format'

const route = useRoute()
const b = ref({})
onMounted(async () => { b.value = await batchDetail(route.params.id) })
</script>

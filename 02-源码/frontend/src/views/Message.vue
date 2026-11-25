<template>
  <div class="page">
    <div class="page-head">
      <div><h2>站内消息</h2><div class="sub">待审批、即将超时、已退回等消息，含业务编号与详情跳转</div></div>
      <el-button @click="all">全部标为已读</el-button>
    </div>
    <div class="card">
      <div class="filter-row" style="margin-bottom: 12px">
        <el-select v-model="q.type" placeholder="消息类型" clearable style="width: 200px">
          <el-option v-for="t in ['待审批', '即将超时', '已退回', '待验收', '处理完成']" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="load">筛选</el-button>
      </div>
      <div v-for="m in rows" :key="m.id" class="msg" :class="{ unread: !m.isRead }" @click="open(m)">
        <div class="dot"></div>
        <div class="body">
          <div class="title">{{ m.title }}<span class="tag tag-info" style="margin-left:8px">{{ m.msgType }}</span></div>
          <div class="content">{{ m.content }}</div>
          <div class="time">{{ fmtTime(m.createTime) }}</div>
        </div>
      </div>
      <el-empty v-if="!rows.length" description="暂无消息" />
      <div class="pager">
        <el-pagination v-model:current-page="q.page" :page-size="q.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { messagePage, readMessage, readAll } from '../api'
import { fmtTime } from '../utils/format'

const router = useRouter()
const rows = ref([]); const total = ref(0)
const q = reactive({ type: '', page: 1, size: 10 })

async function load() { const d = await messagePage(q); rows.value = d.records; total.value = d.total }
async function open(m) {
  if (!m.isRead) { await readMessage(m.id); m.isRead = 1 }
  if (m.linkUrl) router.push(m.linkUrl)
}
async function all() { await readAll(); load() }
onMounted(load)
</script>

<style scoped>
.msg { display: flex; gap: 12px; padding: 13px 6px; border-bottom: 1px solid #F1F5F8; cursor: pointer; }
.msg:hover { background: #FAFCFE; }
.dot { width: 7px; height: 7px; border-radius: 50%; background: #C8D5DF; margin-top: 7px; flex: 0 0 auto; }
.msg.unread .dot { background: #2878B5; }
.msg.unread .title { font-weight: 600; }
.title { font-size: 13px; color: #22384A; }
.content { font-size: 12px; color: #6B7C8C; margin-top: 4px; line-height: 1.7; }
.time { font-size: 11px; color: #A3B1BD; margin-top: 5px; }
</style>

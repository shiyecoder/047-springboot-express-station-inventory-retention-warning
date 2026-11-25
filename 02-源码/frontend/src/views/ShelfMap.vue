<template>
  <div class="page">
    <div class="page-head">
      <div><h2>货架可视图</h2><div class="sub">按区域展示货架占用率，满架与停用货架单独提示</div></div>
      <div>
        <span class="tag tag-ok">可用</span>
        <span class="tag tag-warn" style="margin-left:8px">紧张</span>
        <span class="tag tag-danger" style="margin-left:8px">满载</span>
        <span class="tag tag-muted" style="margin-left:8px">停用</span>
      </div>
    </div>
    <div class="card">
      <div class="summary">共 {{ m.total }} 个货架，其中满载 {{ m.fullCount }} 个</div>
      <div v-for="a in m.areas" :key="a.areaCode" class="area">
        <h4>{{ a.areaCode }} 区 <span>{{ a.shelves.length }} 个货架</span></h4>
        <div class="grid">
          <div v-for="s in a.shelves" :key="s.id" class="shelf" :class="cls(s)">
            <div class="top"><b>{{ s.shelfNo }}</b><span>{{ s.shelfType }}</span></div>
            <div class="bar"><i :style="{ width: Math.min(100, (s.usedCount / s.capacity) * 100) + '%' }"></i></div>
            <div class="bot"><span class="num">{{ s.usedCount }}/{{ s.capacity }}</span><span>{{ s.status }}</span></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { shelfMap } from '../api'

const m = ref({ areas: [], total: 0, fullCount: 0 })
function cls(s) {
  if (s.status === '停用') return 'off'
  if (s.usedCount >= s.capacity) return 'full'
  return s.usedCount / s.capacity > 0.7 ? 'warn' : 'ok'
}
onMounted(async () => { m.value = await shelfMap() })
</script>

<style scoped>
.summary { font-size: 12px; color: #6B7C8C; margin-bottom: 14px; }
.area { margin-bottom: 18px; }
.area h4 { font-size: 13px; margin: 0 0 10px; color: #33475B; }
.area h4 span { font-weight: 400; color: #8A9AA8; font-size: 11px; margin-left: 6px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(148px, 1fr)); gap: 10px; }
.shelf { border: 1px solid var(--line); border-radius: 8px; padding: 9px 11px; background: #fff; }
.shelf .top { display: flex; justify-content: space-between; align-items: baseline; font-size: 12px; }
.shelf .top b { font-size: 14px; color: #0F3B5D; }
.shelf .top span { color: #8A9AA8; font-size: 11px; }
.shelf .bar { height: 6px; border-radius: 3px; background: #EDF2F7; margin: 8px 0 6px; overflow: hidden; }
.shelf .bar i { display: block; height: 100%; background: #2878B5; }
.shelf .bot { display: flex; justify-content: space-between; font-size: 11px; color: #6B7C8C; }
.shelf.ok { border-left: 3px solid #1F7653; }
.shelf.warn { border-left: 3px solid #96601A; }
.shelf.warn .bar i { background: #C98A2E; }
.shelf.full { border-left: 3px solid #B22B31; background: #FFF8F8; }
.shelf.full .bar i { background: #B22B31; }
.shelf.off { opacity: .55; border-left: 3px solid #9AA8B4; }
</style>

<template>
  <div class="dash">
    <div class="kpi-row">
      <div class="kpi"><span class="label">今日入库</span><span class="value num">{{ d.todayIn }}<small>件</small></span></div>
      <div class="kpi"><span class="label">今日出库</span><span class="value num">{{ d.todayOut }}<small>件</small></span></div>
      <div class="kpi"><span class="label">当前在库</span><span class="value num">{{ d.inStock }}<small>件</small></span></div>
      <div class="kpi"><span class="label">滞留预警</span><span class="value num" style="color:#B22B31">{{ d.retention }}<small>件</small></span></div>
      <div class="kpi"><span class="label">异常件</span><span class="value num" style="color:#96601A">{{ d.exception }}<small>件</small></span></div>
      <div class="kpi"><span class="label">平均滞留</span><span class="value num">{{ d.avgDays }}<small>天</small></span></div>
      <div class="kpi"><span class="label">货架利用率</span><span class="value num">{{ d.shelfRate }}<small>%</small></span></div>
    </div>

    <div class="chart-grid">
      <div class="chart-card chart-wide">
        <h3>日入出库趋势（近 14 天）</h3>
        <div ref="c1" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <h3>快递公司件量 TOP6</h3>
        <div ref="c2" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <h3>在库时长分布</h3>
        <div ref="c3" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <h3>时段取件量</h3>
        <div ref="c4" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <h3>异常类型分布</h3>
        <div ref="c5" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <h3>员工处理量 TOP6</h3>
        <div ref="c6" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <h3>推荐结果分布（人工调整率 {{ d.adjustRate }}%）</h3>
        <div ref="c7" class="chart-body"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { dashboard } from '../api'

const d = ref({ todayIn: 0, todayOut: 0, inStock: 0, retention: 0, exception: 0, avgDays: 0, shelfRate: 0, adjustRate: 0 })
const c1 = ref(); const c2 = ref(); const c3 = ref(); const c4 = ref(); const c5 = ref(); const c6 = ref(); const c7 = ref()
const charts = []
const PALETTE = ['#0F3B5D', '#2878B5', '#245A7A', '#1F7653', '#7FA3BE', '#B22B31', '#96601A']

function mk(el, option) {
  if (!el) return
  const inst = echarts.init(el)
  inst.setOption(option)
  charts.push(inst)
}

onMounted(async () => {
  d.value = await dashboard('30d')
  await nextTick()
  const axis = { axisLine: { lineStyle: { color: '#DCE7F0' } }, axisLabel: { color: '#6B7C8C', fontSize: 10 } }

  mk(c1.value, {
    color: PALETTE, tooltip: { trigger: 'axis' }, legend: { right: 0, top: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    grid: { left: 34, right: 10, top: 26, bottom: 22 },
    xAxis: { type: 'category', data: (d.value.trend || []).map((t) => t.dt.slice(5)), ...axis },
    yAxis: { type: 'value', ...axis, splitLine: { lineStyle: { color: '#EEF3F7' } } },
    series: [
      { name: '入库', type: 'line', smooth: true, symbolSize: 4, areaStyle: { opacity: .12 }, data: (d.value.trend || []).map((t) => t.inCnt) },
      { name: '出库', type: 'line', smooth: true, symbolSize: 4, areaStyle: { opacity: .1 }, data: (d.value.trend || []).map((t) => t.outCnt) }
    ]
  })
  mk(c2.value, {
    color: PALETTE, tooltip: { trigger: 'axis' }, grid: { left: 74, right: 14, top: 8, bottom: 6 },
    xAxis: { type: 'value', ...axis, splitLine: { lineStyle: { color: '#EEF3F7' } } },
    yAxis: { type: 'category', data: (d.value.company || []).map((x) => x.name).reverse(), ...axis },
    series: [{ type: 'bar', barWidth: 11, itemStyle: { borderRadius: [0, 3, 3, 0] }, data: (d.value.company || []).map((x) => x.value).reverse() }]
  })
  mk(c3.value, {
    color: PALETTE, tooltip: { trigger: 'item' }, legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10 } },
    series: [{ type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'], label: { show: false },
      data: (d.value.duration || []).map((x) => ({ name: x.name, value: x.value })) }]
  })
  mk(c4.value, {
    color: [PALETTE[1]], tooltip: { trigger: 'axis' }, grid: { left: 30, right: 10, top: 10, bottom: 22 },
    xAxis: { type: 'category', data: (d.value.hour || []).map((x) => x.name), ...axis },
    yAxis: { type: 'value', ...axis, splitLine: { lineStyle: { color: '#EEF3F7' } } },
    series: [{ type: 'bar', barWidth: 8, itemStyle: { borderRadius: [2, 2, 0, 0] }, data: (d.value.hour || []).map((x) => x.value) }]
  })
  mk(c5.value, {
    color: [PALETTE[5], PALETTE[6], PALETTE[2], PALETTE[0]], tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10 } },
    series: [{ type: 'pie', radius: '62%', center: ['50%', '44%'], label: { show: false },
      data: (d.value.exType || []).map((x) => ({ name: x.name, value: x.value })) }]
  })
  mk(c6.value, {
    color: [PALETTE[2]], tooltip: { trigger: 'axis' }, grid: { left: 30, right: 10, top: 10, bottom: 22 },
    xAxis: { type: 'category', data: (d.value.staff || []).map((x) => x.name), ...axis },
    yAxis: { type: 'value', ...axis, splitLine: { lineStyle: { color: '#EEF3F7' } } },
    series: [{ type: 'bar', barWidth: 14, itemStyle: { borderRadius: [2, 2, 0, 0] }, data: (d.value.staff || []).map((x) => x.value) }]
  })
  mk(c7.value, {
    color: [PALETTE[3], PALETTE[1], PALETTE[5]], tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10 } },
    series: [{ type: 'pie', radius: ['38%', '64%'], center: ['50%', '44%'], label: { show: false },
      data: (d.value.grade || []).map((x) => ({ name: x.name, value: x.value })) }]
  })
  window.addEventListener('resize', resize)
})

function resize() { charts.forEach((c) => c.resize()) }
onBeforeUnmount(() => { window.removeEventListener('resize', resize); charts.forEach((c) => c.dispose()) })
</script>

// 全局时间格式化：全系统统一调用，禁止各页面自己拼日期
export function fmtTime(v) {
  if (!v) return '—'
  const s = String(v).replace('T', ' ')
  return s.length > 19 ? s.slice(0, 19) : s
}

export function fmtDate(v) {
  if (!v) return '—'
  return String(v).slice(0, 10)
}

// 手机号脱敏：只有授权员工会拿到完整号码，这里再兜一层
export function maskPhone(p) {
  if (!p) return '—'
  const s = String(p)
  return s.length >= 11 ? s.slice(0, 3) + '****' + s.slice(-4) : s
}

export function fmtNum(v, digits = 0) {
  if (v === null || v === undefined || v === '') return '0'
  const n = Number(v)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

// 状态 → 标签样式（不能只靠颜色表达，标签内同时有中文）
const TAG_MAP = {
  '在库': 'info', '待上架': 'warn', '已通知': 'info', '已取件': 'ok',
  '待退回': 'warn', '已退件': 'muted', '异常件': 'danger',
  '待处理': 'warn', '已处理': 'ok', '已关闭': 'muted',
  '待审批': 'warn', '已驳回': 'danger', '待交接': 'warn', '已交接': 'ok',
  '进行中': 'info', '已完成': 'ok', '满载': 'danger', '可用': 'ok', '停用': 'muted',
  '优选': 'ok', '可用 ': 'info', '受限': 'danger',
  '已通知 ': 'info', '待通知': 'warn'
}

export function tagType(status) {
  return TAG_MAP[status] || 'info'
}

export function gradeTag(g) {
  return g === '优选' ? 'ok' : g === '可用' ? 'info' : 'danger'
}

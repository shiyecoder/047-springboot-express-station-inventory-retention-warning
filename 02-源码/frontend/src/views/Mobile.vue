<template>
  <div class="mobile-page">
    <div class="phone">
      <div class="statusbar"><span>9:41</span><span>驿站作业</span><span>100%</span></div>
      <div class="mhead">
        <div>
          <div class="mh1">今日待办</div>
          <div class="mh2">{{ user.realName }} · {{ roleText }}</div>
        </div>
        <img class="mavatar" :src="user.avatar || '/uploads/seed/avatar/01.jpg'" alt="" />
      </div>
      <div class="mstats">
        <div><b class="num">{{ d.inStock || 0 }}</b><span>在库</span></div>
        <div><b class="num">{{ d.retention || 0 }}</b><span>滞留</span></div>
        <div><b class="num">{{ d.exception || 0 }}</b><span>异常</span></div>
      </div>
      <div class="mlist">
        <div class="mtitle">待上架快件</div>
        <div v-for="p in pending.slice(0, 5)" :key="p.id" class="mitem">
          <div>
            <div class="mno num">{{ p.waybillNo }}</div>
            <div class="mmeta">{{ p.receiverName }} · {{ p.itemType }}</div>
          </div>
          <span class="tag tag-warn">待上架</span>
        </div>
      </div>
      <div class="mbtns">
        <div class="mbtn primary" @click="$router.push('/intake/new')">扫码入库</div>
        <div class="mbtn" @click="$router.push('/pickup')">取件核验</div>
      </div>
    </div>
    <div class="tip">移动端采用 390px 视口，低于 768px 时侧栏自动收起，表格转为信息卡片。</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { dashboard, intakeDesk } from '../api'

const d = ref({}); const pending = ref([]); const user = ref({})
const roleText = computed(() => ({ ADMIN: '系统管理员', MANAGER: '站点负责人', STAFF: '驿站员工', COURIER: '快递员', CUSTOMER: '收件人' }[user.value.roleCode] || '驿站员工'))
onMounted(async () => {
  try { user.value = JSON.parse(localStorage.getItem('user') || '{}') } catch (e) { user.value = {} }
  d.value = await dashboard('7d')
  const k = await intakeDesk()
  pending.value = k.pending || []
})
</script>

<style scoped>
.mobile-page { display: flex; flex-direction: column; align-items: center; padding: 24px; gap: 14px; }
.phone { width: 390px; background: #F4F8FB; border-radius: 22px; overflow: hidden; box-shadow: 0 12px 40px rgba(15,59,93,.16); border: 1px solid #DCE7F0; }
.statusbar { height: 30px; background: #0F3B5D; color: #fff; font-size: 11px; display: flex; justify-content: space-between; align-items: center; padding: 0 16px; }
.mhead { background: #0F3B5D; color: #fff; padding: 8px 18px 18px; display: flex; justify-content: space-between; align-items: center; }
.mh1 { font-size: 17px; font-weight: 600; }
.mh2 { font-size: 11px; opacity: .7; margin-top: 3px; }
.mavatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; border: 2px solid rgba(255,255,255,.4); }
.mstats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; padding: 14px 14px 0; }
.mstats div { background: #fff; border-radius: 8px; padding: 10px; text-align: center; border: 1px solid #E3EBF2; }
.mstats b { display: block; font-size: 19px; color: #0F3B5D; }
.mstats span { font-size: 11px; color: #6B7C8C; }
.mlist { padding: 14px; }
.mtitle { font-size: 12px; color: #33475B; font-weight: 600; margin-bottom: 8px; }
.mitem { background: #fff; border: 1px solid #E3EBF2; border-radius: 8px; padding: 10px 12px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.mno { font-size: 12px; font-weight: 600; color: #22384A; }
.mmeta { font-size: 11px; color: #8A9AA8; margin-top: 2px; }
.mbtns { display: flex; gap: 10px; padding: 0 14px 18px; position: sticky; bottom: 0; }
.mbtn { flex: 1; height: 44px; border-radius: 8px; display: grid; place-items: center; font-size: 14px; background: #fff; border: 1px solid #CBE0ED; color: #245A7A; }
.mbtn.primary { background: #0F3B5D; color: #fff; border-color: #0F3B5D; }
.tip { font-size: 12px; color: #6B7C8C; }
</style>

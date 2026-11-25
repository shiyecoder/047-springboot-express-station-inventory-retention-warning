<template>
  <div class="page">
    <div class="page-head">
      <div><h2>单件快速入库</h2><div class="sub">同一站点有效运单号不可重复入库；系统会推荐可用货位</div></div>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <div class="card">
      <el-form :model="f" label-width="110px" style="max-width: 720px">
        <el-form-item label="运单号" required><el-input v-model="f.waybillNo" maxlength="50" placeholder="扫描或输入运单号" /></el-form-item>
        <el-form-item label="快递公司" required>
          <el-select v-model="f.companyId" placeholder="请选择" style="width: 220px">
            <el-option v-for="c in opts.companies" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="收件人" required><el-input v-model="f.receiverName" style="width: 360px" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="f.receiverPhone" maxlength="11" style="width: 360px" /></el-form-item>
        <el-form-item label="件型" required>
          <el-radio-group v-model="f.itemType">
            <el-radio-button v-for="t in opts.itemTypes" :key="t" :value="t">{{ t }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="尺寸描述"><el-input v-model="f.sizeDesc" placeholder="例如 32×24×18cm" style="width: 360px" /></el-form-item>
        <el-form-item label="特殊件">
          <el-select v-model="f.specialFlag" style="width: 220px">
            <el-option v-for="s in opts.specialFlags" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐货架">
          <el-select v-model="f.shelfId" placeholder="不选则自动推荐" clearable filterable style="width: 240px">
            <el-option v-for="s in opts.shelves" :key="s.id" :label="s.shelfNo + '（' + s.areaCode + '区 余' + (s.capacity - s.usedCount) + '）'" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="f.remark" type="textarea" :rows="2" style="width: 480px" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">保存并入库</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { options, saveParcel } from '../api'

const router = useRouter()
const opts = ref({ companies: [], shelves: [], itemTypes: [], specialFlags: [] })
const saving = ref(false)
const f = ref({ waybillNo: '', companyId: null, receiverName: '', receiverPhone: '', itemType: '小件', sizeDesc: '', specialFlag: '无', shelfId: null, remark: '' })

async function submit() {
  if (!f.value.waybillNo || !f.value.receiverName || !f.value.receiverPhone) return ElMessage.warning('运单号、收件人、手机号必填')
  saving.value = true
  try {
    const d = await saveParcel(f.value)
    ElMessage.success('入库成功，取件码 ' + d.pickupCode + '，货架 ' + d.shelfNo)
    router.push('/parcel/' + d.id)
  } finally { saving.value = false }
}
function reset() { f.value = { waybillNo: '', companyId: null, receiverName: '', receiverPhone: '', itemType: '小件', sizeDesc: '', specialFlag: '无', shelfId: null, remark: '' } }
onMounted(async () => { opts.value = await options() })
</script>

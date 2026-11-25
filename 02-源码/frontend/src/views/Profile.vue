<template>
  <div class="page">
    <div class="page-head"><div><h2>个人中心</h2><div class="sub">修改资料、密码与头像，头像保存后右上角立即同步</div></div></div>
    <div class="detail-grid">
      <div class="card">
        <h3 class="card-title">基本资料</h3>
        <el-form :model="f" label-width="90px" style="max-width: 460px">
          <el-form-item label="用户名"><el-input v-model="f.username" disabled /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="f.realName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="f.phone" maxlength="11" /></el-form-item>
          <el-form-item label="角色"><el-input v-model="f.roleName" disabled /></el-form-item>
          <el-form-item label="所属组织"><el-input v-model="f.orgName" disabled /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
        <el-divider />
        <h3 class="card-title">修改密码</h3>
        <el-form :model="pw" label-width="90px" style="max-width: 460px">
          <el-form-item label="原密码"><el-input v-model="pw.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pw.newPassword" type="password" show-password /></el-form-item>
          <el-form-item><el-button @click="savePwd">修改密码</el-button></el-form-item>
        </el-form>
      </div>
      <div class="card">
        <h3 class="card-title">头像</h3>
        <div class="avatar-box">
          <img :src="f.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
          <el-upload :show-file-list="false" :http-request="upload" accept="image/*">
            <el-button :icon="Upload">上传新头像</el-button>
          </el-upload>
          <div class="hint">支持 jpg / png，单张不超过 2MB</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { me, updateProfile, changePassword } from '../api'
import http from '../api'

const f = ref({})
const pw = ref({ oldPassword: '', newPassword: '' })

async function load() { f.value = await me() }
async function saveProfile() {
  await updateProfile({ realName: f.value.realName, phone: f.value.phone, avatar: f.value.avatar })
  localStorage.setItem('user', JSON.stringify({ id: f.value.id, realName: f.value.realName, avatar: f.value.avatar, roleCode: f.value.roleCode }))
  ElMessage.success('资料已保存，头像已在右上角同步')
  load()
}
async function savePwd() {
  if (!pw.value.oldPassword || !pw.value.newPassword) return ElMessage.warning('请填写原密码与新密码')
  await changePassword(pw.value)
  ElMessage.success('密码已修改')
  pw.value = { oldPassword: '', newPassword: '' }
}
// 上传走 /api/file/upload，返回相对路径后回填并保存
async function upload(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  const d = await http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  f.value.avatar = d.url || d.path
  await saveProfile()
}
onMounted(load)
</script>

<style scoped>
.avatar-box { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 18px 0; }
.avatar-box img { width: 104px; height: 104px; border-radius: 50%; object-fit: cover; border: 3px solid #E3EBF2; }
.hint { font-size: 11px; color: #8A9AA8; }
</style>

<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ $t('menu.projectManagement') }}</span>
      <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon>{{ $t('common.create') }}</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="projects" stripe v-loading="loading">
        <el-table-column prop="name" label="项目名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default>
            <el-tag type="success" size="small">活跃</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑项目' : '新建项目'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="项目名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const projects = ref([
  { id: 1, name: 'TaaS Web Test', description: 'TaaS平台UI自动化测试', createdAt: '2025-12-01', status: 'active' },
  { id: 2, name: 'Demo Site Test', description: '演示站点回归测试', createdAt: '2026-01-15', status: 'active' },
  { id: 3, name: 'Admin Panel Test', description: '后台管理页面测试', createdAt: '2026-03-20', status: 'active' },
])
const dialogVisible = ref(false)
const editingId = ref(null)
const form = ref({ name: '', description: '' })

function handleCreate() {
  editingId.value = null
  form.value = { name: '', description: '' }
  dialogVisible.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  form.value = { name: row.name, description: row.description || '' }
  dialogVisible.value = true
}

function handleSave() {
  if (!form.value.name) { ElMessage.warning('请输入名称'); return }
  if (editingId.value) {
    const item = projects.value.find(p => p.id === editingId.value)
    if (item) Object.assign(item, form.value)
    ElMessage.success('更新成功')
  } else {
    projects.value.push({ id: Date.now(), ...form.value, createdAt: new Date().toISOString().slice(0,10), status: 'active' })
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该项目？', '确认', { type: 'warning' })
    projects.value = projects.value.filter(p => p.id !== row.id)
    ElMessage.success('删除成功')
  } catch { /* cancelled */ }
}
</script>

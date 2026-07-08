<template>
  <div class="project-management">
    <div class="header">
      <h2>{{ $t('apiTesting.project.title') }}</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        {{ $t('apiTesting.project.createProject') }}
      </el-button>
    </div>


    <!-- 项目列表 -->
    <el-table :data="projects" v-loading="loading" style="width: 100%">
      <el-table-column prop="name" :label="$t('apiTesting.project.projectName')" min-width="200" />
      <el-table-column prop="project_type" :label="$t('apiTesting.project.projectType')" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.project_type === 'HTTP' ? 'primary' : 'success'">
            {{ scope.row.project_type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="$t('apiTesting.project.projectStatus')" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 'active' ? '活跃' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" :label="$t('apiTesting.project.createdAt')" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.created_at) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('apiTesting.common.operation')" width="220" fixed="right">
        <template #default="scope">
          <div class="op-buttons">
            <el-button link type="primary" size="small" @click="editProject(scope.row)">{{ $t('apiTesting.common.edit') }}</el-button>
            <el-button link type="primary" size="small" @click="viewProject(scope.row)">{{ $t('apiTesting.common.view') }}</el-button>
            <el-button link type="danger" size="small" @click="deleteProject(scope.row)">{{ $t('apiTesting.common.delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      class="pagination"
    />

    <!-- 新建/编辑项目对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingProject ? $t('apiTesting.project.editProject') : $t('apiTesting.project.createProject')"
      width="600px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item :label="$t('apiTesting.project.projectName')" prop="name">
          <el-input v-model="form.name" :placeholder="$t('apiTesting.project.inputProjectName')" />
        </el-form-item>

        <el-form-item :label="$t('apiTesting.project.projectDescription')" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            :placeholder="$t('apiTesting.project.inputProjectDesc')"
          />
        </el-form-item>

        <el-form-item :label="$t('apiTesting.project.projectType')" prop="project_type">
          <el-radio-group v-model="form.project_type">
            <el-radio value="HTTP">HTTP</el-radio>
            <el-radio value="WEBSOCKET">WebSocket</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="$t('apiTesting.project.projectStatus')" prop="status">
          <el-select v-model="form.status">
            <el-option label="活跃" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </el-form-item>

      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">{{ $t('apiTesting.common.cancel') }}</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ editingProject ? $t('apiTesting.common.update') : $t('apiTesting.common.create') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看项目详情对话框 -->
    <el-dialog
      v-model="showViewDialog"
      :title="$t('apiTesting.project.viewProject')"
      width="600px"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="$t('apiTesting.project.projectName')">{{ viewedProject?.name }}</el-descriptions-item>
        <el-descriptions-item :label="$t('apiTesting.project.projectDescription')">{{ viewedProject?.description || $t('apiTesting.project.none') }}</el-descriptions-item>
        <el-descriptions-item :label="$t('apiTesting.project.projectType')">
          <el-tag :type="viewedProject?.project_type === 'HTTP' ? 'primary' : 'success'">
            {{ viewedProject?.project_type }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('apiTesting.project.projectStatus')">
          <el-tag :type="getStatusType(viewedProject?.status)">
            {{ getStatusText(viewedProject?.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('apiTesting.project.createdAt')">{{ formatDate(viewedProject?.created_at) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('apiTesting.project.updatedAt')">{{ formatDate(viewedProject?.updated_at) }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="showViewDialog = false">{{ $t('apiTesting.common.close') }}</el-button>
        <el-button type="primary" @click="editProject(viewedProject)">{{ $t('apiTesting.common.edit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, ElDescriptions, ElDescriptionsItem } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import api from '@/utils/request'
import dayjs from 'dayjs'

const { t } = useI18n()
const loading = ref(false)
const projects = ref([])
const users = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showCreateDialog = ref(false)
const showViewDialog = ref(false)
const editingProject = ref(null)
const viewedProject = ref(null)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  name: '',
  description: '',
  project_type: 'HTTP',
  status: 'active'
})

const rules = computed(() => ({
  name: [
    { required: true, message: t('apiTesting.project.inputProjectName'), trigger: 'blur' }
  ],
  project_type: [
    { required: true, message: t('apiTesting.common.pleaseSelect'), trigger: 'change' }
  ]
}))

const getStatusType = (status) => {
  const typeMap = {
    'NOT_STARTED': 'info',
    'IN_PROGRESS': 'warning',
    'COMPLETED': 'success'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusKey = {
    'NOT_STARTED': 'notStarted',
    'IN_PROGRESS': 'inProgress',
    'COMPLETED': 'completed'
  }[status]
  return statusKey ? t(`apiTesting.project.status.${statusKey}`) : status
}

const formatDate = (dateString) => {
  return dayjs(dateString).format('YYYY-MM-DD HH:mm')
}

const loadProjects = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/api-testing/projects/', {
      params: {
        page: currentPage.value,
        page_size: pageSize.value
      }
    })
    projects.value = response.data.results
    total.value = response.data.count
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadUsers = async () => {
  try {
    const response = await api.get('/api/api-testing/users/')
    users.value = response.data.results || response.data
  } catch (error) {
    console.error(error)
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadProjects()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadProjects()
}

const editProject = (project) => {
  editingProject.value = project
  form.name = project.name
  form.description = project.description || ''
  form.project_type = project.project_type || 'HTTP'
  form.status = project.status || 'active'
  showCreateDialog.value = true
}

const viewProject = (project) => {
  // 显示项目详情弹框
  showViewDialog.value = true
  viewedProject.value = project
}

const deleteProject = async (project) => {
  try {
    await ElMessageBox.confirm(
      t('apiTesting.project.confirmDelete', { name: project.name }),
      t('apiTesting.messages.confirm.deleteTitle'),
      {
        confirmButtonText: t('apiTesting.common.confirm'),
        cancelButtonText: t('apiTesting.common.cancel'),
        type: 'warning'
      }
    )

    await api.delete(`/api/api-testing/projects/${project.id}/`)
    ElMessage.success(t('apiTesting.messages.success.delete'))
    await loadProjects()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(t('apiTesting.messages.error.deleteFailed'))
      console.error(error)
    }
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    const data = { ...form }
    if (data.start_date) {
      data.start_date = dayjs(data.start_date).format('YYYY-MM-DD')
    }
    if (data.end_date) {
      data.end_date = dayjs(data.end_date).format('YYYY-MM-DD')
    }
    
    if (editingProject.value) {
      await api.patch(`/api/api-testing/projects/${editingProject.value.id}/`, data)
      ElMessage.success('项目更新成功')
    } else {
      await api.post('/api/api-testing/projects/', data)
      ElMessage.success('项目创建成功')
    }

    showCreateDialog.value = false
    await loadProjects()
  } catch (error) {
    ElMessage.error('操作失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  editingProject.value = null
  Object.assign(form, {
    name: '',
    description: '',
    project_type: 'HTTP',
    status: 'active'
  })
  formRef.value?.resetFields()
}

onMounted(async () => {
  await Promise.all([loadProjects(), loadUsers()])
})
</script>

<style scoped>
.project-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.op-buttons {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
}
</style>
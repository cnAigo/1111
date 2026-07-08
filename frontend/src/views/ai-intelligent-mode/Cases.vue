<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">{{ $t('uiAutomation.ai.caseList.title') }}</h1>
    </div>

    <div class="card-container">
      <div class="filter-bar" style="display:flex;justify-content:space-between;align-items:center">
        <el-input
          v-model="searchText"
          :placeholder="$t('uiAutomation.ai.caseList.searchPlaceholder')"
          clearable
          @input="handleSearch"
          style="width: 300px;"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div style="display:flex;gap:8px">
          <el-button type="success" size="small" :disabled="selectedCases.length === 0" @click="batchRun">
            批量执行({{ selectedCases.length }})
          </el-button>
          <el-button type="primary" size="small" @click="showEditDialog = true; editingCase = false; resetEditForm()">
            <el-icon><Plus /></el-icon> 添加
          </el-button>
        </div>
      </div>

      <el-table :data="cases" v-loading="loading" style="width: 100%" @selection-change="val => selectedCases = val">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="name" :label="$t('uiAutomation.ai.caseList.caseName')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="description" :label="$t('uiAutomation.common.description')" width="120" show-overflow-tooltip />
        <el-table-column prop="task_description" :label="$t('uiAutomation.ai.caseList.taskDescription')" min-width="300" show-overflow-tooltip />
        <el-table-column prop="created_at" :label="$t('uiAutomation.common.createTime')" width="180" :formatter="formatDate" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="runCase(row)" style="margin:0 1px">运行</el-button>
            <el-button size="small" type="primary" @click="editCase(row)" style="margin:0 1px">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteCase(row.id)" style="margin:0 1px">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEditDialog" :title="$t('uiAutomation.ai.caseList.editCase')" width="750px" :close-on-click-modal="false">
      <el-form :model="editForm" :rules="formRules" ref="editFormRef" label-width="100px">
        <el-form-item :label="$t('uiAutomation.ai.caseList.caseName')" prop="name">
          <el-input v-model="editForm.name" :placeholder="$t('uiAutomation.ai.caseNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('uiAutomation.common.description')" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="3" :placeholder="$t('uiAutomation.ai.caseDescPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('uiAutomation.ai.caseList.taskDescription')" prop="task_description">
          <el-input
            v-model="editForm.task_description"
            type="textarea"
            :rows="12"
            :placeholder="$t('uiAutomation.ai.taskPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="warning" @click="polishInEdit" :loading="polishing">AI润色</el-button>
          <el-button @click="showEditDialog = false">{{ $t('uiAutomation.common.cancel') }}</el-button>
          <el-button type="primary" @click="confirmEdit" :loading="saving">{{ $t('uiAutomation.common.save') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, VideoPlay, Edit, Delete, Plus, MagicStick } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import {
  getAICases,
  createAICase,
  updateAICase,
  deleteAICase,
  runAICase
} from '@/api/ui-automation'
import request from '@/utils/request'

const { t } = useI18n()
const router = useRouter()
const cases = ref([])
const loading = ref(false)
const selectedCases = ref([])
const searchText = ref('')
const total = ref(0)
const pagination = reactive({
  currentPage: 1,
  pageSize: 20
})

const showEditDialog = ref(false)
const saving = ref(false)
const polishing = ref(false)
const editingCase = ref(false)
const currentCaseId = ref(null)
const editForm = reactive({
  name: '',
  description: '',
  task_description: ''
})
const editFormRef = ref(null)

const formRules = computed(() => ({
  name: [{ required: true, message: t('uiAutomation.ai.rules.nameRequired'), trigger: 'blur' }],
  task_description: [{ required: true, message: t('uiAutomation.ai.caseList.rules.taskDescriptionRequired'), trigger: 'blur' }]
}))

// 加载用例列表
const loadCases = async () => {
  loading.value = true
  try {
    const response = await getAICases({
      page: pagination.currentPage,
      page_size: pagination.pageSize,
      search: searchText.value
    })

    cases.value = response.data.results || []
    total.value = response.data.count || 0
  } catch (error) {
    console.error('获取用例列表失败:', error)
    ElMessage.error(t('uiAutomation.ai.caseList.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadCases()
}

const handleSizeChange = () => {
  pagination.currentPage = 1
  loadCases()
}

const batchRun = async () => {
  if (selectedCases.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定批量执行 ${selectedCases.value.length} 个用例？`, '批量执行', { type: 'info' })
  } catch (e) { return }
  const ids = selectedCases.value.map(c => c.id)
  try {
    await request.post('/api/ui-automation/ai-cases/batch-run/', { ids })
    ElMessage.success(`已启动 ${ids.length} 个用例`)
    selectedCases.value = []
  } catch (e) {
    ElMessage.error('批量执行失败')
  }
}

const handleCurrentChange = () => {
  loadCases()
}

const resetEditForm = () => {
  editForm.name = ''
  editForm.description = ''
  editForm.task_description = ''
  currentCaseId.value = null
}

// AI润色（编辑弹窗内）
const polishInEdit = async () => {
  if (!editForm.task_description.trim()) return
  polishing.value = true
  try {
    const r = await request.post('/api/ui-automation/ai-cases/polish/', { text: editForm.task_description })
    const polished = r.data?.polished || r.data
    if (polished && typeof polished === 'string') {
      editForm.task_description = polished.trim()
      ElMessage.success('润色完成')
    }
  } catch (e) {
    ElMessage.error('润色失败')
  } finally { polishing.value = false }
}

// 编辑用例
const editCase = (row) => {
  editingCase.value = true
  currentCaseId.value = row.id
  editForm.name = row.name
  editForm.description = row.description
  editForm.task_description = row.task_description
  showEditDialog.value = true
}

const confirmEdit = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        if (editingCase.value) {
          await updateAICase(currentCaseId.value, {
            name: editForm.name,
            description: editForm.description,
            task_description: editForm.task_description
          })
          ElMessage.success(t('uiAutomation.ai.caseList.messages.updateSuccess'))
        } else {
          await createAICase({
            name: editForm.name,
            description: editForm.description,
            task_description: editForm.task_description
          })
          ElMessage.success('用例已创建')
        }
        showEditDialog.value = false
        loadCases()
      } catch (error) {
        console.error('更新失败:', error)
        ElMessage.error(t('uiAutomation.ai.caseList.messages.updateFailed'))
      } finally {
        saving.value = false
      }
    }
  })
}

// 删除用例
const deleteCase = async (id) => {
  try {
    await ElMessageBox.confirm(
      t('uiAutomation.ai.caseList.messages.deleteConfirm'),
      t('uiAutomation.messages.confirm.tip'),
      {
        confirmButtonText: t('uiAutomation.common.confirm'),
        cancelButtonText: t('uiAutomation.common.cancel'),
        type: 'warning'
      }
    )

    await deleteAICase(id)
    ElMessage.success(t('uiAutomation.ai.caseList.messages.deleteSuccess'))
    loadCases()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(t('uiAutomation.ai.caseList.messages.deleteFailed'))
    }
  }
}

// 执行用例
const runCase = async (row) => {
  try {
    await runAICase(row.id)
    ElMessage.success(t('uiAutomation.ai.caseList.messages.runSuccess'))
    // 跳转到执行记录页面
    router.push('/ai-intelligent-mode/execution-records')
  } catch (error) {
    console.error('执行失败:', error)
    ElMessage.error(t('uiAutomation.ai.caseList.messages.runFailed'))
  }
}

const formatDate = (row, column, cellValue) => {
  if (!cellValue) return ''
  return new Date(cellValue).toLocaleString()
}

onMounted(() => {
  loadCases()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .page-title {
    font-size: 20px;
    font-weight: 600;
    margin: 0;
  }
}

.card-container {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.filter-bar {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

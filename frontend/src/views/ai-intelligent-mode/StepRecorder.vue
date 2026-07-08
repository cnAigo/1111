<template>
  <!-- 录制步骤库：右侧抽屉 -->
  <el-drawer v-model="visible" title="操作脚本库" direction="rtl" size="480px">
    <!-- 列表 -->
    <div class="recorder-list">
      <div v-if="records.length === 0" style="color:#c0c4cc;text-align:center;padding:60px 0">
        暂无录制脚本<br><br>
        <span style="font-size:13px">从 AI用例管理 运行用例后会自动录制</span>
      </div>

      <div v-for="(group, caseId) in groupedRecords" :key="caseId" class="record-group">
        <div class="record-group__header" @click="toggleGroup(caseId)">
          <span class="record-group__name">{{ getCaseName(caseId) || '用例 #' + caseId }}</span>
          <el-tag size="small" type="info">{{ group.length }}步</el-tag>
          <el-icon :class="{ rotated: expandedGroups.has(caseId) }" style="margin-left:auto"><ArrowDown /></el-icon>
        </div>

        <div v-if="expandedGroups.has(caseId)" class="record-group__steps">
          <div v-for="(step, i) in group" :key="step.id" class="step-line"
               :class="{ editing: editingStepId === step.id }">
            <span class="step-num">{{ i + 1 }}</span>
            <el-tag size="small" :type="tagType(step.action_type || step.actionType)">{{ step.action_type || step.actionType }}</el-tag>
            <span class="step-sel" :title="step.playwright_code">
              <template v-if="step.element_tag">{{ step.element_tag }}</template>
              <template v-if="step.element_text"> "{{ step.element_text.substring(0,30) }}"</template>
              <template v-if="!step.element_tag && !step.element_text">{{ step.selector || step.input_value || '-' }}</template>
            </span>
            <el-button size="small" text @click="editStep(step)"><el-icon><Edit /></el-icon></el-button>
          </div>
          <div style="display:flex;gap:6px;margin-top:8px;padding-left:20px">
            <el-button size="small" type="success" @click="replayGroup(caseId)">▶ 回放</el-button>
            <el-button size="small" type="danger" @click="deleteGroup(caseId)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showEdit" title="编辑步骤" width="500px" append-to-body>
      <el-form label-width="70px" size="small">
        <el-form-item label="动作">
          <el-select v-model="editForm.actionType">
            <el-option label="goto" value="goto" /><el-option label="click" value="click" />
            <el-option label="right_click" value="right_click" /><el-option label="fill" value="fill" />
            <el-option label="type" value="type" /><el-option label="assert" value="assert" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择器">
          <el-input v-model="editForm.selector" />
        </el-form-item>
        <el-form-item label="输入值">
          <el-input v-model="editForm.inputValue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit=false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Edit } from '@element-plus/icons-vue'
import request from '@/utils/request'

const visible = defineModel({ default: false })
const records = ref([])
const expandedGroups = ref(new Set())
const showEdit = ref(false)
const editingStepId = ref(null)
const editForm = ref({ actionType: 'click', selector: '', inputValue: '' })

// Group records by testCaseId
const groupedRecords = computed(() => {
  const groups = {}
  for (const r of records.value) {
    const cid = r.test_case_id || 0
    if (!groups[cid]) groups[cid] = []
    groups[cid].push(r)
  }
  // Sort each group by stepOrder
  for (const g of Object.values(groups)) g.sort((a, b) => (a.step_order || 0) - (b.step_order || 0))
  return groups
})

const loadRecords = async () => {
  try {
    // Load all steps from all test cases
    const r = await request.get('/api/ui-automation/ai-cases/steps/all/')
    records.value = r.data?.steps || []
  } catch { records.value = [] }
}

watch(visible, async (v) => { if (v) await loadRecords() })

const toggleGroup = (id) => {
  if (expandedGroups.value.has(id)) expandedGroups.value.delete(id)
  else expandedGroups.value.add(id)
  expandedGroups.value = new Set(expandedGroups.value) // trigger reactivity
}

const tagType = (a) => a === 'click' || a === 'right_click' ? '' : a === 'fill' || a === 'type' ? 'warning' : a === 'goto' ? 'success' : 'info'

const getCaseName = (id) => {
  // Try to get case name from records
  const r = records.value.find(x => x.test_case_id == id)
  return r?.case_name || null
}

const editStep = (step) => {
  editingStepId.value = step.id
  editForm.value = { actionType: step.action_type || step.actionType, selector: step.selector || '', inputValue: step.input_value || step.inputValue || '' }
  showEdit.value = true
}

const saveEdit = async () => {
  try {
    await request.patch(`/api/ui-automation/test-steps/${editingStepId.value}/`, editForm.value)
    ElMessage.success('已更新'); showEdit.value = false; loadRecords()
  } catch { ElMessage.error('更新失败') }
}

const replayGroup = async (caseId) => {
  try {
    await request.post(`/api/ui-automation/ai-cases/${caseId}/replay/`)
    ElMessage.success('回放已启动，请查看执行记录')
  } catch { ElMessage.error('回放失败') }
}

const deleteGroup = async (caseId) => {
  try {
    await ElMessageBox.confirm('确定删除该用例的所有录制步骤？', '确认', { type: 'warning' })
    await request.delete(`/api/ui-automation/test-steps/by-case/${caseId}/`)
    ElMessage.success('已删除'); loadRecords()
  } catch {}
}
</script>

<style lang="scss" scoped>
.recorder-list { padding: 4px 0; }

.record-group {
  border: 1px solid #eee;
  border-radius: 10px;
  margin-bottom: 10px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: #fafafa;
    cursor: pointer;
    font-size: 14px;
    &:hover { background: #f0f0f0; }
  }
  &__name { font-weight: 600; flex: 1; }
  &__steps { padding: 8px 12px 12px; }
}

.rotated { transform: rotate(180deg); transition: .2s; }

.step-line {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  &:hover { background: #f5f7fa; }
  &.editing { background: #e8f4fd; }
}

.step-num {
  width: 20px; height: 20px;
  background: #1d88e3; color: #fff;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; flex-shrink: 0;
}

.step-sel {
  flex: 1;
  font-size: 12px;
  color: #606266;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  font-family: monospace;
}
</style>

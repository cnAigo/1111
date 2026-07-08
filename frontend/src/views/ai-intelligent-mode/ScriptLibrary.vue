<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">操作脚本库</h1>
      <span style="color:#909399;font-size:13px">从 AI用例管理 运行后自动录制的可回放步骤</span>
    </div>

    <div v-if="Object.keys(groups).length === 0" class="card-container" style="text-align:center;padding:80px 0;color:#c0c4cc">
      <div style="font-size:48px;margin-bottom:16px">📋</div>
      <div>暂无录制脚本</div>
      <div style="font-size:12px;margin-top:8px">从「AI用例管理」运行用例成功后，步骤会自动录制到这里</div>
    </div>

    <div v-for="(group, caseId) in groups" :key="caseId" class="script-card">
      <div class="script-card__header">
        <div>
          <span class="script-card__title">{{ getCaseName(group) || '用例 #' + caseId }}</span>
          <el-tag size="small" type="info" style="margin-left:8px">{{ group.length }} 步</el-tag>
          <span class="script-card__url" v-if="group[0]?.page_url">{{ group[0].page_url }}</span>
        </div>
        <div style="display:flex;gap:8px">
          <el-button size="small" type="success" @click="replayGroup(caseId)">▶ 回放</el-button>
          <el-button size="small" @click="exportCode(caseId, group)">复制代码</el-button>
          <el-button size="small" type="danger" @click="deleteGroup(caseId)">删除</el-button>
        </div>
      </div>

      <div class="script-card__steps">
        <div v-for="(step, i) in group" :key="step.id" class="step-row">
          <span class="step-row__num">{{ i + 1 }}</span>
          <el-tag size="small" :type="tagType(step.action_type)">{{ step.action_type }}</el-tag>

          <!-- Element detail block -->
          <div class="step-row__detail">
            <template v-if="step.element_tag">
              <code class="step-tag">{{ step.element_tag }}</code>
              <span v-if="step.element_text" class="step-text">"{{ step.element_text.substring(0, 50) }}"</span>
            </template>
            <span v-else class="step-text">{{ step.selector || step.input_value || '-' }}</span>
          </div>

          <code class="step-row__code" :title="step.playwright_code">{{ step.playwright_code }}</code>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const records = ref([])

const groups = computed(() => {
  const g = {}
  for (const r of records.value) {
    const cid = r.test_case_id || 0
    if (!g[cid]) g[cid] = []
    g[cid].push(r)
  }
  for (const arr of Object.values(g)) arr.sort((a, b) => (a.step_order || 0) - (b.step_order || 0))
  return g
})

onMounted(async () => {
  try { const r = await request.get('/api/ui-automation/ai-cases/steps/all/'); records.value = r.data?.steps || [] }
  catch {}
})

const getCaseName = (group) => group[0]?.case_name || null

const tagType = (a) => a === 'click' || a === 'right_click' ? '' : a === 'fill' || a === 'type' ? 'warning' : a === 'goto' ? 'success' : 'info'

const replayGroup = async (caseId) => {
  try {
    await request.post(`/api/ui-automation/ai-cases/${caseId}/replay/`)
    ElMessage.success('回放已启动，请查看执行记录')
  } catch { ElMessage.error('回放失败') }
}

const exportCode = (caseId, group) => {
  const lines = group.map(s => s.playwright_code).filter(Boolean)
  navigator.clipboard.writeText(lines.join('\n')).then(() => ElMessage.success('已复制 ' + lines.length + ' 行代码'))
}

const deleteGroup = async (caseId) => {
  try {
    await ElMessageBox.confirm('确定删除该脚本？', '确认', { type: 'warning' })
    await request.delete(`/api/ui-automation/test-steps/by-case/${caseId}/`)
    records.value = records.value.filter(r => r.test_case_id != caseId)
  } catch {}
}
</script>

<style lang="scss" scoped>
.script-card {
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  background: #fff;
  margin-bottom: 16px;
  overflow: hidden;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 20px;
    background: #fafafa;
    border-bottom: 1px solid #eee;
  }
  &__title { font-size: 16px; font-weight: 650; color: #303133; }
  &__url { display: block; font-size: 12px; color: #909399; margin-top: 4px; font-family: monospace; }
  &__steps { padding: 12px 20px; }
}

.step-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }

  &__num {
    width: 24px; height: 24px;
    border-radius: 50%;
    background: #1d88e3;
    color: #fff;
    display: flex; align-items: center; justify-content: center;
    font-size: 12px; font-weight: 700; flex-shrink: 0;
  }
  &__detail { flex: 1; display: flex; align-items: center; gap: 6px; min-width: 0; }
  &__code {
    font-size: 11px;
    color: #409eff;
    max-width: 400px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    background: #f0f7ff;
    padding: 2px 8px;
    border-radius: 4px;
  }
}

.step-tag {
  background: #f0f0f0;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
  color: #606266;
}

.step-text {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

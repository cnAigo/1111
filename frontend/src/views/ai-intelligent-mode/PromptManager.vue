<template>
  <!-- 独立页面模式 -->
  <div v-if="standalone" class="page-container">
    <div class="page-header">
      <h1 class="page-title">提示词管理</h1>
      <el-button type="primary" @click="addNew"><el-icon><Plus /></el-icon> 新建模板</el-button>
    </div>

    <!-- 两大方块并排 -->
    <div class="prompt-two-block">
      <!-- 左：模板列表 -->
      <div class="prompt-block prompt-block--list">
        <div class="prompt-block__title">提示词模板</div>
        <div class="prompt-block__body">
          <div v-if="prompts.length===0" style="color:#c0c4cc;text-align:center;padding:40px 0">暂无模板</div>
          <div
            v-for="p in prompts" :key="p.id"
            class="prompt-card"
            :class="{ active: selectedId===p.id }"
            @click="selectPrompt(p)"
          >
            <div class="prompt-card__name">{{ p.name }}</div>
            <el-tag size="small" :type="p.type==='system'?'':'info'">{{ p.type==='system'?'系统':p.type==='user'?'用户':'步骤' }}</el-tag>
            <el-button size="small" type="danger" text @click.stop="del(p.id)" style="margin-left:auto">删除</el-button>
          </div>
        </div>
      </div>

      <!-- 右：编辑区 -->
      <div class="prompt-block prompt-block--edit">
        <div class="prompt-block__title">{{ editingId ? '编辑 — '+editForm.name : '新建模板' }}</div>
        <div class="prompt-block__body" style="display:flex;flex-direction:column;gap:12px">
          <el-input v-model="editForm.name" placeholder="模板名称" size="large" />
          <el-select v-model="editForm.type" size="large" style="width:100%">
            <el-option label="系统提示 (system)" value="system" />
            <el-option label="用户提示 (user)" value="user" />
            <el-option label="步骤提示 (step)" value="step" />
          </el-select>
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="16"
            placeholder="输入提示词正文..."
          />
          <div style="display:flex;gap:8px;justify-content:flex-end">
            <el-button v-if="editingId && defaultId!==selectedId" @click="setDefault" plain>设为默认</el-button>
            <el-tag v-if="editingId && defaultId===selectedId" type="success">当前默认</el-tag>
            <el-button type="primary" @click="savePrompt" :disabled="!editForm.content.trim()">保存</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 抽屉模式（从 AI 测试页打开） -->
  <el-drawer v-else v-model="visible" title="提示词管理" direction="rtl" size="520px" :close-on-click-modal="true"
    custom-class="prompt-drawer"
  >
    <div class="prompt-panel">
      <!-- 上半：模板列表 -->
      <div class="prompt-list-section">
        <div class="section-label">提示词模板</div>
        <div class="prompt-list">
          <div
            v-for="p in prompts"
            :key="p.id"
            class="prompt-item"
            :class="{ active: selectedId === p.id }"
            @click="selectPrompt(p)"
          >
            <div class="prompt-item-name">{{ p.name }}</div>
            <div class="prompt-item-type">{{ p.type === 'system' ? '系统' : p.type === 'user' ? '用户' : '步骤' }}</div>
          </div>
          <div v-if="prompts.length === 0" class="empty-hint">暂无模板，点击下方"新建模板"添加</div>
        </div>
        <el-button size="small" text type="primary" @click="addNew" style="margin-top:8px">
          <el-icon><Plus /></el-icon> 新建模板
        </el-button>
      </div>

      <!-- 下半：编辑区 -->
      <div class="prompt-edit-section" v-if="selectedId || showNewForm">
        <div class="section-label">{{ editingId ? '编辑' : '新建' }}</div>
        <el-input v-if="showNewForm || editingId" v-model="editForm.name" placeholder="模板名称" size="small" style="margin-bottom:10px" />
        <el-select v-model="editForm.type" size="small" style="width:100%;margin-bottom:10px">
          <el-option label="系统 (system)" value="system" />
          <el-option label="用户 (user)" value="user" />
          <el-option label="步骤 (step)" value="step" />
        </el-select>
        <el-input
          v-model="editForm.content"
          type="textarea"
          :rows="14"
          placeholder="输入提示词正文…&#10;&#10;例如：&#10;你是一个严谨的 Web UI 自动化测试引擎。&#10;将操作指令、DOM属性、截图转换为 Playwright 可执行的严格 JSON。"
        />
      </div>
    </div>

    <!-- 底部固定操作栏 -->
    <template #footer>
      <div class="prompt-footer">
        <el-button @click="setDefault" :disabled="!selectedId" :type="defaultId === selectedId ? 'info' : 'primary'" plain>
          {{ defaultId === selectedId ? '已是默认' : '设为默认使用' }}
        </el-button>
        <el-button type="primary" @click="savePrompt" :disabled="!editForm.content.trim()">
          {{ editingId ? '更新配置' : '保存配置' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({ standalone: { type: Boolean, default: false } })
const visible = defineModel({ default: false })
const showEdit = ref(false)
const prompts = ref([])
const selectedId = ref(null)
const editingId = ref(null)
const defaultId = ref(null)
const showNewForm = ref(false)
const editForm = reactive({ name: '', content: '', type: 'system' })

// 暴露给父组件调用：onSave(activeContent) — 返回当前启用的提示词文本
const emit = defineEmits(['select'])
const load = async () => {
  try { const r = await request.get('/api/ui-automation/prompts/'); prompts.value = r.data.results || [] } catch {}
}

// 抽屉打开时加载
watch(visible, async (v) => { if (v) await load() })

// 选中模板
const selectPrompt = (p) => {
  selectedId.value = p.id; editingId.value = p.id; showNewForm.value = false
  editForm.name = p.name; editForm.content = p.content; editForm.type = p.type
  emit('select', p.content)
}

// 新建
const addNew = () => {
  selectedId.value = null; editingId.value = null; showNewForm.value = true
  editForm.name = ''; editForm.content = ''; editForm.type = 'system'
}

// 保存
const savePrompt = async () => {
  try {
    if (editingId.value) {
      await request.patch(`/api/ui-automation/prompts/${editingId.value}/`, { ...editForm })
    } else {
      const r = await request.post('/api/ui-automation/prompts/', { ...editForm })
      editingId.value = r.data.id; selectedId.value = r.data.id; showNewForm.value = false
    }
    ElMessage.success('已保存'); load()
  } catch { ElMessage.error('保存失败') }
}

const del = async (id) => {
  try { await ElMessageBox.confirm('确定删除？','确认',{type:'warning'}); await request.delete(`/api/ui-automation/prompts/${id}/`); load(); ElMessage.success('已删除') } catch {}
}

// 设为默认
const setDefault = () => { defaultId.value = selectedId.value; ElMessage.success('已设为默认使用的提示词') }

// 获取当前默认提示词文本（供外部调用）
const getActiveContent = () => editForm.content
defineExpose({ getActiveContent, load })
</script>

<style lang="scss" scoped>
/* ── 两大方块布局 ── */
.prompt-two-block {
  display: flex;
  gap: 24px;
  height: calc(100vh - 140px);
}

.prompt-block {
  border: 1px dashed #d0d5dd;
  border-radius: 14px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &--list { flex: 0 0 380px; }
  &--edit { flex: 1; }

  &__title {
    font-size: 15px;
    font-weight: 700;
    color: #1d88e3;
    padding: 16px 20px;
    border-bottom: 1px solid #eee;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 16px 20px;
  }
}

.prompt-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  border-left: 4px solid transparent;
  margin-bottom: 6px;
  transition: all 0.15s;

  &:hover { background: #f5f7fa; }
  &.active {
    background: #e8f4fd;
    border-left-color: #1d88e3;
    .prompt-card__name { color: #1d88e3; font-weight: 650; }
  }

  &__name { font-size: 14px; color: #303133; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

.prompt-drawer :deep(.el-drawer__body) {
  padding: 0 20px;
  display: flex;
  flex-direction: column;
}

.prompt-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-label {
  font-size: 13px;
  font-weight: 650;
  color: #6b7280;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.prompt-list-section {
  flex-shrink: 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.prompt-list {
  max-height: 180px;
  overflow-y: auto;
}

.prompt-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  border-left: 3px solid transparent;
  margin-bottom: 4px;
  transition: all 0.15s;

  &:hover { background: #f5f7fa; }
  &.active {
    background: #e8f4fd;
    border-left-color: #1677FF;
    .prompt-item-name { color: #1677FF; font-weight: 650; }
  }
}

.prompt-item-name { font-size: 14px; color: #303133; margin-bottom: 2px; }
.prompt-item-type { font-size: 11px; color: #909399; }

.empty-hint { font-size: 13px; color: #c0c4cc; text-align: center; padding: 24px 0; }

.prompt-edit-section {
  flex: 1;
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  min-height: 0;

  :deep(.el-textarea) { flex: 1; }
  :deep(.el-textarea__inner) { flex: 1; min-height: 260px; resize: none; font-size: 13px; line-height: 1.7; }
}

.prompt-footer {
  display: flex;
  justify-content: space-between;
  width: 100%;
}
</style>

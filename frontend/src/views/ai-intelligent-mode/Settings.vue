<template>
  <div class="page-container">
    <div class="page-header">
      <el-icon style="margin-right:8px;vertical-align:middle"><Setting /></el-icon>
      <span class="page-title">API 配置</span>
    </div>

    <el-alert type="warning" :closable="false" show-icon style="margin-bottom:20px">
      <template #title><strong>Token 需要开启图形识别（视觉）能力</strong></template>
    </el-alert>

    <el-row :gutter="20">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>AI 模型配置</span></template>
          <el-form :model="form" label-width="100px">
            <el-form-item label="配置名称">
              <el-input v-model="form.name" placeholder="例如：MiMo测试" />
            </el-form-item>
            <el-form-item label="提供商">
              <el-select v-model="form.provider" style="width:100%" @change="onProviderChange">
                <el-option label="OpenAI" value="openai" />
                <el-option label="DeepSeek" value="deepseek" />
                <el-option label="通义千问" value="qwen" />
                <el-option label="硅基流动" value="siliconflow" />
                <el-option label="智谱" value="zhipu" />
                <el-option label="小米MiMo" value="mimo" />
                <el-option label="其他(OpenAI兼容)" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="API Base URL" v-if="form.provider === 'mimo' || form.provider === 'other'">
              <el-input v-model="form.base_url" placeholder="https://api.xiaomi.com/v1" />
            </el-form-item>
            <el-form-item label="API Token">
              <el-input v-model="form.api_key" type="password" show-password placeholder="sk-..." />
            </el-form-item>
            <el-form-item label="模型名称">
              <el-select v-model="form.model_name" style="width:100%" filterable allow-create placeholder="输入或选择模型">
                <el-option v-if="form.provider === 'openai'" label="gpt-4o" value="gpt-4o" />
                <el-option v-if="form.provider === 'openai'" label="gpt-4o-mini" value="gpt-4o-mini" />
                <el-option v-if="form.provider === 'mimo'" label="mimo-v2.5" value="mimo-v2.5" />
                <el-option v-if="form.provider === 'deepseek'" label="deepseek-chat" value="deepseek-chat" />
                <el-option v-if="form.provider === 'qwen'" label="qwen-vl-max" value="qwen-vl-max" />
                <el-option v-if="form.provider === 'zhipu'" label="glm-4v" value="glm-4v" />
                <el-option v-if="form.provider === 'siliconflow'" label="Qwen/Qwen2-VL-72B" value="Qwen/Qwen2-VL-72B" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="testConnection" :loading="testing">测试连接</el-button>
              <el-button @click="saveConfig" :loading="saving">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 已保存的配置列表 -->
        <el-card shadow="never" style="margin-top:16px" v-if="configs.length > 0">
          <template #header><span>已保存配置</span></template>
          <el-table :data="configs" size="small">
            <el-table-column prop="name" label="名称" width="120" />
            <el-table-column prop="model_type" label="提供商" width="90" />
            <el-table-column prop="model_name" label="模型" width="130" />
            <el-table-column prop="base_url" label="API地址" show-overflow-tooltip />
            <el-table-column label="Key" width="80">
              <template #default="{row}"><el-tag :type="row.api_key_length>0?'success':'danger'" size="small">{{row.api_key_length>0?'已设置':'未设置'}}</el-tag></template>
            </el-table-column>
            <el-table-column label="状态" width="70">
              <template #default="{row}"><el-tag :type="row.is_active?'success':'info'" size="small">{{row.is_active?'启用':'停用'}}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{row}">
                <el-button link size="small" @click="editConfig(row)">详情</el-button>
                <el-button v-if="!row.is_active" link size="small" type="success" @click="activateConfig(row)">启用</el-button>
                <el-button v-else link size="small" type="warning" @click="deactivateConfig(row)">停用</el-button>
                <el-button link size="small" type="danger" @click="deleteConfig(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>视觉能力说明</span></template>
          <div v-if="form.provider === 'mimo'">
            <el-tag type="danger" size="large" style="margin-bottom:12px">小米 MiMo</el-tag>
            <p>MiMo 多模态模型支持图像理解和视觉识别。</p>
            <p style="color:#909399;font-size:12px">Base URL: https://api.xiaomi.com（具体地址以官方文档为准）</p>
          </div>
          <div v-else>
            <p>选择支持视觉/多模态的模型，AI 才能分析页面截图。</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const form = reactive({
  name: 'AI智能测试',
  provider: 'openai',
  model_name: 'gpt-4o',
  api_key: '',
  base_url: '',
  _editingId: null
})

const testing = ref(false)
const saving = ref(false)
const configs = ref([])

function onProviderChange(prov) {
  form.base_url = prov === 'mimo' ? 'https://api.xiaomimimo.com/v1' : ''
}

async function testConnection() {
  if (!form.api_key) { ElMessage.warning('请先输入 API Token'); return }
  testing.value = true
  try {
    const resp = await request.post('/api/ui-automation/config/ai-mode/test_connection/', {
      model_type: form.provider === 'mimo' ? 'other' : form.provider,
      model_name: form.model_name,
      api_key: form.api_key,
      base_url: form.base_url
    })
    if (resp.data.success) ElMessage.success('连接成功')
    else ElMessage.error(resp.data.error || '连接失败')
  } catch (e) {
    ElMessage.error('连接失败: ' + (e.response?.data?.error || e.message))
  } finally { testing.value = false }
}

async function saveConfig() {
  if (!form.api_key && !form._editingId) { ElMessage.warning('请先输入 API Token'); return }
  saving.value = true
  try {
    const data = {
      name: form.name,
      model_type: form.provider === 'mimo' ? 'other' : form.provider,
      model_name: form.model_name,
      base_url: form.base_url
    }
    if (form.api_key) data.api_key = form.api_key
    if (form._editingId) {
      await request.patch(`/api/ui-automation/config/ai-mode/${form._editingId}/`, data)
    } else {
      await request.post('/api/ui-automation/config/ai-mode/', data)
    }
    ElMessage.success('保存成功')
    form._editingId = null
    loadConfigs()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.error || e.message))
  } finally { saving.value = false }
}

async function loadConfigs() {
  try {
    const resp = await request.get('/api/ui-automation/config/ai-mode/')
    configs.value = resp.data || []
  } catch (e) { /* ignore */ }
}

async function deactivateConfig(row) {
  try {
    await request.patch(`/api/ui-automation/config/ai-mode/${row.id}/`, { is_active: false })
    ElMessage.success('已停用')
    loadConfigs()
  } catch (e) { ElMessage.error('操作失败') }
}

async function activateConfig(row) {
  try {
    const resp = await request.post(`/api/ui-automation/config/ai-mode/${row.id}/activate/`)
    if (resp.data.success) {
      ElMessage.success('连接成功，已启用')
    } else {
      ElMessage.error('连接失败: ' + (resp.data.error || '未知错误'))
    }
    loadConfigs()
  } catch (e) { ElMessage.error('操作失败') }
}

function editConfig(row) {
  form.name = row.name
  form.provider = row.model_type
  form.model_name = row.model_name
  form.base_url = row.base_url || ''
  form.api_key = '' // API key not returned for security
  form._editingId = row.id
}

async function deleteConfig(row) {
  try {
    await ElMessageBox.confirm('确定删除此配置？', '确认', { type: 'warning' })
    await request.delete(`/api/ui-automation/config/ai-mode/${row.id}/`)
    ElMessage.success('已删除')
    loadConfigs()
  } catch (e) { /* cancelled */ }
}

onMounted(() => loadConfigs())
</script>

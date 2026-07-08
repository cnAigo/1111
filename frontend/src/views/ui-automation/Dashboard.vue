<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ $t('menu.dashboard') }}</span>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background:#e8f4ff;color:#409eff"><el-icon :size="24"><Folder /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">3</div>
              <div class="stat-label">UI测试项目</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background:#f0f9eb;color:#67c23a"><el-icon :size="24"><Document /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">25</div>
              <div class="stat-label">测试用例</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background:#fdf6ec;color:#e6a23c"><el-icon :size="24"><Collection /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">5</div>
              <div class="stat-label">测试套件</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background:#f0f5ff;color:#2f54eb"><el-icon :size="24"><VideoPlay /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">12</div>
              <div class="stat-label">执行记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 -->
    <el-card shadow="never" style="margin-top:20px">
      <template #header><span>快速操作</span></template>
      <el-row :gutter="16">
        <el-col :span="8" v-for="item in quickActions" :key="item.label">
          <div class="quick-action" @click="$router.push(item.path)">
            <div class="action-icon" :style="{ background: item.bg, color: item.color }">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <span>{{ item.label }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 功能特性 -->
    <el-card shadow="never" style="margin-top:20px">
      <template #header><span>核心功能</span></template>
      <el-row :gutter="20">
        <el-col :span="6" v-for="feat in features" :key="feat.title">
          <div class="feature-card">
            <div class="feature-icon"><el-icon :size="28"><component :is="feat.icon" /></el-icon></div>
            <h4>{{ feat.title }}</h4>
            <p>{{ feat.desc }}</p>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const quickActions = [
  { label: '项目管理', path: '/ui-automation/projects', icon: 'Folder', bg: '#e8f4ff', color: '#409eff' },
  { label: '用例管理', path: '/ui-automation/test-cases', icon: 'Document', bg: '#f0f9eb', color: '#67c23a' },
  { label: '执行记录', path: '/ui-automation/executions', icon: 'VideoPlay', bg: '#fdf6ec', color: '#e6a23c' },
  { label: '测试报告', path: '/ui-automation/reports', icon: 'DataAnalysis', bg: '#f0f5ff', color: '#2f54eb' },
]

const features = [
  { title: '元素定位', desc: '支持CSS/XPath/Text等多种定位策略', icon: 'Aim' },
  { title: '双引擎', desc: 'Playwright + Selenium双引擎驱动', icon: 'Cpu' },
  { title: '多浏览器', desc: '支持Chrome/Firefox/Edge/WebKit', icon: 'Monitor' },
  { title: '自动执行', desc: '定时任务 + CI/CD集成', icon: 'AlarmClock' },
]
</script>

<style scoped lang="scss">
.stat-card {
  :deep(.el-card__body) { padding: 16px 20px; }
}
.stat-content { display: flex; align-items: center; gap: 12px; }
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-value { font-size: 24px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 2px; }

.quick-action {
  display: flex; align-items: center; gap: 12px; padding: 12px;
  border-radius: 8px; cursor: pointer; transition: all 0.2s;
  &:hover { background: #f5f7fa; transform: translateX(4px); }
  .action-icon {
    width: 40px; height: 40px; border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
  }
  span { font-size: 14px; color: #303133; }
}

.feature-card {
  text-align: center; padding: 24px 16px;
  .feature-icon {
    width: 64px; height: 64px; border-radius: 50%;
    background: #f5f7fa; display: flex; align-items: center;
    justify-content: center; margin: 0 auto 12px; color: #409eff;
  }
  h4 { font-size: 15px; color: #303133; margin: 0 0 8px; }
  p { font-size: 13px; color: #909399; margin: 0; }
}
</style>

import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import Home from '@/views/Home.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'Home', component: Home },
  { path: '/data-factory', name: 'DataFactory', component: () => import('@/views/data-factory/DataFactory.vue') },

  // ── 接口测试 (全量) ──
  {
    path: '/api-testing',
    component: Layout,
    children: [
      { path: '', redirect: 'dashboard' },
      { path: 'dashboard', name: 'ApiDashboard', component: () => import('@/views/api-testing/Dashboard.vue') },
      { path: 'projects', name: 'ApiProjects', component: () => import('@/views/api-testing/ProjectManagement.vue') },
      { path: 'interfaces', name: 'ApiInterfaces', component: () => import('@/views/api-testing/InterfaceManagement.vue') },
      { path: 'automation', name: 'ApiAutomation', component: () => import('@/views/api-testing/AutomationTesting.vue') },
      { path: 'history', name: 'ApiHistory', component: () => import('@/views/api-testing/RequestHistory.vue') },
      { path: 'environments', name: 'ApiEnvironments', component: () => import('@/views/api-testing/EnvironmentManagement.vue') },
      { path: 'reports', name: 'ApiReports', component: () => import('@/views/api-testing/ReportView.vue') },
      { path: 'scheduled-tasks', name: 'ApiScheduledTasks', component: () => import('@/views/api-testing/ScheduledTasks.vue') },
      { path: 'ai-service-config', name: 'ApiAIServiceConfig', component: () => import('@/views/api-testing/AIServiceConfig.vue') },
      { path: 'notification-logs', name: 'ApiNotificationLogs', component: () => import('@/views/notification/NotificationLogs.vue') },
    ]
  },

  // ── UI自动化测试 (全量) ──
  {
    path: '/ui-automation',
    component: Layout,
    children: [
      { path: '', redirect: 'dashboard' },
      { path: 'dashboard', name: 'UiDashboard', component: () => import('@/views/ui-automation/dashboard/Dashboard.vue') },
      { path: 'projects', name: 'UiProjects', component: () => import('@/views/ui-automation/projects/ProjectList.vue') },
      { path: 'elements-enhanced', name: 'UiElements', component: () => import('@/views/ui-automation/elements/ElementManagerEnhanced.vue') },
      { path: 'test-cases', name: 'UiTestCases', component: () => import('@/views/ui-automation/test-cases/TestCaseManager.vue') },
      { path: 'scripts-enhanced', name: 'UiScriptsEnhanced', component: () => import('@/views/ui-automation/scripts/ScriptEditorEnhanced.vue') },
      { path: 'scripts/editor', name: 'UiScriptEditor', component: () => import('@/views/ui-automation/scripts/ScriptEditorEnhanced.vue') },
      { path: 'scripts', name: 'UiScripts', component: () => import('@/views/ui-automation/scripts/ScriptList.vue') },
      { path: 'suites', name: 'UiSuites', component: () => import('@/views/ui-automation/suites/SuiteList.vue') },
      { path: 'executions', name: 'UiExecutions', component: () => import('@/views/ui-automation/executions/ExecutionList.vue') },
      { path: 'reports', name: 'UiReports', component: () => import('@/views/ui-automation/reports/ReportList.vue') },
      { path: 'scheduled-tasks', name: 'UiScheduledTasks', component: () => import('@/views/ui-automation/scheduled-tasks/ScheduledTasks.vue') },
      { path: 'notification-logs', name: 'UiNotificationLogs', component: () => import('@/views/ui-automation/notification/NotificationLogs.vue') },
    ]
  },

  // ── AI智能模式 ──
  {
    path: '/ai-intelligent-mode',
    component: Layout,
    children: [
      { path: '', redirect: 'testing' },
      { path: 'testing', name: 'AITesting', component: () => import('@/views/ai-intelligent-mode/Testing.vue') },
      { path: 'cases', name: 'AICaseList', component: () => import('@/views/ai-intelligent-mode/Cases.vue') },
      { path: 'execution-records', name: 'AIExecutionRecords', component: () => import('@/views/ai-intelligent-mode/ExecutionRecords.vue') },
      { path: 'settings', name: 'AISettings', component: () => import('@/views/ai-intelligent-mode/Settings.vue') },
      { path: 'prompts', name: 'AIPromptManager', component: () => import('@/views/ai-intelligent-mode/PromptManager.vue'), props: { standalone: true } },
      { path: 'script-library', name: 'ScriptLibrary', component: () => import('@/views/ai-intelligent-mode/ScriptLibrary.vue') },
    ]
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

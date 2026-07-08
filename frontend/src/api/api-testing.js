import request from '@/utils/request'

// Helper wrappers (matching reference project's request({url, method}) pattern)
function apiGet(url, params) { return request.get(url, { params }) }
function apiPost(url, data) { return request.post(url, data) }
function apiPatch(url, data) { return request.patch(url, data) }
function apiDelete(url) { return request.delete(url) }

// ── Dashboard ──
export function getDashboardStats() { return apiGet('/api/api-testing/dashboard/stats/') }

// ── Projects ──
export function getApiProjects(params) { return apiGet('/api/api-testing/projects/', params) }
export function createApiProject(data) { return apiPost('/api/api-testing/projects/', data) }
export function getApiProjectDetail(id) { return apiGet(`/api/api-testing/projects/${id}/`) }
export function updateApiProject(id, data) { return apiPatch(`/api/api-testing/projects/${id}/`, data) }
export function deleteApiProject(id) { return apiDelete(`/api/api-testing/projects/${id}/`) }

// ── Collections ──
export function getApiCollections(params) { return apiGet('/api/api-testing/collections/', params) }
export function searchApiCollections(q) { return apiGet('/api/api-testing/collections/search', { q }) }
export function createApiCollection(data) { return apiPost('/api/api-testing/collections/', data) }

// ── API Requests ──
export function getApiRequests(params) { return apiGet('/api/api-testing/requests/', params) }
export function createApiRequest(data) { return apiPost('/api/api-testing/requests/', data) }
export function getApiRequestDetail(id) { return apiGet(`/api/api-testing/requests/${id}/`) }
export function updateApiRequest(id, data) { return apiPatch(`/api/api-testing/requests/${id}/`, data) }
export function deleteApiRequest(id) { return apiDelete(`/api/api-testing/requests/${id}/`) }
export function executeApiRequest(id, data) { return apiPost(`/api/api-testing/api-requests/${id}/execute/`, data) }

// ── Environments ──
export function getEnvironments(params) { return apiGet('/api/api-testing/environments/', params) }
export function createEnvironment(data) { return apiPost('/api/api-testing/environments/', data) }
export function updateEnvironment(id, data) { return apiPatch(`/api/api-testing/environments/${id}/`, data) }
export function deleteEnvironment(id) { return apiDelete(`/api/api-testing/environments/${id}/`) }
export function activateEnvironment(id) { return apiPost(`/api/api-testing/environments/${id}/activate/`) }

// ── Request History ──
export function getRequestHistory(params) { return apiGet('/api/api-testing/histories/', params) }
export function getRequestHistoryDetail(id) { return apiGet(`/api/api-testing/histories/${id}/`) }
export function deleteRequestHistory(id) { return apiDelete(`/api/api-testing/histories/${id}/`) }
export function batchDeleteRequestHistory(ids) { return apiPost('/api/api-testing/histories/batch-delete/', { ids }) }

// ── Test Suites ──
export function getTestSuites(params) { return apiGet('/api/api-testing/test-suites/', params) }
export function createTestSuite(data) { return apiPost('/api/api-testing/test-suites/', data) }
export function updateTestSuite(id, data) { return apiPatch(`/api/api-testing/test-suites/${id}/`, data) }
export function deleteTestSuite(id) { return apiDelete(`/api/api-testing/test-suites/${id}/`) }
export function executeTestSuite(id, data) { return apiPost(`/api/api-testing/test-suites/${id}/execute/`, data) }

// ── Test Executions ──
export function getTestExecutions(params) { return apiGet('/api/api-testing/test-executions/', params) }
export function getExecutionResult(id) { return apiGet(`/api/api-testing/executions/${id}/`) }
export function generateAllureReport(id) { return apiPost(`/api/api-testing/test-executions/${id}/generate-allure-report/`) }

// ── Scheduled Tasks ──
export function getScheduledTasks(params) { return apiGet('/api/api-testing/scheduled-tasks/', params) }
export function createScheduledTask(data) { return apiPost('/api/api-testing/scheduled-tasks/', data) }
export function updateScheduledTask(id, data) { return apiPatch(`/api/api-testing/scheduled-tasks/${id}/`, data) }
export function deleteScheduledTask(id) { return apiDelete(`/api/api-testing/scheduled-tasks/${id}/`) }
export function runScheduledTask(id) { return apiPost(`/api/api-testing/scheduled-tasks/${id}/run_now/`) }
export function pauseScheduledTask(id) { return apiPost(`/api/api-testing/scheduled-tasks/${id}/pause/`) }
export function activateScheduledTask(id) { return apiPost(`/api/api-testing/scheduled-tasks/${id}/activate/`) }
export function getExecutionLogs(taskId, params) { return apiGet(`/api/api-testing/scheduled-tasks/${taskId}/execution_logs/`, params) }

// ── Operation Logs ──
export function getOperationLogs(params) { return apiGet('/api/api-testing/operation-logs/', params) }

// ── Users ──
export function getUsers(params) { return apiGet('/api/api-testing/users/', params) }

// ── Notification Logs ──
export function getNotificationLogs(params) { return apiGet('/api/api-testing/notification-logs/', params) }

// ── Task Notification Settings ──
export function getTaskNotificationSettings(params) { return apiGet('/api/api-testing/task-notification-settings/', params) }
export function createTaskNotificationSetting(data) { return apiPost('/api/api-testing/task-notification-settings/', data) }
export function updateTaskNotificationSetting(id, data) { return apiPatch(`/api/api-testing/task-notification-settings/${id}/`, data) }

// ── AI Service Configs ──
export function getAIServiceConfigs(params) { return apiGet('/api/api-testing/ai-service-configs/', params) }
export function createAIServiceConfig(data) { return apiPost('/api/api-testing/ai-service-configs/', data) }
export function updateAIServiceConfig(id, data) { return apiPatch(`/api/api-testing/ai-service-configs/${id}/`, data) }
export function deleteAIServiceConfig(id) { return apiDelete(`/api/api-testing/ai-service-configs/${id}/`) }
export function testAIConnection(id, data) { return apiPost(`/api/api-testing/ai-service-configs/${id}/test_connection/`, data) }

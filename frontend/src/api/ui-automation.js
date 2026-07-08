import request from '@/utils/request'

// Helper: the reference project uses request({url, method, params}) pattern
// Our request is axios-like: request.get(url, {params}) or request.post(url, data)
function apiGet(url, params) { return request.get(url, { params }) }
function apiPost(url, data) { return request.post(url, data) }
function apiPatch(url, data) { return request.patch(url, data) }
function apiDelete(url) { return request.delete(url) }

// ── Dashboard ──
export function getDashboardStats() { return apiGet('/api/ui-automation/dashboard/stats/') }

// ── Projects ──
export function getUiProjects(params) { return apiGet('/api/ui-automation/projects/', params) }
export function createUiProject(data) { return apiPost('/api/ui-automation/projects/', data) }
export function getUiProjectDetail(id) { return apiGet(`/api/ui-automation/projects/${id}/`) }
export function updateUiProject(id, data) { return apiPatch(`/api/ui-automation/projects/${id}/`, data) }
export function deleteUiProject(id) { return apiDelete(`/api/ui-automation/projects/${id}/`) }

// ── Locator Strategies ──
export function getLocatorStrategies(params) { return apiGet('/api/ui-automation/locator-strategies/', params) }
export function createLocatorStrategy(data) { return apiPost('/api/ui-automation/locator-strategies/', data) }

// ── Elements ──
export function getElements(params) { return apiGet('/api/ui-automation/elements/', params) }
export function createElement(data) { return apiPost('/api/ui-automation/elements/', data) }
export function getElementDetail(id) { return apiGet(`/api/ui-automation/elements/${id}/`) }
export function updateElement(id, data) { return apiPatch(`/api/ui-automation/elements/${id}/`, data) }
export function deleteElement(id) { return apiDelete(`/api/ui-automation/elements/${id}/`) }

// ── Test Scripts ──
export function getTestScripts(params) { return apiGet('/api/ui-automation/test-scripts/', params) }
export function createTestScript(data) { return apiPost('/api/ui-automation/test-scripts/', data) }
export function getTestScriptDetail(id) { return apiGet(`/api/ui-automation/test-scripts/${id}/`) }
export function updateTestScript(id, data) { return apiPatch(`/api/ui-automation/test-scripts/${id}/`, data) }
export function deleteTestScript(id) { return apiDelete(`/api/ui-automation/test-scripts/${id}/`) }

// ── Test Suites ──
export function getTestSuites(params) { return apiGet('/api/ui-automation/test-suites/', params) }
export function createTestSuite(data) { return apiPost('/api/ui-automation/test-suites/', data) }
export function getTestSuiteDetail(id) { return apiGet(`/api/ui-automation/test-suites/${id}/`) }
export function updateTestSuite(id, data) { return apiPatch(`/api/ui-automation/test-suites/${id}/`, data) }
export function deleteTestSuite(id) { return apiDelete(`/api/ui-automation/test-suites/${id}/`) }
export function getTestSuiteTestCases(id) { return apiGet(`/api/ui-automation/test-suites/${id}/test_cases/`) }
export function addTestCaseToTestSuite(id, data) { return apiPost(`/api/ui-automation/test-suites/${id}/add_test_case/`, data) }
export function removeTestCaseFromTestSuite(suiteId, testCaseId) { return apiDelete(`/api/ui-automation/test-suites/${suiteId}/remove_test_case/`) }
export function updateTestCaseOrder(suiteId, testCaseOrders) { return apiPost(`/api/ui-automation/test-suites/${suiteId}/update_test_case_order/`, { test_case_orders: testCaseOrders }) }
export function runTestSuite(suiteId, data) { return apiPost(`/api/ui-automation/test-suites/${suiteId}/run_suite/`, data) }

// ── Test Executions ──
export function getTestExecutions(params) { return apiGet('/api/ui-automation/test-executions/', params) }
export function batchDeleteTestExecutions(ids) { return apiPost('/api/ui-automation/test-executions/batch-delete/', { ids }) }
export function createTestExecution(data) { return apiPost('/api/ui-automation/test-executions/', data) }
export function getTestExecutionDetail(id) { return apiGet(`/api/ui-automation/test-executions/${id}/`) }
export function deleteTestExecution(id) { return apiDelete(`/api/ui-automation/test-executions/${id}/`) }
export function runTestExecution(id) { return apiPost(`/api/ui-automation/test-executions/${id}/run/`) }
export function abortTestExecution(id) { return apiPost(`/api/ui-automation/test-executions/${id}/abort/`) }

// ── Test Environments ──
export function getTestEnvironments(params) { return apiGet('/api/ui-automation/test-environments/', params) }
export function createTestEnvironment(data) { return apiPost('/api/ui-automation/test-environments/', data) }
export function getTestEnvironmentDetail(id) { return apiGet(`/api/ui-automation/test-environments/${id}/`) }
export function updateTestEnvironment(id, data) { return apiPatch(`/api/ui-automation/test-environments/${id}/`, data) }
export function deleteTestEnvironment(id) { return apiDelete(`/api/ui-automation/test-environments/${id}/`) }

// ── Screenshots ──
export function getScreenshots(params) { return apiGet('/api/ui-automation/screenshots/', params) }
export function createScreenshot(data) { return apiPost('/api/ui-automation/screenshots/', data) }
export function getScreenshotDetail(id) { return apiGet(`/api/ui-automation/screenshots/${id}/`) }
export function deleteScreenshot(id) { return apiDelete(`/api/ui-automation/screenshots/${id}/`) }

// ── Element Groups ──
export function getElementGroups(params) { return apiGet('/api/ui-automation/element-groups/', params) }
export function createElementGroup(data) { return apiPost('/api/ui-automation/element-groups/', data) }
export function getElementGroupDetail(id) { return apiGet(`/api/ui-automation/element-groups/${id}/`) }
export function updateElementGroup(id, data) { return apiPatch(`/api/ui-automation/element-groups/${id}/`, data) }
export function deleteElementGroup(id) { return apiDelete(`/api/ui-automation/element-groups/${id}/`) }
export function getElementGroupTree(params) { return apiGet('/api/ui-automation/element-groups/tree/', params) }

// ── Element Enhanced ──
export function validateElementLocator(id) { return apiPost(`/api/ui-automation/elements/${id}/validate_locator/`) }
export function getElementUsages(id) { return apiGet(`/api/ui-automation/elements/${id}/usages/`) }
export function getElementTree(params) { return apiGet('/api/ui-automation/elements/tree/', params) }
export function addBackupLocator(id, data) { return apiPost(`/api/ui-automation/elements/${id}/add_backup_locator/`, data) }
export function generateElementSuggestions(id) { return apiPost(`/api/ui-automation/elements/${id}/generate_suggestions/`) }

// ── Page Objects ──
export function getPageObjects(params) { return apiGet('/api/ui-automation/page-objects/', params) }
export function createPageObject(data) { return apiPost('/api/ui-automation/page-objects/', data) }
export function getPageObjectDetail(id) { return apiGet(`/api/ui-automation/page-objects/${id}/`) }
export function updatePageObject(id, data) { return apiPatch(`/api/ui-automation/page-objects/${id}/`, data) }
export function deletePageObject(id) { return apiDelete(`/api/ui-automation/page-objects/${id}/`) }
export function generatePageObjectCode(id, data) { return apiPost(`/api/ui-automation/page-objects/${id}/generate_code/`, data) }
export function addElementToPageObject(id, data) { return apiPost(`/api/ui-automation/page-objects/${id}/add_element/`, data) }
export function getPageObjectElements(id) { return apiGet(`/api/ui-automation/page-objects/${id}/elements/`) }

// ── Page Object Elements ──
export function getPageObjectElementDetails(params) { return apiGet('/api/ui-automation/page-object-elements/', params) }
export function createPageObjectElement(data) { return apiPost('/api/ui-automation/page-object-elements/', data) }
export function updatePageObjectElement(id, data) { return apiPatch(`/api/ui-automation/page-object-elements/${id}/`, data) }
export function deletePageObjectElement(id) { return apiDelete(`/api/ui-automation/page-object-elements/${id}/`) }

// ── Script Steps ──
export function getScriptSteps(params) { return apiGet('/api/ui-automation/script-steps/', params) }
export function createScriptStep(data) { return apiPost('/api/ui-automation/script-steps/', data) }
export function batchCreateScriptSteps(data) { return apiPost('/api/ui-automation/script-steps/batch_create/', data) }
export function updateScriptStep(id, data) { return apiPatch(`/api/ui-automation/script-steps/${id}/`, data) }
export function deleteScriptStep(id) { return apiDelete(`/api/ui-automation/script-steps/${id}/`) }

// ── Script Element Usages ──
export function getScriptElementUsages(params) { return apiGet('/api/ui-automation/script-element-usages/', params) }
export function analyzeScriptElements(data) { return apiPost('/api/ui-automation/script-element-usages/analyze_script/', data) }
export function createScriptElementUsage(data) { return apiPost('/api/ui-automation/script-element-usages/', data) }
export function updateScriptElementUsage(id, data) { return apiPatch(`/api/ui-automation/script-element-usages/${id}/`, data) }
export function deleteScriptElementUsage(id) { return apiDelete(`/api/ui-automation/script-element-usages/${id}/`) }

// ── Test Cases ──
export function getTestCases(params) { return apiGet('/api/ui-automation/test-cases/', params) }
export function createTestCase(data) { return apiPost('/api/ui-automation/test-cases/', data) }
export function getTestCaseDetail(id) { return apiGet(`/api/ui-automation/test-cases/${id}/`) }
export function updateTestCase(id, data) { return apiPatch(`/api/ui-automation/test-cases/${id}/`, data) }
export function deleteTestCase(id) { return apiDelete(`/api/ui-automation/test-cases/${id}/`) }
export function runTestCase(testCaseId, data) { return apiPost(`/api/ui-automation/test-cases/${testCaseId}/run/`, data) }
export function copyTestCase(id) { return apiPost(`/api/ui-automation/test-cases/${id}/copy_case/`) }
export function batchRunTestCases(data) { return apiPost('/api/ui-automation/test-cases/batch-run/', data) }

// ── Test Case Executions ──
export function getTestCaseExecutions(params) { return apiGet('/api/ui-automation/test-case-executions/', params) }
export function deleteTestCaseExecution(id) { return apiDelete(`/api/ui-automation/test-case-executions/${id}/`) }
export function batchDeleteTestCaseExecutions(ids) { return apiPost('/api/ui-automation/test-case-executions/batch-delete/', { ids }) }

// ── Operation Records ──
export function getOperationRecords(params) { return apiGet('/api/ui-automation/operation-records/', params) }
export function createOperationRecord(data) { return apiPost('/api/ui-automation/operation-records/', data) }

// ── Scheduled Tasks ──
export function getScheduledTasks(params) { return apiGet('/api/ui-automation/scheduled-tasks/', params) }
export function createScheduledTask(data) { return apiPost('/api/ui-automation/scheduled-tasks/', data) }
export function getScheduledTaskDetail(id) { return apiGet(`/api/ui-automation/scheduled-tasks/${id}/`) }
export function updateScheduledTask(id, data) { return apiPatch(`/api/ui-automation/scheduled-tasks/${id}/`, data) }
export function deleteScheduledTask(id) { return apiDelete(`/api/ui-automation/scheduled-tasks/${id}/`) }
export function pauseScheduledTask(id) { return apiPost(`/api/ui-automation/scheduled-tasks/${id}/pause/`) }
export function resumeScheduledTask(id) { return apiPost(`/api/ui-automation/scheduled-tasks/${id}/resume/`) }
export function runScheduledTask(id) { return apiPost(`/api/ui-automation/scheduled-tasks/${id}/run_now/`) }

// ── Notification Configs ──
export function getNotificationConfigs(params) { return apiGet('/api/ui-automation/notification-configs/', params) }
export function createNotificationConfig(data) { return apiPost('/api/ui-automation/notification-configs/', data) }
export function getNotificationConfigDetail(id) { return apiGet(`/api/ui-automation/notification-configs/${id}/`) }
export function updateNotificationConfig(id, data) { return apiPatch(`/api/ui-automation/notification-configs/${id}/`, data) }
export function deleteNotificationConfig(id) { return apiDelete(`/api/ui-automation/notification-configs/${id}/`) }
export function setDefaultNotificationConfig(id) { return apiPost(`/api/ui-automation/notification-configs/${id}/set_default/`) }

// ── Notification Logs ──
export function getNotificationLogs(params) { return apiGet('/api/ui-automation/notification-logs/', params) }
export function retryNotification(id) { return apiPost(`/api/ui-automation/notification-logs/${id}/retry/`) }

// ── Task Notification Settings ──
export function getTaskNotificationSettings(params) { return apiGet('/api/ui-automation/task-notification-settings/', params) }
export function createTaskNotificationSetting(data) { return apiPost('/api/ui-automation/task-notification-settings/', data) }
export function updateTaskNotificationSetting(id, data) { return apiPatch(`/api/ui-automation/task-notification-settings/${id}/`, data) }
export function getUiUsers(params) { return apiGet('/api-testing/users/', params) }

// ── AI Cases ──
export function getAICases(params) { return apiGet('/api/ui-automation/ai-cases/', params) }
export function createAICase(data) { return apiPost('/api/ui-automation/ai-cases/', data) }
export function getAICaseDetail(id) { return apiGet(`/api/ui-automation/ai-cases/${id}/`) }
export function updateAICase(id, data) { return apiPatch(`/api/ui-automation/ai-cases/${id}/`, data) }
export function deleteAICase(id) { return apiDelete(`/api/ui-automation/ai-cases/${id}/`) }
export function runAICase(id) { return apiPost(`/api/ui-automation/ai-cases/${id}/run/`) }

// ── AI Execution Records ──
export function getAIExecutionRecords(params) { return apiGet('/api/ui-automation/ai-execution-records/', params) }
export function getAIExecutionRecordDetail(id) { return apiGet(`/api/ui-automation/ai-execution-records/${id}/`) }
export function runAdhocAITask(data) { return apiPost('/api/ui-automation/ai-execution-records/run_adhoc/', data) }
export function stopAITask(id) { return apiPost(`/api/ui-automation/ai-execution-records/${id}/stop/`) }
export function batchDeleteAIExecutionRecords(ids) { return apiPost('/api/ui-automation/ai-execution-records/batch_delete/', { ids }) }
export function getAIExecutionReport(id, params) { return apiGet(`/api/ui-automation/ai-execution-records/${id}/report/`, params) }
export function exportAIExecutionReportPDF(id, params) { return request.get(`/api/ui-automation/ai-execution-records/${id}/export-pdf/`, { params, responseType: 'blob' }) }

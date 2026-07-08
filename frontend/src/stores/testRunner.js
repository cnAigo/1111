import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiPost, apiGet } from '@/api/api-testing'

export const useTestRunnerStore = defineStore('testRunner', () => {
  // --- Execution State ---
  const isRunning = ref(false)
  const taskId = ref(null)
  const status = ref('IDLE') // IDLE | RUNNING | SUCCESS | FAILED | STOPPED
  const progress = ref(0)
  const progressTotal = ref(0)
  const estimatedMs = ref(0)
  const durationFmt = ref('')
  const runningLabel = ref('')
  const elapsedSec = ref(0)

  // --- Terminal ---
  const terminalLines = ref([])
  const logFilter = ref('ALL')

  // --- Results ---
  const testResults = ref([])
  const showReport = ref(false)

  // --- History ---
  const historyList = ref([])
  const historyHasMore = ref(false)
  const historyPage = ref(0)
  const historyStats = ref(null)

  // --- Failed Cases ---
  const failedCases = ref([])
  const failedCasesHasMore = ref(false)
  const failedCasesPage = ref(0)

  // --- Case Details ---
  const caseDetails = ref({})

  // --- Config ---
  const cfgUrl = ref('')
  const cfgProjectId = ref('')
  const cfgUsername = ref('')
  const cfgPassword = ref('')

  // --- WebSocket refs (module-level) ---
  let _ws = null
  let _cleanupWs = null
  let _elapsedTimer = null
  let _toastFn = null
  let _termWriter = null

  const estimatedFmt = computed(() => {
    if (!estimatedMs.value) return '--'
    const s = Math.ceil(estimatedMs.value / 1000)
    if (s < 60) return `${s}秒`
    return `${Math.floor(s / 60)}分${s % 60}秒`
  })

  // --- Actions ---
  function setToastFn(fn) { _toastFn = fn }
  function setTerminalWriter(fn) { _termWriter = fn }

  function appendLog(text) {
    terminalLines.value.push(text)
    if (_termWriter) _termWriter(text)
  }

  function clearTerminal() {
    terminalLines.value = []
  }

  function resetRunState() {
    clearTerminal()
    progress.value = 0
    progressTotal.value = 0
    showReport.value = false
    testResults.value = []
  }

  function _connectWs(id) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const url = `${protocol}//${host}/ws/test-run/${id}`
    _ws = new WebSocket(url)

    _ws.onopen = () => {
      isRunning.value = true
      status.value = 'RUNNING'
      taskId.value = id
      localStorage.setItem('taas_taskId', id)
    }

    _ws.onmessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        if (msg.type === 'line') {
          appendLog(msg.data)
        } else if (msg.type === 'progress') {
          progress.value = msg.current || 0
          progressTotal.value = msg.total || 0
          runningLabel.value = msg.label || ''
          durationFmt.value = msg.duration || ''
        } else if (msg.type === 'result') {
          testResults.value = msg.results || []
          status.value = msg.status || 'SUCCESS'
          isRunning.value = false
          showReport.value = true
          _stopElapsed()
          if (_toastFn) _toastFn(status.value === 'SUCCESS' ? 'success' : 'error', `测试${status.value === 'SUCCESS' ? '通过' : '失败'}`)
        } else if (msg.type === 'status') {
          status.value = msg.status
          if (msg.status === 'STOPPED') {
            isRunning.value = false
            _stopElapsed()
          }
        }
      } catch { /* raw text */ }
    }

    _ws.onclose = () => {
      if (isRunning.value) isRunning.value = false
      _stopElapsed()
    }

    _ws.onerror = () => {}
  }

  function _startElapsed() {
    _stopElapsed()
    const startTime = Date.now()
    _elapsedTimer = setInterval(() => {
      elapsedSec.value = Math.floor((Date.now() - startTime) / 1000)
    }, 1000)
  }

  function _stopElapsed() {
    if (_elapsedTimer) { clearInterval(_elapsedTimer); _elapsedTimer = null }
  }

  function _connectCleanupWs(id, onDone) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    _cleanupWs = new WebSocket(`${protocol}//${host}/ws/test-run/${id}`)
    let timeout = setTimeout(() => {
      _cleanupWs?.close()
      onDone?.()
    }, 120000)

    _cleanupWs.onmessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        if (msg.type === 'line') appendLog(msg.data)
      } catch { /* raw text */ }
    }

    _cleanupWs.onclose = () => { clearTimeout(timeout); onDone?.() }
    _cleanupWs.onerror = () => { clearTimeout(timeout); onDone?.() }
  }

  // --- API Actions ---
  async function startTest(selectedModule, selectedClass) {
    resetRunState()
    _startElapsed()

    const body = {
      url: cfgUrl.value,
      projectId: cfgProjectId.value,
      username: cfgUsername.value,
      password: cfgPassword.value,
    }
    if (selectedModule) body.module = selectedModule
    if (selectedClass) body.testClass = selectedClass

    try {
      const est = await apiPost('/api/test/estimate', body)
      estimatedMs.value = est?.estimatedMs || 0
    } catch { /* ignore */ }

    try {
      const res = await apiPost('/api/test/run', body)
      const id = res?.taskId
      if (id) {
        _connectWs(id)
      }
    } catch (e) {
      isRunning.value = false
      _stopElapsed()
      if (_toastFn) _toastFn('error', '启动测试失败: ' + (e.message || '未知错误'))
    }
  }

  async function stopTest() {
    if (!taskId.value) return
    await apiPost(`/api/test/stop/${taskId.value}`)
    isRunning.value = false
    status.value = 'STOPPED'
    _stopElapsed()
  }

  async function resumeTask(id, label) {
    resetRunState()
    _startElapsed()
    runningLabel.value = label || ''
    taskId.value = id
    _connectWs(id)
  }

  async function init() {
    const savedId = localStorage.getItem('taas_taskId')
    if (!savedId) return

    try {
      const s = await apiGet(`/api/test/status?taskId=${savedId}`)
      if (s && s.status === 'RUNNING') {
        resumeTask(savedId, s.label)
      } else {
        localStorage.removeItem('taas_taskId')
      }
    } catch {
      localStorage.removeItem('taas_taskId')
    }
  }

  async function loadResults() {
    try {
      testResults.value = await apiGet('/api/test/results') || []
      showReport.value = true
    } catch { /* ignore */ }
  }

  async function loadHistory(page = 0, append = false) {
    try {
      const data = await apiGet(`/api/test/history?page=${page}&size=20`)
      if (append) {
        historyList.value = [...historyList.value, ...(data?.content || [])]
      } else {
        historyList.value = data?.content || []
      }
      historyHasMore.value = !data?.last
      historyPage.value = page
    } catch { /* ignore */ }
  }

  async function loadHistoryStats() {
    try {
      historyStats.value = await apiGet('/api/test/history/stats')
    } catch { /* ignore */ }
  }

  async function loadFailedCases(page = 0, append = false) {
    try {
      const data = await apiGet(`/api/test/failed-cases?page=${page}&size=20`)
      if (append) {
        failedCases.value = [...failedCases.value, ...(data?.content || [])]
      } else {
        failedCases.value = data?.content || []
      }
      failedCasesHasMore.value = !data?.last
      failedCasesPage.value = page
    } catch { /* ignore */ }
  }

  async function loadCaseDetails() {
    try {
      const data = await apiGet('/api/test/case-details')
      if (Array.isArray(data)) {
        const m = {}
        data.forEach(d => {
          const key = d.className || 'unknown'
          if (!m[key]) m[key] = []
          m[key].push(d)
        })
        caseDetails.value = m
      } else {
        caseDetails.value = data || {}
      }
    } catch { /* ignore */ }
  }

  async function deleteHistory(taskId) {
    await apiPost(`/api/test/history/${taskId}`, {})
  }

  async function rerunFailed(prevTaskId) {
    resetRunState()
    _startElapsed()
    const res = await apiPost('/api/test/rerun-failed', { taskId: prevTaskId })
    const id = res?.taskId
    if (id) _connectWs(id)
  }

  async function startCleanup() {
    const res = await apiPost('/api/test/cleanup', {
      url: cfgUrl.value,
      projectId: cfgProjectId.value,
      username: cfgUsername.value,
      password: cfgPassword.value,
    })
    const id = res?.taskId
    if (id) {
      return new Promise(resolve => _connectCleanupWs(id, resolve))
    }
  }

  return {
    isRunning, taskId, status, progress, progressTotal,
    estimatedMs, estimatedFmt, durationFmt, runningLabel, elapsedSec,
    terminalLines, logFilter, testResults, showReport,
    historyList, historyHasMore, historyPage, historyStats,
    failedCases, failedCasesHasMore, failedCasesPage,
    caseDetails,
    cfgUrl, cfgProjectId, cfgUsername, cfgPassword,
    setToastFn, setTerminalWriter, appendLog, clearTerminal, resetRunState,
    startTest, stopTest, resumeTask, init,
    loadResults, loadHistory, loadHistoryStats,
    loadFailedCases, loadCaseDetails, deleteHistory,
    rerunFailed, startCleanup,
  }
})

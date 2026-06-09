import { useState, useCallback, useEffect, useRef } from 'react';
import { MODULES } from './data/modules';
import { useTestRun } from './hooks/useTestRun';
import { useCaseDetails } from './hooks/useCaseDetails';
import { useConfig } from './hooks/useConfig';
import { apiPost, apiDelete } from './utils/api';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import Toast from './components/Toast';
import ConfigModal from './components/ConfigModal';
import ConfirmDialog from './components/ConfirmDialog';
import Dashboard from './pages/Dashboard';
import Results from './pages/Results';
import History from './pages/History';
import FailedCases from './pages/FailedCases';
import SettingsPage from './pages/Settings';

export default function App() {
  const [activeMenu, setActiveMenu] = useState('dashboard');
  const [selected, setSelected] = useState(new Set());
  const [showReport, setShowReport] = useState(false);
  const [testResults, setTestResults] = useState([]);
  const [historyList, setHistoryList] = useState([]);
  const [failedCases, setFailedCases] = useState([]);
  const [logFilter, setLogFilter] = useState('ALL');
  const [toast, setToast] = useState(null);
  const [cleaning, setCleaning] = useState(false);
  const [expandedHistory, setExpandedHistory] = useState({});
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const [confirm, setConfirm] = useState(null);
  const lastToastRef = useRef({ msg: '', time: 0 });

  const showToast = useCallback((msg, type = 'info') => {
    // Dedup: same message within 5s → skip
    const now = Date.now();
    if (lastToastRef.current.msg === msg && (now - lastToastRef.current.time) < 5000) return;
    lastToastRef.current = { msg, time: now };
    setToast({ msg, type, id: now });
    setTimeout(() => setToast(null), 3500);
  }, []);

  const config = useConfig(showToast);
  const tr = useTestRun(config.cfgUrl, config.cfgProjectId, config.cfgUsername, config.cfgPassword, showToast);
  const caseDetails = useCaseDetails();

  useEffect(() => {
    if (cleaning && (tr.status === 'SUCCESS' || tr.status === 'FAILED' || tr.status === 'STOPPED')) {
      setCleaning(false);
    }
  }, [tr.status, cleaning]);

  // Auto-collapse sidebar when running
  useEffect(() => {
    if (tr.isRunning) setSidebarOpen(false);
  }, [tr.isRunning]);

  const toggleSelect = useCallback((name, add) => {
    setSelected(prev => {
      const next = new Set(prev);
      add ? next.add(name) : next.delete(name);
      return next;
    });
  }, []);

  const wrappedStart = useCallback(async () => {
    if (tr.isRunning) return;
    const names = [...selected];
    if (names.length === 0) { showToast('请先选择测试类', 'warning'); return; }
    const label = names.length <= 3 ? names.join(', ') : `${names.length} 个测试类`;
    setConfirm({ msg: `确认执行 ${label} 吗？`, onConfirm: async () => {
      setConfirm(null);
      setShowReport(false);
      const tid = await tr.startTest(null, { name: names.join(','), type: 'multi' });
    }});
  }, [tr, selected, showToast]);

  useEffect(() => {
    if (tr.status === 'SUCCESS' || tr.status === 'FAILED') {
      setShowReport(true);
      tr.loadResults(setTestResults);
      tr.loadHistory(setHistoryList);
      tr.loadFailedCases(setFailedCases);
      if (Notification.permission === 'granted') {
        new Notification(tr.status === 'SUCCESS' ? '测试通过' : '测试失败', { body: tr.runningLabel });
      } else if (Notification.permission === 'default') {
        Notification.requestPermission();
      }
      document.title = (tr.status === 'SUCCESS' ? '✓ ' : '✗ ') + 'TaaS Console';
    } else if (tr.status === 'RUNNING') {
      document.title = '▶ TaaS Console';
    } else {
      document.title = 'TaaS Console';
    }
  }, [tr.status]);

  useEffect(() => {
    const h = (e) => {
      if (e.ctrlKey && e.key === 'Enter') { e.preventDefault(); wrappedStart(); }
      if (e.key === 'Escape' && tr.isRunning) { e.preventDefault(); tr.stopTest(); }
    };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [wrappedStart, tr.isRunning, tr.stopTest]);

  const navigate = useCallback((key) => {
    setActiveMenu(key);
    if (key === 'results') tr.loadResults(setTestResults);
    if (key === 'history') tr.loadHistory(setHistoryList);
    if (key === 'failed') tr.loadFailedCases(setFailedCases);
    if (key === 'settings') config.setConfigOpen(true);
  }, [tr, config]);

  // Expose navigate for child pages
  useEffect(() => { window.__taasNavigate = navigate; return () => delete window.__taasNavigate; }, [navigate]);

  const onCleanupAction = useCallback(async () => {
    setConfirm({ msg: '确认清理环境？这将删除所有测试数据！', onConfirm: async () => {
      setConfirm(null);
      setCleaning(true);
      setActiveMenu('dashboard');
      tr.setTerminalLines([]);
      try {
        await fetch('/api/test/cleanup', { method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ projectId: config.cfgProjectId, url: config.cfgUrl, username: config.cfgUsername, password: config.cfgPassword })
        });
        tr.resumeTask('cleanup', '清理环境');
      } catch (e) { showToast('清理失败: ' + e.message, 'error'); setCleaning(false); }
    }});
  }, [config, tr, showToast]);

  const toggleHistoryExpand = useCallback(async (taskId) => {
    if (expandedHistory[taskId]) { setExpandedHistory(p => { const n={...p}; delete n[taskId]; return n; }); return; }
    setExpandedHistory(p => ({ ...p, [taskId]: 'loading' }));
    try {
      const r = await fetch(`/api/test/history/${taskId}/cases`);
      const cases = await r.json();
      const h = historyList.find(x => x.taskId === taskId);
      setExpandedHistory(p => ({ ...p, [taskId]: { cases, label: h?.label || '' } }));
    } catch { setExpandedHistory(p => { const n={...p}; delete n[taskId]; return n; }); }
  }, [expandedHistory, historyList]);

  const onRerunClass = useCallback(async (className) => {
    setActiveMenu('dashboard');
    try {
      const body = { url: config.cfgUrl, projectId: config.cfgProjectId, username: config.cfgUsername, password: config.cfgPassword, testClass: className };
      const r = await apiPost('/api/test/run', body);
      if (r.taskId) showToast('已启动: ' + className, 'info');
    } catch (e) { showToast('重跑失败: ' + e.message, 'error'); }
  }, [config, showToast]);

  const onRerun = useCallback(async (tid) => {
    if (tr.isRunning) { showToast('已有任务在运行中', 'warning'); return; }
    setConfirm({ msg: '确认重跑该任务的失败用例？', onConfirm: async () => {
      setConfirm(null); setActiveMenu('dashboard');
      try {
        const d = await apiPost('/api/test/rerun-failed', { taskId: tid });
        if (d.taskId) { tr.resumeTask(d.taskId, d.label || 'Rerun'); showToast('已下发重跑', 'info'); }
      } catch (e) { showToast('重跑失败: ' + e.message, 'error'); }
    }});
  }, [tr, showToast]);

  const onDeleteHistory = useCallback(async (tid) => {
    setConfirm({ msg: '确认删除该历史记录？', onConfirm: async () => {
      setConfirm(null);
      try { await apiDelete(`/api/test/history/${tid}`); setHistoryList(p => p.filter(h => h.taskId !== tid)); showToast('已删除', 'info'); }
      catch (e) { showToast('删除失败: ' + e.message, 'error'); }
    }});
  }, [showToast]);

  const page = activeMenu === 'settings' ? 'settings' : activeMenu === 'results' ? 'results'
    : activeMenu === 'history' ? 'history' : activeMenu === 'failed' ? 'failed' : 'dashboard';

  return (
    <div className="h-screen flex overflow-hidden bg-slate-50">
      <Sidebar activeMenu={activeMenu} onNavigate={navigate} status={tr.status} failedCount={failedCases.length}
        timeDisplay={tr.durationFmt} isRunning={tr.isRunning} elapsedFmt={tr.fmtElapsed(tr.elapsedSec)} cleaning={cleaning}
        open={sidebarOpen} onToggle={() => setSidebarOpen(o => !o)} />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Header activeMenu={activeMenu} isRunning={tr.isRunning} runningLabel={tr.runningLabel}
          onOpenSettings={() => { config.setConfigOpen(true); config.loadConfigs(); }} />
        <div className="flex-1 flex flex-col min-h-0 bg-slate-50/80">
          {page === 'dashboard' && (
            <Dashboard terminalLines={tr.terminalLines} isRunning={tr.isRunning} elapsedSec={tr.elapsedSec}
              showReport={showReport} status={tr.status} logFilter={logFilter} onFilterChange={setLogFilter}
              onClear={() => { tr.setTerminalLines([]); tr.appendLog('— Console cleared —', 'text-slate-500'); }} pct={tr.pct} progress={tr.progress} progressTotal={tr.progressTotal}
              fmtElapsed={tr.fmtElapsed} runningLabel={tr.runningLabel} onCloseReport={() => setShowReport(false)}
              testResults={testResults} historyList={historyList}
              modules={MODULES} selected={selected} onToggle={toggleSelect}
              onStart={wrappedStart} onStop={tr.stopTest} onCleanup={onCleanupAction}
              cleaning={cleaning} selectedCount={selected.size} caseDetails={caseDetails.details} />
          )}
          {page === 'results' && <Results testResults={testResults} caseDetails={caseDetails.details} />}
          {page === 'history' && <History historyList={historyList} onRerun={onRerun} onDelete={onDeleteHistory}
            expandedHistory={expandedHistory} onToggleExpand={toggleHistoryExpand} caseDetails={caseDetails.details} />}
          {page === 'failed' && <FailedCases failedCases={failedCases} onRerunClass={onRerunClass} caseDetails={caseDetails.details} />}
          {page === 'settings' && <SettingsPage onOpen={() => { config.setConfigOpen(true); config.loadConfigs(); }} />}
        </div>
      </div>
      <ConfigModal open={config.configOpen} onClose={() => config.setConfigOpen(false)}
        cfgUrl={config.cfgUrl} setCfgUrl={config.setCfgUrl} cfgProjectId={config.cfgProjectId} setCfgProjectId={config.setCfgProjectId}
        cfgUsername={config.cfgUsername} setCfgUsername={config.setCfgUsername} cfgPassword={config.cfgPassword} setCfgPassword={config.setCfgPassword}
        pwVisible={config.pwVisible} setPwVisible={config.setPwVisible} savedConfigs={config.savedConfigs}
        configFormName={config.configFormName} setConfigFormName={config.setConfigFormName}
        onSave={config.saveConfig} onDelete={config.deleteConfig} />
      <Toast toast={toast} onClose={() => setToast(null)} />
      {confirm && (
        <ConfirmDialog message={confirm.msg} onConfirm={confirm.onConfirm}
          onCancel={() => setConfirm(null)} />
      )}
    </div>
  );
}

import { useState, useCallback, useEffect } from 'react';
import { MODULES, MODULE_OPTIONS } from './data/modules';
import { useTestRun } from './hooks/useTestRun';
import { useCaseDetails } from './hooks/useCaseDetails';
import { useConfig } from './hooks/useConfig';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import Toast from './components/Toast';
import ConfigModal from './components/ConfigModal';
import Dashboard from './pages/Dashboard';
import Results from './pages/Results';
import History from './pages/History';
import FailedCases from './pages/FailedCases';
import SettingsPage from './pages/Settings';

const apiPost = async (url, body) => { const r = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }); return r.json(); };
const apiDelete = async (url) => { const r = await fetch(url, { method: 'DELETE' }); return r.json(); };

export default function App() {
  const [activeMenu, setActiveMenu] = useState('dashboard');
  const [selectedModule, setSelectedModule] = useState('');
  const [selectedClass, setSelectedClass] = useState(null);
  const [showReport, setShowReport] = useState(false);
  const [testResults, setTestResults] = useState([]);
  const [historyList, setHistoryList] = useState([]);
  const [failedCases, setFailedCases] = useState([]);
  const [logFilter, setLogFilter] = useState('ALL');
  const [toast, setToast] = useState(null);
  const [cleaning, setCleaning] = useState(false);
  const [expandedHistory, setExpandedHistory] = useState({});

  // Inline confirm state
  const [confirm, setConfirm] = useState(null); // { msg, onConfirm }

  const showToast = useCallback((msg, type = 'info') => {
    setToast({ msg, type, id: Date.now() });
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

  const wrappedStart = useCallback(async () => {
    if (tr.isRunning) return;
    const tid = await tr.startTest(selectedModule, selectedClass);
  }, [tr, selectedModule, selectedClass]);

  // Load data on completion
  useEffect(() => {
    if (tr.status === 'SUCCESS' || tr.status === 'FAILED') {
      setShowReport(true);
      tr.loadResults(setTestResults);
      tr.loadHistory(setHistoryList);
      tr.loadFailedCases(setFailedCases);
      // Desktop notification
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

  // Keyboard shortcuts
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

  const selectedLabel = selectedClass
    ? `${selectedClass.name} [${selectedClass.type?.toUpperCase()}]`
    : MODULE_OPTIONS.find(o=>o.value===selectedModule)?.label || '全部模块';

  // History — expand to show output
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
        timeDisplay={tr.durationFmt} isRunning={tr.isRunning} elapsedFmt={tr.fmtElapsed(tr.elapsedSec)} cleaning={cleaning} />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Header activeMenu={activeMenu} isRunning={tr.isRunning} runningLabel={tr.runningLabel}
          selectedModule={selectedModule} selectedClass={selectedClass} selectedLabel={selectedLabel}
          onSelectModule={setSelectedModule} onSelectClass={setSelectedClass}
          onStart={wrappedStart} onStop={tr.stopTest}
          onOpenSettings={() => { config.setConfigOpen(true); config.loadConfigs(); }}
          cleaning={cleaning} onCleanup={async () => {
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
          }} />
        <div className="flex-1 flex flex-col min-h-0 bg-slate-50">
          {page === 'dashboard' && (
            <Dashboard terminalLines={tr.terminalLines} isRunning={tr.isRunning} elapsedSec={tr.elapsedSec}
              showReport={showReport} status={tr.status} logFilter={logFilter} onFilterChange={setLogFilter}
              onClear={() => tr.setTerminalLines([])} pct={tr.pct} progress={tr.progress} progressTotal={tr.progressTotal}
              fmtElapsed={tr.fmtElapsed} runningLabel={tr.runningLabel} onCloseReport={() => setShowReport(false)} />
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
      {/* Inline confirm dialog */}
      {confirm && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center" onClick={() => setConfirm(null)}>
          <div className="absolute inset-0 bg-black/30" />
          <div className="relative bg-white rounded-2xl shadow-xl px-6 py-5 max-w-sm w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <p className="text-sm text-slate-700 mb-4">{confirm.msg}</p>
            <div className="flex gap-2 justify-end">
              <button onClick={() => setConfirm(null)} className="px-4 py-2 rounded-lg text-sm border border-slate-200 text-slate-600 hover:bg-slate-50">取消</button>
              <button onClick={confirm.onConfirm} className="px-4 py-2 rounded-lg text-sm bg-blue-600 text-white hover:bg-blue-700">确认</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

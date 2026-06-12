import { useState, useCallback, useEffect, useRef } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { MODULES } from './data/modules';
import { useConfig } from './hooks/useConfig';
import { useTestStore } from './store/useTestStore';
import request from './utils/request';
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
  const navigate = useNavigate();
  const store = useTestStore();

  const [selected, setSelected] = useState(new Set());
  const [toast, setToast] = useState(null);
  const [cleaning, setCleaning] = useState(false);
  const [expandedHistory, setExpandedHistory] = useState({});
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [confirm, setConfirm] = useState(null);
  const lastToastRef = useRef({ msg: '', time: 0 });

  const showToast = useCallback((msg, type = 'info') => {
    const now = Date.now();
    if (lastToastRef.current.msg === msg && (now - lastToastRef.current.time) < 5000) return;
    lastToastRef.current = { msg, time: now };
    setToast({ msg, type, id: now });
    setTimeout(() => setToast(null), 3500);
  }, []);

  const config = useConfig(showToast);

  // ── Sync config to store + inject toast + init ──
  useEffect(() => {
    useTestStore.getState().setToastFn(showToast);
    useTestStore.getState().setConfig({ cfgUrl: config.cfgUrl, cfgProjectId: config.cfgProjectId, cfgUsername: config.cfgUsername, cfgPassword: config.cfgPassword });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [config.cfgUrl, config.cfgProjectId, config.cfgUsername, config.cfgPassword, showToast]);

  useEffect(() => { store.init(); }, []);

  // ── Cleaning state sync ──
  useEffect(() => {
    if (cleaning && (store.status === 'SUCCESS' || store.status === 'FAILED' || store.status === 'STOPPED')) {
      setCleaning(false);
    }
  }, [store.status, cleaning]);

  // Auto-collapse sidebar when running
  useEffect(() => {
    if (store.isRunning) setSidebarOpen(false);
  }, [store.isRunning]);

  const toggleSelect = useCallback((name, add) => {
    setSelected(prev => {
      const next = new Set(prev);
      add ? next.add(name) : next.delete(name);
      return next;
    });
  }, []);

  const wrappedStart = useCallback(async () => {
    if (store.isRunning) return;
    const names = [...selected];
    if (names.length === 0) { showToast('请先选择测试类', 'warning'); return; }
    const label = names.length <= 3 ? names.join(', ') : `${names.length} 个测试类`;

    // Query estimated time from backend
    let estimatedMs = 0;
    try {
      const { data } = await request.post('/api/test/estimate', { testClass: names.join(',') });
      estimatedMs = data.estimatedMs || 0;
    } catch {}

    setConfirm({ msg: `确认执行 ${label} 吗？`, estimatedMs, onConfirm: async () => {
      setConfirm(null);
      store.setShowReport(false);
      setSelected(new Set());
      await store.startTest(null, { name: names.join(','), type: 'multi' });
    }});
  }, [store, selected, showToast]);

  // ── Status effects (notifications, title, data loading) ──
  useEffect(() => {
    const st = store.status;
    if (st === 'SUCCESS' || st === 'FAILED') {
      store.setShowReport(true);
      store.loadResults();
      store.loadHistory();
      store.loadFailedCases();
      store.loadCaseDetails();
      if (Notification.permission === 'granted') {
        new Notification(st === 'SUCCESS' ? '测试通过' : '测试失败', { body: store.runningLabel });
      } else if (Notification.permission === 'default') {
        Notification.requestPermission();
      }
      document.title = (st === 'SUCCESS' ? '✓ ' : '✗ ') + 'TaaS Console';
    } else if (st === 'RUNNING') {
      document.title = '▶ TaaS Console';
    } else {
      document.title = 'TaaS Console';
    }
  }, [store.status]);

  // ── Global API error listener ──
  useEffect(() => {
    const onApiError = (e) => showToast(e.detail?.message || '请求失败', 'error');
    window.addEventListener('api:error', onApiError);
    return () => window.removeEventListener('api:error', onApiError);
  }, [showToast]);

  // ── Keyboard shortcuts ──
  useEffect(() => {
    const h = (e) => {
      if (e.ctrlKey && e.key === 'Enter') { e.preventDefault(); wrappedStart(); }
      if (e.key === 'Escape' && store.isRunning) { e.preventDefault(); store.stopTest(); }
    };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [wrappedStart, store.isRunning, store.stopTest]);

  // ── History expand ──
  const toggleHistoryExpand = useCallback(async (taskId) => {
    if (expandedHistory[taskId]) { setExpandedHistory(p => { const n={...p}; delete n[taskId]; return n; }); return; }
    setExpandedHistory(p => ({ ...p, [taskId]: 'loading' }));
    try {
      const { data: cases } = await request.get(`/api/test/history/${taskId}/cases`);
      const h = store.historyList.find(x => x.taskId === taskId);
      setExpandedHistory(p => ({ ...p, [taskId]: { cases, label: h?.label || '' } }));
    } catch { setExpandedHistory(p => { const n={...p}; delete n[taskId]; return n; }); }
  }, [expandedHistory, store.historyList]);

  // ── Rerun & cleanup actions ──
  const onRerunClass = useCallback(async (className) => {
    navigate('/');
    try {
      const { cfgUrl, cfgProjectId, cfgUsername, cfgPassword } = store;
      const r = await apiPost('/api/test/run', { url: cfgUrl, projectId: cfgProjectId, username: cfgUsername, password: cfgPassword, testClass: className });
      if (r.taskId) showToast('已启动: ' + className, 'info');
    } catch (e) { showToast('重跑失败: ' + e.message, 'error'); }
  }, [store, showToast, navigate]);

  const onRerun = useCallback(async (tid) => {
    if (store.isRunning) { showToast('已有任务在运行中', 'warning'); return; }
    setConfirm({ msg: '确认重跑该任务的失败用例？', onConfirm: async () => {
      setConfirm(null); navigate('/');
      try {
        const d = await apiPost('/api/test/rerun-failed', { taskId: tid });
        if (d.taskId) { store.resumeTask(d.taskId, d.label || 'Rerun'); showToast('已下发重跑', 'info'); }
      } catch (e) { showToast('重跑失败: ' + e.message, 'error'); }
    }});
  }, [store, showToast, navigate]);

  const onDeleteHistory = useCallback(async (tid) => {
    setConfirm({ msg: '确认删除该历史记录？', onConfirm: async () => {
      setConfirm(null);
      try {
        await apiDelete(`/api/test/history/${tid}`);
        store.loadHistory();
        showToast('已删除', 'info');
      } catch (e) { showToast('删除失败: ' + e.message, 'error'); }
    }});
  }, [showToast, store]);

  const onCleanupAction = useCallback(async () => {
    setConfirm({ msg: '确认清理环境？这将删除所有测试数据！', onConfirm: async () => {
      setConfirm(null);
      setCleaning(true);
      navigate('/');
      store.setTerminalLines([]);
      const append = (t, c) => store.appendLog(t, c);
      append('╔══════════════════════════════════════════════════╗', 'text-slate-500');
      append('║  开始环境清理…                                  ║', 'text-orange-400');
      append('╚══════════════════════════════════════════════════╝', 'text-slate-500');
      append('', '');
      try {
        const { cfgProjectId, cfgUrl, cfgUsername, cfgPassword } = store;
        const { data } = await request.post('/api/test/cleanup',
          { projectId: cfgProjectId, url: cfgUrl, username: cfgUsername, password: cfgPassword });
        if (data.code === 409) {
          append('[WARN] ' + data.msg, 'text-amber-400');
          showToast(data.msg, 'warning');
          setCleaning(false);
          return;
        }
        store._connectCleanupWs(data.taskId, () => setCleaning(false));
      } catch (e) { showToast('清理失败: ' + e.message, 'error'); setCleaning(false); }
    }});
  }, [store, showToast, navigate]);

  return (
    <div className="h-screen flex overflow-hidden bg-slate-50">
      <Sidebar cleaning={cleaning} open={sidebarOpen} onToggle={() => setSidebarOpen(o => !o)} />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Header onOpenSettings={() => { config.setConfigOpen(true); config.loadConfigs(); }} />
        <div className="flex-1 flex flex-col min-h-0 bg-slate-50/80">
          <Routes>
            <Route path="/" element={
              <Dashboard modules={MODULES} selected={selected} onToggle={toggleSelect}
                onStart={wrappedStart} onStop={() => store.stopTest()} onCleanup={onCleanupAction}
                cleaning={cleaning} selectedCount={selected.size} />
            } />
            <Route path="/results" element={<Results />} />
            <Route path="/history" element={<History onRerun={onRerun} onDelete={onDeleteHistory}
              expandedHistory={expandedHistory} onToggleExpand={toggleHistoryExpand} />} />
            <Route path="/failed" element={<FailedCases onRerunClass={onRerunClass} />} />
            <Route path="/settings" element={<SettingsPage onOpen={() => { config.setConfigOpen(true); config.loadConfigs(); }} />} />
          </Routes>
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

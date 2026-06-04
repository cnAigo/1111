import { useState, useRef, useCallback, useEffect } from 'react';

export function useTestRun(cfgUrl, cfgProjectId, cfgUsername, cfgPassword, showToast) {
  const [isRunning, setIsRunning] = useState(false);
  const [taskId, setTaskId] = useState(null);
  const [status, setStatus] = useState('IDLE');
  const [progress, setProgress] = useState(0);
  const [progressTotal, setProgressTotal] = useState(0);
  const [durationFmt, setDurationFmt] = useState('');
  const [runningLabel, setRunningLabel] = useState('');
  const [terminalLines, setTerminalLines] = useState([]);
  const [elapsedSec, setElapsedSec] = useState(0);

  const pollRef = useRef(null);
  const lastOutLenRef = useRef(0);
  const startTimeRef = useRef(null);
  const MAX_LOG = 800;

  const apiPost = async (url, body) => {
    const r = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
    return r.json();
  };
  const apiGet = async (url) => { const r = await fetch(url); return r.json(); };

  const appendLog = useCallback((text, cls = '') => {
    setTerminalLines(prev => {
      const next = [...prev, { text, cls, id: Date.now() + Math.random() }];
      return next.length > MAX_LOG ? next.slice(next.length - MAX_LOG) : next;
    });
  }, []);

  const loadResults = useCallback(async (setTestResults) => {
    try { const d = await apiGet('/api/test/results'); if (Array.isArray(d)) setTestResults(d); }
    catch (e) { showToast('加载测试结果失败: ' + e.message, 'error'); }
  }, [showToast]);

  const loadHistory = useCallback(async (setHistoryList) => {
    try { const d = await apiGet('/api/test/history'); if (Array.isArray(d)) setHistoryList(d); }
    catch (e) { showToast('加载历史记录失败: ' + e.message, 'error'); }
  }, [showToast]);

  const loadFailedCases = useCallback(async (setFailedCases) => {
    try { const d = await apiGet('/api/test/failed-cases'); if (Array.isArray(d)) setFailedCases(d); }
    catch (e) { showToast('加载失败用例失败: ' + e.message, 'error'); }
  }, [showToast]);

  const resetRunState = useCallback(() => {
    lastOutLenRef.current = 0; setTerminalLines([]);
    setProgress(0); setProgressTotal(0); setDurationFmt('');
    setRunningLabel('');
  }, []);

  const pollStatus = useCallback(async (tid) => {
    try {
      const sd = await apiGet(`/api/test/status?taskId=${tid}`);
      setDurationFmt(sd.durationFmt || '');
      if (sd.progress !== undefined) setProgress(sd.progress);
      if (sd.progressTotal !== undefined) setProgressTotal(sd.progressTotal);
      const out = sd.output || '';
      if (out.length > lastOutLenRef.current) {
        const newText = out.substring(lastOutLenRef.current);
        lastOutLenRef.current = out.length;
        for (const line of newText.split('\n')) {
          if (!line.trim()) continue;
          let cls = 'text-slate-300';
          if (line.includes('[ERROR]') || line.includes('FAILED')) cls = 'text-red-400';
          else if (line.includes('[WARN]')) cls = 'text-amber-400';
          else if (line.includes('[INFO]')) cls = 'text-blue-400';
          else if (line.includes('PASS') || line.includes('SUCCESS')) cls = 'text-emerald-400';
          appendLog(line, cls);
        }
      }
      if (sd.status === 'SUCCESS' || sd.status === 'FAILED') {
        if (pollRef.current) clearInterval(pollRef.current);
        setIsRunning(false);
        setStatus(sd.status);
        appendLog(sd.status === 'SUCCESS'
          ? '─── ✓ All passed — ' + (sd.durationFmt || '') + ' ───'
          : '✗ ' + (sd.errorMessage || 'Failed'), sd.status === 'SUCCESS' ? 'text-emerald-400' : 'text-red-400');
        showToast(sd.status === 'SUCCESS' ? '测试通过!' : '测试失败', sd.status === 'SUCCESS' ? 'success' : 'error');
        return sd.status;
      } else if (sd.status === 'STOPPED') {
        if (pollRef.current) clearInterval(pollRef.current);
        setIsRunning(false); setStatus('STOPPED');
        appendLog('[WARN] Stopped', 'text-amber-400');
      }
    } catch { /* retry next poll */ }
    return null;
  }, [appendLog, showToast]);

  const startTest = useCallback(async (selectedModule, selectedClass) => {
    if (isRunning) return;
    resetRunState(); setIsRunning(true); setStatus('RUNNING');
    const body = selectedClass
      ? { url: cfgUrl, projectId: cfgProjectId, username: cfgUsername, password: cfgPassword, testClass: selectedClass.name }
      : { url: cfgUrl, projectId: cfgProjectId, username: cfgUsername, password: cfgPassword, module: selectedModule || 'ALL' };
    const label = selectedClass ? selectedClass.name : (selectedModule || 'ALL');
    setRunningLabel(label);
    appendLog('╔══════════════════════════════════════╗', 'text-slate-500');
    appendLog(`║  ▶ ${label}`, 'text-cyan-400');
    appendLog('╚══════════════════════════════════════╝', 'text-slate-500');
    try {
      const d = await apiPost('/api/test/run', body);
      if (d.code === 409) { appendLog(`[WARN] ${d.msg}`, 'text-amber-400'); showToast(d.msg, 'warning'); setIsRunning(false); setStatus('IDLE'); return null; }
      setTaskId(d.taskId); localStorage.setItem('taas_taskId', d.taskId);
      appendLog(`[INFO] Task: ${d.taskId}`, 'text-blue-400');
      lastOutLenRef.current = 0;
      pollRef.current = setInterval(() => pollStatus(d.taskId), 1500);
      return d.taskId;
    } catch (err) { appendLog(`[ERROR] ${err.message}`, 'text-red-400'); showToast('启动失败: ' + err.message, 'error'); setIsRunning(false); setStatus('IDLE'); return null; }
  }, [isRunning, cfgUrl, cfgProjectId, cfgUsername, cfgPassword, resetRunState, appendLog, pollStatus, showToast]);

  const resumeTask = useCallback((tid, label) => {
    setTaskId(tid); setIsRunning(true); setStatus('RUNNING'); setRunningLabel(label || 'Rerun');
    localStorage.setItem('taas_taskId', tid);
    lastOutLenRef.current = 0;
    appendLog(`[INFO] Resumed task: ${tid}`, 'text-blue-400');
    pollRef.current = setInterval(() => pollStatus(tid), 1500);
  }, [appendLog, pollStatus]);

  const stopTest = useCallback(async () => {
    if (!taskId) return;
    try { await apiPost(`/api/test/stop/${taskId}`, {}); if (pollRef.current) clearInterval(pollRef.current); setIsRunning(false); setStatus('IDLE'); localStorage.removeItem('taas_taskId'); appendLog('[WARN] Stopped', 'text-amber-400'); showToast('已停止', 'warning'); }
    catch { showToast('停止失败', 'error'); }
  }, [taskId, appendLog, showToast]);

  // Elapsed timer
  useEffect(() => {
    if (isRunning) { startTimeRef.current = Date.now(); setElapsedSec(0); }
    if (!isRunning) return;
    const t = setInterval(() => { if (startTimeRef.current) setElapsedSec(Math.floor((Date.now() - startTimeRef.current) / 1000)); }, 500);
    return () => clearInterval(t);
  }, [isRunning]);

  // Cleanup
  useEffect(() => { return () => { if (pollRef.current) clearInterval(pollRef.current); }; }, []);
  useEffect(() => { if (status === 'SUCCESS' || status === 'FAILED' || status === 'STOPPED') localStorage.removeItem('taas_taskId'); }, [status]);

  // Recovery on mount
  useEffect(() => {
    const saved = localStorage.getItem('taas_taskId');
    if (!saved) return;
    apiGet(`/api/test/status?taskId=${saved}`).then(sd => {
      if (sd.status === 'RUNNING' || sd.status === 'PENDING') {
        setIsRunning(true); setStatus('RUNNING'); setTaskId(saved);
        appendLog('[INFO] Reconnected: ' + saved, 'text-blue-400');
        lastOutLenRef.current = 0;
        pollRef.current = setInterval(() => pollStatus(saved), 1500);
      } else { localStorage.removeItem('taas_taskId'); }
    }).catch(() => localStorage.removeItem('taas_taskId'));
  }, []);

  const fmtElapsed = (s) => { const m = Math.floor(s / 60); return m > 0 ? `${m}m ${s % 60}s` : `${s}s`; };
  const pct = progressTotal > 0 ? Math.round((progress / progressTotal) * 100) : 0;

  return { isRunning, taskId, status, progress, progressTotal, durationFmt, runningLabel, terminalLines, elapsedSec,
    pct, fmtElapsed, startTest, stopTest, resumeTask, appendLog, loadResults, loadHistory, loadFailedCases, setTerminalLines };
}

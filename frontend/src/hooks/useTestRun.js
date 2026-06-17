import { useState, useRef, useCallback, useEffect } from 'react';
import { writeTerminal, useTestStore } from '../store/useTestStore';

const MAX_EXECUTION_MS = 1800000;     // 30 min total → timeout
const MAX_RETRIES = 2;

// ── ANSI escape code helpers ──
const S = '\x1b[';
const R = S + '0m';
const B = S + '1m';
const ANSI = {
  reset:    R,
  bold:     B,
  red:      S + '91m',
  green:    S + '92m',
  yellow:   S + '93m',
  blue:     S + '94m',
  magenta:  S + '95m',
  cyan:     S + '1;96m',
  white:    S + '37m',
  gray:     S + '90m',
  dimGray:  S + '2;90m',
  rose:     S + '31m',
};
const ansiMap = {
  'text-cyan-400':      ANSI.cyan,
  'text-red-400':        ANSI.red,
  'text-emerald-400':    ANSI.green,
  'text-emerald-300':    ANSI.green,
  'text-amber-400':      ANSI.yellow,
  'text-rose-400':       ANSI.red + B,
  'text-rose-300':       ANSI.rose,
  'text-slate-300':      ANSI.white,
  'text-slate-400':      ANSI.gray,
  'text-slate-500':      ANSI.dimGray,
  'text-slate-600':      ANSI.dimGray,
  'font-bold':           B,
};

const toAnsi = (text, cls = '') => {
  if (!cls) return text + '\r\n';
  let prefix = '';
  for (const [key, code] of Object.entries(ansiMap)) {
    if (cls.includes(key)) prefix += code;
  }
  return prefix + text + ANSI.reset + '\r\n';
};

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
  const [estimatedMs, setEstimatedMs] = useState(0);

  const wsRef = useRef(null);
  const startTimeRef = useRef(null);
  const timeoutRef = useRef(null);

  // ── fetch with timeout ──
  const fetchWithTimeout = useCallback(async (url, options = {}) => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 30000);
    try {
      const r = await fetch(url, { ...options, signal: controller.signal });
      clearTimeout(timer);
      if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
      return r;
    } catch (err) {
      clearTimeout(timer);
      if (err.name === 'AbortError') throw new Error('请求超时 (30s)');
      throw err;
    }
  }, []);

  const apiPost = useCallback(async (url, body, retries = MAX_RETRIES) => {
    for (let i = 0; i <= retries; i++) {
      try {
        const r = await fetchWithTimeout(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
        });
        return await r.json();
      } catch (err) {
        if (i === retries) throw err;
        await new Promise(r => setTimeout(r, 1000 * (i + 1)));
      }
    }
  }, [fetchWithTimeout]);

  const apiGet = useCallback(async (url, retries = MAX_RETRIES) => {
    for (let i = 0; i <= retries; i++) {
      try {
        const r = await fetchWithTimeout(url);
        return await r.json();
      } catch (err) {
        if (i === retries) throw err;
        await new Promise(r => setTimeout(r, 1000 * (i + 1)));
      }
    }
  }, [fetchWithTimeout]);

  const appendLog = useCallback((text, cls = '') => {
    writeTerminal(toAnsi(text, cls));
  }, []);

  const loadResults = useCallback(async (setTestResults) => {
    try { const d = await apiGet('/api/test/results'); if (Array.isArray(d)) setTestResults(d); }
    catch (e) { showToast('加载测试结果失败: ' + e.message, 'error'); }
  }, [apiGet, showToast]);

  const loadHistory = useCallback(async (setHistoryList) => {
    try { const d = await apiGet('/api/test/history'); if (Array.isArray(d)) setHistoryList(d); }
    catch (e) { showToast('加载历史记录失败: ' + e.message, 'error'); }
  }, [apiGet, showToast]);

  const loadFailedCases = useCallback(async (setFailedCases) => {
    try { const d = await apiGet('/api/test/failed-cases'); if (Array.isArray(d)) setFailedCases(d); }
    catch (e) { showToast('加载失败用例失败: ' + e.message, 'error'); }
  }, [apiGet, showToast]);

  const resetRunState = useCallback(() => {
    setTerminalLines([]);
    setProgress(0); setProgressTotal(0); setDurationFmt('');
    setRunningLabel(''); setEstimatedMs(0); useTestStore.getState().setEstimatedMs(0);
  }, []);

  // ── WebSocket connection ──

  const disconnectWs = useCallback(() => {
    if (timeoutRef.current) { clearTimeout(timeoutRef.current); timeoutRef.current = null; }
    if (wsRef.current) {
      try { wsRef.current.close(); } catch {}
      wsRef.current = null;
    }
  }, []);

  const connectWebSocket = useCallback((tid) => {
    disconnectWs();

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/test-run/${tid}`;
    const ws = new WebSocket(wsUrl);

    wsRef.current = ws;

    // Client-side execution timeout
    timeoutRef.current = setTimeout(() => {
      appendLog('', '');
      appendLog('══════════════════════════════════════════', 'text-red-400');
      appendLog('  ✗ 执行超时 (' + Math.round(MAX_EXECUTION_MS / 60000) + '分钟) — 已自动终止', 'text-red-400');
      appendLog('══════════════════════════════════════════', 'text-red-400');
      disconnectWs();
      setIsRunning(false);
      setStatus('FAILED');
      try { apiPost('/api/test/stop/' + tid, {}, 0); } catch {}
      localStorage.removeItem('taas_taskId');
      showToast('执行超时', 'error');
    }, MAX_EXECUTION_MS);

    ws.onopen = () => {
      appendLog('[系统] WebSocket 已连接', 'text-slate-500');
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);

        switch (msg.type) {
          case 'line': {
            writeTerminal(msg.text + '\r\n');
            break;
          }

          case 'progress': {
            if (msg.progress !== undefined) setProgress(msg.progress);
            if (msg.progressTotal !== undefined) setProgressTotal(msg.progressTotal);
            break;
          }

          case 'status': {
            if (msg.status === 'STOPPED') {
              disconnectWs();
              setIsRunning(false);
              setStatus('STOPPED');
              appendLog('', '');
              appendLog(`  ⏹ ${msg.errorMessage || '已停止'}`, 'text-amber-400');
              showToast('已停止', 'warning');
            }
            break;
          }

          case 'result': {
            disconnectWs();
            setIsRunning(false);
            setStatus(msg.status);
            if (msg.durationFmt !== undefined) setDurationFmt(msg.durationFmt);
            if (msg.progress !== undefined) setProgress(msg.progress);
            if (msg.progressTotal !== undefined) setProgressTotal(msg.progressTotal);

            // Fetch detailed results + case details
            (async () => {
              try {
                const [results, detailsArr] = await Promise.all([
                  apiGet('/api/test/results'),
                  apiGet('/api/test/case-details').catch(() => []),
                ]);
                const detailMap = {};
                for (const d of (Array.isArray(detailsArr) ? detailsArr : [])) {
                  const key = d.className || '';
                  if (!detailMap[key]) detailMap[key] = {};
                  if (d.javaMethod) detailMap[key][d.javaMethod] = d;
                }

                appendLog('', '');
                appendLog('══════════════════════════════════════════', 'text-slate-600');
                appendLog('  Test Results', 'text-slate-300');
                appendLog('══════════════════════════════════════════', 'text-slate-600');
                appendLog('', '');

                let grandTotal = 0, grandFailed = 0, grandSkipped = 0, grandWarn = 0;
                for (const cls of (Array.isArray(results) ? results : [])) {
                  const hasFailures = (cls.failures || 0) + (cls.errors || 0) > 0;
                  const warnCount = cls.warnings ? cls.warnings.length : 0;
                  grandWarn += warnCount;
                  const timeStr = cls.time ? ' (' + cls.time + 's)' : '';
                  const marker = hasFailures ? '✗' : '✓';
                  const markerCls = hasFailures ? 'text-red-400' : 'text-emerald-400';

                  let header = marker + ' ' + cls.className + ' — ' + cls.tests + ' tests, ' + (cls.failures || 0) + ' failures' + timeStr;
                  if (warnCount > 0) header += '  ⚠' + warnCount;
                  appendLog(header, markerCls + ' font-bold');

                  const clsDetailMap = detailMap[cls.className] || {};

                  if (cls.cases && cls.cases.length > 0) {
                    for (const c of cls.cases) {
                      const detail = clsDetailMap[c.name] || Object.values(clsDetailMap).find(d => d.caseId && c.name.includes(d.caseId));
                      const caseTime = c.time ? ' (' + c.time + 's)' : '';
                      const detailSuffix = detail
                        ? '  — ' + [detail.title, detail.caseType, detail.httpMethod ? detail.httpMethod + ' ' + (detail.apiUrl || '') : ''].filter(Boolean).join(' | ')
                        : '';

                      if (c.status === 'PASS') {
                        appendLog('    ✓ ' + c.name + caseTime, 'text-emerald-300 text-[11px]');
                        if (detail) {
                          if (detail.title) appendLog('      标题: ' + detail.title, 'text-slate-500 text-[10px]');
                          if (detail.expected) appendLog('      预期: ' + detail.expected, 'text-slate-500 text-[10px]');
                          if (detail.steps) appendLog('      步骤: ' + detail.steps.substring(0, 120) + (detail.steps.length > 120 ? '…' : ''), 'text-slate-600 text-[10px]');
                        }
                      } else {
                        appendLog('    ✗ ' + c.name + caseTime + detailSuffix, 'text-red-400');
                        if (detail) {
                          if (detail.title) appendLog('      标题: ' + detail.title, 'text-rose-400 text-[10px]');
                          if (detail.expected) appendLog('      预期: ' + detail.expected, 'text-rose-400 text-[10px]');
                        }
                        if (c.reason) {
                          for (const reasonLine of c.reason.split('\n')) {
                            const t2 = reasonLine.trim();
                            if (t2) appendLog('      ' + t2, 'text-rose-400 text-[11px]');
                          }
                        }
                      }
                    }
                  }
                  if (cls.warnings && cls.warnings.length > 0) {
                    appendLog('    ⚠ Warnings:', 'text-amber-400 font-bold');
                    for (const w of cls.warnings) {
                      appendLog('      ' + w, 'text-amber-400');
                    }
                  }
                  appendLog('', '');
                  grandTotal += cls.tests || 0;
                  grandFailed += (cls.failures || 0) + (cls.errors || 0);
                  grandSkipped += cls.skipped || 0;
                }

                appendLog('──────────────────────────────────────────', 'text-slate-600');
                let summary = '  Results: ' + grandTotal + ' tests, ' + grandFailed + ' failed, ' + grandSkipped + ' skipped';
                if (grandWarn > 0) {
                  summary += ', ⚠' + grandWarn + ' warnings';
                  appendLog(summary, 'text-amber-400');
                  appendLog('  ⚠ 请检查上方警告项 — 测试虽通过但可能有缺陷', 'text-amber-400');
                } else if (grandFailed > 0) {
                  appendLog(summary, 'text-red-400');
                } else {
                  appendLog(summary + ' — all passed ' + (msg.durationFmt || ''), 'text-emerald-400');
                }
                appendLog('══════════════════════════════════════════', 'text-slate-600');

                showToast(msg.status === 'SUCCESS' ? '测试通过!' : '测试失败', msg.status === 'SUCCESS' ? 'success' : 'error');
              } catch (e) {
                appendLog('', '');
                appendLog('══════════════════════════════════════════', 'text-slate-600');
                appendLog('  ✗ ' + (msg.errorMessage || 'Tests failed'), 'text-red-400');
                appendLog('══════════════════════════════════════════', 'text-slate-600');
              }
              localStorage.removeItem('taas_taskId');
            })();
            break;
          }
        }
      } catch {
        // Ignore non-JSON messages
      }
    };

    ws.onclose = () => {
      appendLog('[系统] WebSocket 已断开', 'text-slate-500');
      if (timeoutRef.current) { clearTimeout(timeoutRef.current); timeoutRef.current = null; }
    };

    ws.onerror = () => {
      // If connection fails while running, fall back to polling
      if (timeoutRef.current) { clearTimeout(timeoutRef.current); timeoutRef.current = null; }
      try { ws.close(); } catch {}
    };
  }, [appendLog, disconnectWs, apiGet, apiPost, showToast]);

  // Parse a line of raw Maven/Surefire output into IntelliJ-style format
  const parseOutputLine = useCallback((rawLine) => {
    const t = rawLine.trim();
    if (!t) return null;

    // Determine log level from the raw line
    let level = 'INFO';
    if (/\[ERROR\]|<<< FAILURE|BUILD FAILURE|Exception:|^\s*at\s+|Caused by:/i.test(t)) {
      level = 'ERROR';
    } else if (/\[WARN\]/i.test(t)) {
      level = 'WARN';
    }

    // Filter: skip Maven build noise
    if (t.startsWith('[INFO]') && /---|Building |Scanning|\.jar|classpath|Download(ing|ed)/i.test(t)) return null;
    if (/^\s*\[(INFO|WARNING)\].*(download|maven|plugin|compiler|resources|surefire)/i.test(t)) return null;

    // Running XxxTest -> class header
    const runningMatch = t.match(/\[INFO\]\s*Running\s+([\w.]+)/);
    if (runningMatch) {
      const fullName = runningMatch[1];
      const shortName = fullName.includes('.') ? fullName.split('.').pop() : fullName;
      return [
        { text: '', cls: '', level },
        { text: '▶ ' + shortName, cls: 'text-cyan-400 font-bold', level },
        { text: '  ' + fullName, cls: 'text-slate-600 text-[11px]', level },
      ];
    }

    // Tests run: summary line
    const summaryMatch = t.match(/Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)/);
    if (summaryMatch) {
      const [, total, failures, errors, skipped] = summaryMatch;
      const hasFailures = parseInt(failures) + parseInt(errors) > 0;
      const timeMatch = t.match(/Time elapsed:\s*([\d.]+)\s*s/);
      const timeStr = timeMatch ? '  (' + timeMatch[1] + 's)' : '';

      if (hasFailures) {
        return [
          { text: '  ✗ Tests run: ' + total + ', Failures: ' + failures + ', Errors: ' + errors + ', Skipped: ' + skipped + timeStr, cls: 'text-red-400', level },
        ];
      } else {
        return [
          { text: '  ✓ Tests run: ' + total + ', Failures: 0, Errors: 0, Skipped: ' + skipped + timeStr, cls: 'text-emerald-400', level },
        ];
      }
    }

    // Individual test failure
    const singleFailMatch = t.match(/(\w[\w\d_.]*)\s+Time elapsed:\s*([\d.]+)\s*s?\s*<<<\s*(FAILURE|ERROR)!?/);
    if (singleFailMatch) {
      return [{ text: '  ✗ ' + singleFailMatch[1] + '  (' + singleFailMatch[2] + 's)', cls: 'text-red-400', level }];
    }

    // Exception messages
    if (/^org\.opentest4j\./.test(t)) {
      const msg = t.replace(/^org\.opentest4j\.\w+:\s*/, '');
      return [{ text: '    ├ ' + msg, cls: 'text-rose-400', level }];
    }
    if (/^com\.microsoft\.playwright\./.test(t)) {
      const msg = t.replace(/^com\.microsoft\.playwright\.\w+:\s*/, '');
      return [{ text: '    ├ ' + msg, cls: 'text-rose-400', level }];
    }
    if (/^[\w.]+Exception:/.test(t)) {
      const msg = t.replace(/^[\w.]+Exception:\s*/, '');
      return [{ text: '    ├ ' + msg, cls: 'text-rose-400', level }];
    }

    // Java stack frames
    if (/^\s*at\s+[\w.$]+\([\w.]+:\d+\)/.test(t)) {
      return [{ text: '    ' + t, cls: 'text-rose-300 text-[11px]', level }];
    }
    if (t.startsWith('... ') && /\d+\s+more/.test(t)) {
      return [{ text: '    ' + t, cls: 'text-rose-300 text-[11px]', level }];
    }

    // Caused by:
    if (/^Caused by:/.test(t)) {
      return [{ text: '    └ ' + t, cls: 'text-rose-300 text-[11px]', level }];
    }

    // Error/warn
    if (t.includes('[ERROR]') || t.includes('<<< FAILURE') || t.includes('BUILD FAILURE')) {
      return [{ text: t.replace(/^\[ERROR\]\s*/, ''), cls: 'text-red-400', level }];
    }
    if (t.includes('[WARN]')) {
      return [{ text: t.replace(/^\[WARN\]\s*/, ''), cls: 'text-amber-400', level }];
    }

    // HTTP 200 -> green
    if (/\b(?:status|code|response|HTTP)\s*[:=]?\s*200\b/i.test(t) || /\b200\s*OK\b/i.test(t)) {
      return [{ text: t.replace(/^\[INFO\]\s*/, ''), cls: 'text-emerald-400', level }];
    }

    // Info lines
    let cleaned = t.replace(/^\[INFO\]\s*/, '');
    if (!cleaned || /^BUILD|^----|^Results\s*:|^Tests\s*:|^Final Memory/i.test(cleaned)) return null;
    if (/^\d+\.\d+\s*s$/.test(cleaned)) return null;

    return [{ text: cleaned, cls: 'text-slate-400', level }];
  }, []);

  // ── Test control ──

  const startTest = useCallback(async (selectedModule, selectedClass) => {
    if (isRunning) return;
    resetRunState(); setIsRunning(true); setStatus('RUNNING');
    const body = selectedClass
      ? { url: cfgUrl, projectId: cfgProjectId, username: cfgUsername, password: cfgPassword, testClass: selectedClass.name }
      : { url: cfgUrl, projectId: cfgProjectId, username: cfgUsername, password: cfgPassword, module: selectedModule || 'ALL' };
    const label = selectedClass ? selectedClass.name : (selectedModule || 'ALL');

    setRunningLabel(label);
    const now = new Date().toLocaleTimeString('zh-CN', { hour12: false });
    appendLog('╔══════════════════════════════════════════════════╗', 'text-slate-500');
    appendLog('║  Testing started at ' + now + '                  ║', 'text-slate-400');
    appendLog('║  ▶ ' + label.padEnd(47) + '║', 'text-cyan-400');
    appendLog('╚══════════════════════════════════════════════════╝', 'text-slate-500');
    appendLog('', '');

    try {
      const d = await apiPost('/api/test/run', body);
      if (d.code === 409) {
        appendLog('[WARN] ' + d.msg, 'text-amber-400');
        showToast(d.msg, 'warning');
        setIsRunning(false); setStatus('IDLE');
        return null;
      }
      setTaskId(d.taskId);
      localStorage.setItem('taas_taskId', d.taskId);
      connectWebSocket(d.taskId);
      // Fetch estimatedMs for time-based progress
      apiGet('/api/test/status?taskId=' + d.taskId).then(sd => {
        if (sd.estimatedMs) { setEstimatedMs(sd.estimatedMs); useTestStore.getState().setEstimatedMs(sd.estimatedMs); }
      }).catch(() => {});
      return d.taskId;
    } catch (err) {
      appendLog('[ERROR] 启动失败: ' + err.message, 'text-red-400');
      showToast('启动失败: ' + err.message, 'error');
      setIsRunning(false); setStatus('IDLE');
      return null;
    }
  }, [isRunning, cfgUrl, cfgProjectId, cfgUsername, cfgPassword, resetRunState, appendLog, showToast, apiPost, connectWebSocket]);

  const resumeTask = useCallback((tid, label) => {
    setTaskId(tid); setIsRunning(true); setStatus('RUNNING'); setRunningLabel(label || 'Rerun');
    localStorage.setItem('taas_taskId', tid);
    const now = new Date().toLocaleTimeString('zh-CN', { hour12: false });
    appendLog('╔══════════════════════════════════════════════════╗', 'text-slate-500');
    appendLog('║  Resumed at ' + now + '                            ║', 'text-slate-400');
    appendLog('║  ▶ ' + (label || 'Rerun').padEnd(47) + '║', 'text-cyan-400');
    appendLog('╚══════════════════════════════════════════════════╝', 'text-slate-500');
    appendLog('', '');
    connectWebSocket(tid);
  }, [appendLog, connectWebSocket]);

  const stopTest = useCallback(async () => {
    if (!taskId) return;
    disconnectWs();
    try {
      await apiPost('/api/test/stop/' + taskId, {});
      setIsRunning(false); setStatus('IDLE');
      localStorage.removeItem('taas_taskId');
      appendLog('[WARN] Stopped by user', 'text-amber-400');
      showToast('已停止', 'warning');
    } catch {
      showToast('停止失败', 'error');
    }
  }, [taskId, appendLog, apiPost, disconnectWs, showToast]);

  // ── Elapsed timer ──
  useEffect(() => {
    if (isRunning) { startTimeRef.current = Date.now(); setElapsedSec(0); }
    if (!isRunning) return;
    const t = setInterval(() => {
      if (startTimeRef.current) setElapsedSec(Math.floor((Date.now() - startTimeRef.current) / 1000));
    }, 500);
    return () => clearInterval(t);
  }, [isRunning]);

  // Cleanup on unmount
  useEffect(() => { return () => { disconnectWs(); }; }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (status === 'SUCCESS' || status === 'FAILED' || status === 'STOPPED') {
      localStorage.removeItem('taas_taskId');
    }
  }, [status]);

  // Initial status sync — reconnect WebSocket if task was running before page refresh
  useEffect(() => {
    apiGet('/api/test/status').then(sd => {
      if (sd.status === 'RUNNING') {
        const saved = localStorage.getItem('taas_taskId');
        if (saved) {
          setIsRunning(true); setStatus('RUNNING'); setTaskId(saved);
          if (sd.estimatedMs) { setEstimatedMs(sd.estimatedMs); useTestStore.getState().setEstimatedMs(sd.estimatedMs); }
          connectWebSocket(saved);
        } else {
          setIsRunning(true); setStatus('RUNNING');
          setRunningLabel(sd.label || '');
          if (sd.durationFmt) setDurationFmt(sd.durationFmt);
          if (sd.estimatedMs) { setEstimatedMs(sd.estimatedMs); useTestStore.getState().setEstimatedMs(sd.estimatedMs); }
        }
      } else if (sd.status === 'SUCCESS' || sd.status === 'FAILED' || sd.status === 'STOPPED') {
        setStatus(sd.status);
        localStorage.removeItem('taas_taskId');
      }
    }).catch(() => {});
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const fmtElapsed = (s) => { const m = Math.floor(s / 60); return m > 0 ? `${m}m ${s % 60}s` : `${s}s`; };

  // Time-based smooth progress: uses elapsed time vs estimated duration
  const pct = (() => {
    if (status === 'SUCCESS' || status === 'FAILED' || status === 'STOPPED') return 100;
    if (!isRunning) return 0;
    if (estimatedMs > 0) {
      return Math.min(Math.floor((elapsedSec * 1000 / estimatedMs) * 100), 95);
    }
    // No estimate — fake slow ramp to 80% over 5 minutes
    return Math.min(Math.floor(elapsedSec / 3.75), 80);
  })();

  return {
    isRunning, taskId, status, progress, progressTotal, durationFmt, runningLabel, terminalLines, elapsedSec,
    pct, estimatedMs, fmtElapsed, startTest, stopTest, resumeTask, appendLog, loadResults, loadHistory, loadFailedCases, setTerminalLines,
  };
}

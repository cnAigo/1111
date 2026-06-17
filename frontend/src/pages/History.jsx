import { useMemo, useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, Clock, RefreshCw, Trash2, ChevronRight, TrendingUp, Activity, BarChart3, FileText, Loader2 } from 'lucide-react';
import StatusBadge from '../components/StatusBadge';
import ProgressBar from '../components/ProgressBar';
import { useTestStore } from '../store/useTestStore';
import request from '../utils/request';

function HistoryStats({ stats }) {
  if (!stats) return null;
  return (
    <div className="grid grid-cols-3 gap-4 mb-4">
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm px-5 py-4 flex items-center gap-4">
        <div className="w-10 h-10 rounded-lg bg-emerald-100 flex items-center justify-center flex-shrink-0">
          <TrendingUp size={18} className="text-emerald-600" />
        </div>
        <div>
          <p className="text-[11px] text-slate-500 uppercase tracking-wider">近7日通过率</p>
          <p className="text-xl font-bold text-slate-800">{stats.recentRate}{stats.recentRate !== '—' ? '%' : ''}</p>
        </div>
      </div>
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm px-5 py-4 flex items-center gap-4">
        <div className="w-10 h-10 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
          <Activity size={18} className="text-blue-600" />
        </div>
        <div>
          <p className="text-[11px] text-slate-500 uppercase tracking-wider">今日执行次数</p>
          <p className="text-xl font-bold text-slate-800">{stats.todayCount}</p>
        </div>
      </div>
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm px-5 py-4 flex items-center gap-4">
        <div className="w-10 h-10 rounded-lg bg-purple-100 flex items-center justify-center flex-shrink-0">
          <BarChart3 size={18} className="text-purple-600" />
        </div>
        <div>
          <p className="text-[11px] text-slate-500 uppercase tracking-wider">总通过率</p>
          <p className="text-xl font-bold text-slate-800">{stats.overallRate}{stats.overallRate !== '—' ? '%' : ''}</p>
          <p className="text-[10px] text-slate-400">{stats.totalCases} 用例总数</p>
        </div>
      </div>
    </div>
  );
}

export default function History({ onRerun, onDelete, expandedHistory, onToggleExpand }) {
  const navigate = useNavigate();
  const historyList = useTestStore(s => s.historyList);
  const historyHasMore = useTestStore(s => s.historyHasMore);
  const historyPage = useTestStore(s => s.historyPage);
  const loadHistory = useTestStore(s => s.loadHistory);
  const loadHistoryStats = useTestStore(s => s.loadHistoryStats);
  const caseDetails = useTestStore(s => s.caseDetails);
  const [logData, setLogData] = useState({});
  const [expandedClass, setExpandedClass] = useState({});
  const [expandedClassLog, setExpandedClassLog] = useState({});
  const [stats, setStats] = useState(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const sentinelRef = useRef(null);

  const toggleClass = (key) => setExpandedClass(p => ({ ...p, [key]: !p[key] }));
  const toggleClassLog = (key) => setExpandedClassLog(p => ({ ...p, [key]: !p[key] }));
  const isClassOpen = (key, clsHasFailures) => key in expandedClass ? expandedClass[key] : clsHasFailures;

  // Load initial data on mount
  useEffect(() => {
    if (historyList.length === 0) loadHistory(0, false);
    loadHistoryStats().then(s => { if (s) setStats(s); });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Infinite scroll via IntersectionObserver
  const loadMore = useCallback(async () => {
    if (loadingMore || !historyHasMore) return;
    setLoadingMore(true);
    await loadHistory(historyPage + 1, true);
    setLoadingMore(false);
  }, [loadingMore, historyHasMore, historyPage, loadHistory]);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) loadMore();
    }, { rootMargin: '200px' });
    observer.observe(el);
    return () => observer.disconnect();
  }, [loadMore]);

  const fetchLog = async (taskId) => {
    if (logData[taskId]) { setLogData(p => { const n = {...p}; delete n[taskId]; return n; }); return; }
    setLogData(p => ({ ...p, [taskId]: 'loading' }));
    try {
      const { data } = await request.get(`/api/test/history/${taskId}/log`);
      setLogData(p => ({ ...p, [taskId]: data.output || '(无日志)' }));
    } catch {
      setLogData(p => ({ ...p, [taskId]: '加载日志失败' }));
    }
  };

  return (
    <div className="flex-1 overflow-y-auto mx-8 mb-6">
      <HistoryStats stats={stats} />

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        {historyList.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-400">
            <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center mb-3">
              <Clock size={24} className="text-slate-300" />
            </div>
            <p className="text-sm text-slate-500">暂无历史</p>
            <p className="text-xs text-slate-400 mt-1">执行测试后记录将显示在这里</p>
            <button onClick={() => navigate('/')}
              className="mt-4 px-4 py-2 rounded-lg text-xs font-medium bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors">
              前往执行大厅
            </button>
          </div>
        ) : (
          <div className="p-5 space-y-2">
            {historyList.map(h => {
              const exp = expandedHistory[h.taskId];
              const isOpen = !!exp;
              const hasFailures = (h.failed || 0) > 0;

              return (
                <div
                  key={h.taskId}
                  className={`border rounded-xl overflow-hidden transition-colors ${
                    h.status === 'SUCCESS' ? 'border-l-4 border-l-emerald-500'
                    : h.status === 'FAILED' ? 'border-l-4 border-l-red-500'
                    : 'border-l-4 border-l-slate-300'
                  }`}
                >
                  <button
                    onClick={() => onToggleExpand(h.taskId)}
                    className="w-full flex items-center gap-3 px-4 py-3 bg-slate-50/50 hover:bg-slate-100 transition-colors text-left"
                  >
                    <ChevronRight
                      size={12}
                      className={`text-slate-400 transition-transform flex-shrink-0 ${isOpen ? 'rotate-90' : ''}`}
                    />
                    {h.status === 'SUCCESS' ? (
                      <CheckCircle2 size={15} className="text-emerald-500 flex-shrink-0" />
                    ) : h.status === 'FAILED' ? (
                      <XCircle size={15} className="text-red-500 flex-shrink-0" />
                    ) : (
                      <Clock size={15} className="text-slate-400 flex-shrink-0" />
                    )}
                    <span className="font-semibold text-sm text-slate-700">{h.label}</span>
                    <span className="text-xs text-slate-400">
                      {(h.createTime || '').substring(0, 19).replace('T', ' ')}
                    </span>
                    <div className="flex items-center gap-3 ml-auto">
                      <StatusBadge variant={h.status === 'SUCCESS' ? 'pass' : 'fail'}>
                        {h.status}
                      </StatusBadge>
                      <div className="w-28">
                        <ProgressBar passed={h.passed || 0} failed={h.failed || 0} skipped={h.skipped || 0} />
                      </div>
                      <span className="text-xs text-slate-400">{h.durationFmt}</span>
                      {hasFailures && (
                        <span
                          onClick={e => { e.stopPropagation(); onRerun(h.taskId); }}
                          className="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium bg-red-50 text-red-600 hover:bg-red-100 cursor-pointer"
                        >
                          <RefreshCw size={10} /> Rerun
                        </span>
                      )}
                      <span
                        onClick={e => { e.stopPropagation(); onDelete(h.taskId); }}
                        className="p-1 rounded-lg text-slate-400 hover:text-red-500 hover:bg-red-50 cursor-pointer"
                      >
                        <Trash2 size={12} />
                      </span>
                    </div>
                  </button>

                  {isOpen && (
                    <div className="border-t border-slate-100 p-4 bg-slate-50/30">
                      {exp === 'loading' ? (
                        <p className="text-xs text-slate-400">加载中...</p>
                      ) : (
                        <div className="space-y-1.5">
                          {exp.cases && exp.cases.length > 0 ? (
                            exp.cases.map((cls, i) => {
                              const clsHasFailures = (cls.failures || 0) + (cls.errors || 0) > 0;
                              return (
                                <div key={i}>
                                  {(() => {
                                    const clsKey = `${h.taskId}-${i}`;
                                    const clsOpen = isClassOpen(clsKey, clsHasFailures);
                                    const logKey = `${h.taskId}-${i}-log`;
                                    return (
                                      <>
                                        <button
                                          onClick={() => toggleClass(clsKey)}
                                          className="w-full flex items-center gap-2 px-3 py-1.5 bg-white rounded-lg cursor-pointer hover:bg-slate-50 text-xs font-mono text-slate-700"
                                        >
                                          <ChevronRight size={10} className={`text-slate-400 transition-transform duration-200 flex-shrink-0 ${clsOpen ? 'rotate-90' : ''}`} />
                                          {cls.className}
                                          {caseDetails && caseDetails[cls.className] && (
                                            <span className="text-[10px] text-slate-400 font-sans">
                                              ({[...new Set(caseDetails[cls.className].map(d => d.module))].join('、')} · {caseDetails[cls.className].length}条)
                                            </span>
                                          )}
                                          <span className="ml-auto flex items-center gap-2">
                                            {clsHasFailures && <span className="w-1.5 h-1.5 rounded-full bg-red-500" />}
                                            <span className="text-[10px]">{cls.cases?.length || 0} cases</span>
                                          </span>
                                        </button>

                                        <div className="grid transition-all duration-300 ease-in-out" style={{ gridTemplateRows: clsOpen ? '1fr' : '0fr' }}>
                                          <div className="overflow-hidden">
                                            {cls.log && (
                                              <div className="mx-2 my-1 bg-slate-900/90 rounded">
                                                <button
                                                  onClick={(e) => { e.stopPropagation(); toggleClassLog(logKey); }}
                                                  className="w-full flex items-center gap-1.5 px-3 py-1 text-[10px] text-emerald-400 font-mono cursor-pointer hover:text-emerald-300 text-left"
                                                >
                                                  <ChevronRight size={8} className={`transition-transform duration-200 flex-shrink-0 ${expandedClassLog[logKey] ? 'rotate-90' : ''}`} />
                                                  执行日志 ({cls.log.split('\n').filter(Boolean).length} lines)
                                                </button>
                                                <div className="grid transition-all duration-300 ease-in-out" style={{ gridTemplateRows: expandedClassLog[logKey] ? '1fr' : '0fr' }}>
                                                  <div className="overflow-hidden">
                                                    <pre className="px-3 pb-2 text-[10px] text-slate-300 font-mono leading-relaxed max-h-[200px] overflow-auto whitespace-pre-wrap break-all">{cls.log}</pre>
                                                  </div>
                                                </div>
                                              </div>
                                            )}
                                            <div className="mt-1 border border-slate-100 rounded-lg overflow-hidden bg-white">
                                              {(cls.cases || []).map(c => {
                                                const rawName = c.name.replace(/[()]/g, '');
                                                const detail = caseDetails
                                                  ? Object.values(caseDetails).flat().find(d =>
                                                      (d.javaMethod && rawName === d.javaMethod)
                                                      || (d.caseId && rawName.includes(d.caseId.replace(/[-_]/g, '')))
                                                      || (d.caseId && rawName.includes(d.caseId)))
                                                  : null;
                                                return (
                                                  <div
                                                    key={c.name}
                                                    className={`px-3 py-2 text-[11px] border-b border-slate-50 last:border-0 ${c.status === 'FAIL' ? 'bg-red-50/30' : ''}`}
                                                  >
                                                    <div className="flex items-center gap-2">
                                                      <StatusBadge variant={c.status === 'PASS' ? 'pass' : 'fail'}>
                                                        {c.status}
                                                      </StatusBadge>
                                                      <span className="font-mono flex-1 truncate text-slate-700">{c.name}</span>
                                                      <span className="text-slate-400 flex-shrink-0">{c.time}s</span>
                                                    </div>
                                                    {detail && (
                                                      <div className="mt-1 ml-0 text-[10px] text-slate-500">
                                                        {detail.title || detail.caseType}
                                                        {detail.steps ? ' — ' + detail.steps.substring(0, 80) : ''}
                                                      </div>
                                                    )}
                                                    {c.reason && (
                                                      <div className="mt-1.5 text-[10px] text-slate-600 bg-slate-100 rounded-md px-2.5 py-1.5 whitespace-pre-wrap break-all font-mono max-h-[120px] overflow-y-auto">
                                                        {c.reason.split('\n').slice(0, 4).join('\n')}
                                                      </div>
                                                    )}
                                                  </div>
                                                );
                                              })}
                                            </div>
                                          </div>
                                        </div>
                                      </>
                                    );
                                  })()}
                                </div>
                              );
                            })
                          ) : (
                            <p className="text-xs text-slate-400">无详细用例数据</p>
                          )}

                          <div className="mt-2 pt-2 border-t border-slate-200">
                            <button
                              onClick={() => fetchLog(h.taskId)}
                              className="flex items-center gap-1.5 text-[11px] text-slate-500 hover:text-blue-600 transition-colors"
                            >
                              <FileText size={12} />
                              {logData[h.taskId] ? '收起日志' : '查看执行日志'}
                            </button>
                            {logData[h.taskId] && logData[h.taskId] !== 'loading' && (
                              <pre className="mt-2 text-[10px] font-mono text-slate-400 bg-slate-900 rounded-lg p-3 max-h-[300px] overflow-auto whitespace-pre-wrap break-all leading-relaxed">
                                {logData[h.taskId]}
                              </pre>
                            )}
                            {logData[h.taskId] === 'loading' && (
                              <p className="mt-1 text-[11px] text-slate-400">加载中...</p>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}

            {/* Infinite scroll sentinel */}
            <div ref={sentinelRef} className="flex items-center justify-center py-4">
              {loadingMore ? (
                <span className="flex items-center gap-2 text-[11px] text-slate-400">
                  <Loader2 size={12} className="animate-spin" /> 加载更多...
                </span>
              ) : historyHasMore ? (
                <span className="text-[11px] text-slate-300">滚动加载更多</span>
              ) : (
                <span className="text-[11px] text-slate-300">— 已加载全部 —</span>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

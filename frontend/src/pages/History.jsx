import { useMemo } from 'react';
import { CheckCircle2, XCircle, Clock, RefreshCw, Trash2, ChevronRight, TrendingUp, Activity, BarChart3 } from 'lucide-react';
import StatusBadge from '../components/StatusBadge';
import ProgressBar from '../components/ProgressBar';

function HistoryStats({ historyList }) {
  const stats = useMemo(() => {
    const today = new Date().toISOString().substring(0, 10);
    const todayItems = historyList.filter(h => (h.createTime || '').substring(0, 10) === today);

    let totalPassed = 0, totalFailed = 0, totalSkipped = 0;
    for (const h of historyList) {
      totalPassed += h.passed || 0;
      totalFailed += h.failed || 0;
      totalSkipped += h.skipped || 0;
    }

    // 7-day pass rate (approximate from last 7 days of records)
    const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().substring(0, 10);
    const recentItems = historyList.filter(h => (h.createTime || '') >= sevenDaysAgo);
    let recentPassed = 0, recentFailed = 0;
    for (const h of recentItems) {
      recentPassed += h.passed || 0;
      recentFailed += h.failed || 0;
    }
    const recentTotal = recentPassed + recentFailed;
    const recentRate = recentTotal > 0 ? ((recentPassed / recentTotal) * 100).toFixed(1) : '—';

    const totalCases = totalPassed + totalFailed + totalSkipped;
    const overallRate = totalCases > 0 ? ((totalPassed / (totalPassed + totalFailed)) * 100).toFixed(1) : '—';

    return { todayCount: todayItems.length, totalCases, totalPassed, totalFailed, totalSkipped, recentRate, overallRate };
  }, [historyList]);

  if (historyList.length === 0) return null;

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

export default function History({ historyList, onRerun, onDelete, expandedHistory, onToggleExpand, caseDetails }) {
  return (
    <div className="flex-1 overflow-y-auto mx-8 mb-6">
      {/* Stats overview */}
      <HistoryStats historyList={historyList} />

      {/* History list */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        {historyList.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-400">
            <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center mb-3">
              <Clock size={24} className="text-slate-300" />
            </div>
            <p className="text-sm text-slate-500">暂无历史</p>
            <p className="text-xs text-slate-400 mt-1">执行测试后记录将显示在这里</p>
            <button onClick={() => window.__taasNavigate?.('dashboard')}
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
                                <details key={i} className="group" open={clsHasFailures}>
                                  <summary className="flex items-center gap-2 px-3 py-1.5 bg-white rounded-lg cursor-pointer hover:bg-slate-50 text-xs font-mono text-slate-700 list-none">
                                    <ChevronRight size={10} className="text-slate-400 group-open:rotate-90 transition-transform" />
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
                                  </summary>
                                  <div className="mt-1 border border-slate-100 rounded-lg overflow-hidden bg-white">
                                    {(cls.cases || []).map(c => {
                                      const detail = caseDetails
                                        ? Object.values(caseDetails).flat().find(d => d.caseId && c.name.includes(d.caseId))
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
                                </details>
                              );
                            })
                          ) : (
                            <p className="text-xs text-slate-400">无详细用例数据</p>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

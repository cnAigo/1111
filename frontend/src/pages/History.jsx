import { CheckCircle2, XCircle, Clock, RefreshCw, Trash2, ChevronRight, Terminal as TerminalIcon } from 'lucide-react';

export default function History({ historyList, onRerun, onDelete, expandedHistory, onToggleExpand, caseDetails }) {
  return (
    <div className="flex-1 overflow-y-auto mx-8 mb-6 bg-white rounded-xl border border-slate-200 shadow-sm">
      {historyList.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400">
          <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center mb-3"><Clock size={24} className="text-slate-300" /></div>
          <p className="text-sm text-slate-500">暂无历史</p><p className="text-xs text-slate-400 mt-1">执行测试后记录将显示在这里</p>
        </div>
      ) : (
        <div className="p-5 space-y-2">
          {historyList.map(h => {
            const exp = expandedHistory[h.taskId];
            const isOpen = !!exp;
            return (
              <div key={h.taskId} className={`border rounded-xl overflow-hidden ${h.status==='SUCCESS'?'border-l-4 border-l-emerald-500':h.status==='FAILED'?'border-l-4 border-l-red-500':'border-l-4 border-l-slate-300'}`}>
                <button onClick={() => onToggleExpand(h.taskId)} className="w-full flex items-center gap-3 px-4 py-3 bg-slate-50/50 hover:bg-slate-100 transition-colors text-left">
                  <ChevronRight size={12} className={`text-slate-400 transition-transform flex-shrink-0 ${isOpen?'rotate-90':''}`} />
                  {h.status==='SUCCESS'?<CheckCircle2 size={15} className="text-emerald-500 flex-shrink-0"/>:h.status==='FAILED'?<XCircle size={15} className="text-red-500 flex-shrink-0"/>:<Clock size={15} className="text-slate-400 flex-shrink-0"/>}
                  <span className="font-semibold text-sm text-slate-700">{h.label}</span>
                  <span className="text-xs text-slate-400">{(h.createTime||'').substring(0,19).replace('T',' ')}</span>
                  <div className="flex items-center gap-2 ml-auto">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${h.status==='SUCCESS'?'bg-emerald-100 text-emerald-700':'bg-red-100 text-red-700'}`}>{h.status}</span>
                    <span className="text-xs text-slate-500">{h.passed||0}P/{h.failed||0}F</span>
                    <span className="text-xs text-slate-400">{h.durationFmt}</span>
                    {h.failed>0 && <span onClick={e=>{e.stopPropagation();onRerun(h.taskId);}} className="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium bg-red-50 text-red-600 hover:bg-red-100 cursor-pointer"><RefreshCw size={10}/> Rerun</span>}
                    <span onClick={e=>{e.stopPropagation();onDelete(h.taskId);}} className="p-1 rounded-lg text-slate-400 hover:text-red-500 hover:bg-red-50 cursor-pointer"><Trash2 size={12}/></span>
                  </div>
                </button>
                {isOpen && (
                  <div className="border-t border-slate-100 p-4 bg-slate-50/30">
                    {exp === 'loading' ? <p className="text-xs text-slate-400">加载中...</p> : (
                      <div className="space-y-1.5">
                        {exp.cases && exp.cases.length > 0 ? exp.cases.map((cls, i) => (
                          <details key={i} className="group" open={(cls.failures||0)+(cls.errors||0)>0}>
                            <summary className="flex items-center gap-2 px-3 py-1.5 bg-white rounded-lg cursor-pointer hover:bg-slate-50 text-xs font-mono text-slate-700 list-none">
                              <ChevronRight size={10} className="text-slate-400 group-open:rotate-90 transition-transform" />
                              {cls.className}
                              {caseDetails && caseDetails[cls.className] && (
                                <span className="text-[10px] text-slate-400 font-sans">
                                  ({[...new Set(caseDetails[cls.className].map(d => d.module))].join('、')} · {caseDetails[cls.className].length}条)
                                </span>
                              )}
                              <span className="ml-auto text-[10px]">{cls.cases?.length||0} cases</span>
                            </summary>
                            <div className="mt-1 border border-slate-100 rounded-lg overflow-hidden bg-white">
                              {(cls.cases||[]).map(c => {
                                const detail = caseDetails ? Object.values(caseDetails).flat().find(d => d.caseId && c.name.includes(d.caseId)) : null;
                                return (
                                <div key={c.name} className={`px-3 py-1.5 text-[11px] border-b border-slate-50 last:border-0 ${c.status==='FAIL'?'bg-red-50/50':''}`}>
                                  <div className="flex items-center gap-2">
                                    {c.status==='PASS'?<CheckCircle2 size={11} className="text-emerald-500 flex-shrink-0"/>:<XCircle size={11} className="text-red-500 flex-shrink-0"/>}
                                    <span className="font-mono flex-1 truncate">{c.name}</span>
                                    <span className="text-slate-400 flex-shrink-0">{c.time}s</span>
                                  </div>
                                  {detail && <div className="mt-0.5 ml-5 text-[10px] text-slate-500">{detail.title || detail.caseType}{detail.steps ? ' — ' + detail.steps.substring(0,80) : ''}</div>}
                                  {c.reason && <div className="mt-1 ml-5 text-[10px] text-red-600 bg-red-50 rounded px-2 py-1 whitespace-pre-wrap break-all">{c.reason.split('\n').slice(0,4).join('\n')}</div>}
                                </div>
                              );})}
                            </div>
                          </details>
                        )) : <p className="text-xs text-slate-400">无详细用例数据</p>}
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
  );
}

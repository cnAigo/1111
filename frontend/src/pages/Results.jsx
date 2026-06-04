import { useState, useMemo } from 'react';
import { CheckCircle2, XCircle, AlertTriangle, ChevronRight, Clock, Search, X } from 'lucide-react';

function Empty({ icon: Icon, title, sub }) {
  return <div className="flex flex-col items-center justify-center py-20 text-slate-400">
    <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center mb-3"><Icon size={24} className="text-slate-300" /></div>
    <p className="text-sm text-slate-500">{title}</p><p className="text-xs text-slate-400 mt-1">{sub}</p>
  </div>;
}

export default function Results({ testResults, caseDetails }) {
  const [filter, setFilter] = useState('all');
  const [search, setSearch] = useState('');
  const [expanded, setExpanded] = useState({});

  const stats = useMemo(() => {
    let total = 0, passed = 0, failed = 0, skipped = 0, timeMs = 0;
    for (const c of testResults) {
      total += c.tests || 0;
      const f = (c.failures || 0) + (c.errors || 0);
      const s = c.skipped || 0;
      failed += f;
      skipped += s;
      passed += (c.tests || 0) - f - s;
      timeMs += parseFloat(c.time || '0') * 1000;
    }
    const fmt = (ms) => { const s = Math.floor(ms/1000); const m = Math.floor(s/60); return m > 0 ? `${m}m${s%60}s` : `${s}s`; };
    return { total, passed, failed, skipped, totalTime: fmt(timeMs) };
  }, [testResults]);

  const filtered = useMemo(() => {
    let list = testResults;
    if (filter !== 'all') {
      list = testResults.filter(cls => {
        const p = (cls.tests||0) - (cls.failures||0) - (cls.errors||0) - (cls.skipped||0);
        const f = (cls.failures||0) + (cls.errors||0);
        if (filter === 'passed') return p > 0;
        if (filter === 'failed') return f > 0;
        if (filter === 'skipped') return (cls.skipped||0) > 0;
        return true;
      });
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(cls => cls.className.toLowerCase().includes(q) || (cls.cases||[]).some(c => c.name.toLowerCase().includes(q)));
    }
    return list;
  }, [testResults, filter, search]);

  const toggleExpand = (name) => setExpanded(p => ({ ...p, [name]: !p[name] }));

  return (
    <div className="flex-1 overflow-y-auto mx-8 mb-6 bg-white rounded-xl border border-slate-200 shadow-sm">
      {testResults.length === 0 ? <Empty icon={CheckCircle2} title="暂无结果" sub="执行测试后结果将显示在这里" /> : (
        <div className="p-5">
          {/* Summary */}
          <div className="flex items-center gap-6 mb-5 p-4 bg-slate-50 rounded-xl border">
            <div>
              <span className="text-3xl font-bold text-slate-800">{stats.total}</span>
              <span className="text-sm text-slate-500 ml-2">total</span>
            </div>
            <div className="flex items-center gap-4 text-sm">
              <span className="flex items-center gap-1"><CheckCircle2 size={13} className="text-emerald-500"/><span className="font-semibold text-emerald-700">{stats.passed}</span> passed</span>
              <span className="flex items-center gap-1"><XCircle size={13} className="text-red-500"/><span className="font-semibold text-red-700">{stats.failed}</span> failed</span>
              {stats.skipped > 0 && <span className="flex items-center gap-1"><AlertTriangle size={13} className="text-amber-500"/><span className="font-semibold text-amber-700">{stats.skipped}</span> skipped</span>}
            </div>
            <div className="flex-1 max-w-xs"><div className="flex h-2 rounded-full overflow-hidden">{stats.passed>0&&<div className="bg-emerald-500" style={{width:`${(stats.passed/stats.total)*100}%`}}/>}{stats.failed>0&&<div className="bg-red-500" style={{width:`${(stats.failed/stats.total)*100}%`}}/>}</div></div>
            <span className="text-xs text-slate-400 flex items-center gap-1"><Clock size={11}/>{stats.totalTime}</span>
          </div>

          {/* Filters + Search */}
          <div className="flex items-center gap-2 mb-4">
            {[{k:'all',l:'All',c:stats.total},{k:'passed',l:'Passed',c:stats.passed},{k:'failed',l:'Failed',c:stats.failed},{k:'skipped',l:'Skipped',c:stats.skipped}].map(f=>(
              <button key={f.k} onClick={()=>setFilter(f.k)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${filter===f.k?'bg-blue-50 text-blue-700 border border-blue-200':'bg-white text-slate-500 border border-slate-200 hover:bg-slate-50'}`}>
                {f.l} <span className="opacity-60">{f.c}</span>
              </button>
            ))}
            <div className="relative ml-auto">
              <Search size={12} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
              <input value={search} onChange={e => setSearch(e.target.value)} placeholder="搜索类名或用例..."
                className="pl-7 pr-6 py-1.5 bg-white border border-slate-200 rounded-lg text-xs w-[200px] outline-none focus:ring-1 focus:ring-blue-500/30" />
              {search && <button onClick={()=>setSearch('')} className="absolute right-1.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"><X size={11}/></button>}
            </div>
          </div>

          {/* Class results */}
          {filtered.map(cls => {
            const passed = (cls.tests||0) - (cls.failures||0) - (cls.errors||0) - (cls.skipped||0);
            const failed = (cls.failures||0) + (cls.errors||0);
            const skipped = cls.skipped || 0;
            const actuallyOpen = expanded[cls.className] !== undefined ? expanded[cls.className] : failed > 0;
            const totalMs = parseFloat(cls.time || '0') * 1000;
            const caseCount = (cls.cases || []).length;

            return (
              <div key={cls.className} className="mb-2 border border-slate-100 rounded-lg overflow-hidden">
                <button onClick={() => toggleExpand(cls.className)}
                  className="w-full flex items-center gap-3 px-4 py-2.5 bg-slate-50 hover:bg-slate-100 transition-colors text-left">
                  <ChevronRight size={12} className={`text-slate-400 transition-transform flex-shrink-0 ${actuallyOpen?'rotate-90':''}`} />
                  <span className="font-mono text-sm font-semibold text-slate-700">{cls.className}</span>
                  <span className="text-[10px] text-slate-400">{caseCount} cases</span>
                  <div className="flex items-center gap-2 ml-auto text-xs">
                    <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 font-medium">{passed} pass</span>
                    {failed > 0 && <span className="px-2 py-0.5 rounded-full bg-red-100 text-red-700 font-medium">{failed} fail</span>}
                    {skipped > 0 && <span className="px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 font-medium">{skipped} skip</span>}
                    <span className="text-slate-400 flex items-center gap-0.5"><Clock size={10}/>{cls.time}s</span>
                  </div>
                </button>
                {actuallyOpen && (
                  <div className="border-t border-slate-100">
                    {/* Show DB case details if available */}
                    {caseDetails && caseDetails[cls.className] && caseDetails[cls.className].length > 0 && (
                      <div className="px-4 py-2.5 bg-blue-50/30 border-b border-blue-100">
                        <p className="text-[11px] text-slate-500">
                          <span className="font-semibold text-slate-600">模块：</span>
                          {[...new Set(caseDetails[cls.className].map(d => d.module))].join('、')}
                          &nbsp;|&nbsp;
                          <span className="font-semibold text-slate-600">用例数：</span>
                          {caseDetails[cls.className].length} 条
                        </p>
                      </div>
                    )}
                    {(cls.cases || []).map(c => {
                      const detail = caseDetails ? Object.values(caseDetails).flat().find(d => d.caseId && c.name.includes(d.caseId)) : null;
                      return (
                      <div key={c.name} className={`px-4 py-2.5 text-[13px] border-b border-slate-50 last:border-0 ${c.status==='FAIL'?'bg-red-50/30':c.status==='SKIP'?'bg-amber-50/30':''}`}>
                        <div className="flex items-center gap-3">
                          {c.status==='PASS' ? <CheckCircle2 size={14} className="text-emerald-500 flex-shrink-0"/>
                            : c.status==='SKIP' ? <AlertTriangle size={14} className="text-amber-500 flex-shrink-0"/>
                            : <XCircle size={14} className="text-red-500 flex-shrink-0"/>}
                          <span className="font-mono text-xs flex-1">{c.name}</span>
                          <span className="text-slate-400 text-xs ml-auto flex items-center gap-0.5"><Clock size={10}/>{c.time || '0'}s</span>
                        </div>
                        {detail && (
                          <div className="mt-1 ml-7 text-[10px] text-slate-500">
                            <span className="font-medium text-slate-600">{detail.title || detail.caseType}</span>
                            {detail.steps && <span className="ml-2 text-slate-400">步骤: {detail.steps.substring(0,100)}{detail.steps.length>100?'…':''}</span>}
                          </div>
                        )}
                        {c.reason && (
                          <div className="mt-1 ml-7 text-[11px] text-red-600 bg-red-50 px-2 py-1 rounded max-w-full break-all">{c.reason.split('\n')[0]}</div>
                        )}
                      </div>
                    );})}
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

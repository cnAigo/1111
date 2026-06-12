import { useState, useMemo, useCallback } from 'react';
import { Virtuoso } from 'react-virtuoso';
import { CheckCircle2, XCircle, AlertTriangle, ChevronRight, Clock, Search, X, ChevronsUpDown } from 'lucide-react';
import StatusBadge from '../components/StatusBadge';
import { useTestStore } from '../store/useTestStore';
import { parseReason } from '../utils/parseReason';

function Empty({ icon: Icon, title, sub }) {
  return <div className="flex flex-col items-center justify-center py-20 text-slate-400">
    <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center mb-3"><Icon size={24} className="text-slate-300" /></div>
    <p className="text-sm text-slate-500">{title}</p><p className="text-xs text-slate-400 mt-1">{sub}</p>
  </div>;
}

// ── Individual class result card (rendered inside Virtuoso) ──
function ClassCard({ cls, expanded, onToggle, caseDetails, filter, getVisibleCases }) {
  const passed = (cls.tests || 0) - (cls.failures || 0) - (cls.errors || 0) - (cls.skipped || 0);
  const failed = (cls.failures || 0) + (cls.errors || 0);
  const skipped = cls.skipped || 0;
  const hasWarnings = cls.warnings && cls.warnings.length > 0;
  const actuallyOpen = expanded[cls.className] !== undefined ? expanded[cls.className] : (failed > 0 || hasWarnings);
  const allCases = cls.cases || [];
  const visibleCases = getVisibleCases(allCases, filter);
  const [caseExpanded, setCaseExpanded] = useState({});

  const toggleCase = (name) => setCaseExpanded(p => ({ ...p, [name]: !p[name] }));

  return (
    <div className="mb-2 border border-slate-100 rounded-lg overflow-hidden">
      <button onClick={() => onToggle(cls.className)}
        className="w-full flex items-center gap-3 px-4 py-2.5 bg-slate-50 hover:bg-slate-100 transition-colors text-left">
        <ChevronRight size={12} className={`text-slate-400 transition-transform flex-shrink-0 ${actuallyOpen ? 'rotate-90' : ''}`} />
        <span className="font-mono text-sm font-semibold text-slate-700">{cls.className}</span>
        <span className="text-[10px] text-slate-400">{allCases.length} cases</span>
        <div className="flex items-center gap-2 ml-auto text-xs">
          {hasWarnings && <span className="flex items-center gap-0.5 text-amber-500" title={cls.warnings.length + ' warning(s)'}><AlertTriangle size={10} />{cls.warnings.length}</span>}
          <StatusBadge variant="pass">{passed} pass</StatusBadge>
          {failed > 0 && <StatusBadge variant="fail">{failed} fail</StatusBadge>}
          {skipped > 0 && <StatusBadge variant="skip">{skipped} skip</StatusBadge>}
          <span className="text-slate-400 flex items-center gap-0.5"><Clock size={10} />{cls.time}s</span>
        </div>
      </button>
      {actuallyOpen && (
        <div className="border-t border-slate-100">
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
          {hasWarnings && (
            <div className="px-4 py-2.5 bg-amber-50/60 border-b border-amber-100">
              <div className="flex items-start gap-1.5">
                <AlertTriangle size={13} className="text-amber-500 flex-shrink-0 mt-0.5" />
                <div className="flex-1 min-w-0">
                  <p className="text-[11px] font-semibold text-amber-800 mb-1">测试中检测到警告信息：</p>
                  {cls.warnings.map((w, wi) => (
                    <div key={wi} className="text-[10px] text-amber-700 leading-relaxed font-mono break-all mb-0.5 last:mb-0">
                      {w}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
          {cls.log && (
            <details open className="px-4 py-2 bg-slate-900/90 border-b border-slate-700">
              <summary className="text-[10px] text-emerald-400 font-mono cursor-pointer hover:text-emerald-300 select-none">
                执行日志 ({cls.log.split('\n').filter(Boolean).length} lines)
              </summary>
              <pre className="mt-1.5 text-[10px] text-slate-300 font-mono leading-relaxed max-h-[260px] overflow-auto whitespace-pre-wrap break-all">{cls.log}</pre>
            </details>
          )}
          {visibleCases.map(c => {
            const clsDetails = caseDetails && caseDetails[cls.className] ? caseDetails[cls.className] : [];
            const rawName = c.name.replace(/[()]/g, '');
            const detail = clsDetails.find(d => d.javaMethod && rawName === d.javaMethod)
                        || clsDetails.find(d => d.caseId && rawName.includes(d.caseId.replace(/[-_]/g, '')))
                        || clsDetails.find(d => d.caseId && rawName.includes(d.caseId));
            const parsed = c.reason ? parseReason(c.reason) : null;
            const isCaseOpen = caseExpanded[c.name] !== undefined ? caseExpanded[c.name] : (c.status === 'FAIL');
            return (
              <div key={c.name} className={`border-b border-slate-50 last:border-0 ${c.status === 'FAIL' ? 'bg-red-50/20' : c.status === 'SKIP' ? 'bg-amber-50/20' : 'bg-emerald-50/10'}`}>
                <button onClick={() => toggleCase(c.name)}
                  className="w-full px-4 py-2 text-[13px] text-left flex items-center gap-3 cursor-pointer hover:bg-slate-50/50">
                  <ChevronRight size={10} className={`text-slate-400 transition-transform flex-shrink-0 ${isCaseOpen ? 'rotate-90' : ''}`} />
                  <StatusBadge variant={c.status === 'PASS' ? 'pass' : c.status === 'SKIP' ? 'skip' : 'fail'}>
                    {c.status}
                  </StatusBadge>
                  <span className="font-mono text-xs flex-1 text-slate-700">{c.name}</span>
                  <span className="text-slate-400 text-xs ml-auto flex items-center gap-0.5"><Clock size={10} />{c.time || '0'}s</span>
                </button>
                {isCaseOpen && detail && (
                  <div className="px-4 pb-2 ml-5 space-y-0.5 pl-2 border-l-2 border-emerald-300">
                    <div className="flex items-center gap-2 flex-wrap">
                      {detail.caseType && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded font-medium bg-slate-100 text-slate-600">{detail.caseType}</span>
                      )}
                      {detail.httpMethod && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded font-mono font-medium bg-indigo-50 text-indigo-600">{detail.httpMethod}</span>
                      )}
                      {detail.apiUrl && (
                        <span className="text-[9px] text-slate-400 font-mono truncate max-w-[300px]">{detail.apiUrl}</span>
                      )}
                    </div>
                    {detail.title && <p className="text-[10px] text-slate-600 font-medium">标题: {detail.title}</p>}
                    {detail.steps && <p className="text-[10px] text-slate-500">步骤: {detail.steps}</p>}
                    {detail.expected && <p className="text-[10px] text-slate-500">预期: {detail.expected}</p>}
                  </div>
                )}
                {isCaseOpen && parsed && (
                  <div className="px-4 pb-2 ml-5 space-y-1.5">
                    {parsed.assertions.length > 0 && (
                      <div className="flex items-center gap-2 flex-wrap">
                        {parsed.assertions.map((a, j) => (
                          <span key={j} className="inline-flex items-center gap-1">
                            <StatusBadge variant="expected">Exp: {a.expected}</StatusBadge>
                            <span className="text-[10px] text-slate-400">|</span>
                            <StatusBadge variant="actual">Act: {a.actual}</StatusBadge>
                          </span>
                        ))}
                      </div>
                    )}
                    {parsed.summary && (
                      <p className="text-[11px] text-slate-600 leading-relaxed">{parsed.summary.split('\n')[0]}</p>
                    )}
                    {parsed.stackTrace && (
                      <details>
                        <summary className="text-[10px] text-slate-400 cursor-pointer hover:text-slate-600">查看堆栈追踪</summary>
                        <pre className="mt-1 text-[10px] font-mono text-slate-500 bg-slate-100 rounded-md p-2 max-h-[120px] overflow-auto whitespace-pre-wrap">
                          {parsed.stackTrace}
                        </pre>
                      </details>
                    )}
                  </div>
                )}
                {isCaseOpen && c.reason && !parsed && (
                  <div className="px-4 pb-2 ml-5 text-[11px] text-slate-600 bg-slate-100 mx-4 rounded-md max-w-full break-all leading-relaxed">
                    {c.reason.split('\n')[0]}
                  </div>
                )}
                {isCaseOpen && !detail && !parsed && !c.reason && (
                  <div className="px-4 pb-2 ml-5 text-[10px] text-slate-500">测试通过</div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function Results() {
  const testResults = useTestStore(s => s.testResults);
  const caseDetails = useTestStore(s => s.caseDetails);
  const [filter, setFilter] = useState('all');
  const [search, setSearch] = useState('');
  const [expanded, setExpanded] = useState({});
  const [expandAll, setExpandAll] = useState(false);

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
    const fmt = (ms) => { const s = Math.floor(ms / 1000); const m = Math.floor(s / 60); return m > 0 ? `${m}m${s % 60}s` : `${s}s`; };
    return { total, passed, failed, skipped, totalTime: fmt(timeMs) };
  }, [testResults]);

  const filtered = useMemo(() => {
    let list = testResults;
    if (filter !== 'all') {
      list = list.filter(cls => {
        const p = (cls.tests || 0) - (cls.failures || 0) - (cls.errors || 0) - (cls.skipped || 0);
        const f = (cls.failures || 0) + (cls.errors || 0);
        const s = cls.skipped || 0;
        if (filter === 'passed') return p > 0;
        if (filter === 'failed') return f > 0;
        if (filter === 'skipped') return s > 0;
        return true;
      });
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(cls => cls.className.toLowerCase().includes(q) || (cls.cases || []).some(c => c.name.toLowerCase().includes(q)));
    }
    return list;
  }, [testResults, search, filter]);

  const getVisibleCases = useCallback((cases, f) => {
    if (!cases) return [];
    if (f === 'all') return cases;
    if (f === 'skipped') return cases;
    return cases.filter(c => {
      if (f === 'passed') return c.status === 'PASS';
      if (f === 'failed') return c.status === 'FAIL';
      return true;
    });
  }, []);

  const toggleExpand = useCallback((name) => setExpanded(p => ({ ...p, [name]: !p[name] })), []);

  const handleExpandAll = useCallback(() => {
    if (expandAll) {
      setExpanded({});
      setExpandAll(false);
    } else {
      const all = {};
      for (const cls of filtered) all[cls.className] = true;
      setExpanded(all);
      setExpandAll(true);
    }
  }, [expandAll, filtered]);

  return (
    <div className="flex-1 flex flex-col min-h-0 mx-8 mb-6 bg-white rounded-xl border border-slate-200 shadow-sm">
      {testResults.length === 0 ? <Empty icon={CheckCircle2} title="暂无结果" sub="执行测试后结果将显示在这里" /> : (
        <>
          {/* Fixed header: Summary + Filters */}
          <div className="flex-shrink-0 rounded-t-xl px-5 pt-5 pb-3 border-b border-slate-100">
            {/* Summary */}
            <div className="flex items-center gap-6 mb-4 p-4 bg-slate-50 rounded-xl border">
              <div>
                <span className="text-3xl font-bold text-slate-800">{stats.total}</span>
                <span className="text-sm text-slate-500 ml-2">total</span>
              </div>
              <div className="flex items-center gap-4 text-sm">
                <span className="flex items-center gap-1"><CheckCircle2 size={13} className="text-emerald-500" /><span className="font-semibold text-emerald-700">{stats.passed}</span> passed</span>
                <span className="flex items-center gap-1"><XCircle size={13} className="text-red-500" /><span className="font-semibold text-red-700">{stats.failed}</span> failed</span>
                {stats.skipped > 0 && <span className="flex items-center gap-1"><AlertTriangle size={13} className="text-amber-500" /><span className="font-semibold text-amber-700">{stats.skipped}</span> skipped</span>}
              </div>
              <div className="flex-1 max-w-xs">
                <div className="flex h-2 rounded-full overflow-hidden">
                  {stats.passed > 0 && <div className="bg-emerald-500 transition-all" style={{ width: `${(stats.passed / stats.total) * 100}%` }} />}
                  {stats.failed > 0 && <div className="bg-red-500 transition-all" style={{ width: `${(stats.failed / stats.total) * 100}%` }} />}
                  {stats.skipped > 0 && <div className="bg-slate-300 transition-all" style={{ width: `${(stats.skipped / stats.total) * 100}%` }} />}
                </div>
              </div>
              <span className="text-xs text-slate-400 flex items-center gap-1"><Clock size={11} />{stats.totalTime}</span>
            </div>

            {/* Filters + Search + Expand/Collapse */}
            <div className="flex items-center gap-2">
              {[{ k: 'all', l: 'All', c: stats.total }, { k: 'passed', l: 'Passed', c: stats.passed }, { k: 'failed', l: 'Failed', c: stats.failed }, { k: 'skipped', l: 'Skipped', c: stats.skipped }].map(f => (
                <button key={f.k} onClick={() => setFilter(f.k)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${filter === f.k ? 'bg-blue-50 text-blue-700 border border-blue-200' : 'bg-white text-slate-500 border border-slate-200 hover:bg-slate-50'}`}>
                  {f.l} <span className="opacity-60">{f.c}</span>
                </button>
              ))}
              <button onClick={handleExpandAll}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                title={expandAll ? '折叠全部' : '展开全部'}>
                <ChevronsUpDown size={14} />
              </button>
              <div className="relative ml-auto">
                <Search size={12} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
                <input value={search} onChange={e => setSearch(e.target.value)} placeholder="搜索类名或用例..."
                  className="pl-7 pr-6 py-1.5 bg-white border border-slate-200 rounded-lg text-xs w-[200px] outline-none focus:ring-1 focus:ring-blue-500/30" />
                {search && <button onClick={() => setSearch('')} className="absolute right-1.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"><X size={11} /></button>}
              </div>
            </div>
          </div>

          {/* Virtual list for class results */}
          <Virtuoso
            style={{ flex: 1 }}
            data={filtered}
            computeItemKey={(i, cls) => cls.className}
            itemContent={(i, cls) => (
              <div className="px-5 pt-0.5">
                <ClassCard
                  cls={cls}
                  expanded={expanded}
                  onToggle={toggleExpand}
                  caseDetails={caseDetails}
                  filter={filter}
                  getVisibleCases={getVisibleCases}
                />
              </div>
            )}
            components={{ Footer: () => <div className="h-5" /> }}
          />
        </>
      )}
    </div>
  );
}

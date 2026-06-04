import { useRef, useEffect, useCallback } from 'react';
import { Terminal as TerminalIcon, Clock, Download } from 'lucide-react';

export default function Terminal({ lines, isRunning, elapsedFmt, onClear, logFilter, onFilterChange, runningLabel, pct, progress, progressTotal }) {
  const exportLog = () => {
    const text = lines.map(l => l.text).join('\n');
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'taas-log-' + new Date().toISOString().slice(0,19).replace(/:/g,'-') + '.log';
    a.click(); URL.revokeObjectURL(url);
  };
  const ref = useRef(null);
  const userScrolledRef = useRef(false);

  const filtered = logFilter === 'ALL' ? lines : lines.filter(l => (l.text || '').toUpperCase().includes(logFilter));

  // Smart scroll: only auto-scroll if user is at the bottom
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const handleScroll = () => { userScrolledRef.current = el.scrollTop + el.clientHeight < el.scrollHeight - 30; };
    el.addEventListener('scroll', handleScroll, { passive: true });
    return () => el.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    if (ref.current && !userScrolledRef.current) ref.current.scrollTop = ref.current.scrollHeight;
  }, [lines]);

  return (
    <div className="flex-1 flex flex-col min-h-0 mx-8 rounded-xl overflow-hidden border border-slate-200 shadow-sm">
      <div className="flex items-center gap-1 px-4 py-1.5 bg-slate-800 border-b border-slate-700">
        <div className="flex items-center gap-1.5 mr-3">
          <div className="w-2.5 h-2.5 rounded-full bg-red-500" />
          <div className="w-2.5 h-2.5 rounded-full bg-amber-500" />
          <div className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
        </div>
        <span className="text-[11px] text-slate-400 font-mono mr-4">Terminal</span>
        {['ALL','INFO','WARN','ERROR'].map(f => (
          <button key={f} onClick={() => onFilterChange(f)} className={`px-2.5 py-0.5 text-[10px] font-medium rounded transition-colors ${logFilter===f?'bg-slate-600 text-slate-200':'text-slate-400 hover:text-slate-300'}`}>{f}</button>
        ))}
        <button onClick={exportLog} className="ml-auto text-[10px] text-slate-400 hover:text-slate-300 px-2" title="导出日志"><Download size={11} /></button>
        <button onClick={onClear} className="text-[10px] text-slate-400 hover:text-slate-300 px-2">Clear</button>
        {isRunning && <span className="text-[10px] text-slate-400 font-mono flex items-center gap-1"><Clock size={11} />{elapsedFmt}</span>}
        <div className="flex items-center gap-1.5 ml-2"><div className={`w-1.5 h-1.5 rounded-full ${isRunning?'bg-emerald-500 animate-pulse':'bg-slate-600'}`} /><span className="text-[10px] text-slate-500 font-mono">{lines.length}</span></div>
      </div>
      <div ref={ref} className="flex-1 bg-slate-900 p-5 font-mono text-[13px] leading-relaxed overflow-y-auto">
        {filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-slate-600 select-none">
            <TerminalIcon size={40} className="mb-3 opacity-15" />
            <p className="text-sm text-slate-500">等待指令...</p>
            <p className="text-xs mt-1 text-slate-600">选择模块后点击"开始测试"启动</p>
          </div>
        ) : filtered.map(line => (<div key={line.id} className={line.cls || 'text-slate-300'}>{line.text || ' '}</div>))}
        {isRunning && <span className="inline-block w-2 h-4 bg-emerald-400 ml-0.5 animate-pulse align-middle" />}
      </div>
      {/* Status bar when running */}
      {isRunning && runningLabel && (
        <div className="px-4 py-1.5 bg-slate-800 border-t border-slate-700 flex items-center gap-3 text-[11px]">
          <span className="text-slate-400">▶ {runningLabel}</span>
          <span className="text-emerald-400 font-mono">{progress}/{progressTotal || '?'} passed</span>
        </div>
      )}
      {/* Progress bar */}
      {isRunning && (
        <div className="h-[3px] bg-slate-800 flex-shrink-0">
          <div className="h-full animate-flow" style={{ width: `${Math.max(pct || (progressTotal > 0 ? 1 : 8), 2)}%` }} />
        </div>
      )}
    </div>
  );
}

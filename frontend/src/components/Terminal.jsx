import { useRef, useEffect, useState, useMemo } from 'react';
import { Terminal as TerminalIcon, Clock, Download, Search, X, ChevronDown, ChevronUp } from 'lucide-react';

export default function Terminal({ lines, isRunning, elapsedFmt, onClear, logFilter, onFilterChange, runningLabel, pct, progress, progressTotal }) {
  const ref = useRef(null);
  const userScrolledRef = useRef(false);
  const [autoScroll, setAutoScroll] = useState(true);
  const [keyword, setKeyword] = useState('');

  const exportLog = () => {
    const text = lines.map(l => l.text).join('\n');
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'taas-log-' + new Date().toISOString().slice(0, 19).replace(/:/g, '-') + '.log';
    a.click(); URL.revokeObjectURL(url);
  };

  const filtered = useMemo(() => {
    let result = logFilter === 'ALL' ? lines : lines.filter(l => (l.text || '').toUpperCase().includes(logFilter));
    if (keyword.trim()) {
      const kw = keyword.toLowerCase();
      result = result.filter(l => (l.text || '').toLowerCase().includes(kw));
    }
    return result;
  }, [lines, logFilter, keyword]);

  const highlightText = (text, kw) => {
    if (!kw.trim()) return text;
    const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const parts = text.split(new RegExp(`(${escaped})`, 'gi'));
    return parts.map((part, i) =>
      part.toLowerCase() === kw.toLowerCase()
        ? <mark key={i} className="bg-yellow-400/50 text-yellow-100 font-semibold rounded-sm px-0.5">{part}</mark>
        : part
    );
  };

  // Smart scroll: only auto-scroll if user is at the bottom AND auto-scroll is enabled
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const handleScroll = () => { userScrolledRef.current = el.scrollTop + el.clientHeight < el.scrollHeight - 30; };
    el.addEventListener('scroll', handleScroll, { passive: true });
    return () => el.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    if (ref.current && !userScrolledRef.current && autoScroll) {
      ref.current.scrollTop = ref.current.scrollHeight;
    }
  }, [lines, autoScroll]);

  return (
    <div className="flex-1 flex flex-col min-h-0 rounded-xl overflow-hidden border border-slate-200 shadow-sm">
      {/* Title bar */}
      <div className="flex items-center gap-1 px-4 py-1.5 bg-slate-800 border-b border-slate-700">
        <div className="flex items-center gap-1.5 mr-3">
          <div className="w-2.5 h-2.5 rounded-full bg-red-500" />
          <div className="w-2.5 h-2.5 rounded-full bg-amber-500" />
          <div className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
        </div>
        <span className="text-[11px] text-slate-400 font-mono mr-4">Console</span>

        {/* Log level filter */}
        {['ALL', 'INFO', 'WARN', 'ERROR'].map(f => (
          <button
            key={f}
            onClick={() => onFilterChange(f)}
            className={`px-2.5 py-0.5 text-[10px] font-medium rounded transition-colors ${logFilter === f ? 'bg-slate-600 text-slate-200' : 'text-slate-400 hover:text-slate-300'}`}
          >
            {f}
          </button>
        ))}

        {/* Auto-scroll toggle */}
        <div className="flex items-center gap-1.5 ml-2">
          <button
            onClick={() => setAutoScroll(!autoScroll)}
            className={`relative w-7 h-4 rounded-full transition-colors ${autoScroll ? 'bg-emerald-500' : 'bg-slate-600'}`}
            title={autoScroll ? '自动滚动: 开' : '自动滚动: 关'}
          >
            <span className={`absolute top-0.5 w-3 h-3 rounded-full bg-white transition-transform ${autoScroll ? 'left-3.5' : 'left-0.5'}`} />
          </button>
          <span className="text-[9px] text-slate-500 font-mono whitespace-nowrap">Auto</span>
        </div>

        <button onClick={exportLog} className="ml-auto px-1.5 py-0.5 text-[10px] text-slate-400 hover:text-slate-200 hover:bg-slate-700 rounded" title="导出日志">
          <Download size={11} />
        </button>
        <button onClick={onClear} className="px-2 py-0.5 text-[10px] text-slate-400 hover:text-slate-200 hover:bg-slate-700 rounded">Clear</button>
        {isRunning && <span className="text-[10px] text-slate-400 font-mono flex items-center gap-1"><Clock size={11} />{elapsedFmt}</span>}
        <div className="flex items-center gap-1.5 ml-2">
          <div className={`w-1.5 h-1.5 rounded-full ${isRunning ? 'bg-emerald-500 animate-pulse' : 'bg-slate-600'}`} />
          <span className="text-[10px] text-slate-500 font-mono">{lines.length}</span>
        </div>
      </div>

      {/* Search bar */}
      <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-850 border-b border-slate-700/50">
        <Search size={11} className="text-slate-500 flex-shrink-0" />
        <input
          type="text"
          value={keyword}
          onChange={e => { setKeyword(e.target.value); }}
          onKeyDown={e => {
            if (e.key === 'Enter' && keyword.trim() && ref.current) {
              const marks = ref.current.querySelectorAll('mark');
              if (marks.length > 0) {
                marks[0].scrollIntoView({ block: 'center', behavior: 'smooth' });
                // Also flash first match briefly for visual cue
                marks[0].classList.add('bg-yellow-300', 'text-yellow-900');
                setTimeout(() => {
                  marks[0].classList.remove('bg-yellow-300', 'text-yellow-900');
                }, 600);
              }
            }
          }}
          placeholder="搜索日志关键词 (Enter 跳转)…"
          className="flex-1 bg-transparent text-[11px] text-slate-300 outline-none placeholder-slate-600 font-mono"
        />
        {keyword && (
          <>
            <button
              onClick={() => {
                if (!ref.current) return;
                const marks = [...ref.current.querySelectorAll('mark')];
                if (marks.length === 0) return;
                const visible = marks.find(m => {
                  const rect = m.getBoundingClientRect();
                  const parent = ref.current.getBoundingClientRect();
                  return rect.top >= parent.top && rect.bottom <= parent.bottom;
                });
                const idx = visible ? marks.indexOf(visible) : marks.length - 1;
                const prev = marks[Math.max(0, idx - 1)];
                prev.scrollIntoView({ block: 'center', behavior: 'smooth' });
              }}
              className="text-slate-500 hover:text-slate-300 p-0.5"
              title="上一个匹配"
            >
              <ChevronUp size={11} />
            </button>
            <button
              onClick={() => {
                if (!ref.current) return;
                const marks = [...ref.current.querySelectorAll('mark')];
                if (marks.length === 0) return;
                const visible = marks.find(m => {
                  const rect = m.getBoundingClientRect();
                  const parent = ref.current.getBoundingClientRect();
                  return rect.top >= parent.top && rect.bottom <= parent.bottom;
                });
                const idx = visible ? marks.indexOf(visible) : -1;
                const next = marks[Math.min(marks.length - 1, idx + 1)];
                next.scrollIntoView({ block: 'center', behavior: 'smooth' });
              }}
              className="text-slate-500 hover:text-slate-300 p-0.5"
              title="下一个匹配"
            >
              <ChevronDown size={11} />
            </button>
            <button onClick={() => setKeyword('')} className="text-slate-500 hover:text-slate-300">
              <X size={11} />
            </button>
            <span className="text-[10px] text-slate-500 font-mono whitespace-nowrap">
              {filtered.length}/{lines.length}
            </span>
          </>
        )}
      </div>

      {/* Log output */}
      <div ref={ref} className="flex-1 bg-slate-900 p-5 font-mono text-[13px] leading-relaxed overflow-y-auto">
        {filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-slate-600 select-none">
            <TerminalIcon size={40} className="mb-3 opacity-15" />
            <p className="text-sm text-slate-500">Console ready</p>
            <p className="text-xs mt-1 text-slate-600">Select test classes and click "Start" to run</p>
            <p className="text-[10px] mt-3 text-slate-700">Output will appear here in IntelliJ IDEA format</p>
          </div>
        ) : (
          <div className="space-y-[1px]">
            {filtered.map(line => {
              const content = keyword.trim()
                ? highlightText(line.text || ' ', keyword)
                : (line.text || ' ');
              return (
                <div key={line.id} className={line.cls || 'text-slate-300'}>
                  {content}
                </div>
              );
            })}
          </div>
        )}
        {isRunning && <span className="inline-block w-2 h-4 bg-emerald-400 ml-0.5 animate-pulse align-middle" />}
      </div>

      {/* Status bar when running */}
      {isRunning && runningLabel && (
        <div className="px-4 py-1.5 bg-slate-800 border-t border-slate-700 flex items-center gap-3 text-[11px]">
          <span className="text-slate-400">▶ {runningLabel}</span>
          <span className="text-emerald-400 font-mono">{progress}/{progressTotal || '?'}</span>
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

import { useRef, useEffect, useState, useCallback } from 'react';
import { Clock, Download, Search, X, ChevronDown, ChevronUp } from 'lucide-react';
import 'xterm/css/xterm.css';
import { useTestStore } from '../store/useTestStore';

export default function Terminal({ onClear }) {
  const isRunning = useTestStore(s => s.isRunning);
  const elapsedSec = useTestStore(s => s.elapsedSec);
  const logFilter = useTestStore(s => s.logFilter);
  const setLogFilter = useTestStore(s => s.setLogFilter);
  const runningLabel = useTestStore(s => s.runningLabel);
  const fmtElapsed = useTestStore(s => s.fmtElapsed);
  const storeStatus = useTestStore(s => s.status);
  const elapsedFmt = fmtElapsed(elapsedSec);
  const progress = useTestStore(s => s.progress);
  const progressTotal = useTestStore(s => s.progressTotal);

  // Actual test progress from WebSocket, fallback to indeterminate
  const pct = (() => {
    if (storeStatus === 'SUCCESS' || storeStatus === 'FAILED' || storeStatus === 'STOPPED') return 100;
    if (!isRunning) return 0;
    if (progressTotal > 0) return Math.min(Math.floor((progress / progressTotal) * 100), 99);
    return -1; // indeterminate
  })();

  const containerRef = useRef(null);
  const xtermRef = useRef(null);
  const fitAddonRef = useRef(null);
  const searchAddonRef = useRef(null);
  const [autoScroll, setAutoScroll] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [lineCount, setLineCount] = useState(0);
  const [ready, setReady] = useState(false);

  // ── Initialize xterm ──
  useEffect(() => {
    let disposed = false;

    (async () => {
      try {
        const [{ Terminal: T }, { FitAddon }, { SearchAddon }] = await Promise.all([
          import('xterm'),
          import('xterm-addon-fit'),
          import('xterm-addon-search'),
        ]);

        if (disposed || !containerRef.current) return;

        const term = new T({
          cursorBlink: false,
          disableStdin: true,
          fontSize: 13,
          fontFamily: "'Cascadia Code', 'Fira Code', 'JetBrains Mono', 'Consolas', monospace",
          theme: {
            background: '#0f172a',
            foreground: '#cbd5e1',
            cursor: '#475569',
            black:   '#1e293b', red: '#ef4444', green: '#22c55e', yellow: '#eab308',
            blue:    '#3b82f6', magenta: '#a855f7', cyan: '#06b6d4', white: '#f1f5f9',
            brightBlack:  '#475569', brightRed: '#f87171', brightGreen: '#4ade80',
            brightYellow: '#facc15', brightBlue: '#60a5fa', brightMagenta: '#c084fc',
            brightCyan:   '#22d3ee', brightWhite: '#f8fafc',
          },
          allowProposedApi: true,
          scrollback: 5000,
        });

        const fitAddon = new FitAddon();
        const searchAddon = new SearchAddon();
        term.loadAddon(fitAddon);
        term.loadAddon(searchAddon);

        term.open(containerRef.current);

        // Fit after a tick so the container has its final size
        requestAnimationFrame(() => {
          try { fitAddon.fit(); } catch {}
        });

        xtermRef.current = term;
        fitAddonRef.current = fitAddon;
        searchAddonRef.current = searchAddon;

        useTestStore.getState().setTerminalWriter((text) => {
          if (!xtermRef.current) return;
          xtermRef.current.write(text);
          setLineCount(xtermRef.current.buffer.active.length);
        });

        term.onWrite(() => setLineCount(term.buffer.active.length));

        // Sync autoScroll toggle when user manually scrolls
        term.onScroll((pos) => {
          const atBottom = pos + term.rows >= term.buffer.active.length;
          setAutoScroll(atBottom);
        });

        term.writeln('\x1b[90mConsole ready — select test classes and click Start\x1b[0m');
        setReady(true);
      } catch (err) {
        console.error('xterm init failed', err);
      }
    })();

    return () => {
      disposed = true;
      if (xtermRef.current) {
        try { xtermRef.current.dispose(); } catch {}
        xtermRef.current = null;
      }
    };
  }, []);

  // ── Fit on resize ──
  useEffect(() => {
    if (!containerRef.current || !fitAddonRef.current) return;
    const el = containerRef.current;
    const ro = new ResizeObserver(() => {
      requestAnimationFrame(() => {
        try { fitAddonRef.current.fit(); } catch {}
      });
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, [ready]);

  // Scroll to bottom when toggle is re-enabled
  const handleAutoScrollToggle = useCallback(() => {
    const next = !autoScroll;
    setAutoScroll(next);
    if (next && xtermRef.current) xtermRef.current.scrollToBottom();
  }, [autoScroll]);

  // ── Clear ──
  const handleClear = useCallback(() => {
    if (xtermRef.current) {
      xtermRef.current.clear();
      xtermRef.current.writeln('\x1b[90mConsole cleared\x1b[0m');
      setLineCount(1);
    }
    onClear();
  }, [onClear]);

  // ── Export ──
  const exportLog = useCallback(() => {
    if (!xtermRef.current) return;
    const buffer = xtermRef.current.buffer.active;
    const rows = [];
    for (let i = 0; i < buffer.length; i++) {
      const line = buffer.getLine(i);
      if (line) rows.push(line.translateToString());
    }
    const blob = new Blob([rows.join('\n')], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'taas-log-' + new Date().toISOString().slice(0, 19).replace(/:/g, '-') + '.log';
    a.click();
    URL.revokeObjectURL(url);
  }, []);

  // ── Search ──
  const handleSearchKeyDown = useCallback((e) => {
    if (e.key === 'Enter' && keyword.trim() && searchAddonRef.current) {
      e.preventDefault();
      if (e.shiftKey) searchAddonRef.current.findPrevious(keyword);
      else searchAddonRef.current.findNext(keyword);
    }
  }, [keyword]);

  const goPrev = () => { if (keyword.trim() && searchAddonRef.current) searchAddonRef.current.findPrevious(keyword); };
  const goNext = () => { if (keyword.trim() && searchAddonRef.current) searchAddonRef.current.findNext(keyword); };

  return (
    <div className="flex-1 flex flex-col min-h-0 rounded-xl overflow-hidden border border-slate-200 shadow-sm">
      {/* Title bar */}
      <div className="flex items-center gap-1 px-4 py-1.5 bg-slate-800 border-b border-slate-700 flex-shrink-0">
        <div className="flex items-center gap-1.5 mr-3">
          <div className="w-2.5 h-2.5 rounded-full bg-red-500" />
          <div className="w-2.5 h-2.5 rounded-full bg-amber-500" />
          <div className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
        </div>
        <span className="text-[11px] text-slate-400 font-mono mr-4">Console</span>

        {['ALL', 'INFO', 'WARN', 'ERROR'].map(f => (
          <button
            key={f} onClick={() => setLogFilter(f)}
            className={`px-2.5 py-0.5 text-[10px] font-medium rounded transition-colors ${logFilter === f ? 'bg-slate-600 text-slate-200' : 'text-slate-400 hover:text-slate-300'}`}
          >{f}</button>
        ))}

        <div className="flex items-center gap-1.5 ml-2">
          <button onClick={handleAutoScrollToggle}
            className={`relative w-7 h-4 rounded-full transition-colors ${autoScroll ? 'bg-emerald-500' : 'bg-slate-600'}`}
            title={autoScroll ? '自动滚动: 开' : '自动滚动: 关'}>
            <span className={`absolute top-0.5 w-3 h-3 rounded-full bg-white transition-transform ${autoScroll ? 'left-3.5' : 'left-0.5'}`} />
          </button>
          <span className="text-[9px] text-slate-500 font-mono whitespace-nowrap">Auto</span>
        </div>

        <button onClick={exportLog} className="ml-auto px-1.5 py-0.5 text-[10px] text-slate-400 hover:text-slate-200 hover:bg-slate-700 rounded" title="导出日志"><Download size={11} /></button>
        <button onClick={handleClear} className="px-2 py-0.5 text-[10px] text-slate-400 hover:text-slate-200 hover:bg-slate-700 rounded">Clear</button>
        {isRunning && <span className="text-[10px] text-slate-400 font-mono flex items-center gap-1"><Clock size={11} />{elapsedFmt}</span>}
        <div className="flex items-center gap-1.5 ml-2">
          <div className={`w-1.5 h-1.5 rounded-full ${isRunning ? 'bg-emerald-500 animate-pulse' : 'bg-slate-600'}`} />
          <span className="text-[10px] text-slate-500 font-mono">{lineCount}</span>
        </div>
      </div>

      {/* Search bar */}
      <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-900 border-b border-slate-700/50 flex-shrink-0">
        <Search size={11} className="text-slate-500 flex-shrink-0" />
        <input type="text" value={keyword} onChange={e => setKeyword(e.target.value)}
          onKeyDown={handleSearchKeyDown}
          placeholder="搜索日志 · Enter 下一个 · Shift+Enter 上一个"
          className="flex-1 bg-transparent text-[11px] text-slate-300 outline-none placeholder-slate-600 font-mono"
        />
        {keyword && (
          <>
            <button onClick={goPrev} className="text-slate-500 hover:text-slate-300 p-0.5" title="上一个 (Shift+Enter)"><ChevronUp size={11} /></button>
            <button onClick={goNext} className="text-slate-500 hover:text-slate-300 p-0.5" title="下一个 (Enter)"><ChevronDown size={11} /></button>
            <button onClick={() => setKeyword('')} className="text-slate-500 hover:text-slate-300"><X size={11} /></button>
          </>
        )}
      </div>

      {/* xterm container — needs explicit position & overflow handling */}
      <div ref={containerRef} className="flex-1 min-h-0" style={{ background: '#0f172a' }} />

      {/* Status bar */}
      {isRunning && runningLabel && (
        <div className="px-4 py-1.5 bg-slate-800 border-t border-slate-700 flex items-center gap-3 text-[11px] flex-shrink-0">
          <span className="text-slate-400">▶ {runningLabel}</span>
          <span className="text-blue-400 font-mono">
            {progressTotal > 0 ? `${progress}/${progressTotal} ${pct}%` : elapsedFmt}
          </span>
        </div>
      )}

      {/* Progress bar */}
      {isRunning && (
        <div className="h-[3px] bg-slate-800 flex-shrink-0">
          {pct >= 0 ? (
            <div className="h-full bg-blue-500 transition-all duration-500 ease-linear" style={{ width: `${pct}%` }} />
          ) : (
            <div className="h-full w-1/3 bg-blue-500 animate-pulse opacity-50" />
          )}
        </div>
      )}
    </div>
  );
}

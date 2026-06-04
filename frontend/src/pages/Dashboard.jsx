import Terminal from '../components/Terminal';
import { BarChart3, ChevronDown, CheckCircle2, XCircle, ExternalLink } from 'lucide-react';

export default function Dashboard({ terminalLines, isRunning, elapsedSec, showReport, status, logFilter, onFilterChange, onClear, pct, progress, progressTotal, fmtElapsed, runningLabel, onCloseReport }) {
  return (
    <div className="flex-1 flex flex-col min-h-0 p-4 pt-2">
      <Terminal lines={terminalLines} isRunning={isRunning} elapsedFmt={fmtElapsed(elapsedSec)}
        logFilter={logFilter} onFilterChange={onFilterChange} onClear={onClear}
        runningLabel={runningLabel} pct={pct} progress={progress} progressTotal={progressTotal} />
      {showReport && (
        <div className="mx-8 mb-6 bg-white rounded-xl border border-slate-200 overflow-hidden animate-slide-up shadow-sm">
          <div className="flex items-center justify-between px-4 py-2 bg-slate-50 border-b">
            <div className="flex items-center gap-2">
              <BarChart3 size={13} className="text-amber-500" />
              <span className="text-[11px] font-semibold text-slate-600">Allure Report</span>
              <span className={`text-[10px] px-1.5 py-0.5 rounded font-bold ${status==='SUCCESS'?'bg-emerald-100 text-emerald-700':'bg-red-100 text-red-700'}`}>{status}</span>
            </div>
            <div className="flex items-center gap-2">
              <button onClick={onCloseReport} className="p-1 rounded text-slate-400 hover:text-slate-600 hover:bg-slate-200" title="收起"><ChevronDown size={14} className="rotate-180 transition-transform" /></button>
              <a href="/allure-report/index.html" target="_blank" rel="noopener noreferrer" className="text-[10px] px-2.5 py-1 rounded-lg bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 flex items-center gap-1"><ExternalLink size={10} /> 独立窗口</a>
            </div>
          </div>
          <iframe src="/allure-report/index.html" className="w-full border-0 flex-1 min-h-[60vh]" title="Allure Report" />
        </div>
      )}
    </div>
  );
}

import { useState, useEffect } from 'react';
import { Play, Square, Trash2, Loader2, Terminal, BarChart3, Layers, CheckSquare, ChevronLeft, ChevronRight, Check, X } from 'lucide-react';
import TerminalPanel from '../components/Terminal';
import ReportPanel from '../components/ReportPanel';
import TestTree from '../components/TestTree';

export default function Dashboard({
  terminalLines, isRunning, elapsedSec, showReport, status, logFilter,
  onFilterChange, onClear, pct, progress, progressTotal, fmtElapsed, runningLabel,
  onCloseReport, testResults, historyList,
  modules, selected, onToggle,
  onStart, onStop, onCleanup, cleaning, selectedCount, caseDetails,
}) {
  const [activeTab, setActiveTab] = useState('terminal');
  const [treeOpen, setTreeOpen] = useState(true);

  const hasReport = showReport && testResults && testResults.length > 0;
  const selectedNames = [...selected];

  // Auto-switch to report tab when test completes
  useEffect(() => {
    if (hasReport) setActiveTab('report');
  }, [hasReport]);

  // Count total warnings across all results
  const totalWarnings = hasReport
    ? testResults.reduce((sum, r) => sum + (r.warnings ? r.warnings.length : 0), 0)
    : 0;

  // Quick filter helpers
  const allClassNames = modules.flatMap(m => m.classes.map(c => c.name));
  const apiClassNames = modules.flatMap(m => m.classes.filter(c => c.type === 'api').map(c => c.name));
  const uiClassNames = modules.flatMap(m => m.classes.filter(c => c.type === 'ui').map(c => c.name));
  const selectAll = (names, add) => names.forEach(n => onToggle(n, add));

  return (
    <div className="flex-1 flex min-h-0 p-4 gap-4">
      {/* ── Left: Test selection panel (1/3) — collapsible ── */}
      <div className={`relative flex-shrink-0 bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col overflow-hidden transition-all duration-300 ${
        treeOpen ? 'w-[calc(33.333%-0.667rem)]' : 'w-0 border-transparent shadow-none'
      }`}>
        {treeOpen && (
          <>
            {/* Collapse button */}
            <button onClick={() => setTreeOpen(false)}
              className="absolute top-2 right-2 z-10 p-1 rounded-md text-slate-300 hover:text-slate-500 hover:bg-slate-100 transition-colors"
              title="收起模块面板">
              <ChevronLeft size={14} />
            </button>

            {/* Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 bg-slate-50/50">
              <div className="flex items-center gap-2">
                <Layers size={15} className="text-slate-500" />
                <span className="text-[13px] font-semibold text-slate-700">测试用例选择</span>
              </div>
              <div className="flex items-center gap-3 text-[11px] text-slate-400">
                <span className="flex items-center gap-1">
                  <CheckSquare size={12} className="text-blue-400" />
                  {selectedCount} 选中
                </span>
              </div>
            </div>

            {/* Quick filter buttons */}
            <div className="px-3 py-2 flex items-center gap-1.5 border-b border-slate-50 bg-white">
              <button onClick={() => selectAll(allClassNames, true)}
                className="flex-1 text-[10px] font-medium px-2 py-1 rounded bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-blue-600 transition-colors">
                <Check size={10} className="inline mr-0.5" />全选
              </button>
              <button onClick={() => selectAll(apiClassNames, true)}
                className="flex-1 text-[10px] font-medium px-2 py-1 rounded bg-amber-50 text-amber-700 hover:bg-amber-100 transition-colors">
                仅API
              </button>
              <button onClick={() => selectAll(uiClassNames, true)}
                className="flex-1 text-[10px] font-medium px-2 py-1 rounded bg-purple-50 text-purple-700 hover:bg-purple-100 transition-colors">
                仅UI
              </button>
              <button onClick={() => selectAll(allClassNames, false)}
                className="flex-1 text-[10px] font-medium px-2 py-1 rounded bg-slate-100 text-slate-500 hover:bg-red-50 hover:text-red-500 transition-colors">
                <X size={10} className="inline mr-0.5" />清空
              </button>
            </div>

            {/* Test tree — scrollable */}
            <div className="flex-1 min-h-0 overflow-y-auto">
              <TestTree modules={modules} selected={selected} onToggle={onToggle} />
            </div>

            {/* Selection summary + actions */}
            <div className="border-t border-slate-100 bg-white">
              {selectedCount > 0 && (
                <div className="px-4 py-2 flex items-center gap-2 text-[11px] flex-wrap">
                  <span className="text-slate-400 flex-shrink-0">已选：</span>
                  {selectedNames.slice(0, 2).map(name => (
                    <span key={name} className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 font-mono text-[10px] max-w-[140px]">
                      <span className="truncate">{name}</span>
                      <button onClick={() => onToggle(name, false)} className="hover:text-red-500 flex-shrink-0">&times;</button>
                    </span>
                  ))}
                  {selectedNames.length > 2 && (
                    <span className="text-slate-400 flex-shrink-0">+{selectedNames.length - 2} more</span>
                  )}
                </div>
              )}

              {/* Action buttons */}
              <div className="px-4 py-3 flex items-center gap-2">
                <button onClick={onStart} disabled={cleaning || selectedCount === 0}
                  className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-semibold transition-all active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed ${
                    isRunning
                      ? 'bg-red-500 hover:bg-red-600 text-white'
                      : 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white shadow-sm shadow-blue-500/20'
                  }`}>
                  {cleaning ? <><Loader2 size={14} className="animate-spin" /> 清理中…</> :
                   isRunning ? <><Square size={14} /> 停止执行</> : <><Play size={14} className="fill-white" /> 开始测试 ({selectedCount})</>}
                </button>
                <button onClick={onCleanup} disabled={cleaning}
                  className={`flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-lg text-xs font-medium transition-colors ${
                    cleaning ? 'text-orange-400 bg-orange-50' : 'text-slate-400 hover:text-red-500 hover:bg-red-50 border border-slate-200 hover:border-red-200'
                  }`}>
                  <Trash2 size={12} /> 清理
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* ── Expand handle when collapsed ── */}
      {!treeOpen && (
        <button onClick={() => setTreeOpen(true)}
          className="flex-shrink-0 w-10 h-10 bg-white rounded-xl border border-slate-200 shadow-sm flex items-center justify-center hover:bg-blue-50 hover:border-blue-200 text-slate-400 hover:text-blue-500 transition-colors"
          title="展开模块面板">
          <ChevronRight size={20} />
        </button>
      )}

      {/* ── Right: Terminal / Report (2/3) ── */}
      <div className="flex-1 flex flex-col min-h-0">
        {/* Tab bar */}
        <div className="flex items-center gap-1 mb-3">
          <button
            onClick={() => setActiveTab('terminal')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-[13px] font-medium transition-all ${
              activeTab === 'terminal'
                ? 'bg-white text-slate-800 shadow-sm border border-slate-200'
                : 'text-slate-500 hover:text-slate-700 hover:bg-white/60'
            }`}>
            <Terminal size={14} />
            实时执行日志
            {isRunning && <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />}
          </button>
          <button
            onClick={() => setActiveTab('report')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-[13px] font-medium transition-all ${
              activeTab === 'report'
                ? 'bg-white text-slate-800 shadow-sm border border-slate-200'
                : 'text-slate-500 hover:text-slate-700 hover:bg-white/60'
            }`}>
            <BarChart3 size={14} />
            测试报告
            {hasReport && (
              <span className={`min-w-[18px] h-[18px] flex items-center justify-center rounded-full text-[10px] font-bold ${
                status === 'SUCCESS'
                  ? totalWarnings > 0 ? 'bg-amber-100 text-amber-700' : 'bg-emerald-100 text-emerald-700'
                  : 'bg-red-100 text-red-700'
              }`}>
                {status === 'SUCCESS' ? (totalWarnings > 0 ? '⚠' : '✓') : '!'}
              </span>
            )}
          </button>
        </div>

        {/* Tab content */}
        <div className="flex-1 flex flex-col min-h-0">
          {activeTab === 'terminal' && (
            <TerminalPanel
              lines={terminalLines} isRunning={isRunning} elapsedFmt={fmtElapsed(elapsedSec)}
              logFilter={logFilter} onFilterChange={onFilterChange} onClear={onClear}
              runningLabel={runningLabel} pct={pct} progress={progress} progressTotal={progressTotal}
            />
          )}
          {activeTab === 'report' && hasReport && (
            <ReportPanel results={testResults} status={status} onClose={onCloseReport} historyList={historyList} caseDetails={caseDetails} />
          )}
          {activeTab === 'report' && !hasReport && (
            <div className="flex-1 flex flex-col items-center justify-center bg-white rounded-xl border border-slate-200 shadow-sm text-slate-400">
              <BarChart3 size={48} className="mb-3 opacity-20" />
              <p className="text-sm">暂无测试报告</p>
              <p className="text-xs mt-1 text-slate-300">执行测试后将在此处展示统计结果</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

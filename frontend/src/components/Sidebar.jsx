import { useLocation, useNavigate } from 'react-router-dom';
import { Terminal, CheckCircle2, History, Shield, Settings, Zap, ChevronLeft, ChevronRight, BarChart3 } from 'lucide-react';
import { MENU_ITEMS, STATUS_CFG } from '../data/modules';
import { useTestStore } from '../store/useTestStore';

export default function Sidebar({ cleaning, open, onToggle }) {
  const location = useLocation();
  const navigate = useNavigate();
  const status = useTestStore(s => s.status);
  const failedCount = useTestStore(s => s.failedCases.length);
  const timeDisplay = useTestStore(s => s.durationFmt);
  const isRunning = useTestStore(s => s.isRunning);
  const elapsedSec = useTestStore(s => s.elapsedSec);
  const fmtElapsed = useTestStore(s => s.fmtElapsed);

  const icons = { dashboard: Terminal, results: CheckCircle2, history: History, failed: Shield, coverage: BarChart3, settings: Settings };
  const sc = STATUS_CFG[status] || STATUS_CFG.IDLE;

  return (
    <>
      {/* Toggle button — positioned relative to sidebar width */}
      <button onClick={onToggle}
        className="absolute top-3 z-20 p-1 rounded-lg bg-white border border-slate-200 text-slate-400 hover:text-slate-600 shadow-sm transition-all"
        style={{ left: open ? 212 : 52 }}>
        {open ? <ChevronLeft size={14} /> : <ChevronRight size={14} />}
      </button>

      <aside className={`flex-shrink-0 bg-white border-r border-slate-200 flex flex-col transition-all duration-200 overflow-hidden ${open ? 'w-60' : 'w-[60px]'}`}>
        {/* Logo */}
        <div className={`flex items-center gap-2.5 ${open ? 'px-5 py-4' : 'px-0 py-4 justify-center'}`}>
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-600 to-indigo-600 flex items-center justify-center shadow-sm flex-shrink-0">
            <Zap size={16} className="text-white" />
          </div>
          {open && (
            <div className="whitespace-nowrap">
              <h1 className="text-slate-800 font-bold text-sm">TaaS</h1>
              <p className="text-slate-400 text-[10px]">Test Console</p>
            </div>
          )}
        </div>

        {/* Menu */}
        <nav className={`flex-1 py-2 space-y-0.5 ${open ? 'px-3' : 'px-1.5'}`}>
          {MENU_ITEMS.map(item => {
            const Icon = icons[item.key];
            const isActive = location.pathname === item.path || (item.path === '/' && location.pathname === '/');
            return (
              <button key={item.key} onClick={() => navigate(item.path)}
                title={!open ? item.label : undefined}
                className={`w-full flex items-center gap-3 rounded-xl text-[13px] font-medium transition-all duration-200 group whitespace-nowrap ${
                  open ? 'px-3.5 py-2.5' : 'px-0 py-2.5 justify-center'
                } ${
                  isActive
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'
                }`}>
                <Icon size={18} className={`flex-shrink-0 ${isActive ? 'text-blue-600' : 'text-slate-400 group-hover:text-slate-500'}`} />
                {open && <span>{item.label}</span>}
                {open && item.key === 'failed' && failedCount > 0 && (
                  <span className="ml-auto min-w-[20px] h-[20px] flex items-center justify-center rounded-full bg-red-100 text-red-600 text-[10px] font-bold">
                    {failedCount}
                  </span>
                )}
                {!open && item.key === 'failed' && failedCount > 0 && (
                  <span className="absolute top-0 right-0 min-w-[16px] h-[16px] flex items-center justify-center rounded-full bg-red-500 text-white text-[9px] font-bold -mt-1 -mr-1">
                    {failedCount}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        {/* Status */}
        <div className={`border-t border-slate-100 flex items-center gap-2 text-[11px] whitespace-nowrap ${open ? 'px-4 py-2.5' : 'px-0 py-2.5 justify-center'}`}>
          <div className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${cleaning ? 'bg-orange-500 animate-pulse' : status === 'RUNNING' ? 'bg-amber-500 animate-pulse' : status === 'SUCCESS' ? 'bg-emerald-500' : status === 'FAILED' ? 'bg-red-500' : 'bg-slate-300'}`} />
          {open && <span className="text-slate-500">{cleaning ? '清理中' : sc.text}</span>}
          {open && <span className="ml-auto font-mono text-slate-400 text-[10px]">{isRunning ? fmtElapsed(elapsedSec) : timeDisplay}</span>}
        </div>
      </aside>
    </>
  );
}

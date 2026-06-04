import { Terminal, CheckCircle2, History, Shield, Settings, Zap } from 'lucide-react';
import { MENU_ITEMS, STATUS_CFG } from '../data/modules';

export default function Sidebar({ activeMenu, onNavigate, status, failedCount, timeDisplay, isRunning, elapsedFmt, cleaning }) {
  const icons = { dashboard: Terminal, results: CheckCircle2, history: History, failed: Shield, settings: Settings };
  const sc = STATUS_CFG[status] || STATUS_CFG.IDLE;

  return (
    <aside className="w-60 flex-shrink-0 bg-white border-r border-slate-200 flex flex-col">
      {/* Logo */}
      <div className="px-5 py-4 flex items-center gap-2.5">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-600 to-indigo-600 flex items-center justify-center shadow-sm">
          <Zap size={16} className="text-white" />
        </div>
        <div>
          <h1 className="text-slate-800 font-bold text-sm">TaaS</h1>
          <p className="text-slate-400 text-[10px]">Test Console</p>
        </div>
      </div>

      {/* Menu */}
      <nav className="flex-1 px-3 py-2 space-y-0.5">
        {MENU_ITEMS.map(item => {
          const Icon = icons[item.key];
          const isActive = activeMenu === item.key;
          return (
            <button key={item.key} onClick={() => onNavigate(item.key)}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-[13px] font-medium transition-all duration-200 group ${
                isActive
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'
              }`}>
              <Icon size={18} className={isActive ? 'text-blue-600' : 'text-slate-400 group-hover:text-slate-500'} />
              <span>{item.label}</span>
              {item.key === 'failed' && failedCount > 0 && (
                <span className="ml-auto min-w-[20px] h-[20px] flex items-center justify-center rounded-full bg-red-100 text-red-600 text-[10px] font-bold">
                  {failedCount}
                </span>
              )}
            </button>
          );
        })}
      </nav>

      {/* Status */}
      <div className="px-4 py-2.5 border-t border-slate-100 flex items-center gap-2 text-[11px]">
        <div className={`w-1.5 h-1.5 rounded-full ${cleaning ? 'bg-orange-500 animate-pulse' : status === 'RUNNING' ? 'bg-amber-500 animate-pulse' : status === 'SUCCESS' ? 'bg-emerald-500' : status === 'FAILED' ? 'bg-red-500' : 'bg-slate-300'}`} />
        <span className="text-slate-500">{cleaning ? '正在清理环境' : sc.text}</span>
        <span className="ml-auto font-mono text-slate-400 text-[10px]">{isRunning ? elapsedFmt : timeDisplay}</span>
      </div>
    </aside>
  );
}

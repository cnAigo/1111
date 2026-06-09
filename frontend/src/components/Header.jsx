import { Settings } from 'lucide-react';

export default function Header({ activeMenu, isRunning, runningLabel, onOpenSettings }) {
  const isDash = activeMenu === 'dashboard';
  const titles = {
    dashboard: '执行大厅',
    results: '测试结果',
    history: '历史记录',
    failed: '失败用例',
    settings: '平台设置',
  };
  return (
    <header className="flex items-center justify-between px-6 py-3.5 bg-white/80 backdrop-blur border-b border-slate-200/60 flex-shrink-0">
      <div>
        <h1 className="text-[15px] font-bold text-slate-800 tracking-tight">
          {titles[activeMenu] || '执行大厅'}
        </h1>
        {isDash && isRunning && (
          <p className="text-[11px] text-slate-400 mt-0.5 flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            执行中: {runningLabel}
          </p>
        )}
      </div>
      <button onClick={onOpenSettings}
        className="p-2 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors">
        <Settings size={16} />
      </button>
    </header>
  );
}

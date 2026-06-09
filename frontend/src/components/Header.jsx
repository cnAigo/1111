import { useLocation } from 'react-router-dom';
import { Settings } from 'lucide-react';
import { useTestStore } from '../store/useTestStore';

const TITLES = {
  '/': '执行大厅',
  '/results': '测试结果',
  '/history': '历史记录',
  '/failed': '失败用例',
  '/settings': '平台设置',
};

export default function Header({ onOpenSettings }) {
  const location = useLocation();
  const isRunning = useTestStore(s => s.isRunning);
  const runningLabel = useTestStore(s => s.runningLabel);
  const isDash = location.pathname === '/';
  return (
    <header className="flex items-center justify-between px-6 py-3.5 bg-white/80 backdrop-blur border-b border-slate-200/60 flex-shrink-0">
      <div>
        <h1 className="text-[15px] font-bold text-slate-800 tracking-tight">
          {TITLES[location.pathname] || '执行大厅'}
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

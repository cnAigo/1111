import { Settings } from 'lucide-react';

export default function SettingsPage({ onOpen }) {
  return (
    <div className="flex-1 mx-8 mb-6 bg-white rounded-xl border border-slate-200 shadow-sm flex items-center justify-center">
      <div className="text-center py-20">
        <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center mx-auto mb-3"><Settings size={24} className="text-slate-300" /></div>
        <p className="text-sm text-slate-500">平台设置</p>
        <p className="text-xs text-slate-400 mt-1 mb-4">管理目标服务器连接与认证信息</p>
        <button onClick={onOpen} className="px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700">打开设置</button>
      </div>
    </div>
  );
}

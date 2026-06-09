import { Settings, X, Eye, EyeOff, Save, Trash2 } from 'lucide-react';

export default function ConfigModal({ open, onClose, cfgUrl, setCfgUrl, cfgProjectId, setCfgProjectId, cfgUsername, setCfgUsername,
  cfgPassword, setCfgPassword, pwVisible, setPwVisible, savedConfigs, configFormName, setConfigFormName, onSave, onDelete }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" onClick={onClose}>
      <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 max-h-[85vh] overflow-y-auto animate-slide-up" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <div className="flex items-center gap-2.5"><Settings size={16} className="text-blue-600" /><h2 className="text-base font-bold text-slate-800">平台设置</h2></div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"><X size={18} /></button>
        </div>
        <div className="px-6 py-4 space-y-4">
          {savedConfigs.length > 0 && (
            <div className="space-y-1.5">
              <label className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide">已保存的配置</label>
              {savedConfigs.map(sc => (
                <div key={sc.id} onClick={() => { setCfgUrl(sc.url||''); setCfgProjectId(sc.projectId||''); setCfgUsername(sc.username||''); setCfgPassword(sc.password || ''); }}
                  className="flex items-center gap-2 px-3 py-2 rounded-lg border border-slate-200 cursor-pointer hover:border-blue-300 hover:bg-blue-50/50 text-sm">
                  <span className="font-medium text-slate-700">{sc.configName}</span><span className="text-slate-400 text-xs">{sc.url}</span>
                  <button onClick={e => { e.stopPropagation(); onDelete(sc.id); }} className="ml-auto p-1 rounded text-slate-400 hover:text-red-500 hover:bg-red-50"><Trash2 size={12} /></button>
                </div>
              ))}
            </div>
          )}
          <div className="flex gap-2 items-end">
            <div className="flex-1"><label className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide block mb-1">名称</label><input value={configFormName} onChange={e => setConfigFormName(e.target.value)} placeholder="如：开发环境" className="w-full px-3 py-2 rounded-lg border border-slate-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 outline-none" /></div>
            <button onClick={onSave} className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700"><Save size={14} /> 保存</button>
          </div>
          <hr className="border-slate-100" />
          <div><label className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide block mb-1">URL *</label><input value={cfgUrl} onChange={e => setCfgUrl(e.target.value)} className="w-full px-3 py-2 rounded-lg border border-slate-200 text-sm font-mono focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 outline-none" /></div>
          <div><label className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide block mb-1">Project ID</label><input value={cfgProjectId} onChange={e => setCfgProjectId(e.target.value)} className="w-full px-3 py-2 rounded-lg border border-slate-200 text-sm font-mono focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 outline-none" /></div>
          <div className="grid grid-cols-2 gap-3">
            <div><label className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide block mb-1">Username</label><input value={cfgUsername} onChange={e => setCfgUsername(e.target.value)} className="w-full px-3 py-2 rounded-lg border border-slate-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 outline-none" /></div>
            <div><label className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide block mb-1">Password</label><div className="relative"><input type={pwVisible?'text':'password'} value={cfgPassword} onChange={e => setCfgPassword(e.target.value)} onFocus={() => { if (cfgPassword === '********') setCfgPassword(''); }} placeholder={cfgPassword ? '' : '输入密码'} className="w-full px-3 py-2 pr-10 rounded-lg border border-slate-200 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 outline-none placeholder:text-slate-400 placeholder:text-[11px]" /><button onClick={() => setPwVisible(!pwVisible)} className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-600">{pwVisible?<EyeOff size={14}/>:<Eye size={14}/>}</button></div></div>
          </div>
        </div>
        <div className="px-6 py-4 border-t flex justify-end"><button onClick={onClose} className="px-5 py-2 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700">完成</button></div>
      </div>
    </div>
  );
}

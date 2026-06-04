import { useState, useRef, useEffect } from 'react';
import { Play, Square, ChevronDown, Search, X, CheckCircle2, Settings, Trash2 } from 'lucide-react';
import { MODULES, MODULE_OPTIONS } from '../data/modules';

export default function Header({ activeMenu, isRunning, runningLabel, selectedModule, selectedClass, selectedLabel,
  onSelectModule, onSelectClass, onStart, onStop, onOpenSettings, cleaning, onCleanup }) {

  const doCleanup = async () => {
    if (cleaning) return;
    if (!confirm('确定要清理整个需求树吗？这将删除所有测试数据！')) return;
    onCleanup();
  };
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const ref = useRef(null);
  const isDash = activeMenu === 'dashboard';

  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, []);

  const filtered = !search.trim() ? MODULES
    : MODULES.map(m => ({ ...m, classes: m.classes.filter(c => c.name.toLowerCase().includes(search.toLowerCase()) || (c.desc||'').toLowerCase().includes(search.toLowerCase())) })).filter(m => m.classes.length > 0);

  return (
    <div className="flex items-center justify-between px-8 py-4 bg-white border-b border-slate-100">
      <div>
        <h1 className="text-lg font-bold text-slate-800">
          {isDash ? '执行大厅' : activeMenu === 'results' ? '测试结果' : activeMenu === 'history' ? '历史记录' : activeMenu === 'failed' ? '失败用例' : '平台设置'}
        </h1>
        {isDash && (
          <p className="text-xs text-slate-500 mt-0.5">
            {isRunning ? `执行中: ${runningLabel}` : selectedClass ? `已选: ${selectedClass.name} — 点击「开始测试」执行` : '选择模块或测试类，点击「开始测试」执行'}
          </p>
        )}
      </div>
      {isDash ? (
        <div className="flex items-center gap-2.5">
          <div className="relative" ref={ref}>
            <button onClick={() => setOpen(!open)} disabled={isRunning}
              className="flex items-center gap-2 bg-white border border-slate-200 text-slate-700 rounded-xl pl-4 pr-3 py-2.5 hover:border-slate-300 outline-none disabled:opacity-50 w-[300px] text-sm font-medium transition-all shadow-sm">
              <span className="flex-1 text-left truncate">{selectedLabel}</span>
              <ChevronDown size={14} className={`text-slate-400 transition ${open ? 'rotate-180' : ''}`} />
            </button>
            {open && (
              <div className="absolute top-full mt-1 right-0 w-[620px] bg-white rounded-xl border border-slate-200 shadow-xl z-30 max-h-[460px] overflow-hidden flex flex-col">
                <div className="p-2 border-b border-slate-100">
                  <div className="relative">
                    <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input value={search} onChange={e => setSearch(e.target.value)} placeholder="搜索模块或测试类..." autoFocus
                      className="w-full pl-8 pr-7 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs outline-none focus:ring-1 focus:ring-blue-500/30" />
                    {search && <button onClick={() => setSearch('')} className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"><X size={12} /></button>}
                  </div>
                </div>
                <div className="overflow-y-auto flex-1 p-1.5">
                  <button onClick={() => { onSelectModule(''); onSelectClass(null); setOpen(false); }}
                    className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-semibold transition-colors ${!selectedModule && !selectedClass ? 'bg-blue-50 text-blue-700' : 'hover:bg-slate-50 text-slate-700'}`}>
                    🚀 全部模块
                    {!selectedModule && !selectedClass && <CheckCircle2 size={14} className="ml-auto text-blue-500" />}
                  </button>
                  {filtered.map(mod => (
                    <div key={mod.tag} className="mt-1">
                      <button onClick={() => { onSelectModule(mod.tag); onSelectClass(null); setOpen(false); }}
                        className={`w-full flex items-center gap-2 px-3 py-1.5 rounded-lg text-[13px] font-semibold transition-colors ${selectedModule===mod.tag && !selectedClass ? 'bg-blue-50 text-blue-700' : 'hover:bg-slate-50 text-slate-600'}`}>
                        <span className="text-xs">{mod.icon}</span> {mod.label}
                        <span className="text-[10px] text-slate-400 font-normal">{mod.classes.length} 类</span>
                        {selectedModule===mod.tag && !selectedClass && <CheckCircle2 size={12} className="ml-auto text-blue-500" />}
                      </button>
                      <div className="ml-7 grid grid-cols-2 gap-0.5 pr-1">
                        {mod.classes.map(cls => {
                          const sel = selectedClass?.name === cls.name;
                          return (
                          <div key={cls.name} className="relative group">
                            <button onClick={() => { onSelectClass(sel ? null : cls); onSelectModule(mod.tag); }} disabled={isRunning}
                              className={`w-full flex items-center gap-1.5 px-2 py-1 rounded text-left transition-colors disabled:opacity-40 ${sel ? 'bg-blue-50 text-blue-700' : 'hover:bg-slate-50'}`}>
                              <span className={`text-[11px] font-mono truncate flex-1 ${sel ? 'text-blue-700' : 'text-slate-600'}`}>{cls.name}</span>
                              <span className={`text-[8px] px-1 py-0.5 rounded font-bold flex-shrink-0 ${cls.type==='api'?'bg-amber-100 text-amber-700':'bg-purple-100 text-purple-700'}`}>{cls.type.toUpperCase()}</span>
                              <span className="w-[14px] flex-shrink-0 flex items-center justify-center">{sel && <CheckCircle2 size={11} className="text-blue-500" />}</span>
                            </button>
                            {/* Hover tooltip */}
                            <div className="absolute left-0 top-full mt-1 w-[320px] bg-slate-800 text-white rounded-xl shadow-2xl z-40 p-3.5 hidden group-hover:block pointer-events-none">
                              <div className="flex items-center gap-1.5 mb-2">
                                <span className={`text-[9px] px-1.5 py-0.5 rounded font-bold ${cls.type==='api'?'bg-amber-500/20 text-amber-300':'bg-purple-500/20 text-purple-300'}`}>{cls.type.toUpperCase()}</span>
                                <span className="text-xs font-semibold text-slate-200 font-mono">{cls.name}</span>
                              </div>
                              {cls.desc && <p className="text-[11px] text-slate-300 leading-relaxed mb-2">{cls.desc}</p>}
                              {cls.methods && cls.methods.length > 0 && (
                                <div className="border-t border-slate-700 pt-2">
                                  <p className="text-[10px] text-slate-500 mb-1">测试场景</p>
                                  {cls.methods.map((m, i) => (
                                    <div key={i} className="flex items-center gap-1.5 text-[10px] py-0.5">
                                      <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${m.type==='good'?'bg-emerald-400':'bg-red-400'}`} />
                                      <span className="text-slate-400">{m.name}</span>
                                      <span className={`text-[8px] ${m.type==='good'?'text-emerald-400':'text-red-400'}`}>{m.type==='good'?'正向':'负向'}</span>
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          </div>
                        );})}
                      </div>
                    </div>
                  ))}
                  {filtered.length === 0 && <p className="text-center text-slate-400 text-xs py-4">无匹配</p>}
                </div>
              </div>
            )}
          </div>
          <button onClick={isRunning ? onStop : onStart} disabled={cleaning}
            className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-semibold transition-all active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed ${isRunning ? 'bg-red-500 hover:bg-red-600 text-white shadow-lg shadow-red-500/25' : 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white shadow-lg shadow-blue-500/25'}`}>
            {cleaning ? <><span className="animate-spin inline-block w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" /> 清理中…</> :
             isRunning ? <><Square size={14} /> 停止</> : <><Play size={14} className="fill-white" /> 开始测试</>}
          </button>
          <button onClick={doCleanup} disabled={cleaning} title="清理测试数据"
            className={`p-2 rounded-lg transition-colors ${cleaning ? 'text-orange-400 animate-pulse' : 'text-slate-400 hover:text-red-500 hover:bg-red-50'}`}>
            <Trash2 size={17} />
          </button>
          <button onClick={onOpenSettings} className="p-2 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"><Settings size={17} /></button>
        </div>
      ) : (
        <div className="flex items-center gap-2.5">
          <button onClick={doCleanup} disabled={cleaning} title="清理测试数据"
            className={`p-2 rounded-lg transition-colors ${cleaning ? 'text-orange-400 animate-pulse' : 'text-slate-400 hover:text-red-500 hover:bg-red-50'}`}>
            <Trash2 size={17} />
          </button>
          <button onClick={onOpenSettings} className="p-2 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"><Settings size={17} /></button>
        </div>
      )}
    </div>
  );
}

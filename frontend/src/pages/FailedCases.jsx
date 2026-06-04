import { useState, useMemo } from 'react';
import { XCircle, Shield, Copy, RefreshCw, ChevronDown, Download } from 'lucide-react';
import { MODULES } from '../data/modules';

export default function FailedCases({ failedCases, onRerunClass }) {
  const [dedupe, setDedupe] = useState(false);
  const [expanded, setExpanded] = useState({});
  const [moduleFilter, setModuleFilter] = useState('');

  const moduleOptions = useMemo(() => {
    const tags = [...new Set(failedCases.map(fc => {
      for (const m of MODULES) for (const c of m.classes) if (c.name === fc.className) return m.tag;
      return null;
    }).filter(Boolean))];
    return tags.map(t => ({ tag: t, label: MODULES.find(m=>m.tag===t)?.label || t }));
  }, [failedCases]);

  // Build class info lookup from MODULES
  const classInfo = useMemo(() => {
    const map = {};
    for (const m of MODULES) for (const c of m.classes) map[c.name] = { ...c, moduleLabel: m.label };
    return map;
  }, []);

  const displayed = useMemo(() => {
    let list = moduleFilter
      ? failedCases.filter(fc => {
          for (const m of MODULES) if (m.tag === moduleFilter) for (const c of m.classes) if (c.name === fc.className) return true;
          return false;
        })
      : failedCases;
    if (dedupe) {
      const seen = new Set();
      list = list.filter(fc => { const k = fc.className+'::'+fc.methodName; if (seen.has(k)) return false; seen.add(k); return true; });
    }
    return list;
  }, [failedCases, dedupe, moduleFilter]);

  const toggleExpand = (i) => setExpanded(p => ({ ...p, [i]: !p[i] }));
  const copyText = async (text) => { try { await navigator.clipboard.writeText(text); } catch {} };

  const exportCSV = () => {
    const BOM = '﻿';
    const header = '模块,类型,测试类,方法名,失败原因,最后失败时间\n';
    const rows = displayed.map(fc => {
      const info = classInfo[fc.className] || {};
      const reason = (fc.reason || '').replace(/"/g, '""').replace(/\n/g, ' ');
      return `"${info.moduleLabel || ''}","${info.type || ''}","${fc.className}","${fc.methodName}","${reason}","${(fc.lastFailTime||'').substring(0,16)}"`;
    }).join('\n');
    const blob = new Blob([BOM + header + rows], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `failed-cases-${new Date().toISOString().substring(0,10)}.csv`;
    a.click(); URL.revokeObjectURL(url);
  };

  return (
    <div className="flex-1 overflow-y-auto mx-8 mb-6 bg-white rounded-xl border border-slate-200 shadow-sm">
      {failedCases.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400">
          <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center mb-3"><Shield size={24} className="text-slate-300" /></div>
          <p className="text-sm text-slate-500">无失败用例</p><p className="text-xs text-slate-400 mt-1">继续保持！</p>
        </div>
      ) : (
        <div className="p-5">
          <div className="flex items-center gap-3 mb-4 flex-wrap">
            <span className="text-sm text-slate-500">{displayed.length} 条</span>
            <label className="flex items-center gap-1.5 text-[11px] text-slate-500 cursor-pointer select-none">
              <input type="checkbox" checked={dedupe} onChange={e=>setDedupe(e.target.checked)} className="w-3 h-3 rounded accent-blue-500" /> 去重
            </label>
            {moduleOptions.length > 0 && (
              <div className="relative">
                <select value={moduleFilter} onChange={e => setModuleFilter(e.target.value)}
                  className="appearance-none bg-slate-50 border border-slate-200 rounded-lg text-[11px] px-2.5 py-1 pr-6 outline-none cursor-pointer">
                  <option value="">全部模块</option>
                  {moduleOptions.map(m => <option key={m.tag} value={m.tag}>{m.label}</option>)}
                </select>
                <ChevronDown size={10} className="absolute right-1.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
              </div>
            )}
            <button onClick={exportCSV}
              className="ml-auto flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-lg bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors">
              <Download size={12}/> 导出 CSV
            </button>
          </div>
          {displayed.map((fc,i) => {
            const isOpen = expanded[i];
            const info = classInfo[fc.className] || {};
            return (
              <div key={i} className="border-b border-slate-50 last:border-0">
                <div className="flex items-center gap-3 px-3 py-2.5 text-[13px] hover:bg-red-50/30 transition-colors cursor-pointer" onClick={() => toggleExpand(i)}>
                  <XCircle size={13} className="text-red-500 flex-shrink-0"/>
                  <span className={`text-[8px] px-1 py-0.5 rounded font-bold flex-shrink-0 ${info.type==='api'?'bg-amber-100 text-amber-700':'bg-purple-100 text-purple-700'}`}>{info.type?.toUpperCase() || '?'}</span>
                  <span className="font-mono text-xs text-slate-700 font-semibold w-40 truncate">{fc.className}</span>
                  <span className="text-[10px] text-slate-400 w-16 truncate flex-shrink-0">{info.moduleLabel || ''}</span>
                  <span className="font-mono text-xs text-red-600 w-48 truncate">{fc.methodName}</span>
                  {!isOpen && fc.reason && <span className="text-[11px] text-red-500 flex-1 truncate">{fc.reason.split('\n')[0]}</span>}
                  <span className="text-xs text-slate-400 ml-auto whitespace-nowrap">{(fc.lastFailTime||'').substring(0,16).replace('T',' ')}</span>
                </div>
                {isOpen && (
                  <div className="px-3 pb-3 pl-9">
                    {info.desc && <p className="text-[11px] text-slate-500 mb-2">{info.desc}</p>}
                    {fc.reason && (
                      <div className="relative bg-red-50 rounded-lg p-2.5 mb-2 text-[11px] font-mono text-red-700 whitespace-pre-wrap break-all max-h-[200px] overflow-y-auto">
                        {fc.reason}
                        <button onClick={(e)=>{e.stopPropagation();copyText(fc.reason);}} className="absolute top-1.5 right-1.5 p-1 rounded bg-red-100 hover:bg-red-200 text-red-500 transition-colors" title="复制">
                          <Copy size={11}/>
                        </button>
                      </div>
                    )}
                    <button onClick={(e)=>{e.stopPropagation(); if(onRerunClass)onRerunClass(fc.className);}} className="flex items-center gap-1 text-[10px] px-2 py-1 rounded bg-red-50 text-red-600 hover:bg-red-100 transition-colors">
                      <RefreshCw size={9}/> 重跑此类
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Shield, Copy, RefreshCw, ChevronDown, Download,
  Eye, ChevronRight, FileText
} from 'lucide-react';
import { MODULES } from '../data/modules';
import StatusBadge from '../components/StatusBadge';
import { useTestStore } from '../store/useTestStore';
import JSONViewer from '../components/JSONViewer';
import { parseReason, errorPreview } from '../utils/parseReason';

export default function FailedCases({ onRerunClass }) {
  const navigate = useNavigate();
  const failedCases = useTestStore(s => s.failedCases);
  const caseDetails = useTestStore(s => s.caseDetails);
  const [dedupe, setDedupe] = useState(false);
  const [expanded, setExpanded] = useState({});
  const [moduleFilter, setModuleFilter] = useState('');

  const moduleOptions = useMemo(() => {
    const tags = [...new Set(failedCases.map(fc => {
      for (const m of MODULES) for (const c of m.classes) if (c.name === fc.className) return m.tag;
      return null;
    }).filter(Boolean))];
    return tags.map(t => ({ tag: t, label: MODULES.find(m => m.tag === t)?.label || t }));
  }, [failedCases]);

  const classInfo = useMemo(() => {
    const map = {};
    for (const m of MODULES) for (const c of m.classes) { map[c.name] = { ...c, moduleLabel: m.label, moduleTag: m.tag, moduleColor: m.color }; }
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
      list = list.filter(fc => { const k = fc.className + '::' + fc.methodName; if (seen.has(k)) return false; seen.add(k); return true; });
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
      return `"${info.moduleLabel || ''}","${info.type || ''}","${fc.className}","${fc.methodName}","${reason}","${(fc.lastFailTime || '').substring(0, 16)}"`;
    }).join('\n');
    const blob = new Blob([BOM + header + rows], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `failed-cases-${new Date().toISOString().substring(0, 10)}.csv`;
    a.click(); URL.revokeObjectURL(url);
  };

  return (
    <div className="flex-1 overflow-y-auto mx-8 mb-6 bg-white rounded-xl border border-slate-200 shadow-sm">
      {failedCases.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400">
          <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center mb-3">
            <Shield size={24} className="text-slate-300" />
          </div>
          <p className="text-sm text-slate-500">无失败用例</p>
          <p className="text-xs text-slate-400 mt-1">继续保持！</p>
          <button onClick={() => navigate('/')}
            className="mt-4 px-4 py-2 rounded-lg text-xs font-medium bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors">
            前往执行大厅
          </button>
        </div>
      ) : (
        <div className="flex flex-col h-full">
          {/* Toolbar */}
          <div className="flex items-center gap-3 px-5 py-3 border-b border-slate-100 flex-wrap">
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-slate-100 text-[11px] font-medium text-slate-600">
              {displayed.length} 条
            </span>
            <label className="flex items-center gap-1.5 text-[11px] text-slate-500 cursor-pointer select-none">
              <input type="checkbox" checked={dedupe} onChange={e => setDedupe(e.target.checked)} className="w-3 h-3 rounded accent-blue-500" /> 去重
            </label>
            {moduleOptions.length > 0 && (
              <div className="relative">
                <select value={moduleFilter} onChange={e => setModuleFilter(e.target.value)}
                  className="appearance-none bg-slate-50 border border-slate-200 rounded-lg text-[11px] px-2.5 py-1 pr-6 outline-none cursor-pointer focus:border-blue-300">
                  <option value="">全部模块</option>
                  {moduleOptions.map(m => <option key={m.tag} value={m.tag}>{m.label}</option>)}
                </select>
                <ChevronDown size={10} className="absolute right-1.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
              </div>
            )}
            <button onClick={exportCSV}
              className="ml-auto flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-lg bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors">
              <Download size={12} /> 导出 CSV
            </button>
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-[13px]">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/50">
                  <th className="text-left px-4 py-2.5 text-[11px] font-semibold text-slate-500 uppercase tracking-wider w-[120px]">所属模块</th>
                  <th className="text-left px-4 py-2.5 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">用例名称</th>
                  <th className="text-left px-4 py-2.5 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">错误概览</th>
                  <th className="text-left px-4 py-2.5 text-[11px] font-semibold text-slate-500 uppercase tracking-wider w-[140px]">发生时间</th>
                  <th className="text-center px-4 py-2.5 text-[11px] font-semibold text-slate-500 uppercase tracking-wider w-[100px]">操作</th>
                </tr>
              </thead>
              <tbody>
                {displayed.map((fc, i) => {
                  const isOpen = expanded[i];
                  const info = classInfo[fc.className] || {};
                  const parsed = parseReason(fc.reason);

                  return (
                    <>
                      <tr key={i} className={`border-b border-slate-50 transition-colors ${isOpen ? 'bg-blue-50/20 border-b-0' : 'hover:bg-slate-50/60'}`}>
                        <td className="px-4 py-3 cursor-pointer" onClick={() => toggleExpand(i)}>
                          <div className="flex items-center gap-1.5">
                            <span
                              className="w-2 h-2 rounded-full flex-shrink-0"
                              style={{ backgroundColor: info.moduleColor || '#94a3b8' }}
                            />
                            <span className="text-[12px] text-slate-600 truncate">{info.moduleLabel || '—'}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3 cursor-pointer" onClick={() => toggleExpand(i)}>
                          <div className="flex items-center gap-1.5">
                            <StatusBadge variant={info.type === 'api' ? 'api' : 'ui'}>{info.type?.toUpperCase() || '?'}</StatusBadge>
                            <span className="font-mono text-[12px] text-slate-800 font-medium">{fc.className}</span>
                            <ChevronRight size={12} className={`text-slate-400 transition-transform ml-1 ${isOpen ? 'rotate-90' : ''}`} />
                          </div>
                          <div className="text-[11px] text-slate-500 font-mono mt-0.5">{fc.methodName}</div>
                        </td>
                        <td className="px-4 py-3 cursor-pointer" onClick={() => toggleExpand(i)}>
                          <span className="text-[12px] text-slate-500 line-clamp-1">
                            {errorPreview(parsed.summary || fc.reason, 50)}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-[11px] text-slate-400 whitespace-nowrap cursor-pointer" onClick={() => toggleExpand(i)}>
                          {(fc.lastFailTime || '').substring(0, 16).replace('T', ' ')}
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-center gap-1">
                            <button
                              onClick={(e) => { e.stopPropagation(); onRerunClass(fc.className); }}
                              className="p-1.5 rounded-lg hover:bg-amber-50 text-slate-400 hover:text-amber-600 transition-colors"
                              title="重跑此类"
                            >
                              <RefreshCw size={13} />
                            </button>
                            <button
                              onClick={(e) => { e.stopPropagation(); toggleExpand(i); }}
                              className="p-1.5 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600 transition-colors"
                              title="查看详情"
                            >
                              <Eye size={13} />
                            </button>
                            <button
                              onClick={(e) => { e.stopPropagation(); copyText(fc.reason || ''); }}
                              className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors"
                              title="复制错误信息"
                            >
                              <Copy size={13} />
                            </button>
                          </div>
                        </td>
                      </tr>
                      {isOpen && (
                        <tr key={`detail-${i}`}>
                          <td colSpan={5} className="px-0 py-0 border-b border-slate-200 bg-slate-50/30">
                            <div className="px-6 py-4 space-y-4">
                              {/* Header info */}
                              <div className="flex items-center gap-2 flex-wrap">
                                <span className="text-[12px] font-semibold text-slate-700">{fc.className}</span>
                                <span className="text-[11px] text-slate-400">·</span>
                                <span className="text-[11px] text-slate-500 font-mono">{fc.methodName}</span>
                                {info.desc && <><span className="text-[11px] text-slate-400">·</span><span className="text-[11px] text-slate-500">{info.desc}</span></>}
                              </div>

                              {/* Expected vs Actual */}
                              {parsed.assertions.length > 0 && (
                                <div>
                                  <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">断言对比</p>
                                  <div className="flex items-center gap-3 flex-wrap">
                                    {parsed.assertions.map((a, j) => (
                                      <div key={j} className="flex items-center gap-2">
                                        <StatusBadge variant="expected">Expected: {a.expected}</StatusBadge>
                                        <span className="text-[11px] text-slate-400">vs</span>
                                        <StatusBadge variant="actual">Actual: {a.actual}</StatusBadge>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              )}

                              {/* JSON blocks */}
                              {parsed.jsonBlocks.length > 0 && (
                                <div>
                                  <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">
                                    Response Data
                                    {parsed.jsonBlocks.length > 1 && <span className="text-slate-400 ml-1">({parsed.jsonBlocks.length} blocks)</span>}
                                  </p>
                                  <div className="space-y-3">
                                    {parsed.jsonBlocks.map((json, j) => (
                                      <JSONViewer key={j} data={json} maxHeight={280} defaultExpanded={j === 0} />
                                    ))}
                                  </div>
                                </div>
                              )}

                              {/* Summary / error message */}
                              {parsed.summary && (
                                <div>
                                  <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">错误信息</p>
                                  <div className="relative rounded-lg bg-slate-100 p-3 text-[12px] font-mono text-slate-700 whitespace-pre-wrap break-all max-h-[160px] overflow-y-auto">
                                    {parsed.summary}
                                    <button
                                      onClick={() => copyText(fc.reason || '')}
                                      className="absolute top-2 right-2 p-1 rounded bg-white/60 hover:bg-white text-slate-400 hover:text-slate-700 transition-colors"
                                      title="复制全部"
                                    >
                                      <Copy size={12} />
                                    </button>
                                  </div>
                                </div>
                              )}

                              {/* Stack trace */}
                              {parsed.stackTrace && (
                                <div>
                                  <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">堆栈追踪</p>
                                  <pre className="text-[11px] font-mono text-slate-500 bg-slate-100 rounded-lg p-3 max-h-[200px] overflow-auto whitespace-pre-wrap break-all">
                                    {parsed.stackTrace}
                                  </pre>
                                </div>
                              )}

                              {/* Action buttons */}
                              <div className="flex items-center gap-2 pt-1 border-t border-slate-200">
                                <button
                                  onClick={() => onRerunClass(fc.className)}
                                  className="flex items-center gap-1.5 text-[11px] px-3 py-1.5 rounded-lg bg-amber-50 text-amber-700 hover:bg-amber-100 transition-colors font-medium"
                                >
                                  <RefreshCw size={11} /> 重跑此类
                                </button>
                                <button
                                  onClick={() => copyText(fc.reason || '')}
                                  className="flex items-center gap-1.5 text-[11px] px-3 py-1.5 rounded-lg bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors"
                                >
                                  <Copy size={11} /> 复制错误信息
                                </button>
                                <button
                                  className="flex items-center gap-1.5 text-[11px] px-3 py-1.5 rounded-lg bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors"
                                  title="查看完整日志"
                                >
                                  <FileText size={11} /> 查看日志
                                </button>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

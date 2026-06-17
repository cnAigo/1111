import { useState, useMemo, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Shield, Copy, RefreshCw, ChevronDown, Download,
  Eye, Bug, Trash2, Clock, Loader2
} from 'lucide-react';
import StatusBadge from '../components/StatusBadge';
import Drawer from '../components/Drawer';
import JSONViewer from '../components/JSONViewer';
import { useTestStore, toast } from '../store/useTestStore';
import request from '../utils/request';
import { parseReason, errorPreview } from '../utils/parseReason';

export default function FailedCases({ modules = [], onRerunClass }) {
  const navigate = useNavigate();
  const failedCases = useTestStore(s => s.failedCases);
  const failedCasesHasMore = useTestStore(s => s.failedCasesHasMore);
  const failedCasesPage = useTestStore(s => s.failedCasesPage);
  const loadFailedCases = useTestStore(s => s.loadFailedCases);
  const caseDetails = useTestStore(s => s.caseDetails);
  const [dedupe, setDedupe] = useState(false);
  const [moduleFilter, setModuleFilter] = useState('');
  const [selectedCase, setSelectedCase] = useState(null); // { fc, info, parsed }
  const [suspected, setSuspected] = useState(() => {
    try { return new Set(JSON.parse(localStorage.getItem('taas_suspected_defects') || '[]')); }
    catch { return new Set(); }
  });

  const [loadingMore, setLoadingMore] = useState(false);
  const sentinelRef = useRef(null);

  // Load initial data on mount
  useEffect(() => {
    if (failedCases.length === 0) loadFailedCases(0, false);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Infinite scroll via IntersectionObserver
  const loadMore = useCallback(async () => {
    if (loadingMore || !failedCasesHasMore) return;
    setLoadingMore(true);
    await loadFailedCases(failedCasesPage + 1, true);
    setLoadingMore(false);
  }, [loadingMore, failedCasesHasMore, failedCasesPage, loadFailedCases]);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) loadMore();
    }, { rootMargin: '200px' });
    observer.observe(el);
    return () => observer.disconnect();
  }, [loadMore]);

  const toggleSuspected = (key) => {
    setSuspected(prev => {
      const next = new Set(prev);
      prev.has(key) ? next.delete(key) : next.add(key);
      localStorage.setItem('taas_suspected_defects', JSON.stringify([...next]));
      return next;
    });
  };

  const moduleOptions = useMemo(() => {
    const tags = [...new Set(failedCases.map(fc => {
      for (const m of modules) for (const c of m.classes) if (c.name === fc.className) return m.tag;
      return null;
    }).filter(Boolean))];
    return tags.map(t => ({ tag: t, label: modules.find(m => m.tag === t)?.label || t }));
  }, [failedCases, modules]);

  const classInfo = useMemo(() => {
    const map = {};
    for (const m of modules) for (const c of m.classes) { map[c.name] = { ...c, moduleLabel: m.label, moduleTag: m.tag, moduleColor: m.color }; }
    return map;
  }, [modules]);

  const displayed = useMemo(() => {
    let list = moduleFilter
      ? failedCases.filter(fc => {
          for (const m of modules) if (m.tag === moduleFilter) for (const c of m.classes) if (c.name === fc.className) return true;
          return false;
        })
      : failedCases;
    if (dedupe) {
      const seen = new Set();
      list = list.filter(fc => { const k = fc.className + '::' + fc.methodName; if (seen.has(k)) return false; seen.add(k); return true; });
    }
    return list;
  }, [failedCases, dedupe, moduleFilter, modules]);

  const openDetail = (fc) => {
    const info = classInfo[fc.className] || {};
    const parsed = parseReason(fc.reason);
    setSelectedCase({ fc, info, parsed });
  };

  const copyText = async (text) => { try { await navigator.clipboard.writeText(text); toast('复制成功', 'success'); } catch { toast('复制失败', 'error'); } };

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
            {suspected.size > 0 && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-red-50 text-[11px] font-medium text-red-600">
                <Bug size={10} /> {suspected.size} 疑似缺陷
              </span>
            )}
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
            <button onClick={() => { if (confirm('确认删除全部失败记录？')) { request.delete('/api/test/history'); window.location.reload(); } }}
              className="flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-lg bg-red-50 text-red-600 hover:bg-red-100 transition-colors">
              <Trash2 size={12} /> 全部删除
            </button>
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
                  <th className="text-center px-4 py-2.5 text-[11px] font-semibold text-slate-500 uppercase tracking-wider w-[120px]">操作</th>
                </tr>
              </thead>
              <tbody>
                {displayed.map((fc, i) => {
                  const info = classInfo[fc.className] || {};
                  const parsed = parseReason(fc.reason);
                  const defKey = `${fc.className}::${fc.methodName}`;
                  const isSuspected = suspected.has(defKey);

                  return (
                    <tr key={i} className={`border-b border-slate-50 transition-colors ${isSuspected ? 'bg-red-50/40 border-b-red-100' : 'hover:bg-slate-50/60'}`}>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1.5">
                          <span
                            className="w-2 h-2 rounded-full flex-shrink-0"
                            style={{ backgroundColor: info.moduleColor || '#94a3b8' }}
                          />
                          <span className="text-[12px] text-slate-600 truncate">{info.moduleLabel || '—'}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1.5">
                          <StatusBadge variant={info.type === 'api' ? 'api' : 'ui'}>{info.type?.toUpperCase() || '?'}</StatusBadge>
                          <span className="font-mono text-[12px] text-slate-800 font-medium truncate max-w-[200px]">{fc.className}</span>
                        </div>
                        <div className="text-[11px] text-slate-500 font-mono mt-0.5 truncate max-w-[280px]">{fc.methodName}</div>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-[12px] text-slate-500 line-clamp-1">
                          {errorPreview(parsed.summary || fc.reason, 60)}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-[11px] text-slate-400 whitespace-nowrap">
                        <div className="flex items-center gap-1">
                          <Clock size={10} />
                          {(fc.lastFailTime || '').substring(0, 16).replace('T', ' ')}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-center gap-1">
                          <button
                            onClick={() => toggleSuspected(defKey)}
                            className={`p-1.5 rounded-lg transition-colors ${isSuspected ? 'bg-red-100 text-red-600 hover:bg-red-200' : 'text-slate-300 hover:text-red-500 hover:bg-red-50'}`}
                            title={isSuspected ? '取消标记疑似缺陷' : '标记为疑似缺陷'}
                          >
                            <Bug size={13} />
                          </button>
                          <button
                            onClick={() => onRerunClass(fc.className)}
                            className="p-1.5 rounded-lg hover:bg-amber-50 text-slate-400 hover:text-amber-600 transition-colors"
                            title="重跑此类"
                          >
                            <RefreshCw size={13} />
                          </button>
                          <button
                            onClick={() => openDetail(fc)}
                            className="p-1.5 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600 transition-colors"
                            title="查看详情"
                          >
                            <Eye size={13} />
                          </button>
                          <button
                            onClick={() => copyText(fc.reason || '')}
                            className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors"
                            title="复制错误信息"
                          >
                            <Copy size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {/* Infinite scroll sentinel */}
            <div ref={sentinelRef} className="flex items-center justify-center py-3">
              {loadingMore ? (
                <span className="flex items-center gap-2 text-[11px] text-slate-400">
                  <Loader2 size={12} className="animate-spin" /> 加载更多...
                </span>
              ) : failedCasesHasMore ? (
                <span className="text-[11px] text-slate-300">滚动加载更多</span>
              ) : (
                <span className="text-[11px] text-slate-300">— 已加载全部 —</span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Detail Drawer */}
      <Drawer
        open={!!selectedCase}
        onClose={() => setSelectedCase(null)}
        title={selectedCase ? `${selectedCase.fc.className} · ${selectedCase.fc.methodName}` : ''}
        width="w-[540px]"
      >
        {selectedCase && (() => {
          const { fc, info, parsed } = selectedCase;
          const defKey = `${fc.className}::${fc.methodName}`;
          const isSuspected = suspected.has(defKey);

          return (
            <div className="space-y-5">
              {/* Meta */}
              <div className="flex items-center gap-2 flex-wrap">
                <span className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                  style={{ backgroundColor: info.moduleColor || '#94a3b8' }} />
                <span className="text-[12px] font-semibold text-slate-700">{info.moduleLabel || '—'}</span>
                <StatusBadge variant={info.type === 'api' ? 'api' : 'ui'}>{info.type?.toUpperCase() || '?'}</StatusBadge>
                {info.desc && <span className="text-[11px] text-slate-500">{info.desc}</span>}
              </div>

              <div className="text-[11px] text-slate-400 flex items-center gap-1">
                <Clock size={10} />
                {(fc.lastFailTime || '').substring(0, 19).replace('T', ' ')}
              </div>

              {/* Assertions */}
              {parsed.assertions.length > 0 && (
                <div>
                  <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">断言对比</p>
                  <div className="space-y-1.5">
                    {parsed.assertions.map((a, j) => (
                      <div key={j} className="flex items-center gap-2 flex-wrap bg-slate-50 rounded-lg px-3 py-2">
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
                      <JSONViewer key={j} data={json} maxHeight={280} defaultExpanded />
                    ))}
                  </div>
                </div>
              )}

              {/* Error summary */}
              {parsed.summary && (
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">错误信息</p>
                    <button
                      onClick={() => copyText(parsed.summary)}
                      className="flex items-center gap-1 text-[10px] text-slate-400 hover:text-slate-600 transition-colors"
                    >
                      <Copy size={11} /> 复制
                    </button>
                  </div>
                  <pre className="text-[12px] font-mono text-slate-700 bg-slate-100 rounded-lg p-3 whitespace-pre-wrap break-all max-h-[200px] overflow-y-auto leading-relaxed">
                    {parsed.summary}
                  </pre>
                </div>
              )}

              {/* Stack trace */}
              {parsed.stackTrace && (
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">堆栈追踪</p>
                    <button
                      onClick={() => copyText(parsed.stackTrace)}
                      className="flex items-center gap-1 text-[10px] text-slate-400 hover:text-slate-600 transition-colors"
                    >
                      <Copy size={11} /> 复制
                    </button>
                  </div>
                  <pre className="text-[11px] font-mono text-slate-500 bg-slate-100 rounded-lg p-3 max-h-[400px] overflow-auto whitespace-pre-wrap break-all leading-relaxed">
                    {parsed.stackTrace}
                  </pre>
                </div>
              )}

              {/* Full raw reason (fallback) */}
              {!parsed.summary && !parsed.stackTrace && fc.reason && (
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">原始错误输出</p>
                    <button
                      onClick={() => copyText(fc.reason)}
                      className="flex items-center gap-1 text-[10px] text-slate-400 hover:text-slate-600 transition-colors"
                    >
                      <Copy size={11} /> 复制
                    </button>
                  </div>
                  <pre className="text-[11px] font-mono text-slate-600 bg-slate-100 rounded-lg p-3 max-h-[400px] overflow-auto whitespace-pre-wrap break-all leading-relaxed">
                    {fc.reason}
                  </pre>
                </div>
              )}

              {/* Action buttons */}
              <div className="flex items-center gap-2 pt-2 border-t border-slate-200">
                <button
                  onClick={() => toggleSuspected(defKey)}
                  className={`flex items-center gap-1.5 text-[11px] px-3 py-1.5 rounded-lg transition-colors font-medium ${isSuspected ? 'bg-red-100 text-red-700 hover:bg-red-200' : 'bg-red-50 text-red-600 hover:bg-red-100'}`}
                >
                  <Bug size={11} /> {isSuspected ? '已标记疑似缺陷' : '标记疑似缺陷'}
                </button>
                <button
                  onClick={() => { onRerunClass(fc.className); setSelectedCase(null); }}
                  className="flex items-center gap-1.5 text-[11px] px-3 py-1.5 rounded-lg bg-amber-50 text-amber-700 hover:bg-amber-100 transition-colors font-medium"
                >
                  <RefreshCw size={11} /> 重跑此类
                </button>
                <button
                  onClick={() => copyText(fc.reason || '')}
                  className="flex items-center gap-1.5 text-[11px] px-3 py-1.5 rounded-lg bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors font-medium ml-auto"
                >
                  <Copy size={11} /> 一键复制全部
                </button>
              </div>
            </div>
          );
        })()}
      </Drawer>
    </div>
  );
}

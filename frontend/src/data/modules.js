// Static module tree — used as fallback when backend /api/test/modules is unavailable.
// Mirrors the data that ModuleScanner produces from src/test/java/cases/manual/*.java
export const MODULES = [
  { tag:'FolderModule', label:'文件夹操作', icon:'📁', color:'#f59e0b', classes:[
    { name:'FolderManualTest', type:'api', desc:'新建/重命名/删除/描述/刷新 · API 测试：根节点新建文件夹、新建子文件夹、重命名-重复、重命名-空、重命名-超长、重命名-XSS … 等 18 个用例' },
  ]},
  { tag:'IOModule', label:'导入导出', icon:'📤', color:'#10b981', classes:[
    { name:'ImportExportManualTest', type:'api', desc:'Excel/Word/ReqIF导入导出 · API 测试' },
  ]},
  { tag:'ReqSpecModule', label:'需求规格', icon:'📋', color:'#3b82f6', classes:[
    { name:'ReqSpecManualTest', type:'api', desc:'CRUD/属性/文件/权限/模式/视图 · API 测试' },
  ]},
  { tag:'AttributeModule', label:'自定义属性', icon:'🏷️', color:'#8b5cf6', classes:[
    { name:'AttributeManualTest', type:'api', desc:'新建/校验/发布/删除/搜索 · API 测试' },
  ]},
  { tag:'ReqItemEditModule', label:'需求条目编辑', icon:'✏️', color:'#ec4899', classes:[
    { name:'ReqItemEditManualTest', type:'api', desc:'富文本/复制/剪切/加锁解锁 · API 测试' },
  ]},
  { tag:'FlowModule', label:'流程定义', icon:'🔀', color:'#94a3b8', classes:[
    { name:'FlowDefineManualTest', type:'api', desc:'流程定义(TODD) · API 测试' },
  ]},
  { tag:'WorkflowModule', label:'需求审签', icon:'✅', color:'#f97316', classes:[
    { name:'WorkflowManualTest', type:'api', desc:'草稿/审批/更改(TODD) · API 测试' },
  ]},
  { tag:'TraceModule', label:'需求追溯', icon:'🔍', color:'#6366f1', classes:[
    { name:'TraceManualTest', type:'api', desc:'追溯(TODD) · API 测试' },
  ]},
  { tag:'CooperationModule', label:'合作区管理', icon:'🤝', color:'#14b8a6', classes:[
    { name:'CooperationManualTest', type:'api', desc:'添加/修改/删除/分配人员 · API 测试' },
  ]},
  { tag:'IndicatorModule', label:'指标管理', icon:'📊', color:'#eab308', classes:[
    { name:'IndicatorManualTest', type:'api', desc:'逻辑架构/节点/指标参数/导入导出 · API 测试' },
  ]},
  { tag:'UiOnlyModule', label:'纯UI记录', icon:'🖥️', color:'#64748b', classes:[
    { name:'UiOnlyManualTest', type:'ui', desc:'仅UI操作记录 · UI 测试' },
  ]},
  { tag:'PermissionModule', label:'权限验证', icon:'🔐', color:'#ef4444', classes:[
    { name:'PermissionManualTest', type:'api', desc:'跨用户写入权限校验 · API 测试' },
  ]},
];

export const MENU_ITEMS = [
  { key: 'dashboard', label: '执行大厅', path: '/' },
  { key: 'results',   label: '测试结果', path: '/results' },
  { key: 'history',   label: '历史记录', path: '/history' },
  { key: 'failed',    label: '失败用例', path: '/failed' },
  { key: 'coverage',  label: '测试覆盖', path: '/coverage' },
  { key: 'settings',  label: '平台设置', path: '/settings' },
];

export const STATUS_CFG = {
  IDLE:    { text: '就绪', dot: 'bg-slate-300' },
  RUNNING: { text: '执行中', dot: 'bg-amber-500 animate-pulse' },
  SUCCESS: { text: '通过', dot: 'bg-emerald-500' },
  FAILED:  { text: '失败', dot: 'bg-red-500' },
  STOPPED: { text: '已停止', dot: 'bg-slate-300' },
};

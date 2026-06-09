export const MODULES = [
  { tag:'ReqFolderModule', label:'需求与目录', icon:'📁', color:'#f59e0b', classes:[
    { name:'FolderApiTest', type:'api', desc:'文件夹CRUD：新建/重命名/删除/恢复/彻底删除', methods:[{ name:'GNYL_012 根节点新建', type:'good' },{ name:'GNYL_020 重复命名', type:'bad' },{ name:'GNYL_022 空名称', type:'bad' },{ name:'GNYL_027 删除空文件夹', type:'good' },{ name:'GNYL_029 恢复文件夹', type:'good' }] },
    { name:'ReqSpecApiTest', type:'api', desc:'需求规格：新建/重命名/删除/恢复' },
    { name:'ReqItemApiTest', type:'api', desc:'需求项：新建/删除/恢复/子项查询' },
    { name:'ReqUpdateApiTest', type:'api', desc:'批量更新列表/编辑描述/边界校验' },
    { name:'SearchApiTest', type:'api', desc:'项目/文件夹/属性多维度搜索' },
    { name:'FavoriteApiTest', type:'api', desc:'收藏夹添加/搜索/删除' },
    { name:'ViewApiTest', type:'api', desc:'需求视图新建/查询/删除' },
    { name:'ReqSpecUITest', type:'ui', desc:'需求规格交互：新建/重命名/删除' },
    { name:'RequirementFolderUITest', type:'ui', desc:'需求文件夹UI：CRUD全流程 (22 tests)' },
    { name:'ReqTest', type:'ui', desc:'需求条目全流程：CRUD/属性编辑' },
  ]},
  { tag:'AttributeModule', label:'自定义属性', icon:'🏷️', color:'#3b82f6', classes:[
    { name:'CustomAttributeApiTest', type:'api', desc:'Int/Float/String/Date 增删改查+批量删除+发布' },
    { name:'BasicAttributeTest', type:'ui', desc:'基础属性页交互' },
    { name:'EnumAttributeTest', type:'ui', desc:'枚举属性下拉/搜索/多选' },
    { name:'AttributeUITest', type:'ui', desc:'综合UI：完整属性生命周期' },
  ]},
  { tag:'IOModule', label:'导入导出', icon:'📤', color:'#10b981', classes:[
    { name:'ExportImportApiTest', type:'api', desc:'Excel/Word/ReqIf导出+模板下载+导入校验' },
    { name:'ExportImportUITest', type:'ui', desc:'导入导出交互：文件选择→导出/导入→下载/验证' },
    { name:'WordImportTest', type:'ui', desc:'Word导入专项' },
  ]},
  { tag:'CollaborationModule', label:'协作区', icon:'🤝', color:'#8b5cf6', classes:[
    { name:'CooperationAreaApiTest', type:'api', desc:'合作区全生命周期+用户管理+安全校验' },
    { name:'ProjectPersonApiTest', type:'api', desc:'项目人员分配/部门树' },
    { name:'CooperationAreaUITest', type:'ui', desc:'合作区管理界面交互' },
  ]},
  { tag:'VersionTraceModule', label:'版本追溯', icon:'🔀', color:'#ec4899', classes:[
    { name:'VersionTraceApiTest', type:'api', desc:'版本列表/访问权限/追溯/变更分析' },
    { name:'UnlockApiTest', type:'api', desc:'解锁/强制解锁' },
    { name:'VersionTraceUITest', type:'ui', desc:'版本历史/差异对比UI' },
  ]},
  { tag:'UserManageModule', label:'用户管理', icon:'👥', color:'#f97316', classes:[
    { name:'UserManageApiTest', type:'api', desc:'用户CRUD+搜索/删除/重置密码/导入' },
    { name:'SystemUserApiTest', type:'api', desc:'系统用户完整CRUD+分页/导出' },
    { name:'SystemPostApiTest', type:'api', desc:'岗位管理CRUD+导出' },
    { name:'UserManagementUITest', type:'ui', desc:'用户管理界面交互' },
  ]},
  { tag:'IndicatorModule', label:'指标管理', icon:'📊', color:'#6366f1', classes:[
    { name:'LogicStructureApiTest', type:'api', desc:'逻辑结构CRUD' },
    { name:'LogicApiTest', type:'api', desc:'逻辑项CRUD' },
    { name:'ParameterApiTest', type:'api', desc:'指标参数CRUD+物理方案' },
  ]},
  { tag:'CommonModule', label:'通用测试', icon:'🛡️', color:'#94a3b8', classes:[
    { name:'CommonUITest', type:'ui', desc:'通用UI基准：登录/导航/面包屑' },
    { name:'CommonCompatibilityTest', type:'ui', desc:'浏览器兼容矩阵' },
    { name:'CommonSecurityTest', type:'ui', desc:'安全基线扫描' },
  ]},
];

export const MODULE_OPTIONS = [
  { value: '', label: '全部模块', tag: 'ALL' },
  { value: 'ReqFolderModule', label: '需求与目录模块', tag: 'ReqFolderModule' },
  { value: 'AttributeModule', label: '自定义属性模块', tag: 'AttributeModule' },
  { value: 'IOModule', label: '导入导出模块', tag: 'IOModule' },
  { value: 'CollaborationModule', label: '协作区管理模块', tag: 'CollaborationModule' },
  { value: 'VersionTraceModule', label: '版本追溯模块', tag: 'VersionTraceModule' },
  { value: 'UserManageModule', label: '用户管理模块', tag: 'UserManageModule' },
  { value: 'IndicatorModule', label: '指标管理模块', tag: 'IndicatorModule' },
  { value: 'CommonModule', label: '通用测试模块', tag: 'CommonModule' },
];

export const MENU_ITEMS = [
  { key: 'dashboard', label: '执行大厅', path: '/' },
  { key: 'results',   label: '测试结果', path: '/results' },
  { key: 'history',   label: '历史记录', path: '/history' },
  { key: 'failed',    label: '失败用例', path: '/failed' },
  { key: 'settings',  label: '平台设置', path: '/settings' },
];

export const STATUS_CFG = {
  IDLE:    { text: '就绪', dot: 'bg-slate-300' },
  RUNNING: { text: '执行中', dot: 'bg-amber-500 animate-pulse' },
  SUCCESS: { text: '通过', dot: 'bg-emerald-500' },
  FAILED:  { text: '失败', dot: 'bg-red-500' },
  STOPPED: { text: '已停止', dot: 'bg-slate-300' },
};

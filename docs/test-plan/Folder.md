# Folder 测试计划

| 序号 | 功能 | 测试点 | 已有代码 |
|------|------|--------|----------|
| 1 | 新建文件夹 | 新建文件夹/子文件夹/同级文件夹 | FolderApiTest (API), FolderUITest (UI) |
| 2 | 重命名文件夹 | 超长/重复/不存在/XXS/空名称 | FolderApiTest (API), FolderUITest (UI) |
| 3 | 删除文件夹 | 有子/无子/取消/清除/直接清除有子 | FolderApiTest (API), FolderUITest (UI) |
| 4 | 文件夹描述 | XXS注入/超长(2000字)/空值 | FolderEditApiTest (API 已写) |
| 5 | 根节点刷新 | 刷新根节点 | RequirementTest |

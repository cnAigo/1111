# 项目问题分析 & 决策板

> 最后更新：2026-06-03
> 用途：列出当前所有已知问题，用户决策优先级，Agent 执行修改后勾掉。

---

## 🔴 Bug — 功能不正确

### B1. `onRerunClass` 第一行 API 调用是废代码

- **文件：** `frontend/src/App.jsx:104`
- **现象：** 调了 `apiPost('/api/test/rerun-failed', { taskId: '__class__' })` 但返回值没用，实际起作用的是下面 `/api/test/run`
- **修改：** 删掉第 104 行
- **决策：** [ ] 修  [ ] 忽略

### B2. `onRerun` 成功后没开始轮询新任务

- **文件：** `frontend/src/App.jsx:112-120`
- **现象：** 历史页点 Rerun → 后端启动新 mvn 进程 → 返回新 taskId → 前端只弹 toast，没开始轮询。用户切回 dashboard 看不到终端输出
- **修改：** 把 `d.taskId` 传给 `useTestRun`，触发轮询。需要在 `useTestRun` 里暴露 `resumeTask(taskId)` 方法
- **决策：** [ ] 修  [ ] 忽略

---

## 🟡 体验 — 功能可用但不顺手

### U1. 配置弹窗加载已保存配置时不填密码

- **文件：** `frontend/src/components/ConfigModal.jsx:19`
- **现象：** 点击已保存配置→URL/项目ID/用户名填了→密码不动。用户不知道是bug还是故意
- **修改：** 清空密码框 + placeholder："密码已脱敏，请重新输入"
- **决策：** [ ] 修  [ ] 忽略

### U2. 前端无路由 URL

- **文件：** `frontend/src/` 全局
- **现象：** 页面切换靠 state，无 `/results` `/history` 路径。浏览器前进后退无效，无法分享链接
- **修改：** 引入 `react-router-dom`，约 2-3 小时工作量
- **决策：** [ ] 修  [ ] 忽略

### U3. `modules.js` 硬编码测试类元数据

- **文件：** `frontend/src/data/modules.js`
- **现象：** 新增/删除测试类需要同时改前端。后端没有接口动态返回
- **修改：** 后端加 `/api/test/classes` 接口，前端动态渲染（工作量较大）
- **决策：** [ ] 修  [ ] 忽略

### U4. 轮询方式不优雅

- **文件：** `frontend/src/hooks/useTestRun.js`
- **现象：** 1.5 秒轮询一次，每次拉全量输出文本，长测试会产生大量冗余传输
- **修改：** 改 SSE 推送或 WebSocket（需要后端配合）
- **决策：** [ ] 修  [ ] 忽略

### U5. Skipped 筛选项始终显示

- **文件：** `frontend/src/pages/Results.jsx:73`
- **现象：** Skipped 为 0 时筛选按钮仍显示
- **修改：** 加 `{stats.skipped > 0 && ...}` 条件
- **决策：** [ ] 修  [ ] 忽略

---

## 🟢 代码质量 — 不影响使用，但不够好

### C1. `pom.xml` testOutputDirectory hack 可清理

- **文件：** `pom.xml`
- **说明：** 测试已改用 ProcessBuilder 独立进程，`testOutputDirectory` / `forkCount=0` / `spring-boot-maven-plugin folders` 三个配置理论上可移除
- **风险：** 移除后需验证 `spring-boot:run` 仍正常
- **决策：** [ ] 修  [ ] 忽略

### C2. `@EnableAsync` 未使用

- **文件：** `src/main/java/org/example/testvue/TestvueApplication.java:8`
- **修改：** 删掉注解
- **决策：** [ ] 修  [ ] 忽略

### C3. Gson 全限定名

- **文件：** `TestRunnerController.java` `TestExecutionService.java`
- **现象：** 多处 `new com.google.gson.Gson()` 全限定名，没 import
- **修改：** 加 `import com.google.gson.Gson`
- **决策：** [ ] 修  [ ] 忽略

---

## 📋 待 git commit 的文件

```
src/main/java/org/example/testvue/config/WebConfig.java
src/main/java/org/example/testvue/dto/Dtos.java
src/main/java/org/example/testvue/entity/TestConfigEntity.java
src/main/java/org/example/testvue/entity/TestHistory.java
src/main/java/org/example/testvue/repository/TestConfigRepository.java
src/main/java/org/example/testvue/repository/TestHistoryRepository.java
src/main/java/org/example/testvue/service/TestExecutionService.java
src/main/java/org/example/testvue/service/SurefireParser.java
src/main/resources/allure.properties
src/main/resources/application-mysql.properties
src/test/  （全部48个测试类 + actions/base/pages/config）
```

---

## 🎯 决策规则

1. 用户在此文件 `[ ]` 中勾选 → 标为 `[x]`
2. Agent 读取此文件 → 按勾选的顺序执行修改
3. 修改完成后 → 在对应条目下加 `✅ 已修复 — <commit-hash>`
4. 全部修完 → 用户可删掉已修复条目

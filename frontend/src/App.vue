<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<style>
/* ── Blue · Cream · Dashed-Border Theme ── */
:root {
  --primary: #1d88e3;
  --primary-light: #e8f4fd;
  --bg: #f3f0ea;
  --card-bg: #fdfbf7;
  --border: #d8d0c0;
  --text: #2c2c2c;
  --text-dim: #6b6560;
}

#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  height: 100vh;
  width: 100vw;
  background: var(--bg);
}

/* ── Light sidebar with blue accent ── */
.el-menu .el-menu-item.is-active {
  background: #e8f4fd !important;
  color: #1d88e3 !important;
  font-weight: 650;
  border-right: 3px solid #1d88e3;
}

/* ── Cards: dashed border ── */
.card-container {
  background: var(--card-bg);
  border: 1.5px dashed var(--border);
  border-radius: 10px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

/* ── Tables ── */
.el-table {
  border-radius: 8px; overflow: hidden;
  border: 1px solid #e8e2d6;
  --el-table-header-bg-color: #f5f1e9;
  --el-table-row-hover-bg-color: #faf7f0;
}
.el-table th.el-table__cell {
  background: #f5f1e9 !important;
  color: #5a5248;
  font-weight: 650;
}
.el-table .el-table__row--striped { background: #faf8f3; }

/* ── Buttons ── */
.el-button { border-radius: 6px; font-weight: 550; }
.el-button--primary {
  --el-button-bg-color: var(--primary);
  --el-button-border-color: var(--primary);
}

/* ── Dialogs ── */
.el-dialog {
  border-radius: 12px;
  --el-dialog-bg-color: var(--card-bg);
}
.el-dialog__header {
  border-bottom: 1px dashed var(--border);
  padding-bottom: 14px;
  margin: 0 20px;
}
.el-dialog__title { font-weight: 700; color: var(--text); }

/* ── Section titles ── */
.section-title {
  font-size: 15px;
  font-weight: 650;
  color: var(--primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--border);
}

/* ── Page header ── */
.page-header {
  padding-bottom: 14px;
  border-bottom: 1px solid #e0d8cc;
  margin-bottom: 20px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
}

/* ── Tags ── */
.status-passed, .status-completed, .status-success { background: #e8f5e9; color: #2e7d32; border-color: #a5d6a7; }
.status-failed, .status-error { background: #ffebee; color: #c62828; border-color: #ef9a9a; }
.status-running, .status-pending { background: #fff3e0; color: #e65100; border-color: #ffcc80; }
.status-stopped, .status-cancelled { background: #eceff1; color: #546e7a; border-color: #b0bec5; }

/* ── Empty / Form ── */
.el-empty { padding: 40px 0; }
.el-empty .el-empty__description { color: #94a3b8; }
.el-form-item__label { color: var(--text-dim); font-weight: 600; }
.el-input__wrapper, .el-textarea__inner { border-radius: 6px; }
</style>

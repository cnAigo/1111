<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ $t('menu.testReport') }}</span>
    </div>
    <el-card shadow="never">
      <el-table :data="reports" stripe>
        <el-table-column prop="name" label="报告名称" />
        <el-table-column prop="executionId" label="执行ID" width="100" />
        <el-table-column prop="passed" label="通过" width="80" />
        <el-table-column prop="failed" label="失败" width="80" />
        <el-table-column prop="passRate" label="通过率" width="100">
          <template #default="{ row }">
            <el-progress :percentage="row.passRate" :color="row.passRate > 90 ? '#67c23a' : '#e6a23c'" />
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default>
            <el-button link type="primary" size="small">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
const reports = [
  { name: '登录模块回归-第101次', executionId: 101, passed: 5, failed: 0, passRate: 100, time: '2026-07-01 14:33' },
  { name: '需求管理冒烟-第100次', executionId: 100, passed: 8, failed: 2, passRate: 80, time: '2026-07-01 10:05' },
  { name: '全量回归-第99次', executionId: 99, passed: 25, failed: 0, passRate: 100, time: '2026-06-30 18:12' },
]
</script>

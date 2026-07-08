<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ $t('menu.executionRecords') }}</span>
    </div>
    <el-card shadow="never">
      <el-table :data="executions" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="suite" label="测试套件" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'passed' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'passed' ? '通过' : row.status === 'failed' ? '失败' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="passed" label="通过" width="80" />
        <el-table-column prop="failed" label="失败" width="80" />
        <el-table-column prop="duration" label="耗时" width="100" />
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default>
            <el-button link type="primary" size="small">详情</el-button>
            <el-button link type="success" size="small">重跑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
const executions = [
  { id: 101, suite: '登录模块回归', status: 'passed', passed: 5, failed: 0, duration: '2m30s', time: '2026-07-01 14:30' },
  { id: 100, suite: '需求管理冒烟', status: 'failed', passed: 8, failed: 2, duration: '4m15s', time: '2026-07-01 10:00' },
  { id: 99, suite: '全量回归测试', status: 'passed', passed: 25, failed: 0, duration: '12m00s', time: '2026-06-30 18:00' },
]
</script>

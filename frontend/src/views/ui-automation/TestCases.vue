<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ $t('menu.caseManagement') }}</span>
      <el-button type="primary"><el-icon><Plus /></el-icon>{{ $t('common.create') }}</el-button>
    </div>
    <el-card shadow="never">
      <el-table :data="cases" stripe>
        <el-table-column prop="name" label="用例名称" />
        <el-table-column prop="module" label="所属模块" width="150" />
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.priority === 'P0' ? 'danger' : row.priority === 'P1' ? 'warning' : 'info'" size="small">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default>
            <el-tag type="success" size="small">通过</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default>
            <el-button link type="primary" size="small">编辑</el-button>
            <el-button link type="success" size="small">执行</el-button>
            <el-button link type="danger" size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
const cases = [
  { id: 1, name: '登录页面-正常登录', module: '登录模块', priority: 'P0', status: 'passed' },
  { id: 2, name: '登录页面-错误密码', module: '登录模块', priority: 'P1', status: 'passed' },
  { id: 3, name: '首页-导航菜单检查', module: '首页', priority: 'P1', status: 'passed' },
  { id: 4, name: '需求管理-创建需求', module: '需求管理', priority: 'P0', status: 'passed' },
  { id: 5, name: '系统管理-用户管理', module: '系统管理', priority: 'P2', status: 'passed' },
]
</script>

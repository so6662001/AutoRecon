<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-logo">
      <span v-if="!collapsed" class="logo-text">提货通</span>
      <span v-else class="logo-icon">提</span>
    </div>
    <el-scrollbar class="sidebar-menu-wrap">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="#001529"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#fff"
        router
      >
        <!-- 工作台 -->
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <!-- 合同管理 -->
        <el-sub-menu index="contract">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>合同管理</span>
          </template>
          <el-menu-item index="/contract/list">合同列表</el-menu-item>
        </el-sub-menu>

        <!-- 提货管理 -->
        <el-sub-menu index="pickup">
          <template #title>
            <el-icon><List /></el-icon>
            <span>提货管理</span>
          </template>
          <el-menu-item index="/pickup/list">提货单列表</el-menu-item>
          <el-menu-item index="/dispatch/manage">派车管理</el-menu-item>
        </el-sub-menu>

        <!-- 结算与证据 -->
        <el-sub-menu index="settlement">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>结算与证据</span>
          </template>
          <el-menu-item index="/settlement/list">结算列表</el-menu-item>
        </el-sub-menu>

        <!-- 确权管理 -->
        <el-sub-menu index="verification">
          <template #title>
            <el-icon><CircleCheck /></el-icon>
            <span>确权管理</span>
          </template>
          <el-menu-item index="/authorized-persons">授权提货人</el-menu-item>
          <el-menu-item index="/verification/records">确权记录</el-menu-item>
          <el-menu-item index="/trading-habits">交易习惯</el-menu-item>
        </el-sub-menu>

        <!-- 仓库与物流 -->
        <el-sub-menu index="warehouse">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>仓库与物流</span>
          </template>
          <el-menu-item index="/warehouse/manage">仓库管理</el-menu-item>
          <el-menu-item index="/carrier/manage">承运公司</el-menu-item>
        </el-sub-menu>

        <!-- 系统设置 -->
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/template/manage">合同模板</el-menu-item>
          <el-menu-item index="/timeout-config">确认时效</el-menu-item>
          <el-menu-item index="/supplement/manage">事后补录</el-menu-item>
          <el-menu-item index="/system/data-auth">
            <el-icon><Lock /></el-icon>
            <template #title>数据授权</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Odometer,
  Document,
  List,
  Money,
  CircleCheck,
  OfficeBuilding,
  Setting,
  Lock,
} from '@element-plus/icons-vue'

defineProps<{
  collapsed: boolean
}>()

const route = useRoute()

const activeMenu = computed(() => {
  const { path } = route
  if (path.startsWith('/contract/') && !path.endsWith('/list')) {
    return '/contract/list'
  }
  if (path.startsWith('/pickup/')) {
    if (path.includes('/delivery')) return '/pickup/list'
    return '/pickup/list'
  }
  if (path.startsWith('/settlement/') && !path.endsWith('/list')) {
    return '/settlement/list'
  }
  return path
})
</script>

<style lang="scss" scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background-color: #001529;
  z-index: 1001;
  transition: width 0.28s;

  &.collapsed {
    width: 64px;
  }
}

.sidebar-logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.2);
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.logo-icon {
  color: #fff;
  font-size: 20px;
  font-weight: 600;
}

.sidebar-menu-wrap {
  height: calc(100vh - var(--header-height));
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
}
</style>

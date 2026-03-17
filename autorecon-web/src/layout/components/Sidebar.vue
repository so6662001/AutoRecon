<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-logo">
      <span v-if="!collapsed" class="logo-text">自动对账</span>
      <span v-else class="logo-icon">对</span>
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

        <!-- 对账管理 -->
        <el-sub-menu index="recon">
          <template #title>
            <el-icon><List /></el-icon>
            <span>对账管理</span>
          </template>
          <el-menu-item index="/recon/bills">对账单列表</el-menu-item>
          <el-menu-item index="/recon/bills/create">发起对账</el-menu-item>
          <el-menu-item index="/recon/batch">批量对账</el-menu-item>
          <el-menu-item index="/recon/match/0">比对引擎</el-menu-item>
        </el-sub-menu>

        <!-- 异议处理 -->
        <el-sub-menu index="disputes">
          <template #title>
            <el-icon><Warning /></el-icon>
            <span>异议处理</span>
          </template>
          <el-menu-item index="/recon/disputes">异议列表</el-menu-item>
        </el-sub-menu>

        <!-- 签章管理 -->
        <el-sub-menu index="sign">
          <template #title>
            <el-icon><Stamp /></el-icon>
            <span>签章管理</span>
          </template>
          <el-menu-item index="/recon/sign/pending">待签章</el-menu-item>
          <el-menu-item index="/recon/sign/seals">印章管理</el-menu-item>
        </el-sub-menu>

        <!-- 模板管理 -->
        <el-menu-item index="/recon/templates">
          <el-icon><Document /></el-icon>
          <template #title>模板管理</template>
        </el-menu-item>

        <!-- 财务管理 -->
        <el-sub-menu index="finance">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>财务管理</span>
          </template>
          <el-menu-item index="/recon/payments">付款管理</el-menu-item>
          <el-menu-item index="/recon/invoices">发票管理</el-menu-item>
          <el-menu-item index="/recon/collection">催收管理</el-menu-item>
          <el-menu-item index="/recon/credit">信用评分</el-menu-item>
        </el-sub-menu>

        <!-- 合同与融资 -->
        <el-sub-menu index="contract-finance">
          <template #title>
            <el-icon><Notebook /></el-icon>
            <span>合同与融资</span>
          </template>
          <el-menu-item index="/recon/contracts">合同管理</el-menu-item>
          <el-menu-item index="/recon/finance">融资管理</el-menu-item>
          <el-menu-item index="/recon/tri-match/0">账票款匹配</el-menu-item>
        </el-sub-menu>

        <!-- 自动化 -->
        <el-sub-menu index="auto">
          <template #title>
            <el-icon><Timer /></el-icon>
            <span>自动化</span>
          </template>
          <el-menu-item index="/recon/auto-plans">自动对账</el-menu-item>
          <el-menu-item index="/recon/calendar">对账日历</el-menu-item>
          <el-menu-item index="/system/subscriptions">提醒订阅</el-menu-item>
        </el-sub-menu>

        <!-- 买方引导 -->
        <el-menu-item index="/engagement">
          <el-icon><Guide /></el-icon>
          <template #title>买方引导</template>
        </el-menu-item>

        <!-- 系统设置 -->
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/system/erp">ERP配置</el-menu-item>
          <el-menu-item index="/system/buyer-config">买方配置</el-menu-item>
          <el-menu-item index="/system/users">用户管理</el-menu-item>
          <el-menu-item index="/system/billing">计费管理</el-menu-item>
          <el-menu-item index="/system/enterprise">企业信息</el-menu-item>
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
  List,
  Warning,
  Stamp,
  Document,
  Money,
  Notebook,
  Timer,
  Guide,
  Setting,
} from '@element-plus/icons-vue'

defineProps<{
  collapsed: boolean
}>()

const route = useRoute()

const activeMenu = computed(() => {
  const { path } = route
  // For nested routes like /recon/bills/:id, highlight parent
  if (path.startsWith('/recon/bills/') && path !== '/recon/bills/create') {
    return '/recon/bills'
  }
  if (path.startsWith('/recon/disputes/')) {
    return '/recon/disputes'
  }
  if (path.startsWith('/recon/match/')) {
    return '/recon/match/0'
  }
  if (path.startsWith('/recon/tri-match/')) {
    return '/recon/tri-match/0'
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

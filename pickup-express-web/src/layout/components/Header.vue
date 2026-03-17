<template>
  <header class="header">
    <div class="header-left">
      <el-button
        :icon="collapsed ? Expand : Fold"
        text
        class="collapse-btn"
        @click="toggleCollapse"
      />
      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          v-for="(item, index) in breadcrumbs"
          :key="index"
          :to="index < breadcrumbs.length - 1 ? item.path : undefined"
        >
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <span class="enterprise-name" v-if="userStore.userInfo?.enterpriseName">
        {{ userStore.userInfo.enterpriseName }}
      </span>
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-area">
          <el-avatar :size="32" class="user-avatar">
            {{ userDisplayName }}
          </el-avatar>
          <span class="user-name">{{
            userStore.userInfo?.realName || userStore.userInfo?.username || '用户'
          }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人信息
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Fold, Expand, ArrowDown, User, Lock, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRoute } from 'vue-router'

const props = defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  (e: 'update:collapsed', value: boolean): void
}>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = computed({
  get: () => props.collapsed,
  set: (v) => emit('update:collapsed', v),
})

const userDisplayName = computed(() => {
  const info = userStore.userInfo
  if (!info) return '?'
  return (info.realName || info.username || '?').charAt(0).toUpperCase()
})

const breadcrumbs = computed(() => {
  const matched = route.matched.filter((r) => r.meta?.title)
  return matched.map((r) => ({
    path: r.path,
    title: r.meta.title as string,
  }))
})

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

function handleCommand(cmd: string) {
  switch (cmd) {
    case 'profile':
      break
    case 'password':
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style lang="scss" scoped>
.header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
}

.enterprise-name {
  margin-right: 16px;
  color: #666;
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-avatar {
  background: var(--el-color-primary);
}

.user-name {
  font-size: 14px;
  color: #333;
}
</style>

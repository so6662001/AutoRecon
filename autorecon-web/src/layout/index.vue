<template>
  <div class="layout-container">
    <Sidebar :collapsed="collapsed" />
    <div class="layout-main" :class="{ 'sidebar-collapsed': collapsed }">
      <Header v-model:collapsed="collapsed" />
      <main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import { useUserStore } from '@/stores/user'

const collapsed = ref(false)
const userStore = useUserStore()

onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.getUserInfo().catch(() => {})
  }
})
</script>

<style lang="scss" scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-left: var(--sidebar-width);
  transition: margin-left 0.28s;

  &.sidebar-collapsed {
    margin-left: 64px;
  }
}

.layout-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #f5f7fa;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<template>
  <div class="layout-container">
    <div v-if="isMobile && sidebarOpen && embedConfig.showSidebar" class="sidebar-overlay" @click="sidebarOpen = false" />
    <Sidebar v-if="embedConfig.showSidebar" :collapsed="collapsed" :class="{ 'mobile-open': isMobile && sidebarOpen, 'mobile-hidden': isMobile && !sidebarOpen }" />
    <div class="layout-main" :class="{ 'sidebar-collapsed': collapsed && !isMobile, 'no-sidebar': !embedConfig.showSidebar || isMobile, 'no-header': !embedConfig.showHeader }">
      <Header v-if="embedConfig.showHeader" v-model:collapsed="collapsed" :is-mobile="isMobile" @toggle-sidebar="toggleMobileSidebar" />
      <main class="layout-content" :class="{ 'mobile-content': isMobile }">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
    <AgreementDialog
      v-model="showAgreementDialog"
      :agreements="pendingAgreements"
      @all-confirmed="showAgreementDialog = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import AgreementDialog from '@/components/AgreementDialog.vue'
import { useUserStore } from '@/stores/user'
import { getEmbedConfig } from '@/config/embed'
import { checkAgreementStatus } from '@/api/system'

const embedConfig = computed(() => getEmbedConfig())
const collapsed = ref(false)
const isMobile = ref(false)
const sidebarOpen = ref(false)
const route = useRoute()
const userStore = useUserStore()

function checkMobile() {
  isMobile.value = typeof window !== 'undefined' && window.innerWidth <= 768
  if (isMobile.value) {
    collapsed.value = false
    sidebarOpen.value = false
  }
}

function toggleMobileSidebar() {
  if (isMobile.value) {
    sidebarOpen.value = !sidebarOpen.value
  } else {
    collapsed.value = !collapsed.value
  }
}

watch(() => route.path, () => {
  if (isMobile.value) sidebarOpen.value = false
})

const showAgreementDialog = ref(false)
const pendingAgreements = ref<
  {
    id: number
    agreementType: number
    versionNo: string
    title: string
    content: string
    summary: string
  }[]
>([])

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)

  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch {
      // ignore
    }
  }

  if (userStore.token) {
    try {
      type AgreementStatus = {
        needConfirm?: boolean
        unconfirmedAgreements?: typeof pendingAgreements.value
      }
      const res = await checkAgreementStatus()
      const status = (res as { data?: AgreementStatus }).data ?? (res as AgreementStatus)
      if (status?.needConfirm && status.unconfirmedAgreements && status.unconfirmedAgreements.length > 0) {
        pendingAgreements.value = status.unconfirmedAgreements
        showAgreementDialog.value = true
      }
    } catch {
      // Ignore in demo mode
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style lang="scss" scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
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

  &.no-sidebar {
    margin-left: 0;
  }
}

.layout-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #f5f7fa;

  &.mobile-content {
    padding: 12px;
  }
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

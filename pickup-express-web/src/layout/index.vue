<template>
  <div class="layout-container">
    <Sidebar v-if="embedConfig.showSidebar" :collapsed="collapsed" />
    <div class="layout-main" :class="{ 'sidebar-collapsed': collapsed, 'no-sidebar': !embedConfig.showSidebar, 'no-header': !embedConfig.showHeader }">
      <Header v-if="embedConfig.showHeader" v-model:collapsed="collapsed" />
      <main class="layout-content">
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
import { ref, onMounted, computed } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import AgreementDialog from '@/components/AgreementDialog.vue'
import { useUserStore } from '@/stores/user'
import { getEmbedConfig } from '@/config/embed'
import { checkAgreementStatus } from '@/api/system'

const embedConfig = computed(() => getEmbedConfig())
const collapsed = ref(false)
const userStore = useUserStore()

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

  &.no-sidebar {
    margin-left: 0;
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

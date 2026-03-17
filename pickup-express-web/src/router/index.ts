import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'

NProgress.configure({ showSpinner: false })

const routes = [
  // Public routes
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/buyer/:token',
    name: 'buyer',
    component: () => import('@/views/buyer/index.vue'),
    meta: { title: '买方查看', public: true },
  },

  // Layout routes (require auth)
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' },
      },
      {
        path: 'contract/list',
        name: 'contractList',
        component: () => import('@/views/contract/list.vue'),
        meta: { title: '合同列表', icon: 'Document' },
      },
      {
        path: 'contract/:id',
        name: 'contractDetail',
        component: () => import('@/views/contract/detail.vue'),
        meta: { title: '合同详情' },
      },
      {
        path: 'pickup/list',
        name: 'pickupList',
        component: () => import('@/views/pickup/list.vue'),
        meta: { title: '提货单列表', icon: 'List' },
      },
      {
        path: 'pickup/:id',
        name: 'pickupDetail',
        component: () => import('@/views/pickup/detail.vue'),
        meta: { title: '提货单详情' },
      },
      {
        path: 'pickup/:id/delivery',
        name: 'pickupDelivery',
        component: () => import('@/views/pickup/delivery.vue'),
        meta: { title: '发货进度' },
      },
      {
        path: 'dispatch/manage',
        name: 'dispatchManage',
        component: () => import('@/views/dispatch/manage.vue'),
        meta: { title: '派车管理', icon: 'Van' },
      },
      {
        path: 'settlement/list',
        name: 'settlementList',
        component: () => import('@/views/settlement/list.vue'),
        meta: { title: '结算列表', icon: 'Money' },
      },
      {
        path: 'settlement/:id',
        name: 'settlementDetail',
        component: () => import('@/views/settlement/detail.vue'),
        meta: { title: '结算详情' },
      },
      {
        path: 'evidence/:pickupOrderId',
        name: 'evidence',
        component: () => import('@/views/evidence/index.vue'),
        meta: { title: '证据包查看' },
      },
      {
        path: 'warehouse/manage',
        name: 'warehouseManage',
        component: () => import('@/views/warehouse/manage.vue'),
        meta: { title: '仓库管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'carrier/manage',
        name: 'carrierManage',
        component: () => import('@/views/carrier/manage.vue'),
        meta: { title: '承运公司管理', icon: 'Truck' },
      },
      {
        path: 'template/manage',
        name: 'templateManage',
        component: () => import('@/views/template/manage.vue'),
        meta: { title: '合同模板管理', icon: 'DocumentCopy' },
      },
      {
        path: 'authorized-persons',
        name: 'authorizedPersons',
        component: () => import('@/views/authorized-person/index.vue'),
        meta: { title: '授权提货人', icon: 'User' },
      },
      {
        path: 'timeout-config',
        name: 'timeoutConfig',
        component: () => import('@/views/timeout-config/index.vue'),
        meta: { title: '确认时效配置', icon: 'Timer' },
      },
      {
        path: 'trading-habits',
        name: 'tradingHabits',
        component: () => import('@/views/trading-habit/index.vue'),
        meta: { title: '交易习惯', icon: 'TrendCharts' },
      },
      {
        path: 'supplement/manage',
        name: 'supplementManage',
        component: () => import('@/views/supplement/manage.vue'),
        meta: { title: '事后补录', icon: 'EditPen' },
      },
      {
        path: 'verification/records',
        name: 'verificationRecords',
        component: () => import('@/views/verification/records.vue'),
        meta: { title: '确权记录', icon: 'CircleCheck' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  const token = userStore.token
  const isPublic = to.meta.public === true

  if (!token && !isPublic && to.path !== '/login') {
    next({ path: '/login', query: { redirect: to.fullPath } })
    NProgress.done()
    return
  }
  if (token && !isPublic && to.path !== '/login') {
    if (!userStore.userInfo) {
      try {
        await userStore.getUserInfo()
      } catch {
        // Continue
      }
    }
  }
  next()
})

router.afterEach((to) => {
  NProgress.done()
  const title = (to.meta.title as string) || '提货通'
  document.title = `${title} - 提货通`
})

export default router

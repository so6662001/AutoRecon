import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'

NProgress.configure({ showSpinner: false })

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // Public routes
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/guest/:token',
      name: 'guest',
      component: () => import('@/views/guest/index.vue'),
      meta: { title: '对账单查看', public: true },
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
        // 对账管理
        {
          path: 'recon/bills',
          name: 'billList',
          component: () => import('@/views/recon/bill/list.vue'),
          meta: { title: '对账单列表', icon: 'List' },
        },
        {
          path: 'recon/bills/create',
          name: 'billCreate',
          component: () => import('@/views/recon/bill/create.vue'),
          meta: { title: '发起对账' },
        },
        {
          path: 'recon/bills/:id',
          name: 'billDetail',
          component: () => import('@/views/recon/bill/detail.vue'),
          meta: { title: '对账单详情' },
        },
        {
          path: 'recon/batch',
          name: 'batchRecon',
          component: () => import('@/views/recon/batch/index.vue'),
          meta: { title: '批量对账' },
        },
        {
          path: 'recon/match/:billId',
          name: 'matchResult',
          component: () => import('@/views/recon/match/index.vue'),
          meta: { title: '比对结果' },
        },
        // 异议处理
        {
          path: 'recon/disputes',
          name: 'disputeList',
          component: () => import('@/views/recon/dispute/list.vue'),
          meta: { title: '异议列表', icon: 'Warning' },
        },
        {
          path: 'recon/disputes/:id',
          name: 'disputeDetail',
          component: () => import('@/views/recon/dispute/detail.vue'),
          meta: { title: '异议详情' },
        },
        // 签章管理
        {
          path: 'recon/sign/pending',
          name: 'signPending',
          component: () => import('@/views/recon/sign/pending.vue'),
          meta: { title: '待签章', icon: 'Stamp' },
        },
        {
          path: 'recon/sign/seals',
          name: 'sealManage',
          component: () => import('@/views/recon/sign/seals.vue'),
          meta: { title: '印章管理' },
        },
        // 模板管理
        {
          path: 'recon/templates',
          name: 'templateList',
          component: () => import('@/views/recon/template/list.vue'),
          meta: { title: '模板管理', icon: 'Document' },
        },
        // 付款管理
        {
          path: 'recon/payments',
          name: 'paymentList',
          component: () => import('@/views/recon/payment/list.vue'),
          meta: { title: '付款管理', icon: 'Money' },
        },
        // 催收管理
        {
          path: 'recon/collection',
          name: 'collectionList',
          component: () => import('@/views/recon/collection/list.vue'),
          meta: { title: '催收管理', icon: 'Bell' },
        },
        {
          path: 'recon/credit',
          name: 'creditScore',
          component: () => import('@/views/recon/credit/index.vue'),
          meta: { title: '信用评分' },
        },
        // 发票管理
        {
          path: 'recon/invoices',
          name: 'invoiceList',
          component: () => import('@/views/recon/invoice/list.vue'),
          meta: { title: '发票管理', icon: 'Tickets' },
        },
        {
          path: 'recon/tri-match/:billId',
          name: 'triMatch',
          component: () => import('@/views/recon/invoice/tri-match.vue'),
          meta: { title: '账票款匹配' },
        },
        // 合同关联
        {
          path: 'recon/contracts',
          name: 'contractList',
          component: () => import('@/views/recon/contract/list.vue'),
          meta: { title: '合同管理', icon: 'Notebook' },
        },
        // 融资管理
        {
          path: 'recon/finance',
          name: 'financeList',
          component: () => import('@/views/recon/finance/list.vue'),
          meta: { title: '融资管理', icon: 'TrendCharts' },
        },
        // 自动对账
        {
          path: 'recon/auto-plans',
          name: 'autoPlanList',
          component: () => import('@/views/recon/auto-plan/list.vue'),
          meta: { title: '自动对账', icon: 'Timer' },
        },
        // 对账日历
        {
          path: 'recon/calendar',
          name: 'reconCalendar',
          component: () => import('@/views/recon/calendar/index.vue'),
          meta: { title: '对账日历', icon: 'Calendar' },
        },
        // 买方引导
        {
          path: 'engagement',
          name: 'engagementList',
          component: () => import('@/views/engagement/list.vue'),
          meta: { title: '买方引导', icon: 'Guide' },
        },
        // 系统设置
        {
          path: 'system/erp',
          name: 'erpConfig',
          component: () => import('@/views/system/erp.vue'),
          meta: { title: 'ERP配置', icon: 'Connection' },
        },
        {
          path: 'system/buyer-config',
          name: 'buyerConfig',
          component: () => import('@/views/system/buyer-config.vue'),
          meta: { title: '买方配置' },
        },
        {
          path: 'system/users',
          name: 'userManage',
          component: () => import('@/views/system/users.vue'),
          meta: { title: '用户管理', icon: 'User' },
        },
        {
          path: 'system/subscriptions',
          name: 'subscriptionConfig',
          component: () => import('@/views/system/subscriptions.vue'),
          meta: { title: '提醒订阅' },
        },
        {
          path: 'system/billing',
          name: 'billingManage',
          component: () => import('@/views/system/billing.vue'),
          meta: { title: '计费管理', icon: 'Wallet' },
        },
        {
          path: 'system/enterprise',
          name: 'enterpriseInfo',
          component: () => import('@/views/system/enterprise.vue'),
          meta: { title: '企业信息' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  const token = userStore.token
  const isPublic = to.meta.public === true

  if (!token && !isPublic && to.path !== '/login') {
    next({ path: '/login', query: { redirect: to.fullPath } })
    NProgress.done()
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
  const title = (router.currentRoute.value.meta.title as string) || '自动对账'
  document.title = `${title} - 自动对账`
})

export default router

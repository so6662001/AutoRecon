<template>
  <div class="page-container pickup-list">
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="提货单号">
        <el-input v-model="searchForm.pickupOrderNo" placeholder="请输入提货单号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="合同号">
        <el-input v-model="searchForm.contractNo" placeholder="请输入合同号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="待派车" value="待派车" />
          <el-option label="已派车" value="已派车" />
          <el-option label="已到达" value="已到达" />
          <el-option label="发货中" value="发货中" />
          <el-option label="已完成" value="已完成" />
          <el-option label="已取消" value="已取消" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="pickupOrderNo" label="提货单号" width="160" />
      <el-table-column prop="contractNo" label="合同号" width="140" />
      <el-table-column prop="buyerName" label="客户" min-width="120" />
      <el-table-column prop="pickupCode" label="提货码" width="100" />
      <el-table-column prop="plateNo" label="车牌号" width="100" />
      <el-table-column prop="driverName" label="驾驶员" width="100" />
      <el-table-column prop="deliveryMode" label="发货模式" width="100">
        <template #default="{ row }">
          <el-tag :type="(deliveryModeTag(row.deliveryMode) || undefined) as any" size="small" :class="{ 'tag-purple': row.deliveryMode === '驾驶员' }">
            {{ row.deliveryMode || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deliveryStatus" label="发货状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(deliveryStatusTag(row.deliveryStatus) || undefined) as any" size="small">
            {{ row.deliveryStatus || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalWeight" label="总重量" width="100" align="right">
        <template #default="{ row }">{{ formatNum(row.totalWeight) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="总金额" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="settlementStatus" label="结算状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(settlementTag(row.settlementStatus) || undefined) as any" size="small">
            {{ row.settlementStatus || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewDetail(row)">查看</el-button>
          <el-button
            v-if="canCancel(row)"
            type="danger"
            link
            size="small"
            @click="handleCancel(row)"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @size-change="loadData"
      @current-change="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { queryPickupOrders, cancelPickupOrder } from '@/api/evidence'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])

const searchForm = reactive({
  pickupOrderNo: '',
  contractNo: '',
  status: '',
  dateRange: [] as string[],
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

function deliveryModeTag(mode: string) {
  // WMS(blue)/H5(green)/第三方(orange)/驾驶员(purple)/补录(gray)
  const map: Record<string, string> = {
    WMS: '',
    H5: 'success',
    第三方: 'warning',
    驾驶员: '',
    补录: 'info',
  }
  return map[mode] ?? 'info'
}

function deliveryStatusTag(status: string) {
  const map: Record<string, string> = {
    待派车: 'info',
    已派车: '',
    发货中: '',
    已完成: 'success',
    已取消: 'danger',
  }
  return map[status] ?? ''
}

function settlementTag(status: string) {
  const map: Record<string, string> = {
    待结算: 'warning',
    已结算: 'success',
    已结清: 'info',
  }
  return map[status] ?? ''
}

function canCancel(row: any) {
  const s = row.deliveryStatus ?? row.status
  return s !== '已完成' && s !== '已取消'
}

function formatNum(val: number | string) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | string) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (searchForm.pickupOrderNo) params.pickupOrderNo = searchForm.pickupOrderNo
    if (searchForm.contractNo) params.contractNo = searchForm.contractNo
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }

    const res = (await queryPickupOrders(params)) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    const total = data?.total ?? list.length

    tableData.value = Array.isArray(list)
      ? list.map((r: any) => ({
          id: r.id,
          pickupOrderNo: r.pickupOrderNo ?? r.pickup_order_no ?? '-',
          contractNo: r.contractNo ?? r.contract_no ?? '-',
          buyerName: r.buyerName ?? r.buyer_name ?? '-',
          pickupCode: r.pickupCode ?? r.pickup_code ?? '-',
          plateNo: r.plateNo ?? r.plate_no ?? '-',
          driverName: r.driverName ?? r.driver_name ?? '-',
          deliveryMode: r.deliveryMode ?? r.delivery_mode ?? '-',
          deliveryStatus: r.deliveryStatus ?? r.delivery_status ?? r.status ?? '-',
          totalWeight: r.totalWeight ?? r.total_weight ?? 0,
          totalAmount: r.totalAmount ?? r.total_amount ?? 0,
          settlementStatus: r.settlementStatus ?? r.settlement_status ?? '-',
          createdAt: r.createdAt ?? r.created_at ?? '-',
        }))
      : []

    pagination.total = total
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.pickupOrderNo = ''
  searchForm.contractNo = ''
  searchForm.status = ''
  searchForm.dateRange = []
  pagination.page = 1
  loadData()
}

function viewDetail(row: any) {
  router.push({ name: 'pickupDetail', params: { id: String(row.id) } })
}

async function handleCancel(row: any) {
  try {
    await cancelPickupOrder(row.id)
    loadData()
  } catch {
    // error handled by request interceptor
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.pickup-list {
  .search-form {
    margin-bottom: 16px;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }

  :deep(.tag-purple) {
    --el-tag-bg-color: #9c27b0;
    --el-tag-border-color: #9c27b0;
    --el-tag-text-color: #fff;
  }
}
</style>

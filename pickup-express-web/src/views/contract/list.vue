<template>
  <div class="page-container contract-list">
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="合同号">
        <el-input v-model="searchForm.contractNo" placeholder="请输入合同号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="客户">
        <el-input v-model="searchForm.buyerName" placeholder="请输入客户" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="合同类型">
        <el-select v-model="searchForm.contractType" placeholder="请选择" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="留货" value="留货" />
          <el-option label="订货" value="订货" />
          <el-option label="框架" value="框架" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="可提货" value="可提货" />
          <el-option label="待签约" value="待签约" />
          <el-option label="已签约" value="已签约" />
          <el-option label="提货中" value="提货中" />
          <el-option label="已提完" value="已提完" />
          <el-option label="已结清" value="已结清" />
          <el-option label="已关闭" value="已关闭" />
        </el-select>
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
      <el-table-column prop="contractNo" label="合同号" width="140" />
      <el-table-column prop="contractType" label="合同类型" width="100">
        <template #default="{ row }">
          <el-tag :type="(contractTypeTag(row.contractType) || undefined) as any" size="small">
            {{ row.contractType || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="buyerName" label="客户名称" min-width="140" />
      <el-table-column prop="productSummary" label="品规简要" min-width="160" show-overflow-tooltip />
      <el-table-column prop="totalQuantity" label="合同数量" width="100" align="right">
        <template #default="{ row }">{{ formatNum(row.totalQuantity) }}</template>
      </el-table-column>
      <el-table-column prop="totalWeight" label="合同重量(吨)" width="120" align="right">
        <template #default="{ row }">{{ formatNum(row.totalWeight) }}</template>
      </el-table-column>
      <el-table-column prop="pickedWeight" label="已提重量" width="100" align="right">
        <template #default="{ row }">{{ formatNum(row.pickedWeight) }}</template>
      </el-table-column>
      <el-table-column prop="remainingWeight" label="未提重量" width="100" align="right">
        <template #default="{ row }">{{ formatNum(row.remainingWeight) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="合同金额" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="paidAmount" label="已付金额" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.paidAmount) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(statusTagType(row.status) || undefined) as any" size="small">
            {{ row.status || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewDetail(row)">查看</el-button>
          <el-button
            v-if="canInitiateSign(row)"
            type="primary"
            link
            size="small"
            @click="initiateSign(row)"
          >
            发起签约
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
import { queryContracts, initiateSign as apiInitiateSign } from '@/api/evidence'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])

const searchForm = reactive({
  contractNo: '',
  buyerName: '',
  contractType: '',
  status: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

function contractTypeTag(type: string) {
  if (type === '留货') return 'success'
  if (type === '订货') return ''
  return 'info'
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    可提货: 'success',
    待签约: 'warning',
    提货中: '',
    已结清: 'info',
    已提完: 'info',
    已签约: 'info',
    已关闭: 'info',
  }
  return map[status] ?? ''
}

function canInitiateSign(row: any) {
  return row.contractType === '订货' && row.status === '待签约'
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
    if (searchForm.contractNo) params.contractNo = searchForm.contractNo
    if (searchForm.buyerName) params.buyerName = searchForm.buyerName
    if (searchForm.contractType) params.contractType = searchForm.contractType
    if (searchForm.status) params.status = searchForm.status

    const res = (await queryContracts(params)) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    const total = data?.total ?? list.length

    tableData.value = Array.isArray(list)
      ? list.map((r: any) => ({
          id: r.id,
          contractNo: r.contractNo ?? r.contract_no ?? '-',
          contractType: r.contractType ?? r.contract_type ?? '-',
          buyerName: r.buyerName ?? r.buyer_name ?? '-',
          productSummary: r.productSummary ?? r.product_summary ?? r.items?.[0]?.productName ?? '-',
          totalQuantity: r.totalQuantity ?? r.total_quantity ?? 0,
          totalWeight: r.totalWeight ?? r.total_weight ?? 0,
          pickedWeight: r.pickedWeight ?? r.picked_weight ?? 0,
          remainingWeight: r.remainingWeight ?? r.remaining_weight ?? 0,
          totalAmount: r.totalAmount ?? r.total_amount ?? 0,
          paidAmount: r.paidAmount ?? r.paid_amount ?? 0,
          status: r.status ?? r.contractStatus ?? '-',
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
  searchForm.contractNo = ''
  searchForm.buyerName = ''
  searchForm.contractType = ''
  searchForm.status = ''
  pagination.page = 1
  loadData()
}

function viewDetail(row: any) {
  router.push({ name: 'contractDetail', params: { id: String(row.id) } })
}

async function initiateSign(row: any) {
  try {
    await apiInitiateSign(row.id)
    loadData()
  } catch {
    // error handled by request interceptor
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.contract-list {
  .search-form {
    margin-bottom: 16px;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>

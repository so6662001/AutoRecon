<template>
  <div class="dispute-list-page">
    <h2 class="page-title">异议列表</h2>

    <div class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="对账单号">
          <el-input
            v-model="searchForm.billNo"
            placeholder="请输入对账单号"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已升级" value="ESCALATED" />
          </el-select>
        </el-form-item>
        <el-form-item label="异议类型">
          <el-select v-model="searchForm.disputeType" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="重量差异" value="WEIGHT" />
            <el-option label="金额差异" value="AMOUNT" />
            <el-option label="数量差异" value="QUANTITY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="billNo" label="对账单号" min-width="140" />
        <el-table-column prop="productName" label="品名" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="100" />
        <el-table-column prop="disputeType" label="异议类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ getDisputeTypeText(row.disputeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="raisedBy" label="提出方" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="!['RESOLVED', 'ESCALATED'].includes(row.status)"
              type="warning"
              link
              size="small"
              @click="handleProcess(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listDisputes } from '@/api/recon'

interface DisputeItem {
  id: number
  billNo: string
  productName?: string
  spec?: string
  disputeType: string
  description?: string
  raisedBy?: string
  status: string
  createdAt: string
}

const router = useRouter()
const loading = ref(false)
const list = ref<DisputeItem[]>([])

const searchForm = reactive({
  billNo: '',
  status: '',
  disputeType: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

function getDisputeTypeText(type: string): string {
  const map: Record<string, string> = {
    WEIGHT: '重量差异',
    AMOUNT: '金额差异',
    QUANTITY: '数量差异',
    OTHER: '其他',
  }
  return map[type] ?? type
}

function getStatusTagType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    PROCESSING: '',
    RESOLVED: 'success',
    ESCALATED: 'danger',
  }
  return map[status] ?? 'info'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    ESCALATED: '已升级',
  }
  return map[status] ?? status
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  searchForm.billNo = ''
  searchForm.status = ''
  searchForm.disputeType = ''
  pagination.page = 1
  fetchList()
}

function handleView(row: DisputeItem) {
  router.push({ name: 'disputeDetail', params: { id: String(row.id) } })
}

function handleProcess(row: DisputeItem) {
  router.push({ name: 'disputeDetail', params: { id: String(row.id) } })
}

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (searchForm.billNo) params.billNo = searchForm.billNo
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.disputeType) params.disputeType = searchForm.disputeType

    const res = await listDisputes(params) as { list?: DisputeItem[]; total?: number }
    list.value = res?.list ?? []
    pagination.total = res?.total ?? 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.dispute-list-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .search-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .table-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>

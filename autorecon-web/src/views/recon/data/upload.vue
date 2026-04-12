<template>
  <div class="page-container">
    <h2>买方数据上传</h2>
    <p class="page-desc">通过 Excel 或在线填写提交买方侧数据，用于与卖方数据比对</p>

    <el-form label-width="100px" class="bill-row">
      <el-form-item label="对账单" required>
        <el-select
          v-model="billId"
          filterable
          placeholder="选择对账单"
          style="width: 360px"
          :loading="billLoading"
          @visible-change="(v: boolean) => v && loadBillOptions()"
        >
          <el-option
            v-for="b in billOptions"
            :key="b.id"
            :label="`${b.billNo} · ${b.buyerName ?? ''}`"
            :value="b.id"
          />
        </el-select>
        <span v-if="billDetail?.buyerId" class="hint">买方ID: {{ billDetail.buyerId }}</span>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="Excel上传" name="excel">
        <el-upload
          class="upload-block"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls,.csv"
          :on-change="onFileChange"
          :on-remove="() => (excelFile = null)"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 .xlsx / .xls / .csv，最大 10MB</div>
          </template>
        </el-upload>

        <div class="actions">
          <el-button type="primary" :disabled="!canAnalyze" :loading="analyzing" @click="runAnalyze">
            分析表头
          </el-button>
          <el-button
            type="success"
            :disabled="!canImport"
            :loading="importing"
            @click="runImportWithMapping"
          >
            确认映射并导入
          </el-button>
          <el-checkbox v-model="saveMappingFlag" :disabled="!resolvedBuyerId">保存映射到买方配置</el-checkbox>
        </div>

        <el-card v-if="previewRows.length" class="preview-card" shadow="never">
          <template #header>解析预览（最近导入）</template>
          <el-table :data="previewRows" size="small" max-height="360" stripe>
            <el-table-column prop="deliveryNo" label="送货单号" width="120" />
            <el-table-column prop="orderNo" label="订单号" width="120" />
            <el-table-column prop="productName" label="品名" min-width="120" />
            <el-table-column prop="spec" label="规格" width="100" />
            <el-table-column label="买方数量" width="100">
              <template #default="{ row }">{{ row.buyerQuantity ?? row.quantity }}</template>
            </el-table-column>
            <el-table-column label="买方重量" width="100">
              <template #default="{ row }">{{ row.buyerWeight ?? row.weight }}</template>
            </el-table-column>
            <el-table-column label="买方金额" width="120">
              <template #default="{ row }">{{ row.buyerAmount ?? row.amount }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="在线填写" name="online">
        <div class="online-toolbar">
          <el-button size="small" @click="addOnlineRow">新增一行</el-button>
          <el-button type="primary" :loading="onlineSaving" :disabled="!billId" @click="submitOnline">
            提交买方数据
          </el-button>
        </div>
        <el-table :data="onlineRows" border size="small" max-height="420">
          <el-table-column label="合同号" min-width="110">
            <template #default="{ row }"><el-input v-model="row.contractNo" /></template>
          </el-table-column>
          <el-table-column label="订单号" min-width="110">
            <template #default="{ row }"><el-input v-model="row.orderNo" /></template>
          </el-table-column>
          <el-table-column label="送货单号" min-width="110">
            <template #default="{ row }"><el-input v-model="row.deliveryNo" /></template>
          </el-table-column>
          <el-table-column label="品名" min-width="120">
            <template #default="{ row }"><el-input v-model="row.productName" /></template>
          </el-table-column>
          <el-table-column label="规格" width="100">
            <template #default="{ row }"><el-input v-model="row.spec" /></template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template #default="{ row }"><el-input-number v-model="row.quantity" :controls="false" class="w-full" /></template>
          </el-table-column>
          <el-table-column label="重量" width="100">
            <template #default="{ row }"><el-input-number v-model="row.weight" :controls="false" class="w-full" /></template>
          </el-table-column>
          <el-table-column label="金额" width="110">
            <template #default="{ row }"><el-input-number v-model="row.totalAmount" :controls="false" class="w-full" /></template>
          </el-table-column>
          <el-table-column label="交货日" width="130">
            <template #default="{ row }">
              <el-date-picker v-model="row.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column fixed="right" label="" width="60">
            <template #default="{ $index }">
              <el-button link type="danger" @click="onlineRows.splice($index, 1)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="mappingVisible" title="字段映射" width="720px" @closed="analysis = null">
      <p v-if="analysis?.autoMapped" class="tip">已根据历史映射自动匹配，可直接确认导入。</p>
      <el-table v-if="analysis" :data="mappingTableRows" border size="small">
        <el-table-column prop="excelCol" label="Excel 列" min-width="160" />
        <el-table-column label="映射到系统字段" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.systemField" clearable filterable placeholder="不导入" style="width: 100%">
              <el-option v-for="opt in systemFieldOptions" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="mappingVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmMapping">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  queryBills,
  getBillDetail,
  analyzeExcelHeaders,
  uploadExcelWithMapping,
  onlineSubmitBuyerData,
} from '@/api/recon'

interface BillOpt {
  id: number
  billNo: string
  buyerName?: string
}

interface BillDetail {
  id: number
  buyerId?: number
  buyerName?: string
}

interface AnalysisVo {
  headers?: string[]
  suggestedMapping?: Record<string, string>
  savedMapping?: Record<string, string>
  autoMapped?: boolean
  unmappedHeaders?: string[]
  systemFields?: Record<string, string[]>
}

interface PreviewItem {
  deliveryNo?: string
  orderNo?: string
  productName?: string
  spec?: string
  quantity?: number
  weight?: number
  amount?: number
  buyerQuantity?: number
  buyerWeight?: number
  buyerAmount?: number
}

interface MappingRow {
  excelCol: string
  systemField: string
}

interface OnlineRow {
  contractNo: string
  orderNo: string
  deliveryNo: string
  productName: string
  spec: string
  quantity: number | undefined
  weight: number | undefined
  totalAmount: number | undefined
  deliveryDate: string
}

const activeTab = ref('excel')
const billLoading = ref(false)
const billOptions = ref<BillOpt[]>([])
const billId = ref<number | undefined>()
const billDetail = ref<BillDetail | null>(null)

const excelFile = ref<File | null>(null)
const analyzing = ref(false)
const importing = ref(false)
const analysis = ref<AnalysisVo | null>(null)
const mappingDraft = ref<Record<string, string>>({})
const mappingVisible = ref(false)
const saveMappingFlag = ref(false)
const previewRows = ref<PreviewItem[]>([])

const onlineRows = ref<OnlineRow[]>([])
const onlineSaving = ref(false)

const resolvedBuyerId = computed(() => billDetail.value?.buyerId)

const canAnalyze = computed(() => billId.value && excelFile.value && resolvedBuyerId.value)
const canImport = computed(() => billId.value && excelFile.value && Object.keys(mappingDraft.value).length > 0)

const systemFieldOptions = computed(() => {
  const sf = analysis.value?.systemFields
  if (!sf) return [] as string[]
  return Object.keys(sf)
})

const mappingTableRows = computed((): MappingRow[] => {
  const headers = analysis.value?.headers ?? []
  return headers.map((h) => ({
    excelCol: h,
    systemField: mappingDraft.value[h] ?? '',
  }))
})

function unwrap<T>(res: unknown): T | undefined {
  if (res && typeof res === 'object' && 'data' in res) {
    return (res as { data: T }).data
  }
  return res as T
}

async function loadBillOptions() {
  billLoading.value = true
  try {
    const res = await queryBills({ page: 1, pageSize: 200 })
    const data = res as { list?: BillOpt[] }
    billOptions.value = data?.list ?? []
  } catch {
    billOptions.value = []
  } finally {
    billLoading.value = false
  }
}

async function loadBillDetail() {
  if (!billId.value) {
    billDetail.value = null
    return
  }
  try {
    const res = await getBillDetail(billId.value)
    billDetail.value = unwrap<BillDetail>(res) ?? (res as BillDetail)
  } catch {
    billDetail.value = null
  }
}

watch(billId, () => {
  loadBillDetail()
})

function onFileChange(uploadFile: { raw?: File }) {
  excelFile.value = uploadFile?.raw ?? null
}

async function runAnalyze() {
  if (!canAnalyze.value || !excelFile.value || !resolvedBuyerId.value) {
    ElMessage.warning('请选择对账单并上传文件')
    return
  }
  analyzing.value = true
  try {
    const res = await analyzeExcelHeaders(resolvedBuyerId.value, excelFile.value)
    const vo = unwrap<AnalysisVo>(res) ?? (res as AnalysisVo)
    analysis.value = vo
    const base = { ...(vo.savedMapping ?? {}), ...(vo.suggestedMapping ?? {}) }
    mappingDraft.value = { ...base }
    mappingVisible.value = true
  } catch {
    /* */
  } finally {
    analyzing.value = false
  }
}

function confirmMapping() {
  const next: Record<string, string> = {}
  for (const row of mappingTableRows.value) {
    if (row.systemField) {
      next[row.excelCol] = row.systemField
    }
  }
  mappingDraft.value = next
  mappingVisible.value = false
  ElMessage.success('映射已更新，可点击「确认映射并导入」')
}

function buildMappingJson() {
  const o: Record<string, string> = {}
  for (const [k, v] of Object.entries(mappingDraft.value)) {
    if (v) o[k] = v
  }
  return JSON.stringify(o)
}

async function runImportWithMapping() {
  if (!billId.value || !excelFile.value) {
    ElMessage.warning('请选择对账单并上传文件')
    return
  }
  if (!Object.keys(mappingDraft.value).length) {
    ElMessage.warning('请先分析表头并配置字段映射')
    return
  }
  importing.value = true
  try {
    const res = await uploadExcelWithMapping(
      billId.value,
      excelFile.value,
      buildMappingJson(),
      saveMappingFlag.value,
      resolvedBuyerId.value
    )
    const list = unwrap<PreviewItem[]>(res) ?? (res as PreviewItem[])
    previewRows.value = Array.isArray(list) ? list : []
    ElMessage.success(`已导入 ${previewRows.value.length} 条`)
  } catch {
    /* */
  } finally {
    importing.value = false
  }
}

function emptyOnlineRow(): OnlineRow {
  return {
    contractNo: '',
    orderNo: '',
    deliveryNo: '',
    productName: '',
    spec: '',
    quantity: undefined,
    weight: undefined,
    totalAmount: undefined,
    deliveryDate: '',
  }
}

function addOnlineRow() {
  onlineRows.value.push(emptyOnlineRow())
}

async function submitOnline() {
  if (!billId.value) {
    ElMessage.warning('请选择对账单')
    return
  }
  const buyerItems = onlineRows.value
    .filter((r) => r.deliveryNo || r.orderNo || r.productName)
    .map((r) => ({
      contractNo: r.contractNo || undefined,
      orderNo: r.orderNo || undefined,
      deliveryNo: r.deliveryNo || undefined,
      productName: r.productName || undefined,
      spec: r.spec || undefined,
      quantity: r.quantity,
      weight: r.weight,
      totalAmount: r.totalAmount,
      deliveryDate: r.deliveryDate || undefined,
    }))
  if (!buyerItems.length) {
    ElMessage.warning('请至少填写一行有效数据（建议填写送货单号或订单号）')
    return
  }
  onlineSaving.value = true
  try {
    await onlineSubmitBuyerData({ billId: billId.value, buyerItems })
    ElMessage.success('提交成功')
  } catch {
    /* */
  } finally {
    onlineSaving.value = false
  }
}

loadBillOptions()
</script>

<style scoped lang="scss">
.page-desc {
  color: #999;
  margin-bottom: 16px;
}
.bill-row {
  margin-bottom: 8px;
}
.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}
.upload-block {
  max-width: 520px;
}
.actions {
  margin: 16px 0;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.preview-card {
  margin-top: 16px;
}
.online-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 12px;
}
.w-full {
  width: 100%;
}
.tip {
  color: #67c23a;
  margin-bottom: 12px;
}
</style>

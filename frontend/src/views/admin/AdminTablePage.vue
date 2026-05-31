<script lang="ts">
export type InventoryPayloadInput = {
  visitDate?: unknown;
  session?: unknown;
  ticketTypeCode?: unknown;
  code?: unknown;
  dailyCapacity?: unknown;
  dailyRemaining?: unknown;
  capacity?: unknown;
  remaining?: unknown;
};

export function buildInventoryPayload(row: InventoryPayloadInput) {
  return {
    visitDate: String(row.visitDate ?? new Date().toISOString().slice(0, 10)),
    session: String(row.session ?? 'AM'),
    ticketTypeCode: String(row.ticketTypeCode ?? row.code ?? ''),
    dailyCapacity: Number(row.dailyCapacity ?? row.capacity ?? 0),
    dailyRemaining: Number(row.dailyRemaining ?? row.remaining ?? 0),
    capacity: Number(row.capacity ?? 0),
    remaining: Number(row.remaining ?? 0),
  };
}
</script>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { Download, Filter, Plus, RefreshCw, Search } from 'lucide-vue-next';
import { adminApi } from '@/api/modules';
import { toast } from '@/utils/message';
import { getAdminModule } from './adminModules';
import type { AdminColumn, AdminFormField, AdminRowAction } from './adminModules';

const route = useRoute();
const loading = ref(false);
const drawerVisible = ref(false);
const inventoryDialogVisible = ref(false);
const manualCheckinVisible = ref(false);
const editingRecord = ref<Record<string, unknown> | null>(null);
const activeInventoryRow = ref<Record<string, unknown> | null>(null);
const formModel = reactive<Record<string, unknown>>({});
const inventoryForm = reactive({ visitDate: '', session: 'AM', ticketTypeCode: '', dailyCapacity: 0, dailyRemaining: 0, capacity: 0, remaining: 0 });
const manualCheckinForm = reactive({ orderNo: '', phone: '', checkerId: 1, remark: '后台人工核销' });
const filters = reactive<Record<string, string>>({});

const domain = computed(() => route.meta.domain as string);
const moduleConfig = computed(() => getAdminModule(domain.value));
const records = ref<Record<string, unknown>[]>([]);
const allRecords = ref<Record<string, unknown>[]>([]);
const selectedRows = ref<Record<string, unknown>[]>([]);
const activeMarketingType = ref<'COUPON' | 'NOTICE'>('COUPON');

const couponColumns: AdminColumn[] = [
  { prop: 'name', label: '优惠券名称', minWidth: 170 },
  { prop: 'discountType', label: '优惠类型', width: 110 },
  { prop: 'discountValue', label: '优惠值', width: 100 },
  { prop: 'thresholdAmount', label: '门槛', width: 100 },
  { prop: 'totalQuantity', label: '总库存', width: 100 },
  { prop: 'claimed', label: '已领取', width: 100 },
  { prop: 'validTo', label: '有效期至', width: 120 },
  { prop: 'scope', label: '适用范围', width: 110 },
  { prop: 'status', label: '状态', width: 110, status: true },
];

const noticeColumns: AdminColumn[] = [
  { prop: 'name', label: '公告标题', minWidth: 170 },
  { prop: 'description', label: '公告内容', minWidth: 220 },
  { prop: 'displayPosition', label: '展示位置', width: 120 },
  { prop: 'priority', label: '优先级', width: 90 },
  { prop: 'period', label: '发布时间', width: 170 },
  { prop: 'status', label: '状态', width: 110, status: true },
];

const couponFormFields: AdminFormField[] = [
  { key: 'resourceType', label: '类型', type: 'select', options: ['COUPON'] },
  { key: 'name', label: '优惠券名称', type: 'text' },
  { key: 'discountType', label: '优惠类型', type: 'select', options: ['AMOUNT', 'PERCENT'] },
  { key: 'discountValue', label: '优惠值', type: 'number' },
  { key: 'thresholdAmount', label: '使用门槛', type: 'number' },
  { key: 'totalQuantity', label: '发放库存', type: 'number' },
  { key: 'validFrom', label: '有效期开始', type: 'date' },
  { key: 'validTo', label: '有效期结束', type: 'date' },
  { key: 'scope', label: '适用范围', type: 'select', options: ['TICKET'] },
  { key: 'status', label: '状态', type: 'select', options: ['ENABLED', 'DISABLED'] },
  { key: 'description', label: '规则说明', type: 'textarea' },
];

const noticeFormFields: AdminFormField[] = [
  { key: 'resourceType', label: '类型', type: 'select', options: ['NOTICE'] },
  { key: 'name', label: '公告标题', type: 'text' },
  { key: 'displayPosition', label: '展示位置', type: 'select', options: ['ALL', 'HOME', 'MEMBER'] },
  { key: 'priority', label: '优先级', type: 'number' },
  { key: 'status', label: '状态', type: 'select', options: ['PUBLISHED', 'DRAFT', 'DISABLED'] },
  { key: 'description', label: '公告内容', type: 'textarea' },
];

const activeColumns = computed(() => {
  if (domain.value !== 'marketing') return moduleConfig.value.columns;
  return activeMarketingType.value === 'COUPON' ? couponColumns : noticeColumns;
});
const activeFormFields = computed(() => {
  if (domain.value !== 'marketing') return moduleConfig.value.formFields;
  return activeMarketingType.value === 'COUPON' ? couponFormFields : noticeFormFields;
});
const visibleRecords = computed(() => {
  if (domain.value !== 'marketing') return records.value;
  return records.value.filter((record) => record.resourceType === activeMarketingType.value);
});

const optionLabels: Record<string, string> = {
  ALL: '全站',
  HOME: '首页公告',
  MEMBER: '会员消息',
  COUPON: '优惠券',
  NOTICE: '公告',
  AMOUNT: '满减',
  PERCENT: '折扣',
  TICKET: '门票预约',
  ENABLED: '启用',
  DISABLED: '停用',
  PUBLISHED: '已发布',
  DRAFT: '草稿',
};

function displayText(value: unknown) {
  return optionLabels[String(value)] ?? String(value ?? '');
}

function resetData() {
  records.value = [];
  allRecords.value = [];
  selectedRows.value = [];
  for (const filter of moduleConfig.value.filters) {
    filters[filter.key] = filter.key === 'session' ? 'AM' : (filter.key === 'visitDate' ? '2026-06-01' : '');
  }
}

function queryParams() {
  const params: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(filters)) {
    if (value && ['visitDate', 'session'].includes(key)) params[key] = value;
  }
  return params;
}

async function loadRecords() {
  loading.value = true;
  try {
    const page = await adminApi.records(domain.value, queryParams());
    allRecords.value = page.records.map((record) => ({ ...(record as Record<string, unknown>) }));
    applyFilters();
  } finally {
    loading.value = false;
  }
}

function statusType(status: unknown) {
  const text = String(status);
  if (['ENABLED', 'PUBLISHED', 'VISIBLE', 'PAID', 'CHECKED_IN', 'PAY_SUCCESS', 'REFUNDED'].includes(text)) return 'success';
  if (['PENDING_PAYMENT', 'UNPAID', 'DRAFT', 'NOT_CHECKED', 'REFUNDING'].includes(text)) return 'warning';
  if (['DISABLED', 'EXCEPTION', 'LOCKED', 'CANCELLED', 'HIDDEN'].includes(text)) return 'danger';
  return 'info';
}

function applyFilters() {
  const keyword = filters.keyword?.trim();
  const status = filters.status;
  records.value = allRecords.value.filter((record) => {
    const text = Object.values(record).join(' ');
    const keywordMatched = !keyword || text.includes(keyword);
    const statusMatched = !status || record.status === status || record.paymentStatus === status;
    return keywordMatched && statusMatched;
  });
}

function resetFilters() {
  resetData();
  void loadRecords();
}

function openCreate() {
  if (!moduleConfig.value.canCreate) return;
  editingRecord.value = null;
  for (const field of activeFormFields.value) {
    formModel[field.key] = defaultValueFor(field);
  }
  if (domain.value === 'marketing') formModel.resourceType = activeMarketingType.value;
  drawerVisible.value = true;
}

function openEdit(row: Record<string, unknown>) {
  if (!moduleConfig.value.canEdit && domain.value !== 'orders' && domain.value !== 'checkins') return;
  editingRecord.value = row;
  for (const field of activeFormFields.value) {
    formModel[field.key] = row[field.key] ?? defaultValueFor(field);
  }
  drawerVisible.value = true;
}

function defaultValueFor(field: AdminFormField) {
  if (field.type === 'number') return 0;
  if (field.type === 'datetime') return new Date().toISOString().slice(0, 16).replace('T', ' ');
  if (field.key === 'resourceType') return activeMarketingType.value;
  if (field.key === 'discountType') return 'AMOUNT';
  if (field.key === 'scope') return 'TICKET';
  if (field.key === 'displayPosition') return 'HOME';
  return '';
}

async function submitForm() {
  const payload: Record<string, unknown> = {};
  for (const field of activeFormFields.value) {
    payload[field.key] = formModel[field.key];
  }
  if (domain.value === 'marketing') payload.resourceType = activeMarketingType.value;
  if (editingRecord.value && moduleConfig.value.canEdit) {
    await adminApi.update(domain.value, Number(editingRecord.value.id), payload);
  } else if (moduleConfig.value.canCreate) {
    await adminApi.create(domain.value, payload);
  }
  drawerVisible.value = false;
  await loadRecords();
  toast.success(editingRecord.value ? '记录已更新' : '记录已新增');
}

function placeholderFor(field: AdminFormField) {
  return `请输入${field.label}`;
}

function openInventory(row: Record<string, unknown>) {
  activeInventoryRow.value = row;
  const payload = buildInventoryPayload(row);
  Object.assign(inventoryForm, payload);
  inventoryDialogVisible.value = true;
}

async function saveInventory() {
  await adminApi.updateInventory(buildInventoryPayload(inventoryForm));
  inventoryDialogVisible.value = false;
  await loadRecords();
  toast.success('库存已更新');
}

function nextStatus(row: Record<string, unknown>) {
  const current = String(row.status ?? '');
  if (domain.value === 'activities') return current === 'PUBLISHED' ? 'DISABLED' : 'PUBLISHED';
  if (domain.value === 'animals') return current === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE';
  if (domain.value === 'marketing') return current === 'ENABLED' || current === 'PUBLISHED' ? 'DISABLED' : (row.resourceType === 'NOTICE' ? 'PUBLISHED' : 'ENABLED');
  return current === 'ENABLED' ? 'DISABLED' : 'ENABLED';
}

async function toggleRowStatus(row: Record<string, unknown>) {
  await adminApi.toggleStatusWithPayload(domain.value, Number(row.id), {
    status: nextStatus(row),
    resourceType: row.resourceType,
    type: row.type,
  });
  await loadRecords();
  toast.success('状态已更新');
}

async function approveRefund(row: Record<string, unknown>) {
  if (!row.refundId) {
    toast.warning('该订单暂无可审核退款');
    return;
  }
  await adminApi.approveRefund(Number(row.refundId));
  await loadRecords();
  toast.success('退款已审核');
}

function openManualCheckin() {
  manualCheckinVisible.value = true;
}

async function submitManualCheckin() {
  await adminApi.manualCheckin({ ...manualCheckinForm });
  manualCheckinVisible.value = false;
  await loadRecords();
  toast.success('人工核销成功');
}

async function handleRowAction(action: AdminRowAction, row: Record<string, unknown>) {
  if (action.key === 'edit' || action.key === 'view') {
    openEdit(row);
    return;
  }
  if (action.key === 'inventory') {
    openInventory(row);
    return;
  }
  if (action.key === 'toggleStatus') {
    await toggleRowStatus(row);
    return;
  }
  if (action.key === 'approveRefund') {
    await approveRefund(row);
  }
}

function exportRecords() {
  const columns = [{ prop: 'id', label: 'ID' }, ...moduleConfig.value.columns];
  const csv = [
    columns.map((column) => column.label).join(','),
    ...records.value.map((record) => columns.map((column) => JSON.stringify(record[column.prop] ?? '')).join(',')),
  ].join('\n');
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${domain.value}-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
  toast.success('已导出当前筛选结果');
}

watch(domain, () => {
  activeMarketingType.value = 'COUPON';
  resetData();
  void loadRecords();
}, { immediate: true });
</script>

<template>
  <div class="admin-page">
    <div class="admin-module-hero">
      <div class="admin-module-title">
        <span class="admin-module-icon">
          <component :is="moduleConfig.icon" :size="22" />
        </span>
        <div>
          <h1>{{ moduleConfig.title }}</h1>
          <p>{{ moduleConfig.subtitle }}</p>
        </div>
      </div>
      <div class="admin-module-actions">
        <el-button v-if="domain === 'checkins'" type="primary" @click="openManualCheckin">人工核销</el-button>
        <el-button @click="exportRecords">
          <Download :size="16" />
          导出
        </el-button>
        <el-button v-if="moduleConfig.canCreate" type="primary" @click="openCreate">
          <Plus :size="16" />
          {{ moduleConfig.primaryAction }}
        </el-button>
      </div>
    </div>

    <div class="admin-stat-grid">
      <article v-for="metric in moduleConfig.metrics" :key="metric.label">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.trend }}</small>
      </article>
    </div>

    <el-tabs v-if="domain === 'marketing'" v-model="activeMarketingType" class="marketing-tabs">
      <el-tab-pane label="优惠券活动" name="COUPON" />
      <el-tab-pane label="公告发布" name="NOTICE" />
    </el-tabs>

    <section class="admin-filter-panel">
      <div class="panel-label">
        <Filter :size="16" />
        筛选条件
      </div>
      <el-form inline>
        <el-form-item v-for="filter in moduleConfig.filters" :key="filter.key" :label="filter.label">
          <el-input v-if="filter.type === 'text'" v-model="filters[filter.key]" :placeholder="filter.placeholder" clearable />
          <el-select v-else-if="filter.type === 'select'" v-model="filters[filter.key]" :placeholder="filter.placeholder ?? '请选择'" clearable style="width: 150px">
            <el-option v-for="option in filter.options" :key="option" :label="displayText(option)" :value="option" />
          </el-select>
          <el-date-picker v-else v-model="filters[filter.key]" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="domain === 'tickets' ? loadRecords() : applyFilters()">
            <Search :size="16" />
            查询
          </el-button>
          <el-button @click="resetFilters">
            <RefreshCw :size="16" />
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="admin-data-panel">
      <div class="table-toolbar">
        <span>共 {{ visibleRecords.length }} 条记录</span>
        <span v-if="selectedRows.length">已选择 {{ selectedRows.length }} 条</span>
      </div>
      <el-table v-loading="loading" :data="visibleRecords" stripe border @selection-change="(rows: Record<string, unknown>[]) => (selectedRows = rows)">
        <el-table-column type="selection" width="44" />
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column v-for="column in activeColumns" :key="column.prop" :prop="column.prop" :label="column.label" :width="column.width" :min-width="column.minWidth">
          <template #default="{ row }">
            <el-tag v-if="column.status" :type="statusType(row[column.prop])">{{ displayText(row[column.prop]) }}</el-tag>
            <span v-else>{{ displayText(row[column.prop]) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button v-for="action in moduleConfig.rowActions" :key="action.key" link :type="action.variant ?? 'primary'" @click="handleRowAction(action, row)">
              {{ action.label }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="admin-pagination">
        <span>第 1 页 / 每页 10 条</span>
        <el-pagination layout="prev, pager, next" :total="visibleRecords.length" />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="editingRecord ? '记录详情/编辑' : moduleConfig.primaryAction" size="420px">
      <el-form label-position="top">
        <el-form-item v-for="field in activeFormFields" :key="field.key" :label="field.label">
          <el-input v-if="field.type === 'text'" v-model="formModel[field.key]" :disabled="editingRecord && !moduleConfig.canEdit" :placeholder="placeholderFor(field)" />
          <el-input-number v-else-if="field.type === 'number'" v-model="formModel[field.key] as number" :disabled="editingRecord && !moduleConfig.canEdit" :min="0" />
          <el-select v-else-if="field.type === 'select'" v-model="formModel[field.key]" :disabled="editingRecord && !moduleConfig.canEdit" placeholder="请选择">
            <el-option v-for="option in field.options" :key="option" :label="displayText(option)" :value="option" />
          </el-select>
          <el-date-picker v-else-if="field.type === 'date'" v-model="formModel[field.key]" :disabled="editingRecord && !moduleConfig.canEdit" value-format="YYYY-MM-DD" type="date" />
          <el-date-picker v-else-if="field.type === 'datetime'" v-model="formModel[field.key]" :disabled="editingRecord && !moduleConfig.canEdit" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" />
          <el-input v-else v-model="formModel[field.key]" :disabled="editingRecord && !moduleConfig.canEdit" type="textarea" :rows="4" :placeholder="placeholderFor(field)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button v-if="moduleConfig.canCreate || moduleConfig.canEdit" type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="inventoryDialogVisible" title="调整库存" width="420px">
      <el-form label-position="top">
        <el-form-item label="库存日期">
          <el-date-picker v-model="inventoryForm.visitDate" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item label="场次">
          <el-select v-model="inventoryForm.session">
            <el-option label="上午" value="AM" />
            <el-option label="下午" value="PM" />
          </el-select>
        </el-form-item>
        <el-form-item label="票种编码">
          <el-input v-model="inventoryForm.ticketTypeCode" disabled />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="inventoryForm.dailyCapacity" :min="0" />
        </el-form-item>
        <el-form-item label="当日总剩余">
          <el-input-number v-model="inventoryForm.dailyRemaining" :min="0" :max="inventoryForm.dailyCapacity" />
        </el-form-item>
        <el-form-item label="场次容量">
          <el-input-number v-model="inventoryForm.capacity" :min="0" />
        </el-form-item>
        <el-form-item label="剩余库存">
          <el-input-number v-model="inventoryForm.remaining" :min="0" :max="inventoryForm.capacity" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inventoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveInventory">保存库存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="manualCheckinVisible" title="人工核销" width="420px">
      <el-form label-position="top">
        <el-form-item label="订单号">
          <el-input v-model="manualCheckinForm.orderNo" placeholder="输入订单号" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="manualCheckinForm.phone" placeholder="订单号为空时按手机号查找" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="manualCheckinForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualCheckinVisible = false">取消</el-button>
        <el-button type="primary" @click="submitManualCheckin">确认核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

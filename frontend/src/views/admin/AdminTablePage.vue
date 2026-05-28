<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { Download, Filter, Plus, RefreshCw, Search } from 'lucide-vue-next';
import { adminApi } from '@/api/modules';
import { toast } from '@/utils/message';
import { getAdminModule } from './adminModules';
import type { AdminFormField } from './adminModules';

const route = useRoute();
const loading = ref(false);
const drawerVisible = ref(false);
const inventoryDialogVisible = ref(false);
const editingRecord = ref<Record<string, unknown> | null>(null);
const activeInventoryRow = ref<Record<string, unknown> | null>(null);
const formModel = reactive<Record<string, unknown>>({});
const inventoryForm = reactive({ capacity: 0, remaining: 0 });
const filters = reactive<Record<string, string>>({});

const domain = computed(() => route.meta.domain as string);
const moduleConfig = computed(() => getAdminModule(domain.value));
const records = ref<Record<string, unknown>[]>([]);
const allRecords = ref<Record<string, unknown>[]>([]);
const selectedRows = ref<Record<string, unknown>[]>([]);

function resetData() {
  records.value = [];
  allRecords.value = [];
  selectedRows.value = [];
  for (const filter of moduleConfig.value.filters) {
    filters[filter.key] = '';
  }
}

async function loadRecords() {
  loading.value = true;
  try {
    const page = await adminApi.records(domain.value);
    allRecords.value = page.records.map((record) => ({ ...(record as Record<string, unknown>) }));
    records.value = allRecords.value.map((record) => ({ ...record }));
  } finally {
    loading.value = false;
  }
}

function statusType(status: unknown) {
  const text = String(status);
  if (['ENABLED', 'PUBLISHED', 'VISIBLE', 'PAID', 'CHECKED_IN', 'PAY_SUCCESS', '投放中', '启用', '展示中'].includes(text)) return 'success';
  if (['PENDING_PAYMENT', 'UNPAID', 'DRAFT', 'NOT_CHECKED', '待支付', '草稿', '未核销'].includes(text)) return 'warning';
  if (['DISABLED', 'EXCEPTION', 'REFUNDING', 'LOCKED', '停用', '异常核销', '退款中'].includes(text)) return 'danger';
  return 'info';
}

function applyFilters() {
  const keyword = filters.keyword?.trim();
  const status = filters.status;
  records.value = allRecords.value.filter((record) => {
    const text = Object.values(record).join(' ');
    const keywordMatched = !keyword || text.includes(keyword);
    const statusMatched = !status || record.status === status;
    return keywordMatched && statusMatched;
  });
}

function resetFilters() {
  resetData();
  void loadRecords();
}

function openCreate() {
  editingRecord.value = null;
  for (const field of moduleConfig.value.formFields) {
    formModel[field.key] = field.type === 'number' ? 0 : '';
  }
  drawerVisible.value = true;
}

function openEdit(row: Record<string, unknown>) {
  editingRecord.value = row;
  for (const field of moduleConfig.value.formFields) {
    formModel[field.key] = row[field.key] ?? '';
  }
  drawerVisible.value = true;
}

async function submitForm() {
  const payload: Record<string, unknown> = {};
  for (const field of moduleConfig.value.formFields) {
    payload[field.key] = formModel[field.key];
  }
  if (editingRecord.value) {
    await adminApi.update(domain.value, Number(editingRecord.value.id), payload);
  } else {
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
  inventoryForm.capacity = Number(row.capacity ?? 0);
  inventoryForm.remaining = Number(row.remaining ?? 0);
  inventoryDialogVisible.value = true;
}

async function saveInventory() {
  if (activeInventoryRow.value) {
    await adminApi.updateInventory({
      visitDate: activeInventoryRow.value.visitDate ?? new Date().toISOString().slice(0, 10),
      session: activeInventoryRow.value.session ?? 'AM',
      ticketTypeCode: activeInventoryRow.value.ticketTypeCode ?? activeInventoryRow.value.code,
      capacity: inventoryForm.capacity,
      remaining: inventoryForm.remaining,
    });
  }
  inventoryDialogVisible.value = false;
  await loadRecords();
  toast.success('库存已更新');
}

async function toggleRowStatus(row: Record<string, unknown>) {
  const current = String(row.status ?? '');
  const nextStatus = ['ENABLED', 'PUBLISHED', 'VISIBLE', '启用', '展示中'].includes(current) ? 'DISABLED' : 'ENABLED';
  await adminApi.toggleStatus(domain.value, Number(row.id), nextStatus);
  await loadRecords();
  toast.success(`状态已更新为 ${nextStatus}`);
}

async function handleRowAction(action: string, row: Record<string, unknown>) {
  if (action.includes('编辑') || action.includes('详情')) {
    openEdit(row);
    return;
  }
  if (action.includes('库存')) {
    openInventory(row);
    return;
  }
  if (action.includes('停用') || action.includes('启用')) {
    await toggleRowStatus(row);
    return;
  }
  toast.info(`${action}功能已进入处理队列`);
}

function exportRecords() {
  toast.success(`已生成${moduleConfig.value.title}导出任务`);
}

watch(domain, () => {
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
        <el-button @click="exportRecords">
          <Download :size="16" />
          导出
        </el-button>
        <el-button type="primary" @click="openCreate">
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

    <section class="admin-filter-panel">
      <div class="panel-label">
        <Filter :size="16" />
        筛选条件
      </div>
      <el-form inline>
        <el-form-item v-for="filter in moduleConfig.filters" :key="filter.key" :label="filter.label">
          <el-input
            v-if="filter.type === 'text'"
            v-model="filters[filter.key]"
            :placeholder="filter.placeholder"
            clearable
          />
          <el-select
            v-else-if="filter.type === 'select'"
            v-model="filters[filter.key]"
            :placeholder="filter.placeholder ?? '请选择'"
            clearable
            style="width: 150px"
          >
            <el-option v-for="option in filter.options" :key="option" :label="option" :value="option" />
          </el-select>
          <el-date-picker v-else v-model="filters[filter.key]" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilters">
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
        <span>共 {{ records.length }} 条记录</span>
        <span v-if="selectedRows.length">已选择 {{ selectedRows.length }} 条</span>
      </div>
      <el-table
        v-loading="loading"
        :data="records"
        stripe
        border
        @selection-change="(rows: Record<string, unknown>[]) => (selectedRows = rows)"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column
          v-for="column in moduleConfig.columns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth"
        >
          <template #default="{ row }">
            <el-tag v-if="column.status" :type="statusType(row[column.prop])">{{ row[column.prop] }}</el-tag>
            <span v-else>{{ row[column.prop] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button
              v-for="action in moduleConfig.rowActions"
              :key="action"
              link
              :type="action.includes('停用') ? 'danger' : 'primary'"
              @click="handleRowAction(action, row)"
            >
              {{ action }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="admin-pagination">
        <span>第 1 页 / 每页 10 条</span>
        <el-pagination layout="prev, pager, next" :total="records.length" />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="editingRecord ? '编辑记录' : moduleConfig.primaryAction" size="420px">
      <el-form label-position="top">
        <el-form-item v-for="field in moduleConfig.formFields" :key="field.key" :label="field.label">
          <el-input
            v-if="field.type === 'text'"
            v-model="formModel[field.key]"
            :placeholder="placeholderFor(field)"
          />
          <el-input-number v-else-if="field.type === 'number'" v-model="formModel[field.key] as number" :min="0" />
          <el-select v-else-if="field.type === 'select'" v-model="formModel[field.key]" placeholder="请选择">
            <el-option v-for="option in field.options" :key="option" :label="option" :value="option" />
          </el-select>
          <el-date-picker v-else-if="field.type === 'date'" v-model="formModel[field.key]" value-format="YYYY-MM-DD" type="date" />
          <el-input v-else v-model="formModel[field.key]" type="textarea" :rows="4" :placeholder="placeholderFor(field)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="inventoryDialogVisible" title="调整库存" width="420px">
      <el-form label-position="top">
        <el-form-item label="票种">
          <el-input :model-value="activeInventoryRow?.name" disabled />
        </el-form-item>
        <el-form-item label="日容量">
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
  </div>
</template>

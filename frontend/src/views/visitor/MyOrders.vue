<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { annualPassProducts, bookingTickets } from '@/stores/booking';
import { useOrderStore } from '@/stores/order';
import type { OrderItemRecord, OrderRecord } from '@/types/api';
import { orderStatusText, paymentStatusText } from '@/utils/status';
import { toast } from '@/utils/message';

const router = useRouter();
const orderStore = useOrderStore();

const orderTypeText: Record<string, string> = {
  TICKET: '门票预约',
  ANNUAL_PASS: '年卡权益预约',
  ANNUAL_PASS_PURCHASE: '年卡购买',
  ANNUAL_PASS_RENEWAL: '年卡续费',
  ACTIVITY: '活动报名',
};

const sessionText: Record<string, string> = {
  AM: '上午场',
  PM: '下午场',
  ACTIVITY: '活动',
};

interface DisplayPurchaseItem {
  code: string;
  name: string;
  quantity: number;
  unitPrice: number;
  kind: '门票' | '年卡' | '年卡权益' | '活动';
}

onMounted(() => orderStore.loadMine());

async function pay(row: OrderRecord) {
  await orderStore.payOrder(row);
  toast.success('支付成功，入园二维码已生成');
}

async function cancel(row: OrderRecord) {
  await orderStore.cancelOrder(row);
  toast.success('订单已取消，库存已释放');
}

async function refund(row: OrderRecord) {
  await orderStore.refundOrder(row);
  toast.success('退款申请已提交');
}

function showPass(row: OrderRecord) {
  router.push({ name: 'entry-pass', query: { orderId: row.id } });
}

function formatCurrency(value: number | string | undefined) {
  const amount = Number(value ?? 0);
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(amount) ? 0 : 2,
    maximumFractionDigits: 2,
  });
}

function orderKind(row: OrderRecord) {
  if (isAnnualPassAdmission(row)) return orderTypeText.ANNUAL_PASS;
  return orderTypeText[row.orderType] ?? row.orderType ?? '门票预约';
}

function itemName(item: OrderItemRecord) {
  return item.ticketTypeName || item.ticketTypeCode;
}

function isAnnualPassPurchase(row: OrderRecord) {
  const annualPassPrice = annualPassProducts[0]?.price;
  return row.orderType === 'ANNUAL_PASS_PURCHASE'
    || row.orderType === 'ANNUAL_PASS_RENEWAL'
    || (Number(row.amount) === annualPassPrice && row.peopleCount === 1);
}

function isAnnualPassAdmission(row: OrderRecord) {
  return row.orderType === 'ANNUAL_PASS'
    || (
      Number(row.amount) === 0
      && row.peopleCount > 0
      && ['PAID', 'RESERVED', 'CHECKED_IN'].includes(row.orderStatus)
      && row.paymentStatus === 'PAY_SUCCESS'
    );
}

function isAnnualPassItem(row: OrderRecord, item: OrderItemRecord) {
  return isAnnualPassPurchase(row) || item.ticketTypeCode === 'ANNUAL' || itemName(item).includes('年卡');
}

function isActivityItem(item: OrderItemRecord) {
  return item.ticketTypeCode.startsWith('ACTIVITY:');
}

function knownTicket(code: string) {
  return bookingTickets.find((ticket) => ticket.code === code);
}

function knownAnnualPass() {
  return annualPassProducts[0];
}

function inferTicketItems(row: OrderRecord): DisplayPurchaseItem[] {
  const total = Number(row.originalAmount || row.amount || 0);
  const people = Number(row.peopleCount || 0);
  const adult = knownTicket('ADULT');
  const child = knownTicket('CHILD');
  if (!adult || !child || people <= 0 || total <= 0 || adult.price === child.price) {
    return [];
  }

  const adultCount = (total - child.price * people) / (adult.price - child.price);
  if (!Number.isInteger(adultCount) || adultCount < 0 || adultCount > people) {
    return [];
  }

  const childCount = people - adultCount;
  const items: DisplayPurchaseItem[] = [];
  if (adultCount > 0) {
    items.push({ code: adult.code, name: adult.name, quantity: adultCount, unitPrice: adult.price, kind: '门票' });
  }
  if (childCount > 0) {
    items.push({ code: child.code, name: child.name, quantity: childCount, unitPrice: child.price, kind: '门票' });
  }
  return items;
}

function purchaseItems(row: OrderRecord): DisplayPurchaseItem[] {
  if (isAnnualPassAdmission(row)) {
    return [{
      code: 'ANNUAL_PASS_ADMISSION',
      name: '年卡权益入园',
      quantity: row.peopleCount,
      unitPrice: 0,
      kind: '年卡权益',
    }];
  }

  if (row.items?.length) {
    return row.items.map((item) => ({
      code: item.ticketTypeCode,
      name: itemName(item),
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      kind: isActivityItem(item) ? '活动' : isAnnualPassItem(row, item) ? '年卡' : '门票',
    }));
  }

  const annualPass = knownAnnualPass();
  if (annualPass && isAnnualPassPurchase(row)) {
    const name = row.orderType === 'ANNUAL_PASS_RENEWAL' ? `${annualPass.name}续费` : annualPass.name;
    return [{ code: annualPass.code, name, quantity: 1, unitPrice: annualPass.price, kind: '年卡' }];
  }

  const inferred = inferTicketItems(row);
  if (inferred.length) return inferred;

  if (row.peopleCount > 0) {
    return [{ code: 'TICKET', name: '门票', quantity: row.peopleCount, unitPrice: Number(row.originalAmount || row.amount || 0), kind: '门票' }];
  }

  return [];
}

function orderTagType(status: string) {
  if (['PAID', 'RESERVED', 'CHECKED_IN'].includes(status)) return 'success';
  if (['PENDING_PAYMENT', 'REFUNDING'].includes(status)) return 'warning';
  if (['CANCELLED', 'REFUNDED'].includes(status)) return 'info';
  return '';
}

function paymentTagType(status: string) {
  if (status === 'PAY_SUCCESS') return 'success';
  if (['UNPAID', 'PAYING'].includes(status)) return 'warning';
  if (['CLOSED', 'PAY_FAILED'].includes(status)) return 'info';
  return '';
}

function purchaseKey(row: OrderRecord, item: DisplayPurchaseItem) {
  return `${row.id}-${item.code}-${item.quantity}-${item.unitPrice}`;
}
</script>

<template>
  <section class="page-section orders-page">
    <div class="section-heading">
      <p class="eyebrow">Orders</p>
      <h2>我的订单</h2>
      <span>待支付订单可继续支付，已支付订单可查看二维码或申请退款。</span>
    </div>

    <el-table v-loading="orderStore.loading" :data="orderStore.orders" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />

      <el-table-column label="购买信息" min-width="240">
        <template #default="{ row }">
          <div class="purchase-cell">
            <div class="purchase-kind">
              <el-tag size="small" :type="isAnnualPassPurchase(row) ? 'success' : 'info'" effect="plain">
                {{ orderKind(row) }}
              </el-tag>
            </div>
            <div v-if="purchaseItems(row).length" class="purchase-items">
              <div v-for="item in purchaseItems(row)" :key="purchaseKey(row, item)" class="purchase-item">
                <strong>{{ item.name }}</strong>
                <span>{{ item.kind }} · x{{ item.quantity }} · ¥{{ formatCurrency(item.unitPrice) }}</span>
              </div>
            </div>
            <span v-else>暂无明细</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="预约信息" min-width="180">
        <template #default="{ row }">
          <div class="reservation-cell">
            <strong>{{ row.visitDate }}</strong>
            <span>{{ sessionText[row.session] ?? row.session }} · {{ row.peopleCount }}人</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="金额" min-width="170">
        <template #default="{ row }">
          <div class="amount-cell">
            <strong>实付 ¥{{ formatCurrency(row.amount) }}</strong>
            <span v-if="Number(row.discountAmount) > 0">
              原价 ¥{{ formatCurrency(row.originalAmount) }}，优惠 ¥{{ formatCurrency(row.discountAmount) }}
            </span>
            <span v-else>原价 ¥{{ formatCurrency(row.originalAmount || row.amount) }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="状态" min-width="180">
        <template #default="{ row }">
          <div class="status-cell">
            <el-tag size="small" :type="orderTagType(row.orderStatus)">
              {{ orderStatusText[row.orderStatus] ?? row.orderStatus }}
            </el-tag>
            <el-tag size="small" effect="plain" :type="paymentTagType(row.paymentStatus)">
              {{ paymentStatusText[row.paymentStatus] ?? row.paymentStatus }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.paymentStatus === 'UNPAID'" link type="primary" @click="pay(row)">支付</el-button>
          <el-button v-if="row.paymentStatus === 'UNPAID'" link type="danger" @click="cancel(row)">取消</el-button>
          <el-button v-if="row.paymentStatus === 'PAY_SUCCESS'" link type="success" @click="showPass(row)">二维码</el-button>
          <el-button v-if="row.paymentStatus === 'PAY_SUCCESS' && row.orderStatus === 'PAID'" link type="warning" @click="refund(row)">退款</el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无订单">
          <RouterLink class="primary-action" to="/booking">去预约门票</RouterLink>
        </el-empty>
      </template>
    </el-table>
  </section>
</template>

<style scoped>
.orders-page :deep(.el-table) {
  border: 1px solid rgba(22, 163, 74, 0.12);
  border-radius: 8px;
}

.purchase-cell,
.reservation-cell,
.amount-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
  line-height: 1.4;
}

.reservation-cell strong,
.amount-cell strong {
  color: #143923;
  font-weight: 800;
}

.purchase-kind {
  display: flex;
}

.purchase-items {
  display: grid;
  gap: 6px;
}

.purchase-item {
  display: grid;
  gap: 2px;
  padding-left: 10px;
  border-left: 2px solid #bbf7d0;
}

.purchase-item strong {
  color: #143923;
  font-weight: 800;
}

.purchase-cell span,
.reservation-cell span,
.amount-cell span {
  min-width: 0;
  color: #5c7164;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.amount-cell strong {
  color: #f97316;
  font-variant-numeric: tabular-nums;
}

.status-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

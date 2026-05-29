<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { BadgeCheck, CreditCard, Minus, Plus, ShoppingCart } from 'lucide-vue-next';
import { annualPassProducts, bookingTickets } from '@/stores/booking';
import { useBookingStore } from '@/stores/booking';
import { useMemberStore } from '@/stores/member';
import { useOrderStore } from '@/stores/order';
import { toast } from '@/utils/message';

const router = useRouter();
const booking = useBookingStore();
const member = useMemberStore();
const orderStore = useOrderStore();
const active = ref(0);
const couponId = ref<number | undefined>();
const annualPassId = ref<number | undefined>();
const useAnnualPass = ref(false);

const sessionOptions = [
  { label: '上午 08:30-12:00', value: 'AM' },
  { label: '下午 13:30-18:00', value: 'PM' },
];

const orderSummary = computed(() =>
  booking.selectedItems.map((item) => {
    const ticket = bookingTickets.find((entry) => entry.code === item.ticketTypeCode);
    return {
      ...item,
      name: ticket?.name ?? item.ticketTypeCode,
      price: ticket?.price ?? 0,
      subtotal: (ticket?.price ?? 0) * item.quantity,
    };
  }),
);

const selectedCoupon = computed(() => member.coupons.find((coupon) => coupon.id === couponId.value));
const selectedAnnualPass = computed(() => member.annualPasses.find((pass) => pass.id === annualPassId.value));
const payableAmount = computed(() => (useAnnualPass.value ? 0 : booking.totalAmount));

onMounted(async () => {
  await Promise.all([member.loadAll(), booking.loadInventory()]);
  annualPassId.value = member.annualPasses[0]?.id;
});

watch(() => booking.visitDate, async () => {
  await booking.loadInventory();
  active.value = 0;
});

function changeQuantity(ticketCode: string, delta: number) {
  booking.setQuantity(ticketCode, (booking.selectedTickets[ticketCode] ?? 0) + delta);
  active.value = booking.selectedCount > 0 ? 1 : 0;
}

function setQuantity(ticketCode: string, value: number | undefined) {
  booking.setQuantity(ticketCode, value ?? 0);
  active.value = booking.selectedCount > 0 ? 1 : 0;
}

async function switchSession(value: string | number | boolean) {
  booking.setSession(value === 'PM' ? 'PM' : 'AM');
  await booking.loadInventory();
  active.value = 0;
}

async function createOrder() {
  if (booking.selectedCount === 0) {
    toast.warning('请至少选择 1 张门票');
    return;
  }
  const lines = orderSummary.value.map((item) => `${item.name} x ${item.quantity}`).join('，');
  await ElMessageBox.confirm(
    `预约日期：${booking.visitDate}\n预约场次：${booking.session === 'AM' ? '上午' : '下午'}\n票种：${lines}\n应付：￥${payableAmount.value}`,
    '确认预约订单',
    { confirmButtonText: '确认提交', cancelButtonText: '再看看', type: 'success' },
  );
  const order = await orderStore.createReservation({
    visitDate: booking.visitDate,
    session: booking.session,
    items: booking.selectedItems,
    couponId: useAnnualPass.value ? undefined : couponId.value,
    annualPassId: useAnnualPass.value ? annualPassId.value : undefined,
    orderType: useAnnualPass.value ? 'ANNUAL_PASS' : 'TICKET',
  });
  booking.commitOrder();
  active.value = 2;

  if (order.paymentStatus === 'PAY_SUCCESS') {
    toast.success(`订单 ${order.orderNo} 已预约成功`);
    router.push({ name: 'entry-pass', query: { orderId: order.id } });
    return;
  }

  try {
    await ElMessageBox.confirm(`订单 ${order.orderNo} 已提交，是否立即模拟支付？`, '提交成功', {
      confirmButtonText: '立即支付',
      cancelButtonText: '去订单页',
      type: 'success',
    });
    await orderStore.payOrder(order);
    toast.success('支付成功，已生成入园二维码');
    router.push({ name: 'entry-pass', query: { orderId: order.id } });
  } catch {
    router.push({ name: 'my-orders' });
  }
}

async function buyAnnualPass(code: string) {
  const product = annualPassProducts.find((item) => item.code === code);
  if (!product) return;
  await ElMessageBox.confirm(
    `${product.name} 需要先完成支付，支付后再到会员中心绑定游客并启用权益。`,
    '购买年卡',
    { confirmButtonText: '提交购买', cancelButtonText: '再看看', type: 'info' },
  );
  const order = await orderStore.createReservation({
    visitDate: booking.visitDate,
    session: booking.session,
    items: [{ ticketTypeCode: product.code, quantity: 1 }],
    orderType: 'ANNUAL_PASS_PURCHASE',
  });
  try {
    await ElMessageBox.confirm(`年卡订单 ${order.orderNo} 已提交，是否立即模拟支付？`, '提交成功', {
      confirmButtonText: '立即支付',
      cancelButtonText: '去订单页',
      type: 'success',
    });
    await orderStore.payOrder(order);
    toast.success('年卡已购买，前往会员中心绑定游客后生效');
    router.push({ name: 'member-center' });
  } catch {
    router.push({ name: 'my-orders' });
  }
}
</script>

<template>
  <section class="page-section ticket-booking-page">
    <div class="section-heading">
      <p class="eyebrow">Ticket Booking</p>
      <h2>门票预约</h2>
      <span>普通门票按日期和场次预约；年卡购买独立处理，支付并绑定后才可作为权益使用。</span>
    </div>
    <el-steps :active="active" finish-status="success">
      <el-step title="选择日期" />
      <el-step title="确认票种" />
      <el-step title="支付预约" />
    </el-steps>

    <div class="booking-panel">
      <div class="booking-panel-title">
        <div>
          <h3>普通门票预约</h3>
          <p>选择入园日期、场次和票种，库存只统计普通门票。</p>
        </div>
        <span class="panel-chip">当日剩余 {{ booking.dailyRemaining }} 张 / 当前场次剩余 {{ booking.totalRemaining }} 张</span>
      </div>

      <el-form class="booking-form" label-width="88px">
        <el-form-item label="预约日期">
          <el-date-picker v-model="booking.visitDate" class="booking-date-picker" style="width: 150px" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item label="预约场次">
          <el-segmented v-model="booking.session" :options="sessionOptions" @change="switchSession" />
        </el-form-item>
        <el-form-item v-if="!useAnnualPass" label="优惠券">
          <el-select v-model="couponId" clearable placeholder="不使用优惠券" style="width: 220px">
            <el-option
              v-for="coupon in member.coupons.filter((item) => item.status === '可用')"
              :key="coupon.id"
              :label="`${coupon.name} · ${coupon.threshold}`"
              :value="coupon.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="ticket-select-list">
        <article v-for="ticket in bookingTickets" :key="ticket.code" class="ticket-select-card">
          <div>
            <strong>{{ ticket.name }}</strong>
            <span>{{ ticket.description }}</span>
            <b>￥{{ ticket.price }}</b>
          </div>
          <div class="ticket-stock">剩余 {{ booking.currentInventory[ticket.code] ?? 0 }} 张</div>
          <div class="quantity-control">
            <button type="button" :disabled="!booking.selectedTickets[ticket.code]" @click="changeQuantity(ticket.code, -1)">
              <Minus :size="16" />
            </button>
            <el-input-number
              :model-value="booking.selectedTickets[ticket.code] ?? 0"
              :min="0"
              :max="booking.currentInventory[ticket.code] ?? 0"
              controls-position="right"
              @update:model-value="(value: number | undefined) => setQuantity(ticket.code, value)"
            />
            <button type="button" :disabled="(booking.selectedTickets[ticket.code] ?? 0) >= (booking.currentInventory[ticket.code] ?? 0)" @click="changeQuantity(ticket.code, 1)">
              <Plus :size="16" />
            </button>
          </div>
        </article>
      </div>

      <section class="annual-benefit-panel">
        <div>
          <strong><BadgeCheck :size="18" /> 已有年卡权益</strong>
          <span>已有生效年卡时，可用年卡完成本次预约；购买中的年卡不会自动启用。</span>
        </div>
        <div class="annual-benefit-controls">
          <el-switch v-model="useAnnualPass" :disabled="!member.annualPasses.length" />
          <el-select v-if="useAnnualPass" v-model="annualPassId" placeholder="选择年卡" style="width: 190px">
            <el-option v-for="pass in member.annualPasses" :key="pass.id" :label="pass.name" :value="pass.id" />
          </el-select>
        </div>
      </section>

      <section class="order-summary-panel">
        <div>
          <strong>订单确认</strong>
          <span v-if="!orderSummary.length">请选择需要预约的票种</span>
          <span v-else>{{ orderSummary.map((item) => `${item.name} x${item.quantity}`).join('，') }}</span>
          <span v-if="selectedCoupon">已选 {{ selectedCoupon.name }}</span>
          <span v-if="useAnnualPass">使用 {{ selectedAnnualPass?.name ?? '年卡' }} 权益预约，本单免支付</span>
        </div>
        <div class="order-total">
          <span>共 {{ booking.selectedCount }} 张</span>
          <strong>￥{{ payableAmount }}</strong>
        </div>
      </section>

      <el-button type="success" size="large" :disabled="booking.selectedCount === 0" @click="createOrder">
        <ShoppingCart :size="18" />
        提交预约订单
      </el-button>
    </div>

    <div class="booking-panel annual-pass-shop">
      <div class="booking-panel-title">
        <div>
          <h3>年卡购买</h3>
          <p>年卡是独立商品，购买支付后需到会员中心绑定游客，完成后才会显示为可用权益。</p>
        </div>
        <span class="panel-chip muted">独立购买</span>
      </div>
      <div class="annual-pass-list">
        <article v-for="product in annualPassProducts" :key="product.code" class="annual-pass-card">
          <div>
            <CreditCard :size="24" />
            <strong>{{ product.name }}</strong>
            <span>{{ product.description }}</span>
          </div>
          <b>￥{{ product.price }}</b>
          <el-button type="primary" plain @click="buyAnnualPass(product.code)">购买年卡</el-button>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ticket-booking-page {
  display: grid;
  gap: 20px;
}

.booking-panel {
  margin-top: 0;
}

.booking-panel-title,
.annual-benefit-panel,
.annual-benefit-controls,
.annual-pass-card,
.annual-pass-card > div:first-child {
  display: flex;
}

.booking-panel-title {
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.booking-panel-title h3 {
  margin: 0 0 6px;
  color: #14532d;
  font-size: 22px;
}

.booking-panel-title p,
.annual-benefit-panel span,
.annual-pass-card span {
  margin: 0;
  color: #5c7164;
  line-height: 1.6;
}

.panel-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 8px;
  color: #14532d;
  background: #bbf7d0;
  font-weight: 900;
  white-space: nowrap;
}

.panel-chip.muted {
  background: #f1f5d8;
}

.annual-benefit-panel {
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  background: #f8fffa;
}

.annual-benefit-panel strong,
.annual-benefit-controls {
  align-items: center;
  gap: 10px;
}

.annual-benefit-panel strong {
  display: inline-flex;
  color: #14532d;
}

.annual-benefit-controls {
  flex: 0 0 auto;
}

.annual-pass-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.annual-pass-card {
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  background: #fff7ed;
}

.annual-pass-card > div:first-child {
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
}

.annual-pass-card svg,
.annual-pass-card b {
  color: #f97316;
}

.annual-pass-card strong {
  color: #14532d;
  font-size: 20px;
}

.annual-pass-card b {
  font-size: 26px;
}

@media (max-width: 860px) {
  .booking-panel-title,
  .annual-benefit-panel,
  .annual-pass-card {
    align-items: stretch;
    flex-direction: column;
  }

  .annual-benefit-controls {
    align-items: stretch;
  }

  .annual-pass-list {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import { Minus, Plus, ShoppingCart } from 'lucide-vue-next';
import { bookingTickets, useBookingStore } from '@/stores/booking';
import { toast } from '@/utils/message';

const booking = useBookingStore();
const active = ref(0);

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

function changeQuantity(ticketCode: string, delta: number) {
  booking.setQuantity(ticketCode, (booking.selectedTickets[ticketCode] ?? 0) + delta);
  active.value = booking.selectedCount > 0 ? 1 : 0;
}

function setQuantity(ticketCode: string, value: number | undefined) {
  booking.setQuantity(ticketCode, value ?? 0);
  active.value = booking.selectedCount > 0 ? 1 : 0;
}

function switchSession(value: string | number | boolean) {
  booking.setSession(value === 'PM' ? 'PM' : 'AM');
  booking.resetSelection();
  active.value = 0;
}

async function createOrder() {
  if (booking.selectedCount === 0) {
    toast.warning('请至少选择 1 张门票');
    return;
  }
  const lines = orderSummary.value.map((item) => `${item.name} x ${item.quantity}`).join('，');
  await ElMessageBox.confirm(
    `预约日期：${booking.visitDate}\n预约场次：${booking.session === 'AM' ? '上午' : '下午'}\n票种：${lines}\n合计：¥${booking.totalAmount}`,
    '确认预约订单',
    { confirmButtonText: '确认提交', cancelButtonText: '再看看', type: 'success' },
  );
  booking.commitOrder();
  active.value = 2;
  toast.success('订单已提交，库存已同步扣减');
}
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <p class="eyebrow">Ticket Booking</p>
      <h2>门票预约</h2>
      <span>选择日期、场次和票种数量，确认价格后提交预约。</span>
    </div>
    <el-steps :active="active" finish-status="success">
      <el-step title="选择日期" />
      <el-step title="确认票种" />
      <el-step title="支付预约" />
    </el-steps>

    <div class="booking-panel">
      <el-form class="booking-form" label-width="88px">
        <el-form-item label="预约日期">
          <el-date-picker v-model="booking.visitDate" class="booking-date-picker" style="width: 150px" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item label="预约场次">
          <el-segmented v-model="booking.session" :options="sessionOptions" @change="switchSession" />
        </el-form-item>
      </el-form>

      <div class="booking-stock-row">
        <div class="stock-summary">当前场次剩余 {{ booking.totalRemaining }} 张</div>
        <span>{{ booking.session === 'AM' ? '上午场' : '下午场' }}库存会随票种选择实时扣减上限</span>
      </div>

      <div class="ticket-select-list">
        <article v-for="ticket in bookingTickets" :key="ticket.code" class="ticket-select-card">
          <div>
            <strong>{{ ticket.name }}</strong>
            <span>{{ ticket.description }}</span>
            <b>¥{{ ticket.price }}</b>
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

      <section class="order-summary-panel">
        <div>
          <strong>订单确认</strong>
          <span v-if="!orderSummary.length">请选择需要预约的票种</span>
          <span v-else>{{ orderSummary.map((item) => `${item.name} x${item.quantity}`).join('，') }}</span>
        </div>
        <div class="order-total">
          <span>共 {{ booking.selectedCount }} 张</span>
          <strong>¥{{ booking.totalAmount }}</strong>
        </div>
      </section>

      <el-button type="success" size="large" :disabled="booking.selectedCount === 0" @click="createOrder">
        <ShoppingCart :size="18" />
        提交预约订单
      </el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import QRCode from 'qrcode';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { orderApi } from '@/api/modules';
import { useOrderStore } from '@/stores/order';
import type { OrderRecord } from '@/types/api';

const route = useRoute();
const orderStore = useOrderStore();
const qrUrl = ref('');
const notice = ref('');
const orderNo = ref('');
const selectedOrderId = computed(() => Number(route.query.orderId || 0));

async function loadPass(order?: OrderRecord) {
  const target = order ?? orderStore.orders.find((item) => item.id === selectedOrderId.value)
    ?? orderStore.orders.find((item) => item.paymentStatus === 'PAY_SUCCESS' && item.orderStatus !== 'CHECKED_IN');
  if (!target) return;
  const pass = await orderApi.qrcode(target.id);
  orderNo.value = pass.orderNo;
  notice.value = pass.entranceNotice;
  qrUrl.value = await QRCode.toDataURL(pass.qrContent, { margin: 1, width: 220 });
}

onMounted(async () => {
  if (!orderStore.orders.length) {
    await orderStore.loadMine();
  }
  await loadPass();
});

watch(() => route.query.orderId, () => loadPass());
</script>

<template>
  <section class="page-section pass-page">
    <div class="section-heading">
      <p class="eyebrow">Entry Pass</p>
      <h2>入园凭证</h2>
      <span>已支付订单可生成二维码，核销后状态会同步到订单。</span>
    </div>
    <div v-if="qrUrl" class="pass-card">
      <img :src="qrUrl" alt="入园二维码" />
      <strong>{{ orderNo }}</strong>
      <span>{{ notice }}</span>
    </div>
    <el-empty v-else description="暂无可入园的已支付订单" />
  </section>
</template>

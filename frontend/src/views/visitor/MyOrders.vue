<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useOrderStore } from '@/stores/order';
import type { OrderRecord } from '@/types/api';
import { orderStatusText, paymentStatusText } from '@/utils/status';
import { toast } from '@/utils/message';

const router = useRouter();
const orderStore = useOrderStore();

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
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <p class="eyebrow">Orders</p>
      <h2>我的订单</h2>
      <span>待支付订单可继续支付，已支付订单可查看二维码或申请退款。</span>
    </div>
    <el-table :data="orderStore.orders" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column prop="visitDate" label="预约日期" />
      <el-table-column prop="peopleCount" label="人数" width="80" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column label="订单状态">
        <template #default="{ row }">{{ orderStatusText[row.orderStatus] ?? row.orderStatus }}</template>
      </el-table-column>
      <el-table-column label="支付状态">
        <template #default="{ row }">{{ paymentStatusText[row.paymentStatus] ?? row.paymentStatus }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.paymentStatus === 'UNPAID'" link type="primary" @click="pay(row)">支付</el-button>
          <el-button v-if="row.paymentStatus === 'UNPAID'" link type="danger" @click="cancel(row)">取消</el-button>
          <el-button v-if="row.paymentStatus === 'PAY_SUCCESS'" link type="success" @click="showPass(row)">二维码</el-button>
          <el-button v-if="row.paymentStatus === 'PAY_SUCCESS' && row.orderStatus === 'PAID'" link type="warning" @click="refund(row)">退款</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

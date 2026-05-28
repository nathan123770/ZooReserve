<script setup lang="ts">
import { onMounted } from 'vue';
import { useOrderStore } from '@/stores/order';
import { orderStatusText, paymentStatusText } from '@/utils/status';

const orderStore = useOrderStore();

onMounted(() => orderStore.loadMine());
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <p class="eyebrow">Orders</p>
      <h2>我的订单</h2>
      <span>覆盖待支付、已预约、已入园、退款等状态。</span>
    </div>
    <el-table :data="orderStore.orders" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column prop="visitDate" label="预约日期" />
      <el-table-column prop="peopleCount" label="人数" />
      <el-table-column prop="amount" label="金额" />
      <el-table-column label="订单状态">
        <template #default="{ row }">{{ orderStatusText[row.orderStatus] }}</template>
      </el-table-column>
      <el-table-column label="支付状态">
        <template #default="{ row }">{{ paymentStatusText[row.paymentStatus] }}</template>
      </el-table-column>
    </el-table>
  </section>
</template>

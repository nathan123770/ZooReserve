<script setup lang="ts">
import * as echarts from 'echarts';
import { onMounted, ref } from 'vue';
import { adminApi } from '@/api/modules';

const summary = ref<Record<string, unknown>>({});
const chartRef = ref<HTMLDivElement | null>(null);

onMounted(async () => {
  summary.value = await adminApi.dashboard();
  if (chartRef.value) {
    echarts.init(chartRef.value).setOption({
      grid: { left: 32, right: 16, top: 24, bottom: 24 },
      xAxis: { type: 'category', data: ['05-24', '05-25', '05-26'] },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: [42, 57, 81], areaStyle: {} }],
    });
  }
});
</script>

<template>
  <div class="admin-page">
    <h1>数据看板</h1>
    <div class="metric-grid">
      <article><span>今日预约</span><strong>{{ summary.todayReservations ?? 0 }}</strong></article>
      <article><span>支付金额</span><strong>¥{{ summary.paidAmount ?? 0 }}</strong></article>
      <article><span>入园人数</span><strong>{{ summary.checkedInPeople ?? 0 }}</strong></article>
      <article><span>剩余容量</span><strong>{{ summary.remainingCapacity ?? 0 }}</strong></article>
    </div>
    <section class="admin-panel">
      <h2>活动报名趋势</h2>
      <div ref="chartRef" class="chart"></div>
    </section>
  </div>
</template>

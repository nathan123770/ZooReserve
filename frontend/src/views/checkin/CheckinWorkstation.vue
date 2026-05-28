<script setup lang="ts">
import { ref } from 'vue';
import { ScanLine, Search, ShieldCheck } from 'lucide-vue-next';
import { checkinApi } from '@/api/modules';
import { checkinStatusText } from '@/utils/status';

const keyword = ref('ZR202606010001');
const result = ref<Record<string, unknown> | null>(null);

async function scan() {
  result.value = await checkinApi.scan(keyword.value);
}

async function search() {
  result.value = await checkinApi.search(keyword.value);
}
</script>

<template>
  <section class="checkin-page">
    <div class="checkin-panel">
      <h1>扫码核销</h1>
      <p>支持二维码内容、订单号或手机号人工查询。</p>
      <el-input v-model="keyword" size="large" placeholder="扫码结果 / 订单号 / 手机号" />
      <div class="checkin-actions">
        <el-button type="success" size="large" @click="scan"><ScanLine :size="18" /> 扫码核销</el-button>
        <el-button size="large" @click="search"><Search :size="18" /> 人工查询</el-button>
      </div>
    </div>

    <div v-if="result" class="checkin-result">
      <ShieldCheck :size="36" />
      <strong>{{ checkinStatusText[String(result.checkinStatus)] }}</strong>
      <span>订单号：{{ result.orderNo }}</span>
      <span>票种：{{ result.ticketSummary }}</span>
      <span>人数：{{ result.peopleCount }}</span>
      <span>备注：{{ result.remark }}</span>
    </div>
  </section>
</template>

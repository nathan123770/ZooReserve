<script setup lang="ts">
import QRCode from 'qrcode';
import { onMounted, ref } from 'vue';
import { orderApi } from '@/api/modules';

const qrUrl = ref('');
const notice = ref('');

onMounted(async () => {
  const pass = await orderApi.qrcode(1);
  notice.value = pass.entranceNotice;
  qrUrl.value = await QRCode.toDataURL(pass.qrContent, { margin: 1, width: 220 });
});
</script>

<template>
  <section class="page-section pass-page">
    <div class="section-heading">
      <p class="eyebrow">Entry Pass</p>
      <h2>入园凭证</h2>
      <span>二维码电子票和入园须知集中展示。</span>
    </div>
    <div class="pass-card">
      <img v-if="qrUrl" :src="qrUrl" alt="入园二维码" />
      <strong>ZR202606010001</strong>
      <span>{{ notice }}</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { CalendarDays, MapPinned, Megaphone, QrCode, Sparkles } from 'lucide-vue-next';
import { memberApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';
import type { MemberCouponRecord, NoticeRecord } from '@/types/api';

const auth = useAuthStore();
const notices = ref<NoticeRecord[]>([]);
const availableCoupons = ref<MemberCouponRecord[]>([]);
const displayedNotices = computed(() => notices.value.slice(0, 3));

onMounted(async () => {
  notices.value = await memberApi.notices('HOME');
  if (auth.isAuthenticated && auth.user?.role === 'VISITOR') {
    availableCoupons.value = await memberApi.availableCoupons();
  }
});
</script>

<template>
  <section class="hero-band">
    <div class="hero-copy">
      <p class="eyebrow">今日 08:30-18:00 开放</p>
      <h1>和家人一起预约一场轻松的动物园之旅</h1>
      <p>查看余票、报名科普活动、生成电子凭证，入园流程一路清清楚楚。</p>
      <div class="hero-actions">
        <RouterLink class="primary-action" to="/booking"><CalendarDays :size="18" /> 快速预约</RouterLink>
        <RouterLink class="secondary-action" to="/guide"><MapPinned :size="18" /> 园区导览</RouterLink>
      </div>
    </div>
    <div class="hero-visual" aria-label="动物园预约概览">
      <div class="sun"></div>
      <div class="hill hill-a"></div>
      <div class="hill hill-b"></div>
      <div class="animal-card">
        <Sparkles :size="20" />
        <strong>今日客流 386</strong>
        <span>剩余容量 1780</span>
      </div>
    </div>
  </section>

  <section v-if="displayedNotices.length" class="home-notice-band" aria-label="园区公告">
    <div class="home-notice-title">
      <Megaphone :size="18" />
      <strong>园区公告</strong>
    </div>
    <div class="home-notice-list">
      <article v-for="notice in displayedNotices" :key="notice.id">
        <strong>{{ notice.title }}</strong>
        <span>{{ notice.content }}</span>
      </article>
    </div>
  </section>

  <section class="visitor-grid">
    <RouterLink class="feature-tile" to="/booking">
      <CalendarDays />
      <strong>门票预约</strong>
      <span>日期、场次、票种库存一屏确认</span>
    </RouterLink>
    <RouterLink class="feature-tile" to="/activities">
      <Sparkles />
      <strong>活动预约</strong>
      <span>讲解、投喂、课堂、夜游都能报名</span>
    </RouterLink>
    <RouterLink class="feature-tile" to="/pass">
      <QrCode />
      <strong>入园凭证</strong>
      <span>二维码醒目展示，核销状态同步</span>
    </RouterLink>
    <RouterLink class="feature-tile" to="/member">
      <Sparkles />
      <strong>优惠券</strong>
      <span>{{ availableCoupons.length ? `${availableCoupons.length} 张优惠券可领取` : '登录后领取可用优惠券' }}</span>
    </RouterLink>
  </section>
</template>

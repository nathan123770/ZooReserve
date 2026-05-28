<script setup lang="ts">
import { CalendarDays, MapPinned, QrCode, Sparkles } from 'lucide-vue-next';
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
  </section>
</template>

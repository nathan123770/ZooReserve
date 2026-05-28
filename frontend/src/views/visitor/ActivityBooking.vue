<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { activityApi } from '@/api/modules';
import type { ActivityRecord } from '@/types/api';
import { toast } from '@/utils/message';

const activities = ref<ActivityRecord[]>([]);

async function signup(id: number) {
  await activityApi.signup(id);
  toast.success('活动报名已提交');
}

onMounted(async () => {
  activities.value = await activityApi.list();
});
</script>

<template>
  <section class="page-section">
    <div class="section-heading">
      <p class="eyebrow">Activities</p>
      <h2>活动预约</h2>
      <span>科普讲解、亲子课堂和夜游活动统一管理。</span>
    </div>
    <div class="visitor-grid">
      <article v-for="activity in activities" :key="activity.id" class="feature-tile">
        <strong>{{ activity.title }}</strong>
        <span>{{ activity.category }} · {{ activity.location }}</span>
        <span>{{ activity.signedCount }}/{{ activity.capacity }} 人已报名</span>
        <el-button type="primary" plain @click="signup(activity.id)">报名</el-button>
      </article>
    </div>
  </section>
</template>

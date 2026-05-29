<script lang="ts">
type ActivityImageInput = Pick<import('@/types/api').ActivityRecord, 'title' | 'category'>;

export type ActivityImage = {
  src: string;
  alt: string;
  credit: string;
};

const activityImages = {
  giraffe: {
    src: 'https://commons.wikimedia.org/wiki/Special:FilePath/Giraffe_standing.jpg?width=900',
    alt: '长颈鹿在开阔草地中活动的真实照片',
    credit: 'Wikimedia Commons',
  },
  classroom: {
    src: 'https://commons.wikimedia.org/wiki/Special:FilePath/Children_walking_forest.jpg?width=900',
    alt: '孩子参与亲子自然课堂的真实照片',
    credit: 'Wikimedia Commons',
  },
  night: {
    src: 'https://commons.wikimedia.org/wiki/Special:FilePath/Elefantentor_Berlin_Zoo_Nacht.jpg?width=900',
    alt: '夜游动物园灯光氛围的真实照片',
    credit: 'Wikimedia Commons',
  },
  rainforest: {
    src: 'https://commons.wikimedia.org/wiki/Special:FilePath/Pacific_Rim_National_Park_-_Rainforest_Trail_%283670678277%29.jpg?width=900',
    alt: '雨林步道和植物环境的真实照片',
    credit: 'Wikimedia Commons',
  },
  fallback: {
    src: 'https://commons.wikimedia.org/wiki/Special:FilePath/Giraffe_Standing_in_Open_Landscape_%2849694993993%29.jpg?width=900',
    alt: '动物园自然游览场景的真实照片',
    credit: 'Wikimedia Commons',
  },
} satisfies Record<string, ActivityImage>;

export function getActivityImage(activity: ActivityImageInput) {
  const text = `${activity.title} ${activity.category}`;
  if (text.includes('长颈鹿') || text.includes('科普')) return activityImages.giraffe;
  if (text.includes('亲子') || text.includes('饲养员') || text.includes('课堂')) return activityImages.classroom;
  if (text.includes('夜游') || text.includes('夏夜')) return activityImages.night;
  if (text.includes('雨林') || text.includes('导览')) return activityImages.rainforest;
  return activityImages.fallback;
}
</script>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Clock, MapPin, UsersRound } from 'lucide-vue-next';
import { activityApi } from '@/api/modules';
import type { ActivityRecord } from '@/types/api';
import { toast } from '@/utils/message';

const activities = ref<ActivityRecord[]>([]);

const activityCards = computed(() =>
  activities.value.map((activity) => ({
    ...activity,
    image: getActivityImage(activity),
    remaining: Math.max(activity.capacity - activity.signedCount, 0),
  })),
);

function formatActivityTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

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
      <span>科普讲解、亲子课堂和夜游活动统一管理，配合真实照片更容易判断活动氛围。</span>
    </div>
    <div class="activity-grid">
      <article v-for="activity in activityCards" :key="activity.id" class="activity-card">
        <div class="activity-image-wrap">
          <img :src="activity.image.src" :alt="activity.image.alt" loading="lazy" />
          <span class="activity-category">{{ activity.category }}</span>
        </div>
        <div class="activity-body">
          <strong>{{ activity.title }}</strong>
          <div class="activity-meta">
            <span><MapPin :size="16" /> {{ activity.location }}</span>
            <span><Clock :size="16" /> {{ formatActivityTime(activity.startTime) }}</span>
            <span><UsersRound :size="16" /> 剩余 {{ activity.remaining }} / {{ activity.capacity }}</span>
          </div>
          <div class="activity-footer">
            <span>{{ activity.signedCount }}/{{ activity.capacity }} 人已报名</span>
            <el-button type="primary" plain @click="signup(activity.id)">报名</el-button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 6px 0 44px;
}

.activity-card {
  overflow: hidden;
  border: 1px solid rgba(22, 163, 74, 0.16);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(20, 83, 45, 0.08);
}

.activity-image-wrap {
  position: relative;
  overflow: hidden;
  aspect-ratio: 4 / 3;
  background: #dcfce7;
}

.activity-image-wrap img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.activity-category {
  position: absolute;
  left: 12px;
  bottom: 12px;
  max-width: calc(100% - 24px);
  padding: 6px 10px;
  border-radius: 8px;
  color: #14532d;
  background: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  font-weight: 900;
}

.activity-body {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.activity-body strong {
  color: #14532d;
  font-size: 19px;
  line-height: 1.3;
}

.activity-meta {
  display: grid;
  gap: 8px;
  color: #5c7164;
  font-size: 14px;
}

.activity-meta span,
.activity-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.activity-footer {
  justify-content: space-between;
  gap: 12px;
  color: #f97316;
  font-weight: 800;
}

.activity-footer .el-button {
  flex: 0 0 auto;
}

@media (max-width: 1060px) {
  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .activity-grid {
    grid-template-columns: 1fr;
  }

  .activity-footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>

<script lang="ts">
export type GuidePoiType = 'animal' | 'service' | 'food' | 'activity' | 'entrance';
export type GuidePoiStatus = 'open' | 'soon' | 'busy' | 'closed';

export type GuidePoi = {
  id: string;
  name: string;
  type: GuidePoiType;
  description: string;
  status: GuidePoiStatus;
  tags: string[];
  position: { x: number; y: number };
  distanceHint: string;
};

export const guidePois: GuidePoi[] = [
  {
    id: 'south-gate',
    name: '南门游客中心',
    type: 'entrance',
    description: '入园检票、咨询、童车租借和失物招领都在这里办理。',
    status: 'open',
    tags: ['热门', '入园', '服务'],
    position: { x: 450, y: 500 },
    distanceHint: '从入口出发约 0 分钟',
  },
  {
    id: 'panda-valley',
    name: '熊猫竹林',
    type: 'animal',
    description: '清晨和午后最容易看到熊猫活动，馆外设有遮阴等候区。',
    status: 'busy',
    tags: ['热门', '亲子', '室外'],
    position: { x: 205, y: 180 },
    distanceHint: '从南门步行约 9 分钟',
  },
  {
    id: 'savanna-loop',
    name: '草原动物区',
    type: 'animal',
    description: '长颈鹿、斑马和羚羊沿环形步道分布，适合慢慢观赏。',
    status: 'open',
    tags: ['热门', '拍照', '开阔'],
    position: { x: 650, y: 208 },
    distanceHint: '从南门步行约 12 分钟',
  },
  {
    id: 'wetland-aviary',
    name: '湿地飞鸟湾',
    type: 'animal',
    description: '临水栈道连接观鸟平台，雨天也可以在半室内区域停留。',
    status: 'open',
    tags: ['安静', '自然', '无障碍'],
    position: { x: 676, y: 382 },
    distanceHint: '从南门步行约 10 分钟',
  },
  {
    id: 'nature-classroom',
    name: '自然课堂',
    type: 'activity',
    description: '每日有科普讲解和手作课程，热门场次建议提前预约。',
    status: 'soon',
    tags: ['热门', '预约', '室内'],
    position: { x: 385, y: 244 },
    distanceHint: '从南门步行约 6 分钟',
  },
  {
    id: 'kids-ranger',
    name: '小小保育员站',
    type: 'activity',
    description: '投喂讲解和保育体验集合点，适合 6 岁以上儿童参加。',
    status: 'open',
    tags: ['预约', '互动', '亲子'],
    position: { x: 304, y: 384 },
    distanceHint: '从南门步行约 7 分钟',
  },
  {
    id: 'forest-cafe',
    name: '森林餐厅',
    type: 'food',
    description: '供应儿童套餐、简餐和饮品，高峰期可先取号再游览周边。',
    status: 'busy',
    tags: ['热门', '午餐', '休息'],
    position: { x: 514, y: 342 },
    distanceHint: '从南门步行约 8 分钟',
  },
  {
    id: 'north-snack',
    name: '北区轻食亭',
    type: 'food',
    description: '靠近草原动物区，提供饮水补给、冰品和轻食。',
    status: 'open',
    tags: ['补给', '休息'],
    position: { x: 744, y: 145 },
    distanceHint: '从南门步行约 14 分钟',
  },
  {
    id: 'family-restroom',
    name: '家庭卫生间',
    type: 'service',
    description: '配置母婴台、儿童洗手台和无障碍隔间。',
    status: 'open',
    tags: ['亲子', '无障碍', '服务'],
    position: { x: 566, y: 458 },
    distanceHint: '从南门步行约 5 分钟',
  },
  {
    id: 'first-aid',
    name: '医务与休息点',
    type: 'service',
    description: '提供基础急救、临时休息和广播寻人协助。',
    status: 'open',
    tags: ['服务', '安心'],
    position: { x: 254, y: 296 },
    distanceHint: '从南门步行约 6 分钟',
  },
];

export function filterGuidePois(type: GuidePoiType | 'all', popularOnly: boolean) {
  return guidePois.filter((poi) => {
    const matchesType = type === 'all' || poi.type === type;
    const matchesPopular = !popularOnly || poi.tags.includes('热门');
    return matchesType && matchesPopular;
  });
}

export function getInitialPoi() {
  return guidePois.find((poi) => poi.type === 'entrance') ?? guidePois[0];
}
</script>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { CalendarCheck, Filter, MapPin, Navigation, Search, Sparkles } from 'lucide-vue-next';

const typeOptions: Array<{ value: GuidePoiType | 'all'; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'animal', label: '展区' },
  { value: 'activity', label: '活动' },
  { value: 'food', label: '餐饮' },
  { value: 'service', label: '服务' },
  { value: 'entrance', label: '入口' },
];

const typeLabels: Record<GuidePoiType, string> = {
  animal: '动物展区',
  service: '游客服务',
  food: '餐饮补给',
  activity: '互动活动',
  entrance: '入口中心',
};

const statusLabels: Record<GuidePoiStatus, string> = {
  open: '开放中',
  soon: '即将开始',
  busy: '较繁忙',
  closed: '暂未开放',
};

const statusTone: Record<GuidePoiStatus, string> = {
  open: 'green',
  soon: 'blue',
  busy: 'orange',
  closed: 'gray',
};

const selectedType = ref<GuidePoiType | 'all'>('all');
const popularOnly = ref(false);
const selectedPoiId = ref(getInitialPoi().id);

const visiblePois = computed(() => filterGuidePois(selectedType.value, popularOnly.value));
const selectedPoi = computed(() => guidePois.find((poi) => poi.id === selectedPoiId.value) ?? getInitialPoi());

function selectPoi(poi: GuidePoi) {
  selectedPoiId.value = poi.id;
}

function setType(type: GuidePoiType | 'all') {
  selectedType.value = type;
  const nextPois = filterGuidePois(type, popularOnly.value);
  if (!nextPois.some((poi) => poi.id === selectedPoiId.value)) {
    selectedPoiId.value = nextPois[0]?.id ?? getInitialPoi().id;
  }
}

function togglePopularOnly() {
  popularOnly.value = !popularOnly.value;
  const nextPois = filterGuidePois(selectedType.value, popularOnly.value);
  if (!nextPois.some((poi) => poi.id === selectedPoiId.value)) {
    selectedPoiId.value = nextPois[0]?.id ?? getInitialPoi().id;
  }
}

function poiStyle(poi: GuidePoi) {
  return {
    left: `${(poi.position.x / 900) * 100}%`,
    top: `${(poi.position.y / 560) * 100}%`,
  };
}
</script>

<template>
  <section class="page-section guide-page">
    <div class="guide-hero">
      <div class="section-heading">
        <p class="eyebrow">Guide</p>
        <h2>园区互动导览</h2>
        <span>手绘园区地图模拟真实游览动线，快速查找展区、活动、餐饮和游客服务。</span>
      </div>
      <div class="guide-summary" aria-label="导览概览">
        <span><MapPin :size="16" /> 10 个点位</span>
        <span><Navigation :size="16" /> 主环线游览</span>
        <span><Sparkles :size="16" /> 亲子友好</span>
      </div>
    </div>

    <div class="guide-toolbar" aria-label="导览筛选">
      <div class="filter-group" role="group" aria-label="点位类型">
        <button
          v-for="option in typeOptions"
          :key="option.value"
          type="button"
          :class="{ active: selectedType === option.value }"
          @click="setType(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
      <button class="popular-toggle" :class="{ active: popularOnly }" type="button" @click="togglePopularOnly">
        <Filter :size="16" />
        {{ popularOnly ? '热门点位' : '显示热门' }}
      </button>
    </div>

    <div class="guide-layout">
      <div class="guide-map-panel">
        <div class="guide-map-stage" aria-label="手绘园区地图">
          <svg class="illustrated-map" viewBox="0 0 900 560" role="img" aria-labelledby="guide-map-title">
            <title id="guide-map-title">ZooReserve 手绘园区导览图</title>
            <defs>
              <linearGradient id="grassGradient" x1="0" x2="1" y1="0" y2="1">
                <stop offset="0%" stop-color="#e9fbdc" />
                <stop offset="100%" stop-color="#c9f3b9" />
              </linearGradient>
              <linearGradient id="waterGradient" x1="0" x2="1" y1="0" y2="1">
                <stop offset="0%" stop-color="#b9efff" />
                <stop offset="100%" stop-color="#76d6f4" />
              </linearGradient>
              <filter id="softShadow" x="-20%" y="-20%" width="140%" height="140%">
                <feDropShadow dx="0" dy="8" stdDeviation="10" flood-color="#14532d" flood-opacity="0.14" />
              </filter>
            </defs>

            <rect width="900" height="560" rx="28" fill="url(#grassGradient)" />
            <path d="M38 94 C126 44 234 62 326 84 C430 109 497 74 594 54 C714 29 810 58 865 118" fill="none" stroke="#a7e6a2" stroke-width="34" stroke-linecap="round" opacity="0.55" />
            <path d="M713 292 C790 255 867 281 878 354 C888 417 828 471 748 454 C688 441 650 397 664 349 C672 322 689 303 713 292Z" fill="url(#waterGradient)" opacity="0.9" />
            <path d="M90 464 C156 416 241 432 296 474 C233 537 137 536 90 464Z" fill="#baf1a8" opacity="0.85" />

            <path d="M450 505 C443 433 393 397 317 374 C245 352 206 310 222 258 C243 190 332 158 439 176 C548 194 631 153 705 121" fill="none" stroke="#f7d99d" stroke-width="48" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M450 505 C443 433 393 397 317 374 C245 352 206 310 222 258 C243 190 332 158 439 176 C548 194 631 153 705 121" fill="none" stroke="#fff7d8" stroke-width="28" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="1 38" />
            <path d="M316 374 C410 344 501 344 587 378 C631 397 679 397 735 367" fill="none" stroke="#f7d99d" stroke-width="34" stroke-linecap="round" />
            <path d="M385 244 C462 246 507 281 514 342" fill="none" stroke="#f7d99d" stroke-width="28" stroke-linecap="round" />

            <g filter="url(#softShadow)">
              <path d="M118 116 C170 77 252 91 286 149 C257 209 168 236 112 196 C86 177 87 139 118 116Z" fill="#d9f99d" />
              <path d="M578 112 C640 66 738 76 790 139 C772 209 687 248 611 220 C558 200 541 151 578 112Z" fill="#fef3c7" />
              <path d="M612 316 C652 286 724 298 747 348 C724 392 651 405 610 370 C593 354 594 332 612 316Z" fill="#dbeafe" />
              <path d="M330 205 C383 168 462 188 486 246 C462 302 376 315 330 267 C313 248 314 222 330 205Z" fill="#ffe4e6" />
              <path d="M438 311 C490 283 563 306 579 365 C542 411 469 407 430 365 C415 349 419 322 438 311Z" fill="#ffedd5" />
            </g>

            <text x="150" y="137" class="map-label">熊猫竹林</text>
            <text x="620" y="145" class="map-label">草原动物区</text>
            <text x="626" y="354" class="map-label">湿地飞鸟湾</text>
            <text x="357" y="238" class="map-label">自然课堂</text>
            <text x="463" y="348" class="map-label">森林餐厅</text>
            <text x="412" y="532" class="map-label">南门</text>
          </svg>

          <button
            v-for="poi in visiblePois"
            :key="poi.id"
            class="map-hotspot"
            :class="[poi.type, statusTone[poi.status], { active: selectedPoi.id === poi.id }]"
            :style="poiStyle(poi)"
            type="button"
            :aria-pressed="selectedPoi.id === poi.id"
            :aria-label="`${poi.name}，${typeLabels[poi.type]}，${statusLabels[poi.status]}`"
            @click="selectPoi(poi)"
          >
            <span class="hotspot-dot"></span>
            <span class="hotspot-label">{{ poi.name }}</span>
          </button>
        </div>
      </div>

      <aside class="guide-detail-panel" aria-label="点位详情">
        <div class="detail-kicker">
          <span :class="['status-pill', statusTone[selectedPoi.status]]">{{ statusLabels[selectedPoi.status] }}</span>
          <span>{{ typeLabels[selectedPoi.type] }}</span>
        </div>
        <h3>{{ selectedPoi.name }}</h3>
        <p>{{ selectedPoi.description }}</p>
        <div class="distance-card">
          <Navigation :size="18" />
          <span>{{ selectedPoi.distanceHint }}</span>
        </div>
        <div class="tag-list" aria-label="点位标签">
          <span v-for="tag in selectedPoi.tags" :key="tag">{{ tag }}</span>
        </div>
        <RouterLink v-if="selectedPoi.type === 'activity'" class="detail-action" to="/activities">
          <CalendarCheck :size="18" />
          查看活动预约
        </RouterLink>
      </aside>
    </div>

    <div class="poi-list" aria-label="当前筛选点位">
      <button
        v-for="poi in visiblePois"
        :key="`list-${poi.id}`"
        class="poi-list-item"
        :class="{ active: selectedPoi.id === poi.id }"
        type="button"
        @click="selectPoi(poi)"
      >
        <Search :size="16" />
        <span>{{ poi.name }}</span>
        <small>{{ statusLabels[poi.status] }}</small>
      </button>
    </div>
  </section>
</template>

<style scoped>
.guide-page {
  display: grid;
  gap: 18px;
}

.guide-hero,
.guide-toolbar,
.guide-layout,
.guide-summary,
.filter-group,
.popular-toggle,
.detail-kicker,
.distance-card,
.tag-list,
.detail-action,
.poi-list,
.poi-list-item {
  display: flex;
}

.guide-hero {
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.guide-summary {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  color: #14532d;
}

.guide-summary span,
.popular-toggle,
.filter-group button {
  align-items: center;
  min-height: 38px;
  border-radius: 8px;
  font-weight: 800;
}

.guide-summary span {
  display: inline-flex;
  gap: 7px;
  padding: 0 12px;
  background: #dcfce7;
}

.guide-toolbar {
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.filter-group {
  flex-wrap: wrap;
  gap: 8px;
}

.filter-group button,
.popular-toggle {
  gap: 7px;
  border: 1px solid rgba(20, 83, 45, 0.16);
  color: #365944;
  background: #fff;
  cursor: pointer;
  transition: background 180ms ease, border-color 180ms ease, color 180ms ease;
}

.filter-group button {
  padding: 0 14px;
}

.filter-group button.active,
.popular-toggle.active {
  color: #fff;
  border-color: #16a34a;
  background: #16a34a;
}

.popular-toggle {
  align-items: center;
  padding: 0 14px;
}

.guide-layout {
  align-items: stretch;
  gap: 16px;
}

.guide-map-panel,
.guide-detail-panel {
  border: 1px solid rgba(22, 163, 74, 0.16);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(20, 83, 45, 0.08);
}

.guide-map-panel {
  flex: 1 1 auto;
  min-width: 0;
  padding: 14px;
}

.guide-map-stage {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  background: #ecfdf5;
  aspect-ratio: 900 / 560;
}

.illustrated-map {
  display: block;
  width: 100%;
  height: 100%;
}

.map-label {
  fill: #14532d;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 0;
}

.map-hotspot {
  position: absolute;
  display: grid;
  grid-template-columns: 18px max-content;
  align-items: center;
  gap: 6px;
  max-width: 150px;
  padding: 6px 9px 6px 6px;
  border: 2px solid #fff;
  border-radius: 999px;
  color: #14532d;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 24px rgba(20, 83, 45, 0.16);
  cursor: pointer;
  transform: translate(-50%, -50%);
  transition: box-shadow 180ms ease, border-color 180ms ease, background 180ms ease;
}

.map-hotspot:focus-visible,
.filter-group button:focus-visible,
.popular-toggle:focus-visible,
.poi-list-item:focus-visible,
.detail-action:focus-visible {
  outline: 3px solid rgba(14, 165, 233, 0.45);
  outline-offset: 2px;
}

.map-hotspot:hover,
.map-hotspot.active {
  border-color: #16a34a;
  box-shadow: 0 12px 30px rgba(20, 83, 45, 0.24);
}

.map-hotspot.active {
  background: #f0fdf4;
}

.hotspot-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #16a34a;
  box-shadow: inset 0 0 0 4px rgba(255, 255, 255, 0.74);
}

.map-hotspot.activity .hotspot-dot,
.status-pill.blue {
  background: #0ea5e9;
}

.map-hotspot.food .hotspot-dot,
.status-pill.orange {
  background: #f59e0b;
}

.map-hotspot.service .hotspot-dot {
  background: #fb7185;
}

.map-hotspot.entrance .hotspot-dot {
  background: #14532d;
}

.hotspot-label {
  overflow: hidden;
  color: #143923;
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-detail-panel {
  display: grid;
  align-content: start;
  flex: 0 0 320px;
  gap: 14px;
  padding: 20px;
}

.detail-kicker {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #5c7164;
  font-size: 13px;
  font-weight: 800;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 8px;
  color: #fff;
  background: #16a34a;
}

.status-pill.gray {
  background: #64748b;
}

.guide-detail-panel h3 {
  margin: 0;
  color: #14532d;
  font-size: 28px;
  line-height: 1.2;
}

.guide-detail-panel p {
  margin: 0;
  color: #4b6353;
  line-height: 1.7;
}

.distance-card {
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  color: #14532d;
  background: #f8fffa;
  font-weight: 800;
}

.tag-list {
  flex-wrap: wrap;
  gap: 8px;
}

.tag-list span {
  padding: 6px 9px;
  border-radius: 8px;
  color: #365944;
  background: #f1f5d8;
  font-size: 13px;
  font-weight: 800;
}

.detail-action {
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  border-radius: 8px;
  color: #fff;
  background: #16a34a;
  font-weight: 900;
}

.poi-list {
  flex-wrap: wrap;
  gap: 10px;
}

.poi-list-item {
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid #dcfce7;
  border-radius: 8px;
  color: #365944;
  background: #fff;
  cursor: pointer;
}

.poi-list-item.active {
  color: #14532d;
  border-color: #16a34a;
  background: #f0fdf4;
  font-weight: 900;
}

.poi-list-item small {
  color: #5c7164;
}

@media (max-width: 920px) {
  .guide-hero,
  .guide-toolbar,
  .guide-layout {
    align-items: stretch;
    flex-direction: column;
  }

  .guide-summary {
    justify-content: flex-start;
  }

  .guide-detail-panel {
    flex-basis: auto;
  }
}

@media (max-width: 560px) {
  .guide-map-panel {
    padding: 8px;
  }

  .guide-map-stage {
    aspect-ratio: 1 / 1;
  }

  .hotspot-label {
    display: none;
  }

  .map-hotspot {
    grid-template-columns: 18px;
    padding: 7px;
  }

  .map-label {
    font-size: 24px;
  }
}
</style>

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
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { Clock, MapPin, TicketPercent, UsersRound } from 'lucide-vue-next';
import { activityApi } from '@/api/modules';
import type { ActivityRecord } from '@/types/api';
import { useAuthStore } from '@/stores/auth';
import { useMemberStore } from '@/stores/member';
import { useOrderStore } from '@/stores/order';
import { toast } from '@/utils/message';

const router = useRouter();
const auth = useAuthStore();
const member = useMemberStore();
const orderStore = useOrderStore();
const activities = ref<ActivityRecord[]>([]);
const selectedCoupons = reactive<Record<number, number | undefined>>({});

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

function formatCurrency(value: number | string | undefined) {
  const amount = Number(value ?? 0);
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(amount) ? 0 : 2,
    maximumFractionDigits: 2,
  });
}

function usableCoupons(activity: ActivityRecord) {
  return member.coupons.filter((coupon) => {
    if (coupon.status !== '可用') return false;
    if (activity.price < coupon.thresholdAmount) return false;
    if (coupon.expiresAt !== '-' && activity.startTime.slice(0, 10) > coupon.expiresAt) return false;
    return couponScopeMatches(coupon.scope, activity.couponScope);
  });
}

function couponScopeMatches(couponScope: string, activityScope: string) {
  return couponScope === activityScope || (activityScope.startsWith('ACTIVITY') && couponScope === 'ACTIVITY');
}

function couponUnavailableText(activity: ActivityRecord) {
  if (!auth.isAuthenticated || auth.user?.role !== 'VISITOR') return '登录后可查看活动优惠券';
  const activityCoupons = member.coupons.filter((coupon) => coupon.scope?.startsWith('ACTIVITY'));
  if (!activityCoupons.length) return '暂无已领取活动券，可先到会员中心领取';
  const matchedScopeCoupons = activityCoupons.filter((coupon) => couponScopeMatches(coupon.scope, activity.couponScope));
  if (!matchedScopeCoupons.length) return `暂无适用于 ${activity.couponScope} 的活动券`;
  const availableStatusCoupons = matchedScopeCoupons.filter((coupon) => coupon.status === '可用');
  if (!availableStatusCoupons.length) return '活动券已使用、锁定或过期';
  const validDateCoupons = availableStatusCoupons.filter((coupon) => coupon.expiresAt === '-' || activity.startTime.slice(0, 10) <= coupon.expiresAt);
  if (!validDateCoupons.length) return '活动券不在活动日期有效期内';
  return `当前活动金额 ¥${formatCurrency(activity.price)} 未达到券门槛`;
}

function selectedCoupon(activity: ActivityRecord) {
  const couponId = selectedCoupons[activity.id];
  return usableCoupons(activity).find((coupon) => coupon.id === couponId);
}

function discountAmount(activity: ActivityRecord) {
  const coupon = selectedCoupon(activity);
  if (!coupon) return 0;
  if (coupon.discountType === 'PERCENT') {
    return Math.max(activity.price - activity.price * coupon.discountValue, 0);
  }
  return Math.min(coupon.discountValue, activity.price);
}

function payableAmount(activity: ActivityRecord) {
  return Math.max(activity.price - discountAmount(activity), 0);
}

function couponConfirmText(activity: ActivityRecord) {
  const coupon = selectedCoupon(activity);
  if (!coupon) return '优惠券：不使用';
  return `优惠券：${coupon.name}，抵扣 ¥${formatCurrency(discountAmount(activity))}`;
}

async function signup(id: number) {
  await activityApi.signup(id);
  toast.success('活动报名已提交');
  activities.value = await activityApi.list();
}

async function submitPaidActivity(activity: ActivityRecord) {
  if (!auth.isAuthenticated || auth.user?.role !== 'VISITOR') {
    toast.warning('请先登录游客账号后再报名收费活动');
    await router.push({ name: 'login', query: { redirect: '/activities' } });
    return;
  }
  await ElMessageBox.confirm(
    `${activity.title}\n活动时间：${formatActivityTime(activity.startTime)}\n${couponConfirmText(activity)}\n应付：￥${formatCurrency(payableAmount(activity))}`,
    '确认活动订单',
    { confirmButtonText: '提交订单', cancelButtonText: '再看看', type: 'success' },
  );
  const order = await orderStore.createReservation({
    visitDate: activity.startTime.slice(0, 10),
    session: 'ACTIVITY',
    items: [],
    couponId: selectedCoupons[activity.id],
    orderType: 'ACTIVITY',
    activityId: activity.id,
    quantity: 1,
  });
  if (order.paymentStatus === 'PAY_SUCCESS') {
    toast.success('活动报名成功');
    activities.value = await activityApi.list();
    return;
  }
  try {
    await ElMessageBox.confirm(`活动订单 ${order.orderNo} 已提交，是否立即模拟支付？`, '提交成功', {
      confirmButtonText: '立即支付',
      cancelButtonText: '去订单页',
      type: 'success',
    });
    await orderStore.payOrder(order);
    toast.success('支付成功，活动报名已生效');
    activities.value = await activityApi.list();
  } catch {
    await router.push({ name: 'my-orders' });
  }
}

onMounted(async () => {
  const tasks: Promise<unknown>[] = [activityApi.list().then((rows) => { activities.value = rows; })];
  if (auth.isAuthenticated && auth.user?.role === 'VISITOR') {
    tasks.push(member.loadAll());
  }
  await Promise.all(tasks);
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
            <span>
              <TicketPercent :size="16" />
              {{ activity.paid ? `收费 ¥${formatCurrency(activity.price)}` : '免费报名' }}
            </span>
          </div>
          <div v-if="activity.paid" class="coupon-panel">
            <el-select
              v-if="usableCoupons(activity).length"
              v-model="selectedCoupons[activity.id]"
              clearable
              placeholder="不使用优惠券"
            >
              <el-option
                v-for="coupon in usableCoupons(activity)"
                :key="coupon.id"
                :label="`${coupon.name} · ${coupon.threshold}`"
                :value="coupon.id"
              />
            </el-select>
            <span v-else class="coupon-empty">{{ couponUnavailableText(activity) }}</span>
            <span v-if="selectedCoupon(activity)" class="coupon-discount">
              已抵扣 ¥{{ formatCurrency(discountAmount(activity)) }}，应付 ¥{{ formatCurrency(payableAmount(activity)) }}
            </span>
          </div>
          <div class="activity-footer">
            <span>{{ activity.signedCount }}/{{ activity.capacity }} 人已报名</span>
            <el-button v-if="activity.paid" type="success" plain @click="submitPaidActivity(activity)">下单报名</el-button>
            <el-button v-else type="primary" plain @click="signup(activity.id)">报名</el-button>
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

.coupon-panel {
  display: grid;
  gap: 8px;
}

.coupon-empty {
  display: block;
  min-height: 32px;
  padding: 8px 10px;
  border: 1px dashed rgba(22, 163, 74, 0.28);
  border-radius: 8px;
  color: #5c7164;
  background: #f7fee7;
  font-size: 13px;
  line-height: 1.4;
}

.coupon-discount {
  color: #15803d;
  font-size: 13px;
  font-weight: 800;
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

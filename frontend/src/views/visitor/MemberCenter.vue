<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { Bell, CreditCard, Edit3, Star, TicketPercent, UserPlus, Users } from 'lucide-vue-next';
import { annualPassProducts } from '@/stores/booking';
import { useMemberStore } from '@/stores/member';
import { useOrderStore } from '@/stores/order';
import type { MemberProfile } from '@/stores/member';
import { toast } from '@/utils/message';

const member = useMemberStore();
const orderStore = useOrderStore();
const router = useRouter();
const profileDialogVisible = ref(false);
const editingProfileId = ref<number | undefined>();
const profileForm = reactive({
  name: '',
  idCard: '',
  phone: '',
  relation: '家人',
  isDefault: false,
});

onMounted(() => member.loadAll());

function openProfileDialog(profile?: MemberProfile) {
  editingProfileId.value = profile?.id;
  profileForm.name = profile?.name ?? '';
  profileForm.idCard = profile?.idCard ?? '';
  profileForm.phone = profile?.phone ?? '';
  profileForm.relation = profile?.relation ?? '家人';
  profileForm.isDefault = profile?.isDefault ?? false;
  profileDialogVisible.value = true;
}

async function saveProfile() {
  if (!profileForm.name || !profileForm.idCard || !profileForm.phone) {
    toast.warning('请填写姓名、证件号和手机号');
    return;
  }
  await member.saveProfile({
    id: editingProfileId.value,
    name: profileForm.name,
    idCard: profileForm.idCard,
    phone: profileForm.phone,
    relation: profileForm.relation,
    isDefault: profileForm.isDefault,
  });
  profileDialogVisible.value = false;
  toast.success(editingProfileId.value ? '常用游客已更新' : '常用游客已新增');
}

async function deleteProfile(profile: MemberProfile) {
  await ElMessageBox.confirm(`确认删除常用游客「${profile.name}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  });
  await member.deleteProfile(profile.id);
  toast.success('常用游客已删除');
}

async function renewAnnualPass() {
  if (!member.annualPass.id) return;
  const product = annualPassProducts[0];
  await ElMessageBox.confirm(
    `${member.annualPass.name} 将生成一笔续费订单，支付成功后有效期顺延一年。\n应付：¥${product.price}`,
    '续费年卡',
    { confirmButtonText: '提交续费订单', cancelButtonText: '再看看', type: 'info' },
  );
  const order = await orderStore.createReservation({
    visitDate: new Date().toISOString().slice(0, 10),
    session: 'AM',
    items: [{ ticketTypeCode: product.code, quantity: 1 }],
    annualPassId: member.annualPass.id,
    orderType: 'ANNUAL_PASS_RENEWAL',
  });

  try {
    await ElMessageBox.confirm(`续费订单 ${order.orderNo} 已提交，是否立即模拟支付？`, '提交成功', {
      confirmButtonText: '立即支付',
      cancelButtonText: '去订单页',
      type: 'success',
    });
    await orderStore.payOrder(order);
    await member.loadAll();
    toast.success('续费成功，年卡有效期已更新');
  } catch {
    toast.info('续费订单已创建，可稍后在订单页继续支付');
    await router.push({ name: 'my-orders' });
  }
}
</script>

<template>
  <section class="page-section member-page">
    <div class="section-heading">
      <p class="eyebrow">Member</p>
      <h2>会员中心</h2>
      <span>管理常用游客、年卡权益、优惠券和消息通知。</span>
    </div>

    <section class="member-overview">
      <article>
        <Users :size="22" />
        <span>默认游客</span>
        <strong>{{ member.defaultProfile?.name ?? '未设置' }}</strong>
      </article>
      <article>
        <CreditCard :size="22" />
        <span>{{ member.annualPass.name }}</span>
        <strong>{{ member.annualPass.status }}</strong>
      </article>
      <article>
        <TicketPercent :size="22" />
        <span>可用优惠券</span>
        <strong>{{ member.coupons.filter((coupon) => coupon.status === '可用').length }} 张</strong>
      </article>
    </section>

    <div class="member-layout">
      <section class="member-panel">
        <div class="member-panel-title">
          <div>
            <h3>常用游客</h3>
            <p>预约时可直接选择实名游客。</p>
          </div>
          <el-button type="primary" @click="openProfileDialog()">
            <UserPlus :size="16" />
            新增游客
          </el-button>
        </div>
        <div class="profile-list">
          <article v-for="profile in member.profiles" :key="profile.id" class="profile-card">
            <div>
              <strong>{{ profile.name }}</strong>
              <span>{{ profile.relation }} · {{ profile.phone }}</span>
              <small>{{ profile.idCard }}</small>
            </div>
            <el-tag v-if="profile.isDefault" type="success"><Star :size="13" /> 默认</el-tag>
            <div class="profile-actions">
              <el-button link type="primary" @click="openProfileDialog(profile)">
                <Edit3 :size="14" />
                编辑
              </el-button>
              <el-button v-if="!profile.isDefault" link type="primary" @click="member.setDefault(profile.id)">设为默认</el-button>
              <el-button link type="danger" @click="deleteProfile(profile)">删除</el-button>
            </div>
          </article>
        </div>
      </section>

      <aside class="member-side">
        <section class="member-panel">
          <div class="member-panel-title compact">
            <h3>年卡权益</h3>
            <el-tag type="success">{{ member.annualPass.status }}</el-tag>
          </div>
          <p>有效期至 {{ member.annualPass.expiresAt }}</p>
          <p>绑定游客：{{ member.annualPass.boundVisitors.join('、') || '未绑定' }}</p>
          <ul>
            <li v-for="benefit in member.annualPass.benefits" :key="benefit">{{ benefit }}</li>
          </ul>
          <el-button plain type="success" :disabled="!member.annualPass.id" @click="renewAnnualPass">续费一年</el-button>
        </section>

        <section class="member-panel">
          <div class="member-panel-title compact">
            <h3>优惠券</h3>
            <TicketPercent :size="18" />
          </div>
          <article v-for="coupon in member.availableCoupons" :key="`available-${coupon.id}`" class="coupon-row available">
            <div>
              <strong>{{ coupon.name }}</strong>
              <span>{{ coupon.threshold }} · {{ coupon.expiresAt }} 到期</span>
            </div>
            <el-button type="success" plain @click="member.claimCoupon(coupon.id)">领取</el-button>
          </article>
          <article v-for="coupon in member.coupons" :key="coupon.id" class="coupon-row">
            <div>
              <strong>{{ coupon.name }}</strong>
              <span>{{ coupon.threshold }} · {{ coupon.expiresAt }} 到期</span>
            </div>
            <el-tag :type="coupon.status === '可用' ? 'success' : coupon.status === '已使用' ? 'info' : 'warning'">{{ coupon.status }}</el-tag>
          </article>
        </section>

        <section class="member-panel">
          <div class="member-panel-title compact">
            <h3>消息通知</h3>
            <Bell :size="18" />
          </div>
          <article v-for="notice in member.notifications" :key="notice.id" class="notice-row">
            <strong>{{ notice.title }}</strong>
            <span>{{ notice.content }}</span>
          </article>
        </section>
      </aside>
    </div>

    <el-dialog v-model="profileDialogVisible" :title="editingProfileId ? '编辑常用游客' : '新增常用游客'" width="460px">
      <el-form label-position="top">
        <el-form-item label="姓名"><el-input v-model="profileForm.name" /></el-form-item>
        <el-form-item label="证件号"><el-input v-model="profileForm.idCard" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="profileForm.phone" /></el-form-item>
        <el-form-item label="关系">
          <el-select v-model="profileForm.relation">
            <el-option label="本人" value="本人" />
            <el-option label="子女" value="子女" />
            <el-option label="家人" value="家人" />
            <el-option label="朋友" value="朋友" />
          </el-select>
        </el-form-item>
        <el-form-item><el-checkbox v-model="profileForm.isDefault">设为默认游客</el-checkbox></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

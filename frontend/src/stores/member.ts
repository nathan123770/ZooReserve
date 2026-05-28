import { computed, reactive, ref } from 'vue';
import { defineStore } from 'pinia';
import { memberApi } from '@/api/modules';
import type { AnnualPassRecord, MemberCouponRecord, MemberProfileRecord } from '@/types/api';

export interface MemberProfile {
  id: number;
  name: string;
  idCard: string;
  phone: string;
  relation: string;
  isDefault: boolean;
}

export interface MemberCoupon {
  id: number;
  name: string;
  threshold: string;
  status: '可用' | '已使用' | '已过期' | '锁定中';
  expiresAt: string;
}

export interface AnnualPassView {
  id?: number;
  name: string;
  status: string;
  expiresAt: string;
  boundVisitors: string[];
  benefits: string[];
}

function boolValue(value: boolean | number | undefined) {
  return value === true || value === 1;
}

function normalizeProfile(profile: MemberProfileRecord): MemberProfile {
  return {
    id: profile.id,
    name: profile.name ?? profile.realName ?? '游客',
    idCard: profile.idCard ?? profile.idcard ?? profile.idCardNo ?? '',
    phone: profile.phone,
    relation: profile.relation ?? '家人',
    isDefault: boolValue(profile.isDefault ?? profile.isdefault),
  };
}

function normalizeCoupon(coupon: MemberCouponRecord): MemberCoupon {
  const statusMap: Record<string, MemberCoupon['status']> = {
    UNUSED: '可用',
    LOCKED: '锁定中',
    USED: '已使用',
    EXPIRED: '已过期',
    可用: '可用',
    已使用: '已使用',
    已过期: '已过期',
  };
  const threshold = coupon.threshold ?? (
    coupon.discountType === 'PERCENT'
      ? `满 ${coupon.thresholdAmount ?? 0} 享 ${Number(coupon.discountValue ?? 1) * 10} 折`
      : `满 ${coupon.thresholdAmount ?? 0} 减 ${coupon.discountValue ?? 0}`
  );
  return {
    id: coupon.id,
    name: coupon.name,
    threshold,
    status: statusMap[coupon.status] ?? '可用',
    expiresAt: coupon.expiresAt ?? '-',
  };
}

function splitValue(value: string[] | string | undefined) {
  if (Array.isArray(value)) return value;
  if (!value) return [];
  return value.split(',').filter(Boolean);
}

function normalizeAnnualPass(pass?: AnnualPassRecord): AnnualPassView {
  if (!pass) {
    return { name: '暂无年卡', status: '未开通', expiresAt: '-', boundVisitors: [], benefits: [] };
  }
  return {
    id: pass.id,
    name: pass.name,
    status: pass.status === 'ACTIVE' ? '生效中' : pass.status,
    expiresAt: pass.expiresAt,
    boundVisitors: splitValue(pass.boundVisitors),
    benefits: splitValue(pass.benefits),
  };
}

export const useMemberStore = defineStore('member', () => {
  const profiles = ref<MemberProfile[]>([]);
  const coupons = ref<MemberCoupon[]>([]);
  const annualPasses = ref<AnnualPassView[]>([]);
  const loading = ref(false);
  const notifications = ref([
    { id: 1, title: '预约提醒', content: '支付成功后可在入园凭证页查看二维码。', status: '未读' },
    { id: 2, title: '会员权益', content: '年卡预约仍会占用对应场次库存，请按预约日期入园。', status: '已读' },
  ]);
  const annualPass = reactive<AnnualPassView>(normalizeAnnualPass());
  const defaultProfile = computed(() => profiles.value.find((profile) => profile.isDefault));

  async function loadAll() {
    loading.value = true;
    try {
      const [profileRows, couponRows, passRows] = await Promise.all([
        memberApi.profiles(),
        memberApi.coupons(),
        memberApi.annualPasses(),
      ]);
      profiles.value = profileRows.map(normalizeProfile);
      coupons.value = couponRows.map(normalizeCoupon);
      annualPasses.value = passRows.map(normalizeAnnualPass);
      Object.assign(annualPass, annualPasses.value[0] ?? normalizeAnnualPass());
    } finally {
      loading.value = false;
    }
  }

  async function saveProfile(profile: Omit<MemberProfile, 'id'> & { id?: number }) {
    const saved = profile.id
      ? await memberApi.updateProfile(profile.id, profile)
      : await memberApi.createProfile(profile);
    const normalized = normalizeProfile(saved);
    const index = profiles.value.findIndex((item) => item.id === normalized.id);
    if (index >= 0) profiles.value[index] = normalized;
    else profiles.value.unshift(normalized);
    if (normalized.isDefault) {
      profiles.value = profiles.value.map((item) => ({ ...item, isDefault: item.id === normalized.id }));
    }
    return normalized;
  }

  async function deleteProfile(id: number) {
    await memberApi.deleteProfile(id);
    profiles.value = profiles.value.filter((profile) => profile.id !== id);
  }

  async function setDefault(id: number) {
    const profile = profiles.value.find((item) => item.id === id);
    if (!profile) return;
    await saveProfile({ ...profile, isDefault: true });
  }

  async function renewAnnualPass(id = annualPass.id) {
    if (!id) return;
    await memberApi.renewAnnualPass(id);
    await loadAll();
  }

  return {
    profiles,
    coupons,
    notifications,
    annualPass,
    annualPasses,
    loading,
    defaultProfile,
    loadAll,
    saveProfile,
    deleteProfile,
    setDefault,
    renewAnnualPass,
  };
});

import { computed, reactive, ref } from 'vue';
import { defineStore } from 'pinia';

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
  status: '可用' | '已使用' | '已过期';
  expiresAt: string;
}

export const useMemberStore = defineStore('member', () => {
  const profiles = ref<MemberProfile[]>([
    { id: 1, name: '林小鹿', idCard: '330***********1024', phone: '13800001234', relation: '本人', isDefault: true },
    { id: 2, name: '林一一', idCard: '330***********0528', phone: '13800001234', relation: '子女', isDefault: false },
  ]);
  const coupons = ref<MemberCoupon[]>([
    { id: 1, name: '亲子满减券', threshold: '满 200 减 30', status: '可用', expiresAt: '2026-06-30' },
    { id: 2, name: '夜游活动券', threshold: '活动报名 8 折', status: '可用', expiresAt: '2026-08-31' },
    { id: 3, name: '餐饮小食券', threshold: '满 50 减 10', status: '已使用', expiresAt: '2026-05-20' },
  ]);
  const notifications = ref([
    { id: 1, title: '预约提醒', content: '您有 2026-06-01 上午场门票待入园。', status: '未读' },
    { id: 2, title: '活动通知', content: '小小饲养员亲子课堂报名即将满员。', status: '已读' },
    { id: 3, title: '退款进度', content: '订单 ZR202606010102 已进入退款审核。', status: '已读' },
  ]);
  const annualPass = reactive({
    name: '亲子年卡',
    status: '生效中',
    expiresAt: '2027-05-28',
    boundVisitors: ['林小鹿', '林一一'],
    benefits: ['全年不限次入园', '活动优先报名', '餐饮 95 折'],
  });
  const defaultProfile = computed(() => profiles.value.find((profile) => profile.isDefault));

  function saveProfile(profile: Omit<MemberProfile, 'id'> & { id?: number }) {
    if (profile.id) {
      const index = profiles.value.findIndex((item) => item.id === profile.id);
      if (index >= 0) profiles.value[index] = { ...profile, id: profile.id };
    } else {
      profiles.value.unshift({ ...profile, id: Date.now() });
    }
    if (profile.isDefault) setDefault(profile.id ?? profiles.value[0].id);
  }

  function deleteProfile(id: number) {
    profiles.value = profiles.value.filter((profile) => profile.id !== id);
    if (!profiles.value.some((profile) => profile.isDefault) && profiles.value[0]) {
      profiles.value[0].isDefault = true;
    }
  }

  function setDefault(id: number) {
    profiles.value = profiles.value.map((profile) => ({ ...profile, isDefault: profile.id === id }));
  }

  return { profiles, coupons, notifications, annualPass, defaultProfile, saveProfile, deleteProfile, setDefault };
});

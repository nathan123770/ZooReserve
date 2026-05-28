<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LogIn } from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';
import type { RoleCode } from '@/types/api';
import { normalizeRole, redirectForRole, roleDefaults } from './loginRouting';
import { toast } from '@/utils/message';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);

const roleOptions: Array<{ label: string; value: RoleCode; username: string; password: string; redirect: string }> = [
  { value: 'VISITOR', ...roleDefaults.VISITOR },
  { value: 'ADMIN', ...roleDefaults.ADMIN },
  { value: 'CHECKER', ...roleDefaults.CHECKER },
];

const role = ref<RoleCode>(normalizeRole(route.query.role));
const currentOption = computed(() => roleOptions.find((option) => option.value === role.value) ?? roleOptions[0]);
const title = computed(() => `${currentOption.value.label}登录`);
const username = ref(currentOption.value.username);
const password = ref(currentOption.value.password);

function applyRole(nextRole: RoleCode) {
  role.value = nextRole;
  username.value = currentOption.value.username;
  password.value = currentOption.value.password;
  void router.replace({
    path: '/login',
    query: {
      role: nextRole,
      redirect: redirectForRole(nextRole, route.query.redirect),
    },
  });
}

watch(
  () => route.query.role,
  (nextRole) => applyRole(normalizeRole(nextRole)),
);

async function submit() {
  loading.value = true;
  try {
    await auth.loginWithCredentials(username.value, password.value, role.value);
    toast.success('登录成功');
    await router.replace(redirectForRole(role.value, route.query.redirect));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">ZR</span>
        <span>ZooReserve</span>
      </RouterLink>
      <div>
        <p class="eyebrow">Sign In</p>
        <h1>{{ title }}</h1>
      </div>

      <el-segmented
        v-model="role"
        :options="roleOptions.map((option) => ({ label: option.label, value: option.value }))"
        @change="(value: string | number | boolean) => applyRole(value as RoleCode)"
      />

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="username" size="large" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" size="large" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="success" size="large" :loading="loading" @click="submit">
          <LogIn :size="18" />
          登录进入
        </el-button>
      </el-form>

      <p v-if="role === 'VISITOR'" class="auth-note">
        还没有账号？
        <RouterLink :to="`/register?redirect=${route.query.redirect ?? '/booking'}`">立即注册</RouterLink>
      </p>
      <p v-else class="auth-note">管理员和核销员账号由后台统一创建；当前调试账号已自动填入。</p>

      <div class="login-shortcuts">
        <RouterLink to="/login?role=VISITOR&redirect=/booking">游客预约</RouterLink>
        <RouterLink to="/register?redirect=/booking">游客注册</RouterLink>
        <RouterLink to="/login?role=ADMIN&redirect=/admin">后台管理</RouterLink>
        <RouterLink to="/login?role=CHECKER&redirect=/checkin">核销端</RouterLink>
      </div>
    </section>
  </main>
</template>

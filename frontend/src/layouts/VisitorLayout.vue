<script setup lang="ts">
import { useRouter } from 'vue-router';
import { LogOut } from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const router = useRouter();

async function logout() {
  auth.logout();
  await router.replace('/');
}
</script>

<template>
  <div class="visitor-shell">
    <header class="visitor-header">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">ZR</span>
        <span>ZooReserve</span>
      </RouterLink>
      <nav>
        <RouterLink to="/booking">门票</RouterLink>
        <RouterLink to="/activities">活动</RouterLink>
        <RouterLink to="/orders">订单</RouterLink>
        <RouterLink to="/pass">凭证</RouterLink>
        <RouterLink to="/guide">导览</RouterLink>
        <RouterLink to="/member">会员</RouterLink>
      </nav>
      <RouterLink v-if="!auth.isAuthenticated" class="role-chip" to="/login?role=VISITOR&redirect=/">登录</RouterLink>
      <div v-else class="visitor-userbar">
        <span class="role-chip">{{ auth.user?.displayName }}</span>
        <button class="icon-logout" type="button" title="退出登录" aria-label="退出登录" @click="logout">
          <LogOut :size="16" />
        </button>
      </div>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

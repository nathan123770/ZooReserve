<script setup lang="ts">
import { useRouter } from 'vue-router';
import { LogOut } from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const router = useRouter();

async function logout() {
  auth.logout();
  await router.replace('/login?role=CHECKER&redirect=/checkin');
}
</script>

<template>
  <div class="checkin-shell">
    <header>
      <RouterLink to="/checkin">入口核销工作台</RouterLink>
      <div class="admin-userbar">
        <span>{{ auth.user?.displayName ?? '核销员' }}</span>
        <button class="plain-logout" type="button" @click="logout">
          <LogOut :size="16" />
          退出登录
        </button>
      </div>
    </header>
    <RouterView />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { LogOut } from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const router = useRouter();

async function logout() {
  auth.logout();
  await router.replace('/login?role=ADMIN&redirect=/admin');
}
</script>

<template>
  <div class="admin-shell">
    <aside>
      <div class="admin-logo">ZooReserve</div>
      <RouterLink to="/admin" active-class="" exact-active-class="router-link-exact-active">数据看板</RouterLink>
      <RouterLink to="/admin/tickets">票务管理</RouterLink>
      <RouterLink to="/admin/orders">订单管理</RouterLink>
      <RouterLink to="/admin/activities">活动管理</RouterLink>
      <RouterLink to="/admin/animals">动物展区</RouterLink>
      <RouterLink to="/admin/checkins">核销管理</RouterLink>
      <RouterLink to="/admin/marketing">营销管理</RouterLink>
      <RouterLink to="/admin/system">系统管理</RouterLink>
    </aside>
    <section>
      <header>
        <strong>动物园预约管理后台</strong>
        <div class="admin-userbar">
          <span>{{ auth.user?.displayName ?? '管理员' }}</span>
          <el-button size="small" @click="logout">
            <LogOut :size="16" />
            退出登录
          </el-button>
        </div>
      </header>
      <main>
        <RouterView />
      </main>
    </section>
  </div>
</template>

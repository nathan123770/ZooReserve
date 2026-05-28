<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { UserPlus } from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/utils/message';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const form = ref({
  username: '',
  displayName: '',
  phone: '',
  password: '',
  confirmPassword: '',
});

async function submit() {
  if (!form.value.username || !form.value.password || !form.value.phone) {
    toast.warning('请填写账号、手机号和密码');
    return;
  }
  if (form.value.password !== form.value.confirmPassword) {
    toast.warning('两次输入的密码不一致');
    return;
  }
  loading.value = true;
  try {
    await auth.register({
      username: form.value.username,
      password: form.value.password,
      phone: form.value.phone,
      displayName: form.value.displayName || form.value.username,
    });
    toast.success('注册成功');
    await router.replace(String(route.query.redirect ?? '/booking'));
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
        <p class="eyebrow">Create Account</p>
        <h1>游客注册</h1>
      </div>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" size="large" autocomplete="username" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.displayName" size="large" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" size="large" autocomplete="tel" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" size="large" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" size="large" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-button type="success" size="large" :loading="loading" @click="submit">
          <UserPlus :size="18" />
          注册并进入
        </el-button>
      </el-form>
      <div class="login-shortcuts">
        <RouterLink to="/login?role=VISITOR&redirect=/booking">已有账号，去登录</RouterLink>
        <RouterLink to="/">返回首页</RouterLink>
      </div>
    </section>
  </main>
</template>

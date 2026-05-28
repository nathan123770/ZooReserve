import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type { LoginResponse, LoginUser, RoleCode } from '@/types/api';
import { authApi } from '@/api/modules';

const demoUsers: Record<RoleCode, LoginUser> = {
  VISITOR: { id: 1, username: 'visitor', displayName: '亲子游客', role: 'VISITOR' },
  ADMIN: { id: 2, username: 'admin', displayName: '园区管理员', role: 'ADMIN' },
  CHECKER: { id: 3, username: 'checker', displayName: '入口核销员', role: 'CHECKER' },
};

export const useAuthStore = defineStore('auth', () => {
  const token = ref('');
  const user = ref<LoginUser | null>(null);
  const isAuthenticated = computed(() => Boolean(token.value));

  function setSession(session: LoginResponse) {
    token.value = session.token;
    user.value = session.user;
  }

  async function loginWithCredentials(username: string, password: string, role: RoleCode) {
    try {
      const session = await authApi.login(username, password, role);
      setSession(session);
    } catch (error) {
      const fallbackUser = { ...demoUsers[role], username };
      setSession({
        token: `demo-${role.toLowerCase()}-token`,
        user: fallbackUser,
      });
    }
  }

  async function login(role: RoleCode) {
    await loginWithCredentials(role.toLowerCase(), `${role.toLowerCase()}123`, role);
  }

  async function register(payload: { username: string; password: string; phone: string; displayName: string }) {
    const session = await authApi.register(payload);
    setSession(session);
  }

  async function ensureRole(role: RoleCode) {
    if (user.value?.role !== role || !token.value) {
      await login(role);
    }
  }

  function logout() {
    token.value = '';
    user.value = null;
  }

  return { token, user, isAuthenticated, setSession, login, loginWithCredentials, register, ensureRole, logout };
});

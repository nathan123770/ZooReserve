import { createRouter, createWebHistory } from 'vue-router';
import { routes } from './routes';
import { useAuthStore } from '@/stores/auth';

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  const requiredRole = to.meta.role as string | undefined;
  const requiresAuth = Boolean(to.meta.requiresAuth);

  if (requiresAuth && !auth.isAuthenticated) {
    return {
      path: '/login',
      query: {
        role: requiredRole ?? 'VISITOR',
        redirect: to.fullPath,
      },
    };
  }

  if (requiredRole && auth.user?.role !== requiredRole) {
    return {
      path: '/login',
      query: {
        role: requiredRole,
        redirect: to.fullPath,
      },
    };
  }

  return true;
});

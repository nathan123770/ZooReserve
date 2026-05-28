import type { RoleCode } from '@/types/api';

export const roleDefaults: Record<RoleCode, { username: string; password: string; redirect: string; label: string }> = {
  VISITOR: { label: '游客', username: 'visitor', password: 'visitor123', redirect: '/' },
  ADMIN: { label: '管理员', username: 'admin', password: 'admin123', redirect: '/admin' },
  CHECKER: { label: '核销员', username: 'checker', password: 'checker123', redirect: '/checkin' },
};

export function normalizeRole(value: unknown): RoleCode {
  const candidate = String(value ?? 'VISITOR').toUpperCase();
  return ['VISITOR', 'ADMIN', 'CHECKER'].includes(candidate) ? (candidate as RoleCode) : 'VISITOR';
}

export function redirectForRole(role: RoleCode, requestedRedirect: unknown): string {
  const redirect = typeof requestedRedirect === 'string' ? requestedRedirect : '';
  if (role === 'ADMIN') return redirect.startsWith('/admin') ? redirect : roleDefaults.ADMIN.redirect;
  if (role === 'CHECKER') return redirect.startsWith('/checkin') ? redirect : roleDefaults.CHECKER.redirect;
  if (redirect.startsWith('/admin') || redirect.startsWith('/checkin') || redirect.startsWith('/login')) {
    return roleDefaults.VISITOR.redirect;
  }
  return redirect || roleDefaults.VISITOR.redirect;
}

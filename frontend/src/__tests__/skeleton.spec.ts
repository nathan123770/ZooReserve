import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { routes } from '../router/routes';
import { orderStatusText, paymentStatusText, checkinStatusText } from '../utils/status';
import { useAuthStore } from '../stores/auth';
import { bookingTickets, useBookingStore } from '../stores/booking';
import { useMemberStore } from '../stores/member';
import { adminModules } from '../views/admin/adminModules';
import { redirectForRole } from '../views/auth/loginRouting';

describe('frontend skeleton contract', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('declares visitor, admin, and checker route families', () => {
    const paths = routes.map((route) => route.path);

    expect(paths).toContain('/');
    expect(paths).toContain('/login');
    expect(paths).toContain('/register');
    expect(paths).toContain('/admin');
    expect(paths).toContain('/checkin');
  });

  it('marks private visitor/admin/checker pages with auth metadata', () => {
    const visitor = routes.find((route) => route.path === '/');
    const visitorPrivatePaths = visitor?.children?.filter((route) => route.meta?.requiresAuth).map((route) => route.path);
    const admin = routes.find((route) => route.path === '/admin');
    const checker = routes.find((route) => route.path === '/checkin');

    expect(visitorPrivatePaths).toEqual(expect.arrayContaining(['booking', 'orders', 'pass', 'member']));
    expect(admin?.meta?.role).toBe('ADMIN');
    expect(checker?.meta?.role).toBe('CHECKER');
  });

  it('defines complete admin module configs for every backstage menu', () => {
    const expectedDomains = ['tickets', 'orders', 'activities', 'animals', 'checkins', 'marketing', 'system'];

    expect(Object.keys(adminModules)).toEqual(expect.arrayContaining(expectedDomains));
    for (const domain of expectedDomains) {
      expect(adminModules[domain].columns.length).toBeGreaterThanOrEqual(5);
      expect(adminModules[domain].records.length).toBeGreaterThanOrEqual(3);
      expect(adminModules[domain].formFields.length).toBeGreaterThanOrEqual(3);
    }
  });

  it('uses session-specific ticket inventory and selected ticket totals', () => {
    const booking = useBookingStore();

    booking.setQuantity('ADULT', 2);
    expect(booking.selectedItems).toEqual([{ ticketTypeCode: 'ADULT', quantity: 2 }]);
    expect(booking.totalAmount).toBe(240);

    const amRemaining = booking.totalRemaining;
    booking.setSession('PM');

    expect(booking.totalRemaining).not.toBe(amRemaining);
    expect(bookingTickets.length).toBeGreaterThanOrEqual(4);
  });

  it('supports member profile defaulting and coupon state', () => {
    const member = useMemberStore();

    member.saveProfile({
      name: '测试游客',
      idCard: '110***********0001',
      phone: '13600000000',
      relation: '亲友',
      isDefault: true,
    });

    expect(member.defaultProfile?.name).toBe('测试游客');
    expect(member.coupons.some((coupon) => coupon.status === '可用')).toBe(true);
    expect(member.notifications.length).toBeGreaterThanOrEqual(3);
  });

  it('keeps role switch redirects inside the selected role area', () => {
    expect(redirectForRole('ADMIN', '/')).toBe('/admin');
    expect(redirectForRole('CHECKER', '/booking')).toBe('/checkin');
    expect(redirectForRole('VISITOR', '/admin/tickets')).toBe('/');
    expect(redirectForRole('VISITOR', '/booking')).toBe('/booking');
  });

  it('maps backend status codes to Chinese labels', () => {
    expect(orderStatusText.PAID).toBe('已预约');
    expect(paymentStatusText.PAY_SUCCESS).toBe('支付成功');
    expect(checkinStatusText.CHECKED_IN).toBe('已核销');
  });

  it('auth store keeps token and role payload', () => {
    const auth = useAuthStore();

    auth.setSession({
      token: 'mock-token',
      user: { id: 1, username: 'admin', displayName: '园区管理员', role: 'ADMIN' },
    });

    expect(auth.token).toBe('mock-token');
    expect(auth.user?.role).toBe('ADMIN');
    expect(auth.isAuthenticated).toBe(true);

    auth.logout();

    expect(auth.token).toBe('');
    expect(auth.user).toBeNull();
    expect(auth.isAuthenticated).toBe(false);
  });
});

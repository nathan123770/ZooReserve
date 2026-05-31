import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { memberApi } from '../api/modules';
import { routes } from '../router/routes';
import { orderStatusText, paymentStatusText, checkinStatusText } from '../utils/status';
import { useAuthStore } from '../stores/auth';
import { bookingTickets, useBookingStore } from '../stores/booking';
import { useMemberStore } from '../stores/member';
import { adminModules } from '../views/admin/adminModules';
import { redirectForRole } from '../views/auth/loginRouting';

vi.mock('../api/modules', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/modules')>();
  return {
    ...actual,
    memberApi: {
      ...actual.memberApi,
      profiles: vi.fn(),
      coupons: vi.fn(),
      availableCoupons: vi.fn(),
      notices: vi.fn(),
      annualPasses: vi.fn(),
      createProfile: vi.fn(),
    },
  };
});

describe('frontend skeleton contract', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(memberApi.profiles).mockReset();
    vi.mocked(memberApi.coupons).mockReset();
    vi.mocked(memberApi.availableCoupons).mockReset();
    vi.mocked(memberApi.notices).mockReset();
    vi.mocked(memberApi.annualPasses).mockReset();
    vi.mocked(memberApi.createProfile).mockReset();
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
    expect(bookingTickets.length).toBeGreaterThanOrEqual(3);
  });

  it('supports member profile defaulting and coupon state', async () => {
    vi.mocked(memberApi.profiles).mockResolvedValueOnce([]);
    vi.mocked(memberApi.coupons).mockResolvedValueOnce([{ id: 1, name: '新客券', threshold: '满 200 减 30', status: 'UNUSED' }]);
    vi.mocked(memberApi.availableCoupons).mockResolvedValueOnce([]);
    vi.mocked(memberApi.notices).mockResolvedValueOnce([
      { id: 1, title: '预约提醒', content: '支付成功后可查看二维码。', status: 'PUBLISHED' },
      { id: 2, title: '会员权益', content: '年卡预约仍会占用库存。', status: 'PUBLISHED' },
    ]);
    vi.mocked(memberApi.annualPasses).mockResolvedValueOnce([]);
    vi.mocked(memberApi.createProfile).mockResolvedValueOnce({
      id: 99,
      name: '测试游客',
      idCard: '110***********0001',
      phone: '13600000000',
      relation: '亲友',
      isDefault: true,
    });
    const member = useMemberStore();

    await member.loadAll();
    await member.saveProfile({
      name: '测试游客',
      idCard: '110***********0001',
      phone: '13600000000',
      relation: '亲友',
      isDefault: true,
    });

    expect(member.defaultProfile?.name).toBe('测试游客');
    expect(member.coupons.some((coupon) => coupon.status === '可用')).toBe(true);
    expect(member.notifications.length).toBeGreaterThanOrEqual(2);
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

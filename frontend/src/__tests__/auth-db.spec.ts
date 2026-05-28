import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { authApi, memberApi, orderApi, paymentApi } from '../api/modules';
import { useAuthStore } from '../stores/auth';
import { useMemberStore } from '../stores/member';
import { useOrderStore } from '../stores/order';

vi.mock('../api/modules', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
  },
  orderApi: {
    create: vi.fn(),
    my: vi.fn(),
    qrcode: vi.fn(),
    cancel: vi.fn(),
    refund: vi.fn(),
  },
  paymentApi: {
    prepay: vi.fn(),
  },
  memberApi: {
    profiles: vi.fn(),
    coupons: vi.fn(),
    annualPasses: vi.fn(),
    createProfile: vi.fn(),
    updateProfile: vi.fn(),
    deleteProfile: vi.fn(),
    claimCoupon: vi.fn(),
    purchaseAnnualPass: vi.fn(),
    renewAnnualPass: vi.fn(),
    addAnnualPassHolder: vi.fn(),
  },
}));

describe('database-backed auth behavior', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(authApi.login).mockReset();
    vi.mocked(orderApi.create).mockReset();
    vi.mocked(paymentApi.prepay).mockReset();
    vi.mocked(memberApi.profiles).mockReset();
    vi.mocked(memberApi.coupons).mockReset();
    vi.mocked(memberApi.annualPasses).mockReset();
  });

  it('does not create a demo session when login fails', async () => {
    vi.mocked(authApi.login).mockRejectedValueOnce(new Error('unauthorized'));
    const auth = useAuthStore();

    await expect(auth.loginWithCredentials('admin', 'wrong', 'ADMIN')).rejects.toThrow('unauthorized');

    expect(auth.token).toBe('');
    expect(auth.user).toBeNull();
    expect(auth.isAuthenticated).toBe(false);
  });

  it('creates reservation orders through the order API', async () => {
    vi.mocked(orderApi.create).mockResolvedValueOnce({
      id: 99,
      orderNo: 'ZR202606010099',
      visitDate: '2026-06-01',
      session: 'AM',
      peopleCount: 2,
      amount: 240,
      orderStatus: 'PENDING_PAYMENT',
      paymentStatus: 'UNPAID',
      createdAt: '2026-05-28T10:00:00',
    });
    const order = useOrderStore();

    const created = await order.createReservation({
      visitDate: '2026-06-01',
      session: 'AM',
      items: [{ ticketTypeCode: 'ADULT', quantity: 2 }],
    });

    expect(orderApi.create).toHaveBeenCalledWith({
      visitDate: '2026-06-01',
      session: 'AM',
      items: [{ ticketTypeCode: 'ADULT', quantity: 2 }],
    });
    expect(created.orderNo).toBe('ZR202606010099');
    expect(order.orders[0]?.orderNo).toBe('ZR202606010099');
  });

  it('pays pending reservation orders through the payment API', async () => {
    vi.mocked(paymentApi.prepay).mockResolvedValueOnce({
      orderNo: 'ZR202606010099',
      channel: 'MOCK',
      amount: 240,
      paymentStatus: 'PAY_SUCCESS',
      mockPayUrl: 'mock://pay/ZR202606010099',
    });
    const order = useOrderStore();
    order.orders = [{
      id: 99,
      orderNo: 'ZR202606010099',
      visitDate: '2026-06-01',
      session: 'AM',
      peopleCount: 2,
      amount: 240,
      orderStatus: 'PENDING_PAYMENT',
      paymentStatus: 'UNPAID',
      createdAt: '2026-05-28T10:00:00',
    }];

    await order.payOrder(order.orders[0]);

    expect(paymentApi.prepay).toHaveBeenCalledWith('ZR202606010099');
    expect(order.orders[0].orderStatus).toBe('PAID');
    expect(order.orders[0].paymentStatus).toBe('PAY_SUCCESS');
  });

  it('loads member center data from member APIs', async () => {
    vi.mocked(memberApi.profiles).mockResolvedValueOnce([{ id: 1, name: '林小鹿', idCard: '330', phone: '138', relation: '本人', isDefault: true }]);
    vi.mocked(memberApi.coupons).mockResolvedValueOnce([{ id: 1, name: '新客券', threshold: '满 200 减 30', status: '可用', expiresAt: '2026-12-31' }]);
    vi.mocked(memberApi.annualPasses).mockResolvedValueOnce([{ id: 1, name: '亲子年卡', status: 'ACTIVE', expiresAt: '2027-05-28', boundVisitors: ['林小鹿'], benefits: ['全年入园'] }]);
    const member = useMemberStore();

    await member.loadAll();

    expect(memberApi.profiles).toHaveBeenCalled();
    expect(memberApi.coupons).toHaveBeenCalled();
    expect(memberApi.annualPasses).toHaveBeenCalled();
    expect(member.defaultProfile?.name).toBe('林小鹿');
    expect(member.annualPass.name).toBe('亲子年卡');
  });
});

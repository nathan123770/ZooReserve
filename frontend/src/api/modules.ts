import { getData, postData } from './http';
import type { ActivityRecord, LoginResponse, OrderRecord, RoleCode, TicketInventory, TicketType } from '@/types/api';

export const authApi = {
  login: (username: string, password: string, role: RoleCode) =>
    postData<LoginResponse>('/auth/login', { username, password, role }),
  register: (payload: { username: string; password: string; phone: string; displayName: string }) =>
    postData<LoginResponse>('/auth/register', payload),
};

export const ticketApi = {
  types: () => getData<TicketType[]>('/tickets/types'),
  inventory: (date: string, session: string) => getData<TicketInventory[]>('/tickets/inventory', { date, session }),
};

export const orderApi = {
  create: (payload: unknown) => postData<OrderRecord>('/orders', payload),
  my: () => getData<OrderRecord[]>('/orders/my'),
  qrcode: (id: number) => getData<{ qrContent: string; entranceNotice: string }>(`/orders/${id}/qrcode`),
};

export const activityApi = {
  list: () => getData<ActivityRecord[]>('/activities'),
  signup: (id: number) => postData<{ status: string }>(`/activities/${id}/signup`),
};

export const adminApi = {
  dashboard: () => getData<Record<string, unknown>>('/admin/dashboard/summary'),
  records: (domain: string) => getData<{ records: unknown[] }>(domain === 'orders' ? '/admin/orders' : `/admin/${domain}`),
};

export const checkinApi = {
  scan: (qrContent: string) => postData<Record<string, unknown>>('/checkin/scan', { qrContent, checkerId: 1 }),
  search: (keyword: string) => getData<Record<string, unknown>>('/checkin/order/search', { orderNo: keyword, phone: keyword }),
  manual: (payload: unknown) => postData<Record<string, unknown>>('/checkin/manual', payload),
};

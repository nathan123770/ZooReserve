import { ref } from 'vue';
import { defineStore } from 'pinia';
import { orderApi, paymentApi } from '@/api/modules';
import type { OrderRecord } from '@/types/api';
import type { SelectedTicket } from './booking';

export const useOrderStore = defineStore('order', () => {
  const orders = ref<OrderRecord[]>([]);
  const loading = ref(false);

  async function loadMine() {
    loading.value = true;
    try {
      orders.value = await orderApi.my();
    } finally {
      loading.value = false;
    }
  }

  async function createReservation(payload: {
    visitDate: string;
    session: string;
    items: SelectedTicket[];
    couponId?: number;
    annualPassId?: number;
    visitorProfileIds?: number[];
    orderType?: string;
  }) {
    const order = await orderApi.create(payload);
    orders.value = [order, ...orders.value];
    return order;
  }

  function replaceOrder(order: OrderRecord) {
    const index = orders.value.findIndex((item) => item.id === order.id || item.orderNo === order.orderNo);
    if (index >= 0) {
      orders.value[index] = order;
    } else {
      orders.value.unshift(order);
    }
  }

  async function payOrder(order: OrderRecord) {
    const payment = await paymentApi.prepay(order.orderNo);
    replaceOrder({ ...order, orderStatus: 'PAID', paymentStatus: payment.paymentStatus });
    return payment;
  }

  async function cancelOrder(order: OrderRecord) {
    const updated = await orderApi.cancel(order.id);
    replaceOrder(updated);
    return updated;
  }

  async function refundOrder(order: OrderRecord) {
    const updated = await orderApi.refund(order.id);
    replaceOrder(updated);
    return updated;
  }

  async function createSampleOrder() {
    return createReservation({
      visitDate: '2026-06-01',
      session: 'AM',
      items: [{ ticketTypeCode: 'ADULT', quantity: 2 }],
    });
  }

  return { orders, loading, loadMine, createReservation, payOrder, cancelOrder, refundOrder, createSampleOrder };
});

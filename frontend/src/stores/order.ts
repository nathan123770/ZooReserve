import { ref } from 'vue';
import { defineStore } from 'pinia';
import { orderApi } from '@/api/modules';
import type { OrderRecord } from '@/types/api';

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

  async function createSampleOrder() {
    const order = await orderApi.create({
      visitDate: '2026-06-01',
      session: 'AM',
      items: [{ ticketTypeCode: 'ADULT', quantity: 2 }],
    });
    orders.value = [order, ...orders.value];
    return order;
  }

  return { orders, loading, loadMine, createSampleOrder };
});

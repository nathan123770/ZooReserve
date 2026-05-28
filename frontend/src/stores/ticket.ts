import { ref } from 'vue';
import { defineStore } from 'pinia';
import { ticketApi } from '@/api/modules';
import type { TicketInventory, TicketType } from '@/types/api';

export const useTicketStore = defineStore('ticket', () => {
  const ticketTypes = ref<TicketType[]>([]);
  const inventory = ref<TicketInventory[]>([]);
  const loading = ref(false);

  async function load(date = '2026-06-01', session = 'AM') {
    loading.value = true;
    try {
      const [types, stocks] = await Promise.all([ticketApi.types(), ticketApi.inventory(date, session)]);
      ticketTypes.value = types;
      inventory.value = stocks;
    } finally {
      loading.value = false;
    }
  }

  return { ticketTypes, inventory, loading, load };
});

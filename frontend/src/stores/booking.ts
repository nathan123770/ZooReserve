import { computed, reactive, ref } from 'vue';
import { defineStore } from 'pinia';
import { ticketApi } from '@/api/modules';

export interface BookingTicket {
  code: string;
  name: string;
  description: string;
  price: number;
}

export interface SessionInventory {
  AM: Record<string, number>;
  PM: Record<string, number>;
}

export interface SelectedTicket {
  ticketTypeCode: string;
  quantity: number;
}

export const bookingTickets: BookingTicket[] = [
  { code: 'ADULT', name: '成人票', description: '18 周岁以上游客', price: 120 },
  { code: 'CHILD', name: '儿童票', description: '1.2 米至 1.5 米儿童', price: 60 },
  { code: 'SENIOR', name: '老人票', description: '65 周岁以上游客', price: 60 },
];

export const annualPassProducts: BookingTicket[] = [
  { code: 'ANNUAL', name: '亲子年卡', description: '两大一小全年畅游，购买后支付并绑定游客后生效', price: 699 },
];

const initialInventory: SessionInventory = {
  AM: { ADULT: 520, CHILD: 318, SENIOR: 396 },
  PM: { ADULT: 260, CHILD: 184, SENIOR: 210 },
};

export const useBookingStore = defineStore('booking', () => {
  const visitDate = ref('2026-06-01');
  const session = ref<'AM' | 'PM'>('AM');
  const sessionInventory = reactive<SessionInventory>(structuredClone(initialInventory));
  const dailyInventory = reactive<Record<string, number>>({});
  const loadingInventory = ref(false);
  const selectedTickets = reactive<Record<string, number>>({});

  const currentInventory = computed(() => sessionInventory[session.value]);
  const totalRemaining = computed(() => Object.values(currentInventory.value).reduce((sum, value) => sum + value, 0));
  const dailyRemaining = computed(() => Object.values(dailyInventory).reduce((sum, value) => sum + value, 0));
  const selectedItems = computed<SelectedTicket[]>(() =>
    Object.entries(selectedTickets)
      .filter(([, quantity]) => quantity > 0)
      .map(([ticketTypeCode, quantity]) => ({ ticketTypeCode, quantity })),
  );
  const selectedCount = computed(() => selectedItems.value.reduce((sum, item) => sum + item.quantity, 0));
  const totalAmount = computed(() =>
    selectedItems.value.reduce((sum, item) => {
      const ticket = bookingTickets.find((entry) => entry.code === item.ticketTypeCode);
      return sum + (ticket?.price ?? 0) * item.quantity;
    }, 0),
  );

  function setSession(value: 'AM' | 'PM') {
    session.value = value;
  }

  async function loadInventory() {
    loadingInventory.value = true;
    try {
      const stocks = await ticketApi.inventory(visitDate.value, session.value);
      const current = sessionInventory[session.value];
      for (const key of Object.keys(current)) {
        delete current[key];
      }
      for (const key of Object.keys(dailyInventory)) {
        delete dailyInventory[key];
      }
      for (const stock of stocks) {
        current[stock.ticketTypeCode] = stock.remaining;
        dailyInventory[stock.ticketTypeCode] = stock.dailyRemaining;
      }
      resetSelection();
    } finally {
      loadingInventory.value = false;
    }
  }

  function setQuantity(ticketCode: string, quantity: number) {
    const max = currentInventory.value[ticketCode] ?? 0;
    selectedTickets[ticketCode] = Math.max(0, Math.min(quantity, max));
  }

  function resetSelection() {
    for (const ticket of bookingTickets) {
      selectedTickets[ticket.code] = 0;
    }
  }

  function commitOrder() {
    for (const item of selectedItems.value) {
      currentInventory.value[item.ticketTypeCode] -= item.quantity;
      dailyInventory[item.ticketTypeCode] = Math.max(0, (dailyInventory[item.ticketTypeCode] ?? 0) - item.quantity);
    }
    const orderItems = selectedItems.value;
    resetSelection();
    return orderItems;
  }

  return {
    visitDate,
    session,
    sessionInventory,
    dailyInventory,
    loadingInventory,
    selectedTickets,
    currentInventory,
    totalRemaining,
    dailyRemaining,
    selectedItems,
    selectedCount,
    totalAmount,
    setSession,
    loadInventory,
    setQuantity,
    resetSelection,
    commitOrder,
  };
});

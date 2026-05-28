import { computed, reactive, ref } from 'vue';
import { defineStore } from 'pinia';

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
  { code: 'ANNUAL', name: '亲子年卡', description: '两大一小全年畅游', price: 699 },
];

const initialInventory: SessionInventory = {
  AM: { ADULT: 520, CHILD: 318, SENIOR: 396, ANNUAL: 42 },
  PM: { ADULT: 260, CHILD: 184, SENIOR: 210, ANNUAL: 28 },
};

export const useBookingStore = defineStore('booking', () => {
  const visitDate = ref('2026-06-01');
  const session = ref<'AM' | 'PM'>('AM');
  const sessionInventory = reactive<SessionInventory>(structuredClone(initialInventory));
  const selectedTickets = reactive<Record<string, number>>({});

  const currentInventory = computed(() => sessionInventory[session.value]);
  const totalRemaining = computed(() => Object.values(currentInventory.value).reduce((sum, value) => sum + value, 0));
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
    }
    const orderItems = selectedItems.value;
    resetSelection();
    return orderItems;
  }

  return {
    visitDate,
    session,
    sessionInventory,
    selectedTickets,
    currentInventory,
    totalRemaining,
    selectedItems,
    selectedCount,
    totalAmount,
    setSession,
    setQuantity,
    resetSelection,
    commitOrder,
  };
});

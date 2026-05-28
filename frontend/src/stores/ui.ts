import { ref } from 'vue';
import { defineStore } from 'pinia';

export const useUiStore = defineStore('ui', () => {
  const adminSidebarCollapsed = ref(false);
  const visitorTheme = ref<'family' | 'fresh'>('family');

  function toggleAdminSidebar() {
    adminSidebarCollapsed.value = !adminSidebarCollapsed.value;
  }

  return { adminSidebarCollapsed, visitorTheme, toggleAdminSidebar };
});

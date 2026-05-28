import type { RouteRecordRaw } from 'vue-router';
import VisitorLayout from '@/layouts/VisitorLayout.vue';
import AdminLayout from '@/layouts/AdminLayout.vue';
import CheckinLayout from '@/layouts/CheckinLayout.vue';
import LoginPage from '@/views/auth/LoginPage.vue';
import RegisterPage from '@/views/auth/RegisterPage.vue';
import VisitorHome from '@/views/visitor/VisitorHome.vue';
import TicketBooking from '@/views/visitor/TicketBooking.vue';
import ActivityBooking from '@/views/visitor/ActivityBooking.vue';
import MyOrders from '@/views/visitor/MyOrders.vue';
import EntryPass from '@/views/visitor/EntryPass.vue';
import GuideMap from '@/views/visitor/GuideMap.vue';
import MemberCenter from '@/views/visitor/MemberCenter.vue';
import AdminDashboard from '@/views/admin/AdminDashboard.vue';
import AdminTablePage from '@/views/admin/AdminTablePage.vue';
import CheckinWorkstation from '@/views/checkin/CheckinWorkstation.vue';

export const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/register', name: 'register', component: RegisterPage },
  {
    path: '/',
    component: VisitorLayout,
    children: [
      { path: '', name: 'visitor-home', component: VisitorHome },
      { path: 'booking', name: 'ticket-booking', component: TicketBooking, meta: { requiresAuth: true, role: 'VISITOR' } },
      { path: 'activities', name: 'activity-booking', component: ActivityBooking },
      { path: 'orders', name: 'my-orders', component: MyOrders, meta: { requiresAuth: true, role: 'VISITOR' } },
      { path: 'pass', name: 'entry-pass', component: EntryPass, meta: { requiresAuth: true, role: 'VISITOR' } },
      { path: 'guide', name: 'guide-map', component: GuideMap },
      { path: 'member', name: 'member-center', component: MemberCenter, meta: { requiresAuth: true, role: 'VISITOR' } },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', name: 'admin-dashboard', component: AdminDashboard },
      { path: 'tickets', name: 'admin-tickets', component: AdminTablePage, meta: { title: '票务管理', domain: 'tickets' } },
      { path: 'orders', name: 'admin-orders', component: AdminTablePage, meta: { title: '订单管理', domain: 'orders' } },
      { path: 'activities', name: 'admin-activities', component: AdminTablePage, meta: { title: '活动管理', domain: 'activities' } },
      { path: 'animals', name: 'admin-animals', component: AdminTablePage, meta: { title: '动物/展区管理', domain: 'animals' } },
      { path: 'checkins', name: 'admin-checkins', component: AdminTablePage, meta: { title: '核销管理', domain: 'checkins' } },
      { path: 'marketing', name: 'admin-marketing', component: AdminTablePage, meta: { title: '营销管理', domain: 'marketing' } },
      { path: 'system', name: 'admin-system', component: AdminTablePage, meta: { title: '系统管理', domain: 'system' } },
    ],
  },
  {
    path: '/checkin',
    component: CheckinLayout,
    meta: { requiresAuth: true, role: 'CHECKER' },
    children: [{ path: '', name: 'checkin-workstation', component: CheckinWorkstation }],
  },
];

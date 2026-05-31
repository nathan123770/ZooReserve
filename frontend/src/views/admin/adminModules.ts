import {
  CalendarCheck,
  ChartNoAxesColumnIncreasing,
  ClipboardCheck,
  Megaphone,
  PawPrint,
  Settings,
  ShieldCheck,
  Ticket,
} from 'lucide-vue-next';
import type { Component } from 'vue';

export interface AdminMetric {
  label: string;
  value: string;
  trend: string;
}

export interface AdminFilter {
  key: string;
  label: string;
  type: 'text' | 'select' | 'date';
  placeholder?: string;
  options?: string[];
}

export interface AdminColumn {
  prop: string;
  label: string;
  width?: number;
  minWidth?: number;
  status?: boolean;
}

export interface AdminFormField {
  key: string;
  label: string;
  type: 'text' | 'number' | 'select' | 'date' | 'datetime' | 'textarea';
  options?: string[];
}

export interface AdminRowAction {
  key: string;
  label: string;
  variant?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
}

export interface AdminModuleConfig {
  domain: string;
  title: string;
  subtitle: string;
  icon: Component;
  primaryAction: string;
  canCreate: boolean;
  canEdit: boolean;
  canStatus: boolean;
  metrics: AdminMetric[];
  filters: AdminFilter[];
  columns: AdminColumn[];
  records: Record<string, unknown>[];
  formFields: AdminFormField[];
  rowActions: AdminRowAction[];
}

const statusOptions = ['ENABLED', 'DISABLED'];
const commonFilters: AdminFilter[] = [
  { key: 'keyword', label: '关键词', type: 'text', placeholder: '输入名称、编号或手机号' },
  { key: 'status', label: '状态', type: 'select', placeholder: '全部状态', options: ['ENABLED', 'DISABLED', 'PUBLISHED', 'VISIBLE', 'CHECKED_IN'] },
];

export const adminModules: Record<string, AdminModuleConfig> = {
  tickets: {
    domain: 'tickets',
    title: '票务管理',
    subtitle: '按日期和场次维护票种、价格与库存，避免聚合库存误改。',
    icon: Ticket,
    primaryAction: '新增票种',
    canCreate: true,
    canEdit: true,
    canStatus: true,
    metrics: [
      { label: '在售票种', value: '3', trend: '普通票独立管理' },
      { label: '库存维度', value: '日期/场次', trend: '上午、下午分开' },
      { label: '库存闭环', value: '已接入', trend: '游客端同步读取' },
    ],
    filters: [
      ...commonFilters,
      { key: 'visitDate', label: '库存日期', type: 'date' },
      { key: 'session', label: '场次', type: 'select', options: ['AM', 'PM'] },
    ],
    columns: [
      { prop: 'code', label: '票种编码', width: 120 },
      { prop: 'name', label: '票种名称', minWidth: 140 },
      { prop: 'price', label: '价格', width: 100 },
      { prop: 'visitDate', label: '库存日期', width: 120 },
      { prop: 'session', label: '场次', width: 90 },
      { prop: 'dailyCapacity', label: '当日总容量', width: 120 },
      { prop: 'dailyRemaining', label: '当日总剩余', width: 120 },
      { prop: 'capacity', label: '容量', width: 90 },
      { prop: 'remaining', label: '剩余', width: 90 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, code: 'ADULT', name: '成人票', price: 120, visitDate: '2026-06-01', session: 'AM', dailyCapacity: 1200, dailyRemaining: 1040, capacity: 600, remaining: 520, status: 'ENABLED' },
      { id: 2, code: 'CHILD', name: '儿童票', price: 60, visitDate: '2026-06-01', session: 'AM', dailyCapacity: 700, dailyRemaining: 618, capacity: 350, remaining: 318, status: 'ENABLED' },
      { id: 3, code: 'SENIOR', name: '老人票', price: 60, visitDate: '2026-06-01', session: 'AM', dailyCapacity: 600, dailyRemaining: 396, capacity: 300, remaining: 198, status: 'ENABLED' },
    ],
    formFields: [
      { key: 'code', label: '票种编码', type: 'text' },
      { key: 'name', label: '票种名称', type: 'text' },
      { key: 'price', label: '价格', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: statusOptions },
      { key: 'description', label: '说明', type: 'textarea' },
    ],
    rowActions: [
      { key: 'edit', label: '编辑' },
      { key: 'inventory', label: '库存', variant: 'warning' },
      { key: 'toggleStatus', label: '启停', variant: 'danger' },
    ],
  },
  orders: {
    domain: 'orders',
    title: '订单管理',
    subtitle: '查询预约订单、支付状态和退款记录，退款审核形成真实业务结果。',
    icon: ClipboardCheck,
    primaryAction: '新增订单',
    canCreate: false,
    canEdit: false,
    canStatus: false,
    metrics: [
      { label: '订单列表', value: '实时', trend: '来自 reservation_order' },
      { label: '退款审核', value: '已接入', trend: '调用退款审批接口' },
      { label: '导出', value: 'CSV', trend: '导出当前筛选结果' },
    ],
    filters: [
      { key: 'keyword', label: '订单/手机号', type: 'text', placeholder: '订单号、手机号、游客' },
      { key: 'status', label: '订单状态', type: 'select', options: ['PENDING_PAYMENT', 'PAID', 'CHECKED_IN', 'REFUNDING', 'REFUNDED', 'CANCELLED'] },
      { key: 'visitDate', label: '预约日期', type: 'date' },
    ],
    columns: [
      { prop: 'orderNo', label: '订单号', minWidth: 160 },
      { prop: 'visitor', label: '游客', width: 110 },
      { prop: 'phone', label: '手机号', width: 130 },
      { prop: 'visitDate', label: '预约日期', width: 120 },
      { prop: 'amount', label: '金额', width: 100 },
      { prop: 'paymentStatus', label: '支付', width: 120, status: true },
      { prop: 'status', label: '订单状态', width: 120, status: true },
    ],
    records: [
      { id: 1, orderNo: 'ZR202606010001', visitor: 'visitor', phone: '13800001234', visitDate: '2026-06-01', amount: 240, paymentStatus: 'PAY_SUCCESS', status: 'PAID' },
      { id: 2, orderNo: 'ZR202606010029', visitor: 'visitor', phone: '13900004567', visitDate: '2026-06-01', amount: 360, paymentStatus: 'UNPAID', status: 'PENDING_PAYMENT' },
      { id: 3, orderNo: 'ZR202606010102', visitor: 'visitor', phone: '13700008888', visitDate: '2026-06-02', amount: 60, paymentStatus: 'PAY_SUCCESS', status: 'REFUNDING' },
    ],
    formFields: [
      { key: 'orderNo', label: '订单号', type: 'text' },
      { key: 'status', label: '订单状态', type: 'select', options: ['PENDING_PAYMENT', 'PAID', 'CHECKED_IN', 'REFUNDING', 'REFUNDED'] },
      { key: 'remark', label: '处理备注', type: 'textarea' },
    ],
    rowActions: [
      { key: 'view', label: '详情' },
      { key: 'approveRefund', label: '退款审核', variant: 'warning' },
    ],
  },
  activities: {
    domain: 'activities',
    title: '活动管理',
    subtitle: '发布免费或收费活动，收费活动可进入订单系统并使用活动优惠券。',
    icon: CalendarCheck,
    primaryAction: '发布活动',
    canCreate: true,
    canEdit: true,
    canStatus: true,
    metrics: [
      { label: '活动订单', value: '收费接入', trend: 'ACTIVITY 独立订单' },
      { label: '报名统计', value: '实时', trend: '支付后写入报名' },
      { label: '活动券', value: '按类型', trend: 'ACTIVITY_PARENT_CHILD / ACTIVITY_NIGHT' },
    ],
    filters: [...commonFilters, { key: 'date', label: '活动日期', type: 'date' }],
    columns: [
      { prop: 'title', label: '活动名称', minWidth: 180 },
      { prop: 'category', label: '类型', width: 120 },
      { prop: 'startTime', label: '开始时间', width: 170 },
      { prop: 'capacity', label: '容量', width: 90 },
      { prop: 'signed', label: '报名', width: 90 },
      { prop: 'paid', label: '收费', width: 90 },
      { prop: 'price', label: '价格', width: 90 },
      { prop: 'couponScope', label: '适用券', width: 130 },
      { prop: 'location', label: '地点', width: 130 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, title: '长颈鹿科普讲解', category: '科普讲解', startTime: '2026-06-01 10:00:00', capacity: 40, signed: 18, paid: 0, price: 0, couponScope: 'ACTIVITY', location: '草食动物区', status: 'PUBLISHED' },
      { id: 2, title: '小小饲养员亲子课堂', category: '亲子课堂', startTime: '2026-06-01 14:30:00', capacity: 24, signed: 21, paid: 1, price: 88, couponScope: 'ACTIVITY_PARENT_CHILD', location: '自然教育中心', status: 'PUBLISHED' },
      { id: 3, title: '夏夜动物园', category: '夜游活动', startTime: '2026-06-02 19:00:00', capacity: 100, signed: 63, paid: 1, price: 128, couponScope: 'ACTIVITY_NIGHT', location: '主入口集合', status: 'DRAFT' },
    ],
    formFields: [
      { key: 'title', label: '活动名称', type: 'text' },
      { key: 'category', label: '类型', type: 'select', options: ['科普讲解', '动物投喂', '亲子课堂', '夜游活动', '主题导览'] },
      { key: 'startTime', label: '开始时间', type: 'datetime' },
      { key: 'capacity', label: '容量', type: 'number' },
      { key: 'location', label: '集合地点', type: 'text' },
      { key: 'paid', label: '是否收费', type: 'select', options: ['0', '1'] },
      { key: 'price', label: '活动价格', type: 'number' },
      { key: 'couponScope', label: '适用券类型', type: 'select', options: ['ACTIVITY', 'ACTIVITY_PARENT_CHILD', 'ACTIVITY_NIGHT'] },
      { key: 'status', label: '状态', type: 'select', options: ['PUBLISHED', 'DRAFT', 'DISABLED'] },
    ],
    rowActions: [
      { key: 'edit', label: '编辑' },
      { key: 'toggleStatus', label: '发布/停用', variant: 'danger' },
    ],
  },
  animals: {
    domain: 'animals',
    title: '动物展区管理',
    subtitle: '维护动物档案、所属展区、媒体地址和导览展示状态。',
    icon: PawPrint,
    primaryAction: '新增动物',
    canCreate: true,
    canEdit: true,
    canStatus: true,
    metrics: [
      { label: '动物档案', value: '真实', trend: 'animal 表' },
      { label: '展区联动', value: '已接入', trend: 'zone 自动维护' },
      { label: '媒体字段', value: '已保留', trend: '编辑不丢失' },
    ],
    filters: [
      { key: 'keyword', label: '动物/展区', type: 'text', placeholder: '动物名称、物种、展区' },
      { key: 'status', label: '展示状态', type: 'select', options: ['VISIBLE', 'HIDDEN', 'DISABLED'] },
    ],
    columns: [
      { prop: 'name', label: '名称', minWidth: 140 },
      { prop: 'species', label: '物种', minWidth: 150 },
      { prop: 'zone', label: '展区', minWidth: 140 },
      { prop: 'media', label: '媒体地址', minWidth: 150 },
      { prop: 'guidePoint', label: '导览点位', width: 120 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, name: '团团', species: '大熊猫', zone: '熊猫馆', media: '/media/animals/panda.jpg', guidePoint: 'P-01', status: 'VISIBLE' },
      { id: 2, name: '星河', species: '长颈鹿', zone: '草食动物区', media: '/media/animals/giraffe.jpg', guidePoint: 'G-03', status: 'VISIBLE' },
      { id: 3, name: '雨林馆', species: '综合展区', zone: '热带雨林', media: '/media/zones/rainforest.jpg', guidePoint: 'R-02', status: 'HIDDEN' },
    ],
    formFields: [
      { key: 'name', label: '名称', type: 'text' },
      { key: 'species', label: '物种', type: 'text' },
      { key: 'zone', label: '所属展区', type: 'text' },
      { key: 'media', label: '媒体地址', type: 'text' },
      { key: 'status', label: '状态', type: 'select', options: ['VISIBLE', 'HIDDEN', 'DISABLED'] },
      { key: 'description', label: '介绍', type: 'textarea' },
    ],
    rowActions: [
      { key: 'edit', label: '编辑' },
      { key: 'toggleStatus', label: '展示/隐藏', variant: 'danger' },
    ],
  },
  checkins: {
    domain: 'checkins',
    title: '核销管理',
    subtitle: '查看扫码和人工核销记录，后台人工核销直接接入核销接口。',
    icon: ShieldCheck,
    primaryAction: '人工核销',
    canCreate: false,
    canEdit: false,
    canStatus: false,
    metrics: [
      { label: '核销记录', value: '实时', trend: 'checkin_record' },
      { label: '人工核销', value: '已接入', trend: '后台动作可落库' },
      { label: '占位按钮', value: '已移除', trend: '只保留真实动作' },
    ],
    filters: [
      { key: 'keyword', label: '订单/核销员', type: 'text', placeholder: '订单号、核销员、备注' },
      { key: 'status', label: '核销状态', type: 'select', options: ['CHECKED_IN', 'EXCEPTION'] },
      { key: 'date', label: '核销日期', type: 'date' },
    ],
    columns: [
      { prop: 'orderNo', label: '订单号', minWidth: 160 },
      { prop: 'checker', label: '核销员', width: 120 },
      { prop: 'people', label: '人数', width: 80 },
      { prop: 'checkedAt', label: '核销时间', width: 170 },
      { prop: 'remark', label: '备注', minWidth: 150 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, orderNo: 'ZR202606010001', checker: '入口核销员', people: 2, checkedAt: '2026-06-01 09:12:00', remark: '扫码核销', status: 'CHECKED_IN' },
      { id: 2, orderNo: 'ZR202606010088', checker: '西门核销员', people: 3, checkedAt: '2026-06-01 10:03:00', remark: '重复扫码拦截', status: 'EXCEPTION' },
      { id: 3, orderNo: 'ZR202606010129', checker: '入口核销员', people: 1, checkedAt: '2026-06-01 11:03:00', remark: '人工核销', status: 'CHECKED_IN' },
    ],
    formFields: [
      { key: 'orderNo', label: '订单号', type: 'text' },
      { key: 'phone', label: '手机号', type: 'text' },
      { key: 'remark', label: '核销备注', type: 'textarea' },
    ],
    rowActions: [{ key: 'view', label: '详情' }],
  },
  marketing: {
    domain: 'marketing',
    title: '营销管理',
    subtitle: '区分优惠券和公告，支持创建、编辑与启停。',
    icon: Megaphone,
    primaryAction: '新增营销',
    canCreate: true,
    canEdit: true,
    canStatus: true,
    metrics: [
      { label: '优惠券', value: '可编辑', trend: '额度/门槛/库存' },
      { label: '公告', value: '可发布', trend: '内容与状态' },
      { label: '状态闭环', value: '已接入', trend: '启停真实落库' },
    ],
    filters: [
      { key: 'keyword', label: '营销名称', type: 'text', placeholder: '优惠券、公告名称' },
      { key: 'status', label: '投放状态', type: 'select', options: ['ENABLED', 'DISABLED', 'PUBLISHED', 'DRAFT'] },
    ],
    columns: [
      { prop: 'name', label: '营销名称', minWidth: 170 },
      { prop: 'type', label: '类型', width: 110 },
      { prop: 'discountValue', label: '优惠额', width: 100 },
      { prop: 'thresholdAmount', label: '门槛', width: 100 },
      { prop: 'totalQuantity', label: '库存', width: 100 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, name: '六一亲子满减券', type: '优惠券', resourceType: 'COUPON', discountValue: 30, thresholdAmount: 200, totalQuantity: 5000, status: 'ENABLED' },
      { id: 2, name: '年卡会员折扣', type: '优惠券', resourceType: 'COUPON', discountValue: 20, thresholdAmount: 100, totalQuantity: 1000, status: 'ENABLED' },
      { id: 3, name: '夜游公告轮播', type: '公告', resourceType: 'NOTICE', description: '夏夜动物园开放公告', status: 'PUBLISHED' },
    ],
    formFields: [
      { key: 'resourceType', label: '类型', type: 'select', options: ['COUPON', 'NOTICE'] },
      { key: 'name', label: '营销名称', type: 'text' },
      { key: 'discountValue', label: '优惠金额', type: 'number' },
      { key: 'thresholdAmount', label: '使用门槛', type: 'number' },
      { key: 'totalQuantity', label: '发放库存', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: ['ENABLED', 'DISABLED', 'PUBLISHED', 'DRAFT'] },
      { key: 'description', label: '公告内容/规则说明', type: 'textarea' },
    ],
    rowActions: [
      { key: 'edit', label: '编辑' },
      { key: 'toggleStatus', label: '启停', variant: 'danger' },
    ],
  },
  system: {
    domain: 'system',
    title: '系统管理',
    subtitle: '轻量维护后台账号状态；完整 RBAC 留到后续阶段。',
    icon: Settings,
    primaryAction: '新增用户',
    canCreate: true,
    canEdit: true,
    canStatus: true,
    metrics: [
      { label: '后台用户', value: '轻量维护', trend: '账号/状态' },
      { label: '角色权限', value: '后续扩展', trend: '本阶段不做 RBAC' },
      { label: '日志', value: '只读', trend: 'operation_log' },
    ],
    filters: [
      { key: 'keyword', label: '用户/权限', type: 'text', placeholder: '用户、角色、权限' },
      { key: 'status', label: '账号状态', type: 'select', options: ['ENABLED', 'LOCKED', 'DISABLED'] },
    ],
    columns: [
      { prop: 'username', label: '账号', minWidth: 140 },
      { prop: 'displayName', label: '姓名', width: 120 },
      { prop: 'role', label: '角色', width: 120 },
      { prop: 'lastLogin', label: '创建时间', width: 170 },
      { prop: 'scope', label: '权限范围', minWidth: 160 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, username: 'admin', displayName: '园区管理员', role: '管理员', lastLogin: '2026-05-28 15:30:00', scope: '全部菜单', status: 'ENABLED' },
      { id: 2, username: 'checker01', displayName: '入口核销员', role: '核销员', lastLogin: '2026-05-28 09:00:00', scope: '核销站', status: 'ENABLED' },
      { id: 3, username: 'ops01', displayName: '运营专员', role: '运营', lastLogin: '2026-05-27 18:12:00', scope: '活动/营销', status: 'LOCKED' },
    ],
    formFields: [
      { key: 'username', label: '账号', type: 'text' },
      { key: 'displayName', label: '姓名', type: 'text' },
      { key: 'password', label: '初始密码', type: 'text' },
      { key: 'status', label: '状态', type: 'select', options: ['ENABLED', 'LOCKED', 'DISABLED'] },
      { key: 'scope', label: '权限范围', type: 'textarea' },
    ],
    rowActions: [
      { key: 'edit', label: '编辑' },
      { key: 'toggleStatus', label: '启停', variant: 'danger' },
    ],
  },
};

export const fallbackAdminModule: AdminModuleConfig = {
  ...adminModules.tickets,
  icon: ChartNoAxesColumnIncreasing,
};

export function getAdminModule(domain: string): AdminModuleConfig {
  return adminModules[domain] ?? fallbackAdminModule;
}

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
  type: 'text' | 'number' | 'select' | 'date' | 'textarea';
  options?: string[];
}

export interface AdminModuleConfig {
  domain: string;
  title: string;
  subtitle: string;
  icon: Component;
  primaryAction: string;
  metrics: AdminMetric[];
  filters: AdminFilter[];
  columns: AdminColumn[];
  records: Record<string, unknown>[];
  formFields: AdminFormField[];
  rowActions: string[];
}

const commonFilters: AdminFilter[] = [
  { key: 'keyword', label: '关键词', type: 'text', placeholder: '输入名称、编号或手机号' },
  { key: 'status', label: '状态', type: 'select', placeholder: '全部状态', options: ['启用', '草稿', '待审核', '已停用'] },
];

export const adminModules: Record<string, AdminModuleConfig> = {
  tickets: {
    domain: 'tickets',
    title: '票务管理',
    subtitle: '维护票种、价格、日期库存、场次和节假日规则。',
    icon: Ticket,
    primaryAction: '新增票种',
    metrics: [
      { label: '在售票种', value: '12', trend: '较昨日 +2' },
      { label: '今日余票', value: '1,780', trend: '上午场充足' },
      { label: '节假日规则', value: '6', trend: '国庆规则已发布' },
    ],
    filters: [
      ...commonFilters,
      { key: 'date', label: '库存日期', type: 'date' },
    ],
    columns: [
      { prop: 'code', label: '票种编码', width: 120 },
      { prop: 'name', label: '票种名称', minWidth: 140 },
      { prop: 'price', label: '价格', width: 110 },
      { prop: 'capacity', label: '日容量', width: 100 },
      { prop: 'remaining', label: '剩余库存', width: 110 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, code: 'ADULT', name: '成人票', price: '¥120', capacity: 1200, remaining: 520, status: '启用' },
      { id: 2, code: 'CHILD', name: '儿童票', price: '¥60', capacity: 700, remaining: 318, status: '启用' },
      { id: 3, code: 'ANNUAL', name: '亲子年卡', price: '¥699', capacity: 120, remaining: 42, status: '启用' },
    ],
    formFields: [
      { key: 'name', label: '票种名称', type: 'text' },
      { key: 'price', label: '价格', type: 'number' },
      { key: 'capacity', label: '日容量', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: ['启用', '草稿', '已停用'] },
      { key: 'description', label: '说明', type: 'textarea' },
    ],
    rowActions: ['编辑', '库存', '停用'],
  },
  orders: {
    domain: 'orders',
    title: '订单管理',
    subtitle: '查询预约订单、支付状态、退款审核和异常订单。',
    icon: ClipboardCheck,
    primaryAction: '导出订单',
    metrics: [
      { label: '今日订单', value: '386', trend: '支付率 93%' },
      { label: '待支付', value: '24', trend: '需关注超时取消' },
      { label: '退款中', value: '9', trend: '平均处理 18 分钟' },
    ],
    filters: [
      { key: 'keyword', label: '订单/手机号', type: 'text', placeholder: '订单号、手机号、游客姓名' },
      { key: 'status', label: '订单状态', type: 'select', options: ['待支付', '已预约', '已入园', '退款中', '已退款'] },
      { key: 'date', label: '预约日期', type: 'date' },
    ],
    columns: [
      { prop: 'orderNo', label: '订单号', minWidth: 160 },
      { prop: 'visitor', label: '联系人', width: 110 },
      { prop: 'phone', label: '手机号', width: 130 },
      { prop: 'visitDate', label: '预约日期', width: 120 },
      { prop: 'amount', label: '金额', width: 100 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, orderNo: 'ZR202606010001', visitor: '林女士', phone: '13800001234', visitDate: '2026-06-01', amount: '¥240', status: '已预约' },
      { id: 2, orderNo: 'ZR202606010029', visitor: '陈先生', phone: '13900004567', visitDate: '2026-06-01', amount: '¥360', status: '待支付' },
      { id: 3, orderNo: 'ZR202606010102', visitor: '王同学', phone: '13700008888', visitDate: '2026-06-02', amount: '¥60', status: '退款中' },
    ],
    formFields: [
      { key: 'orderNo', label: '订单号', type: 'text' },
      { key: 'status', label: '订单状态', type: 'select', options: ['待支付', '已预约', '已入园', '退款中', '已退款'] },
      { key: 'remark', label: '处理备注', type: 'textarea' },
    ],
    rowActions: ['详情', '退款审核', '备注'],
  },
  activities: {
    domain: 'activities',
    title: '活动管理',
    subtitle: '发布科普讲解、投喂、亲子课堂和夜游活动。',
    icon: CalendarCheck,
    primaryAction: '发布活动',
    metrics: [
      { label: '已发布活动', value: '18', trend: '本周新增 4' },
      { label: '报名人数', value: '624', trend: '亲子课堂最热门' },
      { label: '平均满员率', value: '78%', trend: '较上周 +8%' },
    ],
    filters: [
      ...commonFilters,
      { key: 'date', label: '活动日期', type: 'date' },
    ],
    columns: [
      { prop: 'title', label: '活动名称', minWidth: 180 },
      { prop: 'category', label: '类型', width: 120 },
      { prop: 'startTime', label: '开始时间', width: 170 },
      { prop: 'capacity', label: '容量', width: 90 },
      { prop: 'signed', label: '报名', width: 90 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, title: '长颈鹿科普讲解', category: '科普讲解', startTime: '2026-06-01 10:00', capacity: 40, signed: 18, status: '启用' },
      { id: 2, title: '小小饲养员亲子课堂', category: '亲子课堂', startTime: '2026-06-01 14:30', capacity: 24, signed: 21, status: '启用' },
      { id: 3, title: '夏夜动物园', category: '夜游活动', startTime: '2026-06-02 19:00', capacity: 100, signed: 63, status: '草稿' },
    ],
    formFields: [
      { key: 'title', label: '活动名称', type: 'text' },
      { key: 'category', label: '类型', type: 'select', options: ['科普讲解', '动物投喂', '亲子课堂', '夜游活动'] },
      { key: 'capacity', label: '容量', type: 'number' },
      { key: 'location', label: '集合地点', type: 'text' },
      { key: 'description', label: '活动介绍', type: 'textarea' },
    ],
    rowActions: ['编辑', '报名', '签到'],
  },
  animals: {
    domain: 'animals',
    title: '动物展区管理',
    subtitle: '维护动物档案、展区介绍、图片视频和导览点位。',
    icon: PawPrint,
    primaryAction: '新增动物',
    metrics: [
      { label: '动物档案', value: '128', trend: '12 个重点展示' },
      { label: '展区点位', value: '32', trend: '无障碍点位 8' },
      { label: '待更新媒体', value: '5', trend: '需要补图' },
    ],
    filters: [
      { key: 'keyword', label: '动物/展区', type: 'text', placeholder: '动物名称、物种、展区' },
      { key: 'status', label: '展示状态', type: 'select', options: ['展示中', '维护中', '隐藏'] },
    ],
    columns: [
      { prop: 'name', label: '名称', minWidth: 140 },
      { prop: 'species', label: '物种', minWidth: 150 },
      { prop: 'zone', label: '展区', minWidth: 140 },
      { prop: 'media', label: '媒体', width: 100 },
      { prop: 'guidePoint', label: '导览点位', width: 120 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, name: '团团', species: '大熊猫', zone: '熊猫馆', media: '6 张', guidePoint: 'P-01', status: '展示中' },
      { id: 2, name: '星河', species: '长颈鹿', zone: '草食动物区', media: '4 张', guidePoint: 'G-03', status: '展示中' },
      { id: 3, name: '雨林馆', species: '综合展区', zone: '热带雨林', media: '12 张', guidePoint: 'R-02', status: '维护中' },
    ],
    formFields: [
      { key: 'name', label: '名称', type: 'text' },
      { key: 'species', label: '物种', type: 'text' },
      { key: 'zone', label: '所属展区', type: 'text' },
      { key: 'guidePoint', label: '导览点位', type: 'text' },
      { key: 'description', label: '介绍', type: 'textarea' },
    ],
    rowActions: ['编辑', '媒体', '导览'],
  },
  checkins: {
    domain: 'checkins',
    title: '核销管理',
    subtitle: '查看二维码核销、人工核销和异常核销日志。',
    icon: ShieldCheck,
    primaryAction: '人工核销',
    metrics: [
      { label: '今日核销', value: '219', trend: '入园率 56.7%' },
      { label: '异常拦截', value: '7', trend: '重复核销 4' },
      { label: '核销员在线', value: '5', trend: '主入口 3 人' },
    ],
    filters: [
      { key: 'keyword', label: '订单/核销员', type: 'text', placeholder: '订单号、手机号、核销员' },
      { key: 'status', label: '核销状态', type: 'select', options: ['未核销', '已核销', '异常核销'] },
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
      { id: 1, orderNo: 'ZR202606010001', checker: '入口核销员', people: 2, checkedAt: '2026-06-01 09:12', remark: '扫码核销', status: '已核销' },
      { id: 2, orderNo: 'ZR202606010088', checker: '西门核销员', people: 3, checkedAt: '2026-06-01 10:03', remark: '重复扫码拦截', status: '异常核销' },
      { id: 3, orderNo: 'ZR202606010129', checker: '入口核销员', people: 1, checkedAt: '-', remark: '等待入园', status: '未核销' },
    ],
    formFields: [
      { key: 'orderNo', label: '订单号', type: 'text' },
      { key: 'phone', label: '手机号', type: 'text' },
      { key: 'remark', label: '异常备注', type: 'textarea' },
    ],
    rowActions: ['详情', '备注', '同步'],
  },
  marketing: {
    domain: 'marketing',
    title: '营销管理',
    subtitle: '配置优惠券、满减、会员折扣、公告轮播和投放状态。',
    icon: Megaphone,
    primaryAction: '新增活动',
    metrics: [
      { label: '进行中营销', value: '8', trend: '亲子券最活跃' },
      { label: '优惠券领取', value: '1,246', trend: '核销率 34%' },
      { label: '公告轮播', value: '5', trend: '2 条定时发布' },
    ],
    filters: [
      { key: 'keyword', label: '营销名称', type: 'text', placeholder: '优惠券、公告、满减名称' },
      { key: 'status', label: '投放状态', type: 'select', options: ['投放中', '未开始', '已结束', '草稿'] },
    ],
    columns: [
      { prop: 'name', label: '营销名称', minWidth: 170 },
      { prop: 'type', label: '类型', width: 120 },
      { prop: 'budget', label: '预算/库存', width: 120 },
      { prop: 'claimed', label: '领取/触达', width: 120 },
      { prop: 'period', label: '投放周期', minWidth: 170 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, name: '六一亲子满减券', type: '满减券', budget: '5000 张', claimed: '1246', period: '05-20 至 06-10', status: '投放中' },
      { id: 2, name: '年卡会员折扣', type: '会员折扣', budget: '不限量', claimed: '328', period: '全年', status: '投放中' },
      { id: 3, name: '夜游公告轮播', type: '公告轮播', budget: '首页曝光', claimed: '18,920', period: '06-01 至 08-31', status: '草稿' },
    ],
    formFields: [
      { key: 'name', label: '营销名称', type: 'text' },
      { key: 'type', label: '类型', type: 'select', options: ['优惠券', '满减券', '会员折扣', '公告轮播'] },
      { key: 'budget', label: '预算/库存', type: 'text' },
      { key: 'period', label: '投放周期', type: 'text' },
      { key: 'description', label: '规则说明', type: 'textarea' },
    ],
    rowActions: ['编辑', '投放', '数据'],
  },
  system: {
    domain: 'system',
    title: '系统管理',
    subtitle: '管理后台用户、角色、权限、菜单、日志和系统参数。',
    icon: Settings,
    primaryAction: '新增用户',
    metrics: [
      { label: '后台用户', value: '16', trend: '核销员 8 人' },
      { label: '角色权限', value: '5', trend: 'RBAC 已启用' },
      { label: '今日日志', value: '342', trend: '无高危操作' },
    ],
    filters: [
      { key: 'keyword', label: '用户/权限', type: 'text', placeholder: '用户、角色、菜单、参数' },
      { key: 'status', label: '账号状态', type: 'select', options: ['启用', '锁定', '已停用'] },
    ],
    columns: [
      { prop: 'username', label: '账号', minWidth: 140 },
      { prop: 'displayName', label: '姓名', width: 120 },
      { prop: 'role', label: '角色', width: 120 },
      { prop: 'lastLogin', label: '最近登录', width: 170 },
      { prop: 'scope', label: '权限范围', minWidth: 160 },
      { prop: 'status', label: '状态', width: 110, status: true },
    ],
    records: [
      { id: 1, username: 'admin', displayName: '园区管理员', role: '管理员', lastLogin: '2026-05-28 15:30', scope: '全部菜单', status: '启用' },
      { id: 2, username: 'checker01', displayName: '入口核销员', role: '核销员', lastLogin: '2026-05-28 09:00', scope: '核销端', status: '启用' },
      { id: 3, username: 'ops01', displayName: '运营专员', role: '运营', lastLogin: '2026-05-27 18:12', scope: '活动/营销', status: '锁定' },
    ],
    formFields: [
      { key: 'username', label: '账号', type: 'text' },
      { key: 'displayName', label: '姓名', type: 'text' },
      { key: 'role', label: '角色', type: 'select', options: ['管理员', '运营', '核销员', '客服'] },
      { key: 'status', label: '状态', type: 'select', options: ['启用', '锁定', '已停用'] },
      { key: 'scope', label: '权限范围', type: 'textarea' },
    ],
    rowActions: ['编辑', '授权', '日志'],
  },
};

export const fallbackAdminModule: AdminModuleConfig = {
  ...adminModules.tickets,
  icon: ChartNoAxesColumnIncreasing,
};

export function getAdminModule(domain: string): AdminModuleConfig {
  return adminModules[domain] ?? fallbackAdminModule;
}

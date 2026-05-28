export const orderStatusText: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '已预约',
  RESERVED: '已预约',
  CHECKED_IN: '已入园',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
};

export const paymentStatusText: Record<string, string> = {
  UNPAID: '未支付',
  PAYING: '支付中',
  PAY_SUCCESS: '支付成功',
  PAY_FAILED: '支付失败',
  CLOSED: '已关闭',
};

export const checkinStatusText: Record<string, string> = {
  NOT_CHECKED: '未核销',
  PARTIAL_CHECKED: '部分核销',
  CHECKED_IN: '已核销',
  EXCEPTION: '异常核销',
};

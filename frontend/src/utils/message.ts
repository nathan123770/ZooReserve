import { ElMessage } from 'element-plus';

const duration = 1400;

export const toast = {
  success(message: string) {
    ElMessage.success({ message, duration, showClose: false });
  },
  warning(message: string) {
    ElMessage.warning({ message, duration, showClose: false });
  },
  info(message: string) {
    ElMessage.info({ message, duration, showClose: false });
  },
  error(message: string) {
    ElMessage.error({ message, duration: 1800, showClose: false });
  },
};

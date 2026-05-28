import axios from 'axios';
import { useAuthStore } from '@/stores/auth';
import type { ApiResponse } from '@/types/api';
import { toast } from '@/utils/message';

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

http.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    toast.error(error?.response?.data?.message ?? '请求失败，请稍后重试');
    return Promise.reject(error);
  },
);

export async function getData<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await http.get<ApiResponse<T>>(url, { params });
  return response.data.data;
}

export async function postData<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.post<ApiResponse<T>>(url, body);
  return response.data.data;
}

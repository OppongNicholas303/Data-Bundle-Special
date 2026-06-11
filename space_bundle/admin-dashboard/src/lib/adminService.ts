import { apiClient } from "./api";
import type {
  AdminStats, AdminUser, AdminAgent, Bundle, AdminOrder,
  Commission, CreateBundlePayload, UpdateBundlePayload, WithdrawalRequest, DailyAnalytics,
} from "@/types";

type ApiWrap<T> = { data: T };

export const adminService = {
  // Stats
  getStats: async (): Promise<AdminStats> => {
    const res = await apiClient.get("/admin/stats") as ApiWrap<AdminStats>;
    return res.data;
  },

  // Users
  getUsers: async (): Promise<AdminUser[]> => {
    const res = await apiClient.get("/admin/users") as ApiWrap<AdminUser[]>;
    return res.data;
  },
  toggleUserLock: async (id: string): Promise<AdminUser> => {
    const res = await apiClient.put(`/admin/users/${id}/toggle-lock`) as ApiWrap<AdminUser>;
    return res.data;
  },
  toggleUserEnabled: async (id: string): Promise<AdminUser> => {
    const res = await apiClient.put(`/admin/users/${id}/toggle-enabled`) as ApiWrap<AdminUser>;
    return res.data;
  },
  updateUserRoles: async (id: string, roles: string[]): Promise<AdminUser> => {
    const res = await apiClient.put(`/admin/users/${id}/roles`, { roles }) as ApiWrap<AdminUser>;
    return res.data;
  },

  // Agents
  getAgents: async (): Promise<AdminAgent[]> => {
    const res = await apiClient.get("/admin/agents") as ApiWrap<AdminAgent[]>;
    return res.data;
  },
  toggleAgentActive: async (id: string): Promise<AdminAgent> => {
    const res = await apiClient.put(`/admin/agents/${id}/toggle-active`) as ApiWrap<AdminAgent>;
    return res.data;
  },
  getAgentCommissions: async (id: string): Promise<Commission[]> => {
    const res = await apiClient.get(`/admin/agents/${id}/commissions`) as ApiWrap<Commission[]>;
    return res.data;
  },
  getAgentOrders: async (id: string): Promise<AdminOrder[]> => {
    const res = await apiClient.get(`/admin/agents/${id}/orders`) as ApiWrap<AdminOrder[]>;
    return res.data;
  },
  getAgentWithdrawals: async (id: string): Promise<WithdrawalRequest[]> => {
    const res = await apiClient.get(`/admin/agents/${id}/withdrawals`) as ApiWrap<WithdrawalRequest[]>;
    return res.data;
  },

  // Bundles
  getBundles: async (): Promise<Bundle[]> => {
    const res = await apiClient.get("/admin/bundles") as ApiWrap<Bundle[]>;
    return res.data;
  },
  createBundle: async (payload: CreateBundlePayload): Promise<Bundle> => {
    const res = await apiClient.post("/admin/bundles", payload) as ApiWrap<Bundle>;
    return res.data;
  },
  updateBundle: async (id: string, payload: UpdateBundlePayload): Promise<Bundle> => {
    const res = await apiClient.put(`/admin/bundles/${id}`, payload) as ApiWrap<Bundle>;
    return res.data;
  },
  setBundleStatus: async (id: string, status: string): Promise<Bundle> => {
    const res = await apiClient.put(`/admin/bundles/${id}/status`, { status }) as ApiWrap<Bundle>;
    return res.data;
  },
  deleteBundle: async (id: string): Promise<void> => {
    await apiClient.delete(`/admin/bundles/${id}`);
  },

  // Orders
  getOrders: async (status?: string, network?: string, from?: string, to?: string): Promise<AdminOrder[]> => {
    const params = new URLSearchParams();
    if (status)  params.set("status",  status);
    if (network) params.set("network", network);
    if (from)    params.set("from",    from);
    if (to)      params.set("to",      to);
    const query = params.toString();
    const res = await apiClient.get(`/admin/orders${query ? `?${query}` : ""}`) as ApiWrap<AdminOrder[]>;
    return res.data;
  },

  // Analytics
  getDailyAnalytics: async (days = 30): Promise<DailyAnalytics[]> => {
    const res = await apiClient.get(`/admin/analytics/daily?days=${days}`) as ApiWrap<DailyAnalytics[]>;
    return res.data;
  },

  // Migrations
  backfillOrderCosts: async (): Promise<Record<string, number>> => {
    const res = await apiClient.post("/admin/migrations/backfill-order-costs") as ApiWrap<Record<string, number>>;
    return res.data;
  },

  // Withdrawals
  getWithdrawals: async (status?: string): Promise<WithdrawalRequest[]> => {
    const query = status ? `?status=${status}` : "";
    const res = await apiClient.get(`/admin/withdrawals${query}`) as ApiWrap<WithdrawalRequest[]>;
    return res.data;
  },
  approveWithdrawal: async (id: string, note?: string): Promise<WithdrawalRequest> => {
    const res = await apiClient.post(`/admin/withdrawals/${id}/approve`, { note }) as ApiWrap<WithdrawalRequest>;
    return res.data;
  },
  rejectWithdrawal: async (id: string, note?: string): Promise<WithdrawalRequest> => {
    const res = await apiClient.post(`/admin/withdrawals/${id}/reject`, { note }) as ApiWrap<WithdrawalRequest>;
    return res.data;
  },
};

import { apiClient } from "./api";
import type {
  AdminStats, AdminUser, AdminAgent, Bundle, AdminOrder,
  Commission, CreateBundlePayload, UpdateBundlePayload, WithdrawalRequest, DailyAnalytics,
  AdminMashupPackage, MashupSyncResult, AgentBundlePricing, AgentMashupPricing, AgentCheckerPricing, AdminTransaction,
  Announcement, CheckerTransaction, ResultCheckerPricing, SmsPackage
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
  getUserWallet: async (id: string): Promise<{ balance: number; commissionBalance?: number; currency: string }> => {
    const res = await apiClient.get(`/admin/users/${id}/wallet`) as ApiWrap<{ balance: number; commissionBalance?: number; currency: string }>;
    return res.data;
  },
  getUserTransactions: async (id: string): Promise<any[]> => {
    const res = await apiClient.get(`/admin/users/${id}/transactions`) as ApiWrap<any[]>;
    return res.data;
  },
  creditUserWallet: async (id: string, amount: number, description: string): Promise<string> => {
    const res = await apiClient.post(`/admin/users/${id}/wallet/credit`, { amount, description }) as ApiWrap<string>;
    return res.data;
  },
  debitUserWallet: async (id: string, amount: number, description: string): Promise<string> => {
    const res = await apiClient.post(`/admin/users/${id}/wallet/debit`, { amount, description }) as ApiWrap<string>;
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
  getAgentCommissions: async (id: string, status?: string, from?: string, to?: string): Promise<Commission[]> => {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    const query = params.toString();
    const res = await apiClient.get(`/admin/agents/${id}/commissions${query ? `?${query}` : ''}`) as ApiWrap<Commission[]>;
    return res.data;
  },
  getAgentOrders: async (id: string, status?: string, network?: string, from?: string, to?: string): Promise<AdminOrder[]> => {
    const params = new URLSearchParams();
    if (status)  params.set('status',  status);
    if (network) params.set('network', network);
    if (from)    params.set('from',    from);
    if (to)      params.set('to',      to);
    const query = params.toString();
    const res = await apiClient.get(`/admin/agents/${id}/orders${query ? `?${query}` : ''}`) as ApiWrap<AdminOrder[]>;
    return res.data;
  },
  getAgentWithdrawals: async (id: string): Promise<WithdrawalRequest[]> => {
    const res = await apiClient.get(`/admin/agents/${id}/withdrawals`) as ApiWrap<WithdrawalRequest[]>;
    return res.data;
  },

  // Wallet
  getAgentWallet: async (agentProfileId: string): Promise<{ balance: number; commissionBalance?: number; currency: string; updatedAt?: string | null }> => {
    const res = await apiClient.get(`/admin/agents/${agentProfileId}/wallet`) as ApiWrap<{ balance: number; commissionBalance?: number; currency: string; updatedAt?: string | null }>;
    return res.data;
  },
  topUpAgentWallet: async (agentProfileId: string, amount: number, note?: string): Promise<{ balance: number; commissionBalance?: number; currency: string; updatedAt?: string | null }> => {
    const res = await apiClient.post(`/admin/agents/${agentProfileId}/wallet/topup`, { amount, note }) as ApiWrap<{ balance: number; commissionBalance?: number; currency: string; updatedAt?: string | null }>;
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
   markOrderComplete: async (orderId: string, providerReference?: string): Promise<AdminOrder> => {
     const res = await apiClient.post(`/admin/orders/${orderId}/mark-complete`, { providerReference }) as ApiWrap<AdminOrder>;
     return res.data;
   },
   markOrderCompleteByAdmin: async (orderId: string): Promise<AdminOrder> => {
     const res = await apiClient.post(`/admin/orders/${orderId}/mark-complete-by-admin`) as ApiWrap<AdminOrder>;
     return res.data;
   },
   reprocessOrder: async (orderId: string): Promise<AdminOrder> => {
     const res = await apiClient.post(`/admin/orders/${orderId}/reprocess`) as ApiWrap<AdminOrder>;
     return res.data;
   },

   // Transactions
   getAllTransactions: async (status?: string, type?: string, search?: string, fromDate?: string, toDate?: string): Promise<AdminTransaction[]> => {
     const params = new URLSearchParams();
     if (status) params.set("status", status);
     if (type) params.set("type", type);
     if (search) params.set("search", search);
     if (fromDate) params.set("fromDate", fromDate);
     if (toDate) params.set("toDate", toDate);
     const query = params.toString();
     const res = await apiClient.get(`/admin/transactions${query ? `?${query}` : ""}`) as ApiWrap<AdminTransaction[]>;
     return res.data;
   },

   // Analytics
   getDailyAnalytics: async (days = 30): Promise<DailyAnalytics[]> => {
     const res = await apiClient.get(`/admin/analytics/daily?days=${days}`) as ApiWrap<DailyAnalytics[]>;
     return res.data;
   },
   getAgentCommissionsSummary: async (agentId?: string, from?: string, to?: string): Promise<{ dailySummary: any[]; totalCommissions: number; totalOrders: number; fromDate: string; toDate: string; agentId: string }> => {
     const params = new URLSearchParams();
     if (agentId) params.set('agentId', agentId);
     if (from) params.set('from', from);
     if (to) params.set('to', to);
     const query = params.toString();
     const res = await apiClient.get(`/admin/analytics/agent-commissions${query ? `?${query}` : ''}`) as ApiWrap<any>;
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

  // Agent Pricing
  getAgentPricing: async (agentProfileId: string): Promise<AgentBundlePricing[]> => {
    const res = await apiClient.get(`/admin/agents/${agentProfileId}/pricing`) as ApiWrap<AgentBundlePricing[]>;
    return res.data;
  },
  getPricingForBundle: async (bundleId: string): Promise<AgentBundlePricing[]> => {
    const res = await apiClient.get(`/admin/agents/pricing/bundle/${bundleId}`) as ApiWrap<AgentBundlePricing[]>;
    return res.data;
  },
  setAgentBasePrice: async (agentProfileId: string, bundleId: string, basePrice: number, sellingPrice?: number): Promise<AgentBundlePricing> => {
    const res = await apiClient.put(`/admin/agents/pricing`, { agentProfileId, bundleId, basePrice, sellingPrice: sellingPrice ?? null }) as ApiWrap<AgentBundlePricing>;
    return res.data;
  },
  setBulkBasePrice: async (bundleId: string, basePrice: number): Promise<{ bundleId: string; basePrice: number; agentsUpdated: number }> => {
    const res = await apiClient.put(`/admin/agents/pricing/bulk`, { bundleId, basePrice }) as ApiWrap<{ bundleId: string; basePrice: number; agentsUpdated: number }>;
    return res.data;
  },
  getAgentMashupPricing: async (agentProfileId: string): Promise<AgentMashupPricing[]> => {
    const res = await apiClient.get(`/admin/agents/${agentProfileId}/mashup/pricing`) as ApiWrap<AgentMashupPricing[]>;
    return res.data;
  },
  getMashupPricingForBundle: async (mashupBundleId: string): Promise<AgentMashupPricing[]> => {
    const res = await apiClient.get(`/admin/agents/mashup/pricing/bundle/${mashupBundleId}`) as ApiWrap<AgentMashupPricing[]>;
    return res.data;
  },
  setAgentMashupBasePrice: async (agentProfileId: string, bundleId: string, basePrice: number, sellingPrice?: number): Promise<AgentMashupPricing> => {
    const res = await apiClient.put(`/admin/agents/mashup/pricing`, { agentProfileId, bundleId, basePrice, sellingPrice: sellingPrice ?? null }) as ApiWrap<AgentMashupPricing>;
    return res.data;
  },
  setBulkMashupBasePrice: async (bundleId: string, basePrice: number): Promise<{ bundleId: string; basePrice: number; agentsUpdated: number }> => {
    const res = await apiClient.put(`/admin/agents/mashup/pricing/bulk`, { bundleId, basePrice }) as ApiWrap<{ bundleId: string; basePrice: number; agentsUpdated: number }>;
    return res.data;
  },
  
  // Checker Agent Pricing
  getCheckerConfigs: async (): Promise<any[]> => {
    const res = await apiClient.get("/admin/results-checker/pricing") as ApiWrap<any[]>;
    return res.data;
  },
  getAgentCheckerPricing: async (agentProfileId: string): Promise<AgentCheckerPricing[]> => {
    const res = await apiClient.get(`/admin/agents/${agentProfileId}/checker/pricing`) as ApiWrap<AgentCheckerPricing[]>;
    return res.data;
  },
  getCheckerPricingForService: async (serviceName: string): Promise<AgentCheckerPricing[]> => {
    const res = await apiClient.get(`/admin/agents/checker/pricing/service/${serviceName}`) as ApiWrap<AgentCheckerPricing[]>;
    return res.data;
  },
  setAgentCheckerBasePrice: async (agentProfileId: string, serviceName: string, basePrice: number, sellingPrice?: number): Promise<AgentCheckerPricing> => {
    const res = await apiClient.put(`/admin/agents/checker/pricing`, { agentProfileId, bundleId: serviceName, basePrice, sellingPrice: sellingPrice ?? null }) as ApiWrap<AgentCheckerPricing>;
    return res.data;
  },
  setBulkCheckerBasePrice: async (serviceName: string, basePrice: number): Promise<{ agentsUpdated: number }> => {
    const res = await apiClient.put(`/admin/agents/checker/pricing/bulk`, { bundleId: serviceName, basePrice }) as ApiWrap<{ agentsUpdated: number }>;
    return res.data;
  },

  // Mashup Packages
  getMashupPackages: async (): Promise<AdminMashupPackage[]> => {
    const res = await apiClient.get("/admin/mashup/packages") as ApiWrap<AdminMashupPackage[]>;
    return res.data;
  },
  syncMashupPackages: async (): Promise<MashupSyncResult> => {
    const res = await apiClient.post("/admin/mashup/packages/sync") as ApiWrap<MashupSyncResult>;
    return res.data;
  },
  setMashupPrice: async (id: string, sellingPrice: number): Promise<AdminMashupPackage> => {
    const res = await apiClient.put(`/admin/mashup/packages/${id}/price`, { sellingPrice }) as ApiWrap<AdminMashupPackage>;
    return res.data;
  },
  setMashupStatus: async (id: string, status: string): Promise<AdminMashupPackage> => {
    const res = await apiClient.put(`/admin/mashup/packages/${id}/status`, { status }) as ApiWrap<AdminMashupPackage>;
    return res.data;
  },

  // Transactions
  getAllTransactions: async (params?: {
    status?: string;
    type?: string;
    search?: string;
    fromDate?: string;
    toDate?: string;
  }): Promise<AdminTransaction[]> => {
    const query = new URLSearchParams();
    if (params?.status) query.append("status", params.status);
    if (params?.type) query.append("type", params.type);
    if (params?.search) query.append("search", params.search);
    if (params?.fromDate) query.append("fromDate", params.fromDate);
    if (params?.toDate) query.append("toDate", params.toDate);
    const res = await apiClient.get(`/admin/transactions?${query.toString()}`) as ApiWrap<AdminTransaction[]>;
    return res.data;
  },

  // Announcements
  getAnnouncements: async (): Promise<Announcement[]> => {
    const res = await apiClient.get("/announcements") as ApiWrap<Announcement[]>;
    return res.data;
  },
  createAnnouncement: async (data: { title: string; message: string; active: boolean }): Promise<Announcement> => {
    const res = await apiClient.post("/announcements", data) as ApiWrap<Announcement>;
    return res.data;
  },
  updateAnnouncement: async (id: string, data: { title: string; message: string; active: boolean }): Promise<Announcement> => {
    const res = await apiClient.put(`/announcements/${id}`, data) as ApiWrap<Announcement>;
    return res.data;
  },
  deleteAnnouncement: async (id: string): Promise<void> => {
    await apiClient.delete(`/announcements/${id}`);
  },

  // CheckerPort Transactions
  getCheckerTransactions: async (): Promise<CheckerTransaction[]> => {
    const res = await apiClient.get("/admin/results-checker/transactions") as ApiWrap<CheckerTransaction[]>;
    return res.data;
  },
  retryCheckerTransaction: async (referenceId: string): Promise<CheckerTransaction> => {
    const res = await apiClient.post(`/admin/results-checker/transactions/${referenceId}/retry`) as ApiWrap<CheckerTransaction>;
    return res.data;
  },
  updateCheckerPricing: async (serviceName: string, retailPrice: number | null): Promise<ResultCheckerPricing> => {
    const res = await apiClient.put(`/admin/results-checker/pricing/${serviceName}`, { retailPrice }) as ApiWrap<ResultCheckerPricing>;
    return res.data;
  },

  // SMS Packages
  getSmsPackages: async (): Promise<SmsPackage[]> => {
    const res = await apiClient.get("/admin/sms-packages") as ApiWrap<SmsPackage[]>;
    return res.data;
  },
  createSmsPackage: async (data: { name: string; messagesCount: number; price: number; active: boolean }): Promise<SmsPackage> => {
    const res = await apiClient.post("/admin/sms-packages", data) as ApiWrap<SmsPackage>;
    return res.data;
  },
  updateSmsPackage: async (id: string, data: { name?: string; messagesCount?: number; price?: number; active: boolean }): Promise<SmsPackage> => {
    const res = await apiClient.put(`/admin/sms-packages/${id}`, data) as ApiWrap<SmsPackage>;
    return res.data;
  },
  deleteSmsPackage: async (id: string): Promise<void> => {
    await apiClient.delete(`/admin/sms-packages/${id}`);
  },
};

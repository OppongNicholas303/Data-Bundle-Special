import { useState, useMemo, Fragment } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Search, ChevronDown, ChevronUp, RefreshCw, TrendingUp, ShoppingCart, Wallet } from "lucide-react";
import { toast } from "sonner";
import { adminService } from "@/lib/adminService";
import { formatCurrency, formatDate } from "@/lib/utils";
import type { AdminAgent } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";

function AgentDetail({ agent }: { agent: AdminAgent }) {
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<"overview" | "orders" | "commissions" | "withdrawals">("overview");
  const [period, setPeriod] = useState<"TODAY" | "7D" | "ALL">("TODAY");
  const [orderStatus, setOrderStatus] = useState<string | undefined>(undefined);
  const [commissionStatus, setCommissionStatus] = useState<string | undefined>(undefined);

  const computeRange = (p: "TODAY" | "7D" | "ALL") => {
    if (p === "ALL") return { from: undefined, to: undefined };
    const now = new Date();
    const to = now.toISOString().slice(0, 10);
    if (p === "TODAY") return { from: to, to };
    const fromDate = new Date(now);
    fromDate.setDate(now.getDate() - 6); // last 7 days
    const from = fromDate.toISOString().slice(0, 10);
    return { from, to };
  };
  const { from, to } = computeRange(period);
  const { data: commissions = [], isLoading: loadingC } = useQuery({
    queryKey: ["agent-commissions", agent.id, period, commissionStatus],
    queryFn: () => adminService.getAgentCommissions(agent.id, commissionStatus, from, to),
  });
  const { data: orders = [], isLoading: loadingO } = useQuery({
    queryKey: ["agent-orders", agent.id, period, orderStatus],
    queryFn: () => adminService.getAgentOrders(agent.id, orderStatus, undefined, from, to),
  });
  const { data: withdrawals = [], isLoading: loadingW } = useQuery({
    queryKey: ["agent-withdrawals", agent.id],
    queryFn: () => adminService.getAgentWithdrawals(agent.id),
  });

  const { data: wallet, isLoading: loadingWallet } = useQuery({
    queryKey: ["agent-wallet", agent.id],
    queryFn: () => adminService.getAgentWallet(agent.id),
  });

  const [topUpOpen, setTopUpOpen] = useState(false);
  const [topUpAmount, setTopUpAmount] = useState("");
  const [adminNote, setAdminNote] = useState("");

  const topUpMutation = useMutation({
    mutationFn: () => adminService.topUpAgentWallet(agent.id, parseFloat(topUpAmount || "0"), adminNote),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: ["agent-wallet", agent.id] });
      qc.invalidateQueries({ queryKey: ["admin-agents"] });
      toast.success("Top-up successful");
      setTopUpOpen(false);
      setTopUpAmount("");
      setAdminNote("");
    },
    onError: (err: Error) => toast.error(err.message),
  });

  return (
    <tr>
      <td colSpan={6} className="p-0">
        <div className="bg-muted/5 border-b flex flex-col">
          {/* Custom Tabs Navigation */}
          <div className="flex border-b px-6 gap-6 bg-muted/10">
            {["overview", "orders", "commissions", "withdrawals"].map(tab => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab as any)}
                className={`py-3 text-sm font-medium border-b-2 capitalize transition-colors ${
                  activeTab === tab
                    ? "border-primary text-primary"
                    : "border-transparent text-muted-foreground hover:text-foreground"
                }`}
              >
                {tab}
              </button>
            ))}
          </div>

          <div className="p-6 overflow-hidden">
            {activeTab === "overview" && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* Lifetime Stats */}
                <Card className="shadow-none border-border/50">
                  <CardContent className="p-6">
                    <p className="text-sm font-medium text-muted-foreground flex items-center gap-2 mb-2">
                      <TrendingUp className="h-4 w-4 text-primary" /> Lifetime Performance
                    </p>
                    <div className="space-y-4">
                      <div>
                        <p className="text-xs text-muted-foreground">Total Sales Volume</p>
                        <p className="text-xl font-bold">{formatCurrency(agent.totalSales || 0)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">Total Profit Earned</p>
                        <p className="text-xl font-bold text-success">{formatCurrency(agent.totalProfit || 0)}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                {/* Wallet Balance */}
                <Card className="shadow-none border-border/50 bg-primary/5 border-primary/20">
                  <CardContent className="p-6 flex flex-col h-full justify-between">
                    <div>
                      <p className="text-sm font-medium text-primary flex items-center gap-2 mb-2">
                        <Wallet className="h-4 w-4" /> Wallet Balances
                      </p>
                      {loadingWallet ? (
                        <p className="text-3xl font-bold">...</p>
                      ) : wallet ? (
                        <div className="space-y-2">
                          <div>
                            <p className="text-xs text-muted-foreground">Main</p>
                            <p className="text-2xl font-bold">{formatCurrency(wallet.balance)}</p>
                          </div>
                          <div>
                            <p className="text-xs text-muted-foreground">Commission</p>
                            <p className="text-2xl font-bold text-success">{formatCurrency(wallet.commissionBalance || 0)}</p>
                          </div>
                        </div>
                      ) : (
                        <p className="text-3xl font-bold">—</p>
                      )}
                    </div>
                    <div className="mt-6">
                      <Button className="w-full" onClick={() => setTopUpOpen(true)}>Top Up Wallet</Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}
            
            {activeTab === "orders" && (
              <div className="flex flex-col h-full">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
                  <div className="flex items-center gap-3">
                    <div className="space-y-1">
                      <label className="text-xs text-muted-foreground">Time Period</label>
                      <Select value={period} onValueChange={(val: any) => setPeriod(val)}>
                        <SelectTrigger className="w-[140px] h-9"><SelectValue /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="TODAY">Today</SelectItem>
                          <SelectItem value="7D">Last 7 days</SelectItem>
                          <SelectItem value="ALL">All time</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs text-muted-foreground">Order Status</label>
                      <Select value={orderStatus || "ALL"} onValueChange={(val: any) => setOrderStatus(val === "ALL" ? undefined : val)}>
                        <SelectTrigger className="w-[140px] h-9"><SelectValue /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="ALL">All statuses</SelectItem>
                          <SelectItem value="PENDING">PENDING</SelectItem>
                          <SelectItem value="COMPLETED">COMPLETED</SelectItem>
                          <SelectItem value="FAILED">FAILED</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => qc.invalidateQueries({ queryKey: ["agent-orders", agent.id] })}>
                    <RefreshCw className="h-4 w-4 mr-2" /> Refresh Orders
                  </Button>
                </div>

                <div className="border rounded-lg bg-card overflow-hidden">
                  <table className="w-full text-sm">
                    <thead className="bg-muted/50 text-muted-foreground">
                      <tr>
                        <th className="text-left py-3 px-4 font-medium">Order Details</th>
                        <th className="text-left py-3 px-4 font-medium">Recipient</th>
                        <th className="text-right py-3 px-4 font-medium">Date</th>
                        <th className="text-right py-3 px-4 font-medium">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {loadingO ? (
                        <tr><td colSpan={4} className="py-8 text-center text-muted-foreground">Loading orders...</td></tr>
                      ) : orders.length === 0 ? (
                        <tr><td colSpan={4} className="py-8 text-center text-muted-foreground">No orders found for this period.</td></tr>
                      ) : (
                        orders.map(o => (
                          <tr key={o.id} className="hover:bg-muted/30 transition-colors">
                            <td className="py-3 px-4">
                              <p className="font-medium">{o.bundleCode}</p>
                              <p className="text-xs text-muted-foreground">{o.network?.toUpperCase()} • {o.id.slice(-8)}</p>
                            </td>
                            <td className="py-3 px-4">
                              <p>{o.phoneNumber}</p>
                            </td>
                            <td className="py-3 px-4 text-right whitespace-nowrap">
                              <p>{new Date(o.createdAt).toLocaleDateString()}</p>
                              <p className="text-xs text-muted-foreground">{new Date(o.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                            </td>
                            <td className="py-3 px-4 text-right">
                              <span className={`status-${o.status.toLowerCase()}`}>{o.status}</span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {activeTab === "commissions" && (
              <div className="flex flex-col h-full">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
                  <div className="flex items-center gap-3">
                    <div className="space-y-1">
                      <label className="text-xs text-muted-foreground">Time Period</label>
                      <Select value={period} onValueChange={(val: any) => setPeriod(val)}>
                        <SelectTrigger className="w-[140px] h-9"><SelectValue /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="TODAY">Today</SelectItem>
                          <SelectItem value="7D">Last 7 days</SelectItem>
                          <SelectItem value="ALL">All time</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs text-muted-foreground">Status</label>
                      <Select value={commissionStatus || "ALL"} onValueChange={(val: any) => setCommissionStatus(val === "ALL" ? undefined : val)}>
                        <SelectTrigger className="w-[140px] h-9"><SelectValue /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="ALL">All statuses</SelectItem>
                          <SelectItem value="PENDING">PENDING</SelectItem>
                          <SelectItem value="SETTLED">SETTLED</SelectItem>
                          <SelectItem value="REVERSED">REVERSED</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => qc.invalidateQueries({ queryKey: ["agent-commissions", agent.id] })}>
                    <RefreshCw className="h-4 w-4 mr-2" /> Refresh
                  </Button>
                </div>

                <div className="border rounded-lg bg-card overflow-hidden">
                  <table className="w-full text-sm">
                    <thead className="bg-muted/50 text-muted-foreground">
                      <tr>
                        <th className="text-left py-3 px-4 font-medium">Order Ref</th>
                        <th className="text-right py-3 px-4 font-medium">Date</th>
                        <th className="text-right py-3 px-4 font-medium">Profit</th>
                        <th className="text-right py-3 px-4 font-medium">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {loadingC ? (
                        <tr><td colSpan={4} className="py-8 text-center text-muted-foreground">Loading commissions...</td></tr>
                      ) : commissions.length === 0 ? (
                        <tr><td colSpan={4} className="py-8 text-center text-muted-foreground">No commissions found for this period.</td></tr>
                      ) : (
                        commissions.map(c => (
                          <tr key={c.id} className="hover:bg-muted/30 transition-colors">
                            <td className="py-3 px-4 font-mono text-xs">{c.orderId.slice(-12)}</td>
                            <td className="py-3 px-4 text-right whitespace-nowrap">
                              <p>{new Date(c.createdAt).toLocaleDateString()}</p>
                              <p className="text-xs text-muted-foreground">{new Date(c.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                            </td>
                            <td className="py-3 px-4 text-right font-semibold text-success">
                              {formatCurrency(c.profit)}
                            </td>
                            <td className="py-3 px-4 text-right">
                              <span className={c.status === "SETTLED" ? "status-completed" : "status-pending"}>{c.status}</span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {activeTab === "withdrawals" && (
              <div className="flex flex-col h-full">
                <div className="flex justify-end mb-6">
                  <Button variant="outline" size="sm" onClick={() => qc.invalidateQueries({ queryKey: ["agent-withdrawals", agent.id] })}>
                    <RefreshCw className="h-4 w-4 mr-2" /> Refresh
                  </Button>
                </div>

                <div className="border rounded-lg bg-card overflow-hidden">
                  <table className="w-full text-sm">
                    <thead className="bg-muted/50 text-muted-foreground">
                      <tr>
                        <th className="text-left py-3 px-4 font-medium">Withdrawal Info</th>
                        <th className="text-right py-3 px-4 font-medium">Date</th>
                        <th className="text-right py-3 px-4 font-medium">Amount</th>
                        <th className="text-right py-3 px-4 font-medium">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {loadingW ? (
                        <tr><td colSpan={4} className="py-8 text-center text-muted-foreground">Loading withdrawals...</td></tr>
                      ) : withdrawals.length === 0 ? (
                        <tr><td colSpan={4} className="py-8 text-center text-muted-foreground">No withdrawals yet.</td></tr>
                      ) : (
                        withdrawals.map(w => (
                          <tr key={w.id} className="hover:bg-muted/30 transition-colors">
                            <td className="py-3 px-4">
                              <p className="font-medium">{w.momoProvider} - {w.momoNumber}</p>
                              <p className="text-xs text-muted-foreground">{w.id.slice(-8)}</p>
                            </td>
                            <td className="py-3 px-4 text-right whitespace-nowrap">
                              <p>{new Date(w.createdAt).toLocaleDateString()}</p>
                              <p className="text-xs text-muted-foreground">{new Date(w.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                            </td>
                            <td className="py-3 px-4 text-right font-semibold">
                              {formatCurrency(w.amount)}
                            </td>
                            <td className="py-3 px-4 text-right">
                              <span className={`status-${w.status.toLowerCase() === 'approved' ? 'completed' : w.status.toLowerCase() === 'rejected' ? 'failed' : 'pending'}`}>
                                {w.status}
                              </span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
          
          {/* Top-up dialog */}
          <Dialog open={topUpOpen} onOpenChange={open => !open && setTopUpOpen(false)}>
            <DialogContent className="max-w-sm">
              <DialogHeader>
                <DialogTitle className="flex items-center gap-2">Top Up Wallet</DialogTitle>
                <DialogDescription>Credit {agent.businessName}'s wallet immediately.</DialogDescription>
              </DialogHeader>

              <div className="space-y-3">
                <div className="space-y-1.5">
                  <Label htmlFor="topup-amount">Amount (GHS)</Label>
                  <Input id="topup-amount" type="number" step="0.01" min="0.01" value={topUpAmount} onChange={e => setTopUpAmount(e.target.value)} />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="topup-note">Admin note (optional)</Label>
                  <Input id="topup-note" placeholder="e.g. Manual credit" value={adminNote} onChange={e => setAdminNote(e.target.value)} />
                </div>

                <div className="flex gap-2 pt-2">
                  <Button variant="outline" className="flex-1" onClick={() => setTopUpOpen(false)} disabled={topUpMutation.isPending}>Cancel</Button>
                  <Button className="flex-1" onClick={() => topUpMutation.mutate()} disabled={topUpMutation.isPending || !topUpAmount || parseFloat(topUpAmount) <= 0}>
                    {topUpMutation.isPending ? "Processing..." : "Top Up"}
                  </Button>
                </div>
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </td>
    </tr>
  );
}

export default function AgentsPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const { data: agents = [], isLoading, refetch } = useQuery({
    queryKey: ["admin-agents"],
    queryFn: adminService.getAgents,
  });

  const toggleMutation = useMutation({
    mutationFn: (id: string) => adminService.toggleAgentActive(id),
    onSuccess: (updated) => {
      qc.setQueryData<AdminAgent[]>(["admin-agents"], old =>
        old?.map(a => a.id === updated.id ? updated : a) ?? []
      );
      toast.success(updated.active ? "Agent activated" : "Agent deactivated");
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const filtered = useMemo(() => agents.filter(a => {
    const matchSearch =
      a.businessName.toLowerCase().includes(search.toLowerCase()) ||
      a.referralCode.toLowerCase().includes(search.toLowerCase()) ||
      a.userId.includes(search);
    const matchStatus =
      filterStatus === "ALL" ||
      (filterStatus === "ACTIVE" && a.active) ||
      (filterStatus === "INACTIVE" && !a.active);
    return matchSearch && matchStatus;
  }).sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()), [agents, search, filterStatus]);

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search by business name or code..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="pl-9"
            aria-label="Search agents"
          />
        </div>
        <Select value={filterStatus} onValueChange={setFilterStatus}>
          <SelectTrigger className="w-full sm:w-40" aria-label="Filter by status">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Status</SelectItem>
            <SelectItem value="ACTIVE">Active</SelectItem>
            <SelectItem value="INACTIVE">Inactive</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" size="icon" onClick={() => refetch()} aria-label="Refresh">
          <RefreshCw className="h-4 w-4" />
        </Button>
      </div>

      <p className="text-sm text-muted-foreground">{filtered.length} agent{filtered.length !== 1 ? "s" : ""} found</p>

      <Card>
        <CardContent className="p-0 overflow-x-auto">
          {isLoading ? (
            <div className="p-8 text-center text-muted-foreground text-sm">Loading agents...</div>
          ) : filtered.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground text-sm">No agents found.</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/30">
                  <th className="text-left p-4 font-medium text-muted-foreground">Business</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden sm:table-cell">Code</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden lg:table-cell">Total Sales</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden lg:table-cell">Total Profit</th>
                  <th className="text-left p-4 font-medium text-muted-foreground">Status</th>
                  <th className="text-right p-4 font-medium text-muted-foreground">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(agent => (
                  <Fragment key={agent.id}>
                    <tr className="border-b last:border-0 hover:bg-muted/20 transition-colors">
                      <td className="p-4">
                        <div>
                          <p className="font-medium">{agent.businessName}</p>
                          <p className="text-xs text-muted-foreground hidden sm:block">{formatDate(agent.createdAt)}</p>
                        </div>
                      </td>
                      <td className="p-4 hidden sm:table-cell">
                        <code className="bg-muted px-2 py-0.5 rounded text-xs font-mono">{agent.referralCode}</code>
                      </td>
                      <td className="p-4 hidden lg:table-cell font-semibold">{formatCurrency(agent.totalSales)}</td>
                      <td className="p-4 hidden lg:table-cell text-success font-semibold">{formatCurrency(agent.totalProfit)}</td>
                      <td className="p-4">
                        {agent.active ? (
                          <Badge variant="success" className="text-[10px]">Active</Badge>
                        ) : (
                          <Badge variant="muted" className="text-[10px]">Inactive</Badge>
                        )}
                      </td>
                      <td className="p-4">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => toggleMutation.mutate(agent.id)}
                            disabled={toggleMutation.isPending}
                            className="h-8 px-2 text-xs"
                            aria-label={agent.active ? "Deactivate agent" : "Activate agent"}
                          >
                            {agent.active ? "Deactivate" : "Activate"}
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => setExpandedId(expandedId === agent.id ? null : agent.id)}
                            aria-label={expandedId === agent.id ? "Collapse details" : "Expand details"}
                          >
                            {expandedId === agent.id ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                          </Button>
                        </div>
                      </td>
                    </tr>
                    {expandedId === agent.id && <AgentDetail key={`detail-${agent.id}`} agent={agent} />}
                  </Fragment>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

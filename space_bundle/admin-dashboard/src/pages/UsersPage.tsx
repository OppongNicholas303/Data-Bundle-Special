import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Search, Lock, Unlock, UserCheck, UserX, Shield, RefreshCw, Wallet } from "lucide-react";
import { toast } from "sonner";
import { adminService } from "@/lib/adminService";
import { formatDate } from "@/lib/utils";
import type { AdminUser } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription,
} from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

const ALL_ROLES = ["ROLE_USER", "ROLE_ADMIN", "ROLE_AGENT", "ROLE_SUPPORT"];

function RoleBadge({ role }: { role: string }) {
  const map: Record<string, "default" | "success" | "warning" | "destructive" | "muted"> = {
    ROLE_ADMIN: "destructive",
    ROLE_AGENT: "success",
    ROLE_SUPPORT: "warning",
    ROLE_USER: "muted",
  };
  return <Badge variant={map[role] ?? "muted"} className="text-[10px]">{role.replace("ROLE_", "")}</Badge>;
}

export default function UsersPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [filterRole, setFilterRole] = useState("ALL");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [rolesUser, setRolesUser] = useState<AdminUser | null>(null);
  const [pendingRoles, setPendingRoles] = useState<string[]>([]);

  const [walletUser, setWalletUser] = useState<AdminUser | null>(null);
  const [walletAction, setWalletAction] = useState<"credit" | "debit">("credit");
  const [walletAmount, setWalletAmount] = useState("");
  const [walletDescription, setWalletDescription] = useState("");
  const [txFilter, setTxFilter] = useState("ALL");

  const { data: walletData, isLoading: isLoadingWallet, refetch: refetchWallet } = useQuery({
    queryKey: ["admin-user-wallet", walletUser?.id],
    queryFn: () => walletUser ? adminService.getUserWallet(walletUser.id) : null,
    enabled: !!walletUser,
  });

  const { data: transactionsData, refetch: refetchTransactions } = useQuery({
    queryKey: ["admin-user-transactions", walletUser?.id],
    queryFn: () => walletUser ? adminService.getUserTransactions(walletUser.id) : null,
    enabled: !!walletUser,
  });

  const creditMutation = useMutation({
    mutationFn: ({ id, amount, description }: { id: string, amount: number, description: string }) => adminService.creditUserWallet(id, amount, description),
    onSuccess: () => {
      toast.success("Wallet credited successfully");
      refetchWallet();
      refetchTransactions();
      setWalletAmount("");
      setWalletDescription("");
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const debitMutation = useMutation({
    mutationFn: ({ id, amount, description }: { id: string, amount: number, description: string }) => adminService.debitUserWallet(id, amount, description),
    onSuccess: () => {
      toast.success("Wallet debited successfully");
      refetchWallet();
      refetchTransactions();
      setWalletAmount("");
      setWalletDescription("");
    },
    onError: (err: Error) => toast.error(err.message),
  });


  const { data: users = [], isLoading, refetch } = useQuery({
    queryKey: ["admin-users"],
    queryFn: adminService.getUsers,
  });

  const lockMutation = useMutation({
    mutationFn: (id: string) => adminService.toggleUserLock(id),
    onSuccess: (updated) => {
      qc.setQueryData<AdminUser[]>(["admin-users"], old =>
        old?.map(u => u.id === updated.id ? updated : u) ?? []
      );
      toast.success(updated.accountNonLocked ? "User unlocked" : "User locked");
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const enableMutation = useMutation({
    mutationFn: (id: string) => adminService.toggleUserEnabled(id),
    onSuccess: (updated) => {
      qc.setQueryData<AdminUser[]>(["admin-users"], old =>
        old?.map(u => u.id === updated.id ? updated : u) ?? []
      );
      toast.success(updated.enabled ? "User enabled" : "User disabled");
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const rolesMutation = useMutation({
    mutationFn: ({ id, roles }: { id: string; roles: string[] }) =>
      adminService.updateUserRoles(id, roles),
    onSuccess: (updated) => {
      qc.setQueryData<AdminUser[]>(["admin-users"], old =>
        old?.map(u => u.id === updated.id ? updated : u) ?? []
      );
      toast.success("Roles updated");
      setRolesUser(null);
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const filtered = useMemo(() => {
    return users.filter(u => {
      const matchSearch =
        u.username.toLowerCase().includes(search.toLowerCase()) ||
        u.email.toLowerCase().includes(search.toLowerCase()) ||
        u.phoneNumber?.includes(search);
      const matchRole = filterRole === "ALL" || u.roles.includes(filterRole);
      const matchStatus =
        filterStatus === "ALL" ||
        (filterStatus === "ENABLED" && u.enabled) ||
        (filterStatus === "DISABLED" && !u.enabled) ||
        (filterStatus === "LOCKED" && !u.accountNonLocked);
      return matchSearch && matchRole && matchStatus;
    });
  }, [users, search, filterRole, filterStatus]);

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search by name, email, phone..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="pl-9"
            aria-label="Search users"
          />
        </div>
        <Select value={filterRole} onValueChange={setFilterRole}>
          <SelectTrigger className="w-full sm:w-40" aria-label="Filter by role">
            <SelectValue placeholder="Role" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Roles</SelectItem>
            {ALL_ROLES.map(r => <SelectItem key={r} value={r}>{r.replace("ROLE_", "")}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={filterStatus} onValueChange={setFilterStatus}>
          <SelectTrigger className="w-full sm:w-40" aria-label="Filter by status">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Status</SelectItem>
            <SelectItem value="ENABLED">Enabled</SelectItem>
            <SelectItem value="DISABLED">Disabled</SelectItem>
            <SelectItem value="LOCKED">Locked</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" size="icon" onClick={() => refetch()} aria-label="Refresh">
          <RefreshCw className="h-4 w-4" />
        </Button>
      </div>

      <p className="text-sm text-muted-foreground">{filtered.length} user{filtered.length !== 1 ? "s" : ""} found</p>

      {/* Table */}
      <Card>
        <CardContent className="p-0 overflow-x-auto">
          {isLoading ? (
            <div className="p-8 text-center text-muted-foreground text-sm">Loading users...</div>
          ) : filtered.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground text-sm">No users match your filters.</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/30">
                  <th className="text-left p-4 font-medium text-muted-foreground">User</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden md:table-cell">Phone</th>
                  <th className="text-left p-4 font-medium text-muted-foreground">Roles</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden lg:table-cell">Joined</th>
                  <th className="text-left p-4 font-medium text-muted-foreground">Status</th>
                  <th className="text-right p-4 font-medium text-muted-foreground">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(user => (
                  <tr key={user.id} className="border-b last:border-0 hover:bg-muted/20 transition-colors">
                    <td className="p-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary to-secondary flex items-center justify-center text-primary-foreground font-semibold text-xs flex-shrink-0">
                          {user.username.charAt(0).toUpperCase()}
                        </div>
                        <div className="min-w-0">
                          <p className="font-medium truncate">{user.username}</p>
                          <p className="text-xs text-muted-foreground truncate">{user.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-4 text-muted-foreground hidden md:table-cell">{user.phoneNumber || "—"}</td>
                    <td className="p-4">
                      <div className="flex flex-wrap gap-1">
                        {user.roles.map(r => <RoleBadge key={r} role={r} />)}
                      </div>
                    </td>
                    <td className="p-4 text-muted-foreground text-xs hidden lg:table-cell">{formatDate(user.createdAt)}</td>
                    <td className="p-4">
                      <div className="flex flex-col gap-1">
                        {user.enabled ? (
                          <Badge variant="success" className="text-[10px] w-fit">Enabled</Badge>
                        ) : (
                          <Badge variant="destructive" className="text-[10px] w-fit">Disabled</Badge>
                        )}
                        {!user.accountNonLocked && (
                          <Badge variant="warning" className="text-[10px] w-fit">Locked</Badge>
                        )}
                      </div>
                    </td>
                    <td className="p-4">
                      <div className="flex items-center justify-end gap-1 flex-wrap">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => enableMutation.mutate(user.id)}
                          disabled={enableMutation.isPending}
                          title={user.enabled ? "Disable user" : "Enable user"}
                          aria-label={user.enabled ? "Disable user" : "Enable user"}
                          className="h-8 px-2 text-xs"
                        >
                          {user.enabled ? <UserX className="h-3.5 w-3.5" /> : <UserCheck className="h-3.5 w-3.5" />}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => lockMutation.mutate(user.id)}
                          disabled={lockMutation.isPending}
                          title={user.accountNonLocked ? "Lock account" : "Unlock account"}
                          aria-label={user.accountNonLocked ? "Lock account" : "Unlock account"}
                          className="h-8 px-2 text-xs"
                        >
                          {user.accountNonLocked ? <Lock className="h-3.5 w-3.5" /> : <Unlock className="h-3.5 w-3.5" />}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => { setRolesUser(user); setPendingRoles([...user.roles]); }}
                          title="Manage roles"
                          aria-label="Manage roles"
                          className="h-8 px-2 text-xs"
                        >
                          <Shield className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setWalletUser(user)}
                          title="Manage wallet"
                          aria-label="Manage wallet"
                          className="h-8 px-2 text-xs text-primary"
                        >
                          <Wallet className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      {/* Roles Dialog */}
      <Dialog open={!!rolesUser} onOpenChange={open => !open && setRolesUser(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Manage Roles — {rolesUser?.username}</DialogTitle>
            <DialogDescription>Toggle roles for this user. Changes take effect immediately.</DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2">
            {ALL_ROLES.map(role => (
              <label key={role} className="flex items-center gap-3 cursor-pointer p-2 rounded-lg hover:bg-accent transition-colors">
                <input
                  type="checkbox"
                  checked={pendingRoles.includes(role)}
                  onChange={e => {
                    setPendingRoles(prev =>
                      e.target.checked ? [...prev, role] : prev.filter(r => r !== role)
                    );
                  }}
                  className="w-4 h-4 accent-primary"
                  aria-label={`Role ${role}`}
                />
                <span className="text-sm font-medium">{role}</span>
                <RoleBadge role={role} />
              </label>
            ))}
          </div>
          <div className="flex gap-2 pt-2">
            <Button variant="outline" onClick={() => setRolesUser(null)} className="flex-1">Cancel</Button>
            <Button
              onClick={() => rolesUser && rolesMutation.mutate({ id: rolesUser.id, roles: pendingRoles })}
              disabled={rolesMutation.isPending}
              className="flex-1"
            >
              {rolesMutation.isPending ? "Saving..." : "Save Roles"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Wallet Dialog */}
      <Dialog open={!!walletUser} onOpenChange={open => !open && setWalletUser(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Wallet — {walletUser?.username}</DialogTitle>
            <DialogDescription>
              Adjust user's wallet balance.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="p-4 bg-muted/50 rounded-lg text-center">
              <p className="text-sm text-muted-foreground mb-1">Current Balance</p>
              {isLoadingWallet ? (
                <p className="text-2xl font-bold animate-pulse">...</p>
              ) : (
                <p className="text-2xl font-bold">
                  {walletData?.currency} {walletData?.balance?.toFixed(2)}
                </p>
              )}
            </div>

            <div className="flex gap-2">
              <Button 
                variant={walletAction === "credit" ? "default" : "outline"} 
                className="flex-1"
                onClick={() => setWalletAction("credit")}
              >
                Credit
              </Button>
              <Button 
                variant={walletAction === "debit" ? "default" : "outline"} 
                className="flex-1"
                onClick={() => setWalletAction("debit")}
              >
                Debit
              </Button>
            </div>

            <div className="space-y-2">
              <Input 
                type="number" 
                placeholder="Amount (GHS)" 
                value={walletAmount}
                onChange={e => setWalletAmount(e.target.value)}
              />
              <Input 
                type="text" 
                placeholder="Description (Optional)" 
                value={walletDescription}
                onChange={e => setWalletDescription(e.target.value)}
              />
            </div>

            <Button
              className="w-full"
              disabled={!walletAmount || Number(walletAmount) <= 0 || creditMutation.isPending || debitMutation.isPending}
              onClick={() => {
                if (!walletUser) return;
                const amt = Number(walletAmount);
                if (walletAction === "credit") {
                  creditMutation.mutate({ id: walletUser.id, amount: amt, description: walletDescription });
                } else {
                  debitMutation.mutate({ id: walletUser.id, amount: amt, description: walletDescription });
                }
              }}
            >
              {(creditMutation.isPending || debitMutation.isPending) ? "Processing..." : `Confirm ${walletAction === "credit" ? "Credit" : "Debit"}`}
            </Button>

            <div className="pt-4 border-t mt-4">
              <div className="flex justify-between items-center mb-2">
                <p className="text-sm font-semibold">Recent Transactions</p>
                <Select value={txFilter} onValueChange={setTxFilter}>
                  <SelectTrigger className="w-[110px] h-7 text-xs">
                    <SelectValue placeholder="Filter" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">All</SelectItem>
                    <SelectItem value="CREDIT">Credits</SelectItem>
                    <SelectItem value="DEBIT">Debits</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2 max-h-48 overflow-y-auto pr-2">
                {!transactionsData ? (
                  <p className="text-xs text-muted-foreground text-center">Loading transactions...</p>
                ) : transactionsData.length === 0 ? (
                  <p className="text-xs text-muted-foreground text-center">No transactions found.</p>
                ) : (
                  transactionsData
                    .filter((tx: any) => txFilter === "ALL" || tx.type === txFilter)
                    .map((tx: any) => (
                    <div key={tx.id} className="flex justify-between items-center bg-muted/30 p-2 rounded text-xs">
                      <div>
                        <p className="font-medium">{tx.type} <span className="text-muted-foreground font-normal">({tx.status})</span></p>
                        <p className="text-[10px] text-muted-foreground">{tx.description || "No description"}</p>
                        <p className="text-[9px] text-muted-foreground mt-0.5">{formatDate(tx.createdAt)}</p>
                      </div>
                      <div className={`font-bold ${tx.type === 'CREDIT' || tx.type === 'REFUND' || tx.type === 'COMMISSION' ? 'text-green-600' : 'text-red-600'}`}>
                        {tx.type === 'CREDIT' || tx.type === 'REFUND' || tx.type === 'COMMISSION' ? '+' : '-'}{tx.currency} {tx.amount.toFixed(2)}
                      </div>
                    </div>
                  ))
                )}
                {transactionsData && transactionsData.filter((tx: any) => txFilter === "ALL" || tx.type === txFilter).length === 0 && transactionsData.length > 0 && (
                  <p className="text-xs text-muted-foreground text-center">No transactions match the filter.</p>
                )}
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

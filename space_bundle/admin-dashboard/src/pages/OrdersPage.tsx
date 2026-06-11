import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, RefreshCw, Calendar, X } from "lucide-react";
import { adminService } from "@/lib/adminService";
import { formatCurrency, formatDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

const STATUSES = ["CREATED", "PENDING_PAYMENT", "PAID", "PROCESSING", "COMPLETED", "FAILED", "REFUNDED"];
const NETWORKS = ["mtn", "telecel", "airteltigo"];

function StatusBadge({ status }: { status: string }) {
  const cls: Record<string, string> = {
    COMPLETED: "status-completed", PROCESSING: "status-processing",
    PENDING_PAYMENT: "status-pending", PAID: "status-pending",
    CREATED: "status-pending", FAILED: "status-failed", REFUNDED: "status-refunded",
  };
  return <span className={cls[status] ?? "status-inactive"}>{status.replace(/_/g, " ")}</span>;
}

export default function OrdersPage() {
  const [search,        setSearch]        = useState("");
  const [filterStatus,  setFilterStatus]  = useState("ALL");
  const [filterNetwork, setFilterNetwork] = useState("ALL");
  const [fromDate,      setFromDate]      = useState("");
  const [toDate,        setToDate]        = useState("");

  const { data: orders = [], isLoading, refetch } = useQuery({
    queryKey: ["admin-orders", filterStatus, filterNetwork, fromDate, toDate],
    queryFn: () => adminService.getOrders(
      filterStatus  !== "ALL" ? filterStatus  : undefined,
      filterNetwork !== "ALL" ? filterNetwork : undefined,
      fromDate || undefined,
      toDate   || undefined,
    ),
  });

  const filtered = useMemo(() => orders.filter(o => {
    const s = search.toLowerCase();
    return !s || o.phoneNumber.includes(s) || o.bundleCode.toLowerCase().includes(s) ||
      o.id.includes(s) || o.userId.includes(s);
  }), [orders, search]);

  const totalRevenue   = filtered.filter(o => o.status === "COMPLETED").reduce((s, o) => s + o.amount, 0);
  const totalProfit    = filtered.filter(o => o.status === "COMPLETED").reduce((s, o) => s + o.platformProfit, 0);
  const completedCount = filtered.filter(o => o.status === "COMPLETED").length;
  const failedCount    = filtered.filter(o => o.status === "FAILED").length;
  const hasDateFilter  = fromDate || toDate;
  const hasAnyFilter   = hasDateFilter || filterStatus !== "ALL" || filterNetwork !== "ALL";

  return (
    <div className="space-y-4">
      {/* Row 1 — search + status + network + refresh */}
      <div className="flex flex-col sm:flex-row gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search phone, bundle, order ID..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="pl-9"
            aria-label="Search orders"
          />
        </div>
        <Select value={filterStatus} onValueChange={setFilterStatus}>
          <SelectTrigger className="w-full sm:w-44" aria-label="Filter by status">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Status</SelectItem>
            {STATUSES.map(s => <SelectItem key={s} value={s}>{s.replace(/_/g, " ")}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={filterNetwork} onValueChange={setFilterNetwork}>
          <SelectTrigger className="w-full sm:w-40" aria-label="Filter by network">
            <SelectValue placeholder="Network" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Networks</SelectItem>
            {NETWORKS.map(n => <SelectItem key={n} value={n}>{n.toUpperCase()}</SelectItem>)}
          </SelectContent>
        </Select>
        <Button variant="outline" size="icon" onClick={() => refetch()} aria-label="Refresh">
          <RefreshCw className="h-4 w-4" />
        </Button>
      </div>

      {/* Row 2 — date range */}
      <div className="flex flex-col sm:flex-row items-end gap-2">
        <div className="flex items-center gap-1.5 self-start sm:self-end pb-2">
          <Calendar className="h-4 w-4 text-muted-foreground" />
          <span className="text-sm text-muted-foreground font-medium whitespace-nowrap">Date range:</span>
        </div>
        <div className="flex flex-1 flex-col sm:flex-row gap-2">
          <div className="flex-1 space-y-1">
            <Label htmlFor="from-date" className="text-xs text-muted-foreground">From</Label>
            <Input
              id="from-date"
              type="date"
              value={fromDate}
              onChange={e => setFromDate(e.target.value)}
              max={toDate || undefined}
              className="h-9 text-sm"
            />
          </div>
          <div className="flex-1 space-y-1">
            <Label htmlFor="to-date" className="text-xs text-muted-foreground">To</Label>
            <Input
              id="to-date"
              type="date"
              value={toDate}
              onChange={e => setToDate(e.target.value)}
              min={fromDate || undefined}
              className="h-9 text-sm"
            />
          </div>
          {hasDateFilter && (
            <div className="flex items-end">
              <Button
                variant="ghost" size="sm"
                onClick={() => { setFromDate(""); setToDate(""); }}
                className="h-9 text-muted-foreground hover:text-foreground gap-1"
                aria-label="Clear dates"
              >
                <X className="h-3.5 w-3.5" />Clear
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Period summary — shown when any filter is active */}
      {hasAnyFilter && (
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          {[
            { label: "Filtered",   value: String(filtered.length),      cls: "" },
            { label: "Completed",  value: String(completedCount),        cls: "text-success" },
            { label: "Failed",     value: String(failedCount),           cls: "text-destructive" },
            { label: "Revenue",    value: formatCurrency(totalRevenue),  cls: "text-primary" },
            { label: "Profit",     value: formatCurrency(totalProfit),   cls: "text-success" },
          ].map(({ label, value, cls }) => (
            <Card key={label} className="hover:shadow-sm transition-shadow">
              <CardContent className="p-3 text-center">
                <p className="text-xs text-muted-foreground">{label}</p>
                <p className={`text-lg font-bold truncate ${cls}`}>{value}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Active filter badges + count */}
      <div className="flex items-center gap-2 flex-wrap">
        <span className="text-sm text-muted-foreground">{filtered.length} order{filtered.length !== 1 ? "s" : ""}</span>
        {hasDateFilter && (
          <Badge variant="outline" className="text-xs gap-1">
            <Calendar className="h-3 w-3" />
            {fromDate && toDate ? `${fromDate} → ${toDate}` : fromDate ? `From ${fromDate}` : `Until ${toDate}`}
          </Badge>
        )}
        {filterStatus  !== "ALL" && <Badge variant="outline" className="text-xs">{filterStatus.replace(/_/g, " ")}</Badge>}
        {filterNetwork !== "ALL" && <Badge variant="outline" className="text-xs">{filterNetwork.toUpperCase()}</Badge>}
      </div>

      {/* Table */}
      <Card>
        <CardContent className="p-0 overflow-x-auto">
          {isLoading ? (
            <div className="p-8 text-center text-muted-foreground text-sm">Loading orders...</div>
          ) : filtered.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground text-sm">
              No orders found for the selected filters.
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/30">
                  <th className="text-left p-4 font-medium text-muted-foreground">Order</th>
                  <th className="text-left p-4 font-medium text-muted-foreground">Phone</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden sm:table-cell">Bundle</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden md:table-cell">Network</th>
                  <th className="text-left p-4 font-medium text-muted-foreground">Charged</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden md:table-cell">Cost</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden md:table-cell">Commission</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden md:table-cell">Profit</th>
                  <th className="text-left p-4 font-medium text-muted-foreground">Status</th>
                  <th className="text-left p-4 font-medium text-muted-foreground hidden lg:table-cell">Date</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(order => (
                  <tr key={order.id} className="border-b last:border-0 hover:bg-muted/20 transition-colors">
                    <td className="p-4">
                      <p className="font-mono text-xs text-muted-foreground">…{order.id.slice(-8)}</p>
                      {order.agentId && <span className="text-[10px] text-primary font-medium">Agent</span>}
                    </td>
                    <td className="p-4 font-medium">{order.phoneNumber}</td>
                    <td className="p-4 hidden sm:table-cell text-muted-foreground">{order.bundleCode}</td>
                    <td className="p-4 hidden md:table-cell">
                      <span className="uppercase text-xs font-semibold">{order.network}</span>
                    </td>
                    <td className="p-4 font-semibold">{formatCurrency(order.amount)}</td>
                    <td className="p-4 hidden md:table-cell text-muted-foreground text-sm">{formatCurrency(order.costPrice)}</td>
                    <td className="p-4 hidden md:table-cell text-muted-foreground text-sm">
                      {order.commissionAmount > 0 ? formatCurrency(order.commissionAmount) : <span className="text-muted-foreground/50">—</span>}
                    </td>
                    <td className="p-4 hidden md:table-cell">
                      <span className={`font-semibold text-sm ${order.platformProfit > 0 ? 'text-success' : order.platformProfit < 0 ? 'text-destructive' : 'text-muted-foreground'}`}>
                        {formatCurrency(order.platformProfit)}
                      </span>
                    </td>
                    <td className="p-4"><StatusBadge status={order.status} /></td>
                    <td className="p-4 text-xs text-muted-foreground hidden lg:table-cell">{formatDate(order.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

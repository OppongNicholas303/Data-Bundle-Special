import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw, Check, X, RotateCcw, SearchCheck } from "lucide-react";
import { toast } from "sonner";
import { adminService } from "@/lib/adminService";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { SmsCampaign, SmsSenderIdRequest } from "@/types";

export default function SmsOperationsPage() {
  const qc = useQueryClient();
  const senderQuery = useQuery({ queryKey: ["sms-sender-requests"], queryFn: adminService.getSmsSenderIdRequests });
  const campaignQuery = useQuery({ queryKey: ["sms-admin-campaigns"], queryFn: adminService.getSmsCampaigns });
  const settingsQuery = useQuery({ queryKey: ["sms-operations-settings"], queryFn: adminService.getSmsOperationsSettings });
  const refresh = () => { void senderQuery.refetch(); void campaignQuery.refetch(); void settingsQuery.refetch(); };
  const update = useMutation({
    mutationFn: (task: () => Promise<unknown>) => task(),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ["sms-sender-requests"] }); void qc.invalidateQueries({ queryKey: ["sms-admin-campaigns"] }); void qc.invalidateQueries({ queryKey: ["sms-operations-settings"] }); toast.success("SMS operation updated"); },
    onError: (error: Error) => toast.error(error.message || "SMS action failed"),
  });
  const senderIds = senderQuery.data ?? [];
  const campaigns = campaignQuery.data ?? [];
  const pending = senderIds.filter((item: SmsSenderIdRequest) => item.status === "PENDING");
  const senderActions = senderIds.filter((item: SmsSenderIdRequest) => ["PENDING", "ADMIN_APPROVED"].includes(item.status));
  const attention = campaigns.filter((item: SmsCampaign) => ["FAILED_NEEDS_ADMIN_ACTION", "UNKNOWN", "PARTIALLY_DELIVERED"].includes(item.status));
  const busy = senderQuery.isFetching || campaignQuery.isFetching || settingsQuery.isFetching || update.isPending;
  const settings = settingsQuery.data;

  return <div className="space-y-6 p-6">
    <div className="flex items-center justify-between gap-4">
      <div><h1 className="text-2xl font-bold">SMS Operations</h1><p className="text-sm text-muted-foreground">Review sender IDs and recover user campaigns after provider issues.</p></div>
      <Button variant="outline" onClick={refresh} disabled={busy}><RefreshCw className="mr-2 h-4 w-4"/>Refresh</Button>
    </div>
    <div className="grid gap-4 md:grid-cols-3">
      <Card><CardContent className="p-4"><p className="text-sm text-muted-foreground">Pending sender IDs</p><p className="text-2xl font-bold">{pending.length}</p></CardContent></Card>
      <Card><CardContent className="p-4"><p className="text-sm text-muted-foreground">Campaigns needing review</p><p className="text-2xl font-bold">{attention.length}</p></CardContent></Card>
      <Card><CardContent className="space-y-2 p-4"><p className="text-sm text-muted-foreground">Moolre bundle / sending gate</p><p className="text-sm">Bundle ready: <Badge variant="outline">{settings?.providerBundleReady ? "YES" : "NO"}</Badge> · Sending: <Badge variant="outline">{settings?.sendingEnabled ? "ON" : "PAUSED"}</Badge></p><p className="text-xs text-muted-foreground">{settings?.providerBundleBalanceNote || "After topping up in Moolre, mark the provider bundle ready here."}</p><div className="flex flex-wrap gap-2"><Button size="sm" variant="outline" disabled={busy} onClick={() => update.mutate(() => adminService.updateSmsOperationsSettings({ providerBundleReady: !settings?.providerBundleReady }))}>{settings?.providerBundleReady ? "Mark bundle not ready" : "Confirm bundle topped up"}</Button><Button size="sm" variant="outline" disabled={busy} onClick={() => { const note = prompt("Provider bundle balance / top-up note:", settings?.providerBundleBalanceNote || ""); if (note !== null) update.mutate(() => adminService.updateSmsOperationsSettings({ providerBundleBalanceNote: note })); }}>Update bundle note</Button><Button size="sm" disabled={busy || !settings?.providerBundleReady} onClick={() => update.mutate(() => adminService.updateSmsOperationsSettings({ sendingEnabled: !settings?.sendingEnabled }))}>{settings?.sendingEnabled ? "Pause sending" : "Enable sending"}</Button></div></CardContent></Card>
    </div>

    <Card><CardHeader><CardTitle>Sender ID requests</CardTitle></CardHeader><CardContent className="space-y-3">
      <p className="text-sm text-muted-foreground">Approve the request here, create and verify the Sender ID manually in the provider portal, then activate it for the user.</p>
      {senderQuery.isLoading ? <p>Loading sender IDs…</p> : senderActions.length === 0 ? <p className="text-sm text-muted-foreground">No sender ID requests need review.</p> : senderActions.map((item: SmsSenderIdRequest) => <div key={item.id} className="flex flex-wrap items-center justify-between gap-3 rounded-md border p-3">
        <div><p className="font-semibold">{item.senderId}</p><p className="text-xs text-muted-foreground">User {item.userId} · submitted {new Date(item.createdAt).toLocaleString()}</p></div>
        <div className="flex gap-2">
          {item.status === "PENDING" && <Button size="sm" disabled={busy} onClick={() => update.mutate(() => adminService.reviewSmsSenderId(item.id, "ADMIN_APPROVED"))}><Check className="mr-1 h-4 w-4"/>Approve request</Button>}
          {item.status === "ADMIN_APPROVED" && <>
            <Button size="sm" disabled={busy} onClick={() => { if (confirm("Confirm that you manually verified this Sender ID is approved in the provider portal? It will become available to the user.")) update.mutate(() => adminService.reviewSmsSenderId(item.id, "APPROVED")); }}><Check className="mr-1 h-4 w-4"/>Verify & activate</Button>
          </>}
          <Button size="sm" variant="destructive" disabled={busy} onClick={() => update.mutate(() => adminService.reviewSmsSenderId(item.id, "REJECTED"))}><X className="mr-1 h-4 w-4"/>Reject</Button>
        </div>
      </div>)}
      <p className="text-xs text-muted-foreground">Only activate a Sender ID after confirming its approval manually in the provider portal.</p>
    </CardContent></Card>

    <details className="rounded-lg border p-4"><summary className="cursor-pointer font-semibold">Sender ID history ({senderIds.length})</summary><div className="mt-3 space-y-2">{senderIds.filter((item: SmsSenderIdRequest) => !["PENDING", "ADMIN_APPROVED"].includes(item.status)).map((item: SmsSenderIdRequest) => <div key={item.id} className="flex flex-wrap justify-between gap-2 border-b py-2 text-sm"><span>{item.senderId} · user {item.userId}</span><span>App: {item.status}{item.reviewNote ? ` · ${item.reviewNote}` : ""}</span></div>)}</div></details>

    <Card><CardHeader><CardTitle>Campaigns</CardTitle></CardHeader><CardContent className="space-y-3">
      {campaignQuery.isLoading ? <p>Loading campaigns…</p> : campaigns.length === 0 ? <p className="text-sm text-muted-foreground">No SMS campaigns yet.</p> : campaigns.map((campaign: SmsCampaign) => <div key={campaign.id} className="space-y-3 rounded-md border p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div><div className="flex items-center gap-2"><p className="font-semibold">{campaign.senderId} · {campaign.recipients?.length ?? 0} recipients</p><Badge variant={campaign.status === "ACCEPTED" ? "default" : "outline"}>{campaign.status}</Badge></div>
            <p className="text-xs text-muted-foreground">Campaign {campaign.id} · user {campaign.userId} · {new Date(campaign.createdAt).toLocaleString()}</p>
            <p className="text-sm">Credits held: {campaign.creditsReserved}</p>
            {campaign.failureCode && <p className="text-sm text-destructive">{campaign.failureCode}: {campaign.failureReason}</p>}
            {campaign.providerResponseMessage && <p className="text-xs text-muted-foreground">{campaign.providerResponseMessage}</p>}
          </div>
          <div className="flex flex-wrap gap-2">
            {["PENDING_CREDIT_RESERVATION", "RESERVING_CREDITS"].includes(campaign.status) && <Button size="sm" variant="outline" disabled={busy} onClick={() => update.mutate(() => adminService.recoverSmsReservation(campaign.id))}>Recover reservation</Button>}
            {campaign.status === "FAILED_NEEDS_ADMIN_ACTION" && <>
              <Button size="sm" disabled={busy} onClick={() => { if (confirm("Confirm the Moolre SMS bundle has been topped up. Retry this rejected campaign?")) update.mutate(() => adminService.retrySmsCampaign(campaign.id)); }}><RotateCcw className="mr-1 h-4 w-4"/>Retry after top-up</Button>
              <Button size="sm" variant="destructive" disabled={busy} onClick={() => { const reason = prompt("Reason for canceling and returning held credits:"); if (reason?.trim()) update.mutate(() => adminService.cancelSmsCampaign(campaign.id, reason.trim())); }}>Cancel and release credits</Button>
            </>}
            {["UNKNOWN", "ACCEPTED", "PARTIALLY_DELIVERED", "SUBMITTING", "RECONCILING"].includes(campaign.status) && <Button size="sm" variant="outline" disabled={busy} onClick={() => update.mutate(() => adminService.reconcileSmsCampaign(campaign.id))}><SearchCheck className="mr-1 h-4 w-4"/>Check Moolre status</Button>}
          </div>
        </div>
        {campaign.deliveryDetails?.length ? <details><summary className="cursor-pointer text-sm">Recipient statuses ({campaign.deliveryDetails.length})</summary><div className="mt-2 max-h-56 space-y-1 overflow-auto">{campaign.deliveryDetails.map(item => <div key={item.reference} className="flex flex-wrap items-center justify-between gap-2 border-b py-1 text-xs"><span>{item.recipient} · {item.reference}</span><span className="flex items-center gap-2">{item.status}{(["ACCEPTED", "UNKNOWN"].includes(item.status) || item.status.startsWith("PROVIDER_STATUS_")) && <><Button size="sm" variant="ghost" disabled={busy} onClick={() => { if (confirm(`Confirm Moolre shows ${item.recipient} as delivered?`)) update.mutate(() => adminService.resolveSmsRecipient(campaign.id, item.reference, "DELIVERED")); }}>Delivered</Button><Button size="sm" variant="ghost" disabled={busy} onClick={() => { if (confirm(`Confirm Moolre shows ${item.recipient} was not delivered?`)) update.mutate(() => adminService.resolveSmsRecipient(campaign.id, item.reference, "FAILED")); }}>Failed</Button></>}</span></div>)}</div></details> : null}
      </div>)}
    </CardContent></Card>
  </div>;
}

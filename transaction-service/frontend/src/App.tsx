import { useEffect, useState } from 'react'
import { Link, Navigate, NavLink, Outlet, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from './auth'

const navigation = [
  { label: 'Dashboard', path: '/', icon: GridIcon },
  { label: 'Analytics', path: '/analytics', icon: ChartIcon },
  { label: 'Transactions', path: '/transactions', icon: ReceiptIcon },
  { label: 'Create transaction', path: '/transactions/create', icon: PlusIcon },
]

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
      <Route element={<AppShell />}>
        <Route index element={<Dashboard />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="transactions" element={<Transactions />} />
        <Route path="transactions/create" element={<CreateTransaction />} />
        <Route path="transactions/:id" element={<TransactionDetail />} />
      </Route>
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}

function ProtectedRoute() {
  const { token } = useAuth()
  return token ? <Outlet /> : <NavigateToLogin />
}

function NavigateToLogin() { return <Navigate to="/login" replace /> }

function Login() {
  const { token, signIn, signInDemo, signUp } = useAuth()
  const [signup, setSignup] = useState(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  useEffect(() => { if (token) navigate('/', { replace: true }) }, [navigate, token])
  async function submit(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); setError(null); setLoading(true); try { if (signup) await signUp(name.trim(), email.trim(), password, confirmPassword); else await signIn(username.trim(), password); navigate('/', { replace: true }) } catch (requestError) { setError(requestError instanceof Error ? requestError.message : 'Could not authenticate.') } finally { setLoading(false) } }
  async function exploreDemo() { setError(null); setLoading(true); try { await signInDemo(); navigate('/', { replace: true }) } catch (requestError) { setError(requestError instanceof Error ? requestError.message : 'Could not open the demo.') } finally { setLoading(false) } }
  return <div className="flex min-h-screen items-center justify-center bg-slate-950 px-5"><div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-2xl"><div className="mb-8 flex items-center gap-3"><div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600 text-white"><PulseIcon /></div><div><p className="text-lg font-bold">PulseOps</p><p className="text-xs text-slate-400">Secure operations workspace</p></div></div><h1 className="text-2xl font-bold tracking-tight">{signup ? 'Create account' : 'Sign in'}</h1><p className="mt-2 text-sm leading-6 text-slate-500">{signup ? 'Create a private operations workspace.' : 'Use the public demo or your registered email.'}</p><form className="mt-6 space-y-4" onSubmit={submit}>{signup ? <><label className="block text-sm font-medium text-slate-700">Name<input className="mt-2 w-full rounded-xl border border-slate-200 px-3 py-3" required value={name} onChange={(event) => setName(event.target.value)} /></label><label className="block text-sm font-medium text-slate-700">Email<input type="email" className="mt-2 w-full rounded-xl border border-slate-200 px-3 py-3" required value={email} onChange={(event) => setEmail(event.target.value)} /></label></> : <label className="block text-sm font-medium text-slate-700">Username or email<input className="mt-2 w-full rounded-xl border border-slate-200 px-3 py-3" required value={username} onChange={(event) => setUsername(event.target.value)} /></label>}<label className="block text-sm font-medium text-slate-700">Password<input type="password" minLength={8} className="mt-2 w-full rounded-xl border border-slate-200 px-3 py-3" required value={password} onChange={(event) => setPassword(event.target.value)} /></label>{signup && <label className="block text-sm font-medium text-slate-700">Confirm password<input type="password" minLength={8} className="mt-2 w-full rounded-xl border border-slate-200 px-3 py-3" required value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} /></label>}{error && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}<button disabled={loading} className="w-full rounded-xl bg-indigo-600 px-4 py-3 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-60">{loading ? 'Please wait…' : signup ? 'Create account' : 'Sign in'}</button></form>{!signup && <><div className="my-5 flex items-center gap-3 text-xs text-slate-400"><span className="h-px flex-1 bg-slate-200" />or<span className="h-px flex-1 bg-slate-200" /></div><button disabled={loading} className="w-full rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-3 text-sm font-semibold text-indigo-700 hover:bg-indigo-100 disabled:opacity-60" onClick={exploreDemo}>Explore demo</button></>}<button className="mt-4 w-full text-sm font-semibold text-indigo-600" onClick={() => { setSignup(!signup); setError(null) }}>{signup ? 'Already have an account? Sign in' : 'Need an account? Sign up'}</button></div></div>
}

function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const { signOut } = useAuth()
  const location = useLocation()
  const currentPage = navigation.find((item) => item.path === location.pathname)?.label ?? (location.pathname.startsWith('/transactions/') ? 'Transaction details' : 'Dashboard')

  return (
    <div className="app-shell min-h-screen text-slate-900 antialiased">
      {sidebarOpen && <button aria-label="Close menu" className="fixed inset-0 z-30 bg-slate-950/40 lg:hidden" onClick={() => setSidebarOpen(false)} />}
      <aside aria-label="Primary navigation" className={`app-sidebar fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-slate-200/80 px-5 py-6 transition-transform lg:translate-x-0 ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
        <div className="flex items-center gap-3 px-2">
          <div className="brand-mark flex h-10 w-10 items-center justify-center rounded-xl text-white"><PulseIcon /></div>
          <div><p className="text-lg font-bold tracking-tight text-white">PulseOps</p><p className="text-xs text-indigo-200/70">Transaction platform</p></div>
        </div>
        <nav className="mt-12 space-y-2" aria-label="Primary navigation">
          <p className="mb-4 px-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-indigo-200/60">Workspace</p>
          {navigation.map(({ label, path, icon: Icon }) => (
            <NavLink key={path} to={path} end={path === '/'} onClick={() => setSidebarOpen(false)} className={({ isActive }) => `flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-medium transition ${isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-500 hover:bg-slate-50 hover:text-slate-900'}`}>
              <Icon /><span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto rounded-2xl bg-slate-900 p-4 text-white">
          <div className="mb-3 flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500"><SparkIcon /></div>
          <p className="text-sm font-semibold">Ready to move money?</p>
          <p className="mt-1 text-xs leading-5 text-slate-400">Create and monitor transactions from one place.</p>
        </div>
      </aside>

      <div className="lg:pl-72">
        <header className="app-header sticky top-0 z-20 flex h-20 items-center justify-between border-b border-slate-200/80 px-5 md:px-10">
          <div className="flex items-center gap-4"><button className="rounded-lg p-2 text-slate-500 hover:bg-white lg:hidden" onClick={() => setSidebarOpen(true)} aria-label="Open menu" aria-expanded={sidebarOpen}><MenuIcon /></button><div><p className="text-xs font-medium text-slate-400">Workspace /</p><h1 className="text-xl font-bold tracking-tight">{currentPage}</h1></div></div>
          <div className="flex items-center gap-3"><button className="rounded-xl border border-slate-200 bg-white p-2.5 text-slate-500 hover:text-slate-900" aria-label="Notifications"><BellIcon /></button><div className="hidden h-9 w-px bg-slate-200 sm:block" /><div className="flex items-center gap-3"><div className="flex h-9 w-9 items-center justify-center rounded-full bg-indigo-100 text-sm font-bold text-indigo-700">DO</div><div className="hidden sm:block"><p className="text-sm font-semibold">Demo Operator</p><p className="text-xs text-slate-400">Operations</p></div><button onClick={signOut} className="text-xs font-semibold text-slate-500 hover:text-slate-900">Sign out</button></div></div>
        </header>
        <main className="px-4 py-7 sm:px-6 md:px-10 md:py-9"><Outlet /></main>
      </div>
    </div>
  )
}

function Dashboard() {
  const { token } = useAuth()
  const [counts, setCounts] = useState({ total: 0, completed: 0, processing: 0, failed: 0 })
  const [distribution, setDistribution] = useState<Record<string, number>>({})
  const [activity, setActivity] = useState<ActivityPoint[]>([])
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([])
  const [failedTransactions, setFailedTransactions] = useState<Transaction[]>([])
  const [events, setEvents] = useState<(StatusHistory & { transactionId: string })[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)
    const headers: HeadersInit = token ? { Authorization: `Bearer ${token}` } : {}
    Promise.all([
      fetch('/api/v1/dashboard/metrics', { headers, signal: controller.signal }),
      fetch('/api/v1/transactions?page=0&size=6', { headers, signal: controller.signal }),
      fetch('/api/v1/transactions?page=0&size=5&status=FAILED', { headers, signal: controller.signal }),
    ]).then(async ([metricsResponse, transactionsResponse, failuresResponse]) => {
        if (!metricsResponse.ok || !transactionsResponse.ok || !failuresResponse.ok) throw new Error(metricsResponse.status === 401 ? 'Sign in to view dashboard data.' : 'Could not load dashboard data.')
        return [
          await metricsResponse.json() as { total: number; statusCounts: Record<string, number>; activity: ActivityPoint[] },
          await transactionsResponse.json() as TransactionPage,
          await failuresResponse.json() as TransactionPage,
        ] as const
      })
      .then(async ([metrics, transactionPage, failurePage]) => {
        setCounts({ total: metrics.total, completed: metrics.statusCounts.COMPLETED ?? 0, processing: metrics.statusCounts.PROCESSING ?? 0, failed: metrics.statusCounts.FAILED ?? 0 })
        setDistribution(metrics.statusCounts)
        setActivity(metrics.activity)
        setRecentTransactions(transactionPage.content)
        setFailedTransactions(failurePage.content)
        const histories = await Promise.all(transactionPage.content.map(async (transaction) => {
          const response = await fetch(`/api/v1/transactions/${transaction.id}/status`, { headers, signal: controller.signal })
          if (!response.ok) return []
          return (await response.json() as StatusHistory[]).map((event) => ({ ...event, transactionId: transaction.id }))
        }))
        setEvents(histories.flat().sort((a, b) => new Date(b.changedAt).getTime() - new Date(a.changedAt).getTime()).slice(0, 8))
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return
        setError(userFriendlyError(requestError, 'Could not load dashboard data.'))
      })
      .finally(() => setLoading(false))

    return () => controller.abort()
  }, [retryKey])

  return <PageIntro eyebrow="Operations control room" title="Transaction command center" description="Observe the full event-driven flow from intake to completion, with every metric sourced from the transaction service." action={{ label: 'Create transaction', to: '/transactions/create' }}>
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{loading ? <>{['Total transactions', 'Completed', 'Processing', 'Failed'].map((label) => <MetricSkeleton key={label} label={label} />)}</> : error ? <RetryPanel message={error} onRetry={() => setRetryKey((value) => value + 1)} /> : <><MetricCard label="Total volume" value={counts.total.toLocaleString()} detail="Persisted transaction records" /><MetricCard label="Completed" value={counts.completed.toLocaleString()} detail="Successfully processed" tone="emerald" /><MetricCard label="In flight" value={counts.processing.toLocaleString()} detail="Currently processing" tone="violet" /><MetricCard label="Needs attention" value={counts.failed.toLocaleString()} detail="Failed transactions" tone="rose" /></>}</div>
    {!loading && !error && <><Pipeline distribution={distribution} /><div className="mt-5 grid gap-5 xl:grid-cols-[1.1fr_.9fr]"><StatusDistribution distribution={distribution} /><ProcessingActivity activity={activity} /></div><div className="mt-5 grid gap-5 xl:grid-cols-[1.25fr_.75fr]"><RecentTransactions transactions={recentTransactions} /><EventFeed events={events} /></div><FailureOperations transactions={failedTransactions} /></>}
    <SystemHealth />
  </PageIntro>
}

type AnalyticsData = {
  range: string
  totalTransactions: number
  successfulTransactions: number
  failedTransactions: number
  successRate: number
  failureRate: number
  averageProcessingLatencyMs: number
  p95ProcessingLatencyMs: number
  recentFailureCount: number
  volume: AnalyticsBucket[]
  throughput: AnalyticsBucket[]
  currencyDistribution: Record<string, number>
  statusDistribution: Record<string, number>
}
type AnalyticsBucket = { bucket: string; count: number }

function Analytics() {
  const { token } = useAuth()
  const [range, setRange] = useState('7d')
  const [data, setData] = useState<AnalyticsData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)
    fetch(`/api/v1/analytics?range=${range}`, { headers: token ? { Authorization: `Bearer ${token}` } : {}, signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) throw new Error(response.status === 401 ? 'Sign in to view analytics.' : 'Could not load analytics.')
        return response.json() as Promise<AnalyticsData>
      })
      .then(setData)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return
        setError(userFriendlyError(requestError, 'Could not load analytics.'))
      })
      .finally(() => setLoading(false))
    return () => controller.abort()
  }, [range, retryKey, token])

  return <PageIntro eyebrow="Insights" title="Operational analytics" description="Understand volume, reliability, and processing performance from persisted transaction history.">
    <div className="mb-5 flex justify-end"><label className="flex items-center gap-3 text-sm font-semibold text-slate-600">Time range<select className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10" value={range} onChange={(event) => setRange(event.target.value)}><option value="24h">Last 24 hours</option><option value="7d">Last 7 days</option><option value="30d">Last 30 days</option></select></label></div>
    {loading ? <AnalyticsSkeleton /> : error ? <RetryPanel message={error} onRetry={() => setRetryKey((value) => value + 1)} /> : data && data.totalTransactions === 0 ? <EmptyPanel title="No analytics for this period" description="Analytics will appear here after transactions are created and processed." /> : data ? <div className="space-y-5"><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><MetricCard label="Transactions" value={data.totalTransactions.toLocaleString()} detail="Created in selected period" /><MetricCard label="Success rate" value={`${data.successRate.toFixed(1)}%`} detail={`${data.successfulTransactions.toLocaleString()} completed`} tone="emerald" /><MetricCard label="Failure rate" value={`${data.failureRate.toFixed(1)}%`} detail={`${data.failedTransactions.toLocaleString()} failed`} tone="rose" /><MetricCard label="P95 latency" value={formatLatency(data.p95ProcessingLatencyMs)} detail={`Average ${formatLatency(data.averageProcessingLatencyMs)}`} tone="violet" /></div><div className="grid gap-5 xl:grid-cols-2"><AnalyticsChart title="Transaction volume" description="Created transactions by time bucket" buckets={data.volume} color="bg-indigo-500" /><AnalyticsChart title="Throughput" description="Completed transactions by time bucket" buckets={data.throughput} color="bg-emerald-500" /></div><div className="grid gap-5 xl:grid-cols-3"><DistributionPanel title="Currency distribution" values={data.currencyDistribution} /><DistributionPanel title="Status distribution" values={data.statusDistribution} /><div className="fintech-card p-6"><p className="section-kicker">Reliability</p><h2 className="mt-1 font-semibold text-slate-950">Recent failures</h2><p className="mt-5 text-4xl font-bold tracking-tight text-rose-600">{data.recentFailureCount.toLocaleString()}</p><p className="mt-2 text-sm text-slate-500">Failed transactions recorded in the selected period.</p></div></div><section className="fintech-card p-6"><div className="flex flex-wrap items-end justify-between gap-3"><div><p className="section-kicker">Processing performance</p><h2 className="mt-1 font-semibold text-slate-950">Latency summary</h2></div><span className="text-xs text-slate-400">Completed transactions only</span></div><div className="mt-6 grid gap-4 sm:grid-cols-2"><LatencyMetric label="Average processing latency" value={formatLatency(data.averageProcessingLatencyMs)} /><LatencyMetric label="P95 processing latency" value={formatLatency(data.p95ProcessingLatencyMs)} /></div></section></div> : null}
  </PageIntro>
}

function AnalyticsChart({ title, description, buckets, color }: { title: string; description: string; buckets: AnalyticsBucket[]; color: string }) {
  const max = Math.max(...buckets.map((bucket) => bucket.count), 0)
  return <section className="fintech-card p-6"><h2 className="font-semibold text-slate-950">{title}</h2><p className="mt-1 text-xs text-slate-400">{description}</p>{buckets.length === 0 ? <ChartEmpty message="No data in this period." /> : <div className="chart-grid mt-7 flex h-48 items-end gap-1.5 rounded-lg px-2 pt-3 sm:gap-3">{buckets.map((bucket) => <div key={bucket.bucket} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-2"><span className="text-[10px] font-semibold text-slate-500">{bucket.count || ''}</span><div className={`w-full rounded-t-md ${color} transition-all hover:opacity-80`} style={{ height: `${Math.max((bucket.count / max) * 100, bucket.count ? 8 : 2)}%` }} title={`${bucket.count} at ${formatDate(bucket.bucket)}`} /><span className="max-w-full truncate text-[10px] font-medium text-slate-400">{formatBucket(bucket.bucket)}</span></div>)}</div>}</section>
}

function DistributionPanel({ title, values }: { title: string; values: Record<string, number> }) { const entries = Object.entries(values); const max = Math.max(...entries.map(([, value]) => value), 0); return <section className="fintech-card p-6"><h2 className="font-semibold text-slate-950">{title}</h2>{entries.length === 0 ? <ChartEmpty message="No data in this period." /> : <div className="mt-6 space-y-4">{entries.map(([label, value]) => <div key={label}><div className="mb-1.5 flex justify-between text-xs"><span className="font-medium text-slate-600">{label}</span><span className="text-slate-400">{value.toLocaleString()}</span></div><div className="h-2 rounded-full bg-slate-100"><div className="h-2 rounded-full bg-indigo-500" style={{ width: `${Math.max((value / max) * 100, 4)}%` }} /></div></div>)}</div>}</section> }
function LatencyMetric({ label, value }: { label: string; value: string }) { return <div className="rounded-xl border border-slate-100 bg-slate-50 p-4"><p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</p><p className="mt-3 text-2xl font-bold text-slate-950">{value}</p></div> }
function AnalyticsSkeleton() { return <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">{[1, 2, 3, 4].map((item) => <MetricSkeleton key={item} label="Loading analytics" />)}</div> }

function SystemHealth() {
  const [status, setStatus] = useState<'loading' | 'UP' | 'DOWN'>('loading')
  const [components, setComponents] = useState<Record<string, string>>({})
  useEffect(() => {
    fetch('/actuator/health').then(async (response) => {
      if (!response.ok) { setStatus('DOWN'); return }
      const body = await response.json() as { status?: string; components?: Record<string, { status?: string }> }
      setStatus(body.status === 'UP' ? 'UP' : 'DOWN')
      setComponents(Object.fromEntries(Object.entries(body.components ?? {}).map(([key, value]) => [key, value.status ?? 'UNKNOWN'])))
    }).catch(() => setStatus('DOWN'))
  }, [])
  const isUp = status === 'UP'
  const services = [['db', 'PostgreSQL'], ['kafka', 'Kafka'], ['redis', 'Redis']].filter(([key]) => components[key])
  return <section className="fintech-card mt-6 p-5"><div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-xs font-bold uppercase tracking-[.16em] text-slate-400">System health</p><h2 className="mt-1 text-base font-semibold text-slate-950">{status === 'loading' ? 'Checking service health…' : isUp ? 'Live service status' : 'API service unavailable'}</h2></div><div className="flex flex-wrap gap-2 text-xs font-semibold text-slate-600"><HealthPill label="API" status={status} />{services.map(([key, label]) => <HealthPill key={key} label={label} status={components[key]} />)}</div></div></section>
}

function HealthPill({ label, status }: { label: string; status: string }) { const isUp = status === 'UP'; const isLoading = status === 'loading'; return <span className={`flex items-center gap-2 rounded-full px-3 py-2 ${isUp ? 'bg-emerald-50 text-emerald-700' : isLoading ? 'bg-slate-100 text-slate-500' : 'bg-rose-50 text-rose-700'}`}><i className={`h-2 w-2 rounded-full ${isUp ? 'health-dot bg-emerald-500' : isLoading ? 'animate-pulse bg-slate-400' : 'bg-rose-500'}`} />{label} · {isLoading ? 'Checking' : status}</span> }

function Pipeline({ distribution }: { distribution: Record<string, number> }) {
  const nodes = [['API', 'Request intake', 'api'], ['PostgreSQL', 'Source of truth', 'database'], ['Kafka', 'transaction.created', 'event'], ['Consumer', 'Status transitions', 'consumer'], ['Redis', 'Idempotency + limits', 'cache']] as const
  const stages = [['RECEIVED', 'Received', 'bg-blue-500'], ['VALIDATING', 'Validating', 'bg-amber-500'], ['PROCESSING', 'Processing', 'bg-violet-500'], ['COMPLETED', 'Completed', 'bg-emerald-500']] as const
  return <section className="pipeline-panel mt-5 overflow-hidden rounded-2xl p-5 text-white sm:p-6"><div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-end"><div><p className="text-[11px] font-bold uppercase tracking-[.18em] text-indigo-200">Event-driven architecture</p><h2 className="mt-2 text-xl font-semibold tracking-tight">One trace from request to completion</h2></div><p className="text-xs text-indigo-200">Counts are persisted transaction statuses</p></div><div className="architecture-flow mt-7 grid gap-2 sm:grid-cols-5">{nodes.map(([label, description, key], index) => <div key={key} className="relative"><div className="architecture-node h-full rounded-xl border border-white/10 bg-white/[.07] p-3"><div className="flex items-center justify-between"><span className="text-[10px] font-bold uppercase tracking-[.16em] text-indigo-200">0{index + 1}</span><span className="h-2 w-2 rounded-full bg-indigo-300" /></div><p className="mt-4 text-sm font-semibold">{label}</p><p className="mt-1 text-xs leading-4 text-indigo-200">{description}</p></div>{index < nodes.length - 1 && <span className="pipeline-connector hidden sm:block" />}</div>)}</div><div className="mt-5 flex flex-wrap gap-2 border-t border-white/10 pt-4">{stages.map(([key, label, color]) => <span key={key} className="inline-flex items-center gap-2 rounded-full bg-white/[.08] px-3 py-1.5 text-xs text-indigo-100"><i className={`h-1.5 w-1.5 rounded-full ${color}`} />{label} <strong className="text-white">{distribution[key] ?? 0}</strong></span>)}</div></section>
}

function RecentTransactions({ transactions }: { transactions: Transaction[] }) {
  return <section className="fintech-card overflow-hidden"><div className="flex items-start justify-between border-b border-slate-100 px-5 py-5"><div><p className="section-kicker">Latest records</p><h2 className="mt-1 font-semibold text-slate-950">Recent transactions</h2></div><Link className="text-xs font-bold text-indigo-600 hover:text-indigo-800" to="/transactions">View all</Link></div>{transactions.length === 0 ? <ChartEmpty message="No transactions have been recorded." /> : <div className="divide-y divide-slate-100">{transactions.map((transaction) => <Link key={transaction.id} to={`/transactions/${transaction.id}`} className="flex items-center justify-between gap-4 px-5 py-4 transition hover:bg-indigo-50/40"><div className="min-w-0"><p className="truncate font-mono text-xs font-semibold text-slate-700">{transaction.id}</p><p className="mt-1 text-xs text-slate-400">{formatDate(transaction.createdAt)}</p></div><div className="shrink-0 text-right"><p className="text-sm font-bold text-slate-900">{formatAmount(transaction.amount)} <span className="text-xs font-medium text-slate-400">{transaction.currency}</span></p><div className="mt-1"><StatusBadge status={transaction.status} /></div></div></Link>)}</div>}</section>
}

function EventFeed({ events }: { events: (StatusHistory & { transactionId: string })[] }) {
  return <section className="fintech-card overflow-hidden"><div className="border-b border-slate-100 px-5 py-5"><p className="section-kicker">Event stream</p><h2 className="mt-1 font-semibold text-slate-950">Latest status activity</h2></div>{events.length === 0 ? <ChartEmpty message="No status events have been recorded." /> : <div className="divide-y divide-slate-100">{events.map((event) => <Link key={event.id} to={`/transactions/${event.transactionId}`} className="flex gap-3 px-5 py-4 transition hover:bg-indigo-50/40"><span className="event-dot mt-1.5 h-2 w-2 shrink-0 rounded-full bg-indigo-500" /><div className="min-w-0"><p className="text-sm text-slate-700">Transaction <span className="font-mono text-xs font-semibold">{event.transactionId.slice(0, 8)}</span> moved to <strong>{event.status}</strong></p><p className="mt-1 text-xs text-slate-400">{formatDate(event.changedAt)}</p></div></Link>)}</div>}</section>
}

function FailureOperations({ transactions }: { transactions: Transaction[] }) {
  return <section className="failure-panel fintech-card mt-5 overflow-hidden"><div className="flex flex-col justify-between gap-3 border-b border-rose-100 px-5 py-5 sm:flex-row sm:items-center"><div><p className="section-kicker text-rose-600">Failure operations</p><h2 className="mt-1 font-semibold text-slate-950">Exceptions requiring attention</h2><p className="mt-1 text-xs text-slate-500">Persisted failures from the transaction processor.</p></div><Link className="text-xs font-bold text-rose-700 hover:text-rose-900" to="/transactions?status=FAILED">View failed transactions</Link></div>{transactions.length === 0 ? <div className="flex items-center gap-3 px-5 py-6 text-sm text-slate-500"><span className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">✓</span>No failed transactions require attention.</div> : <div className="divide-y divide-rose-50">{transactions.map((transaction) => <Link key={transaction.id} to={`/transactions/${transaction.id}`} className="grid gap-3 px-5 py-4 transition hover:bg-rose-50/40 md:grid-cols-[1.3fr_1.5fr_.7fr_1fr_auto] md:items-center"><span className="font-mono text-xs font-semibold text-slate-700">{transaction.id}</span><span className="truncate text-sm text-slate-600">{transaction.failureReason ?? 'Failure reason unavailable'}</span><span className="text-xs text-slate-500">{transaction.retryCount ?? 0} retries</span><span className="text-xs text-slate-500">{transaction.lastFailureAt ? formatDate(transaction.lastFailureAt) : '—'}</span><StatusBadge status="FAILED" /></Link>)}</div>}</section>
}

type Transaction = {
  id: string
  status: string
  amount: number | string
  currency: string
  createdAt: string
  updatedAt?: string
  failureReason?: string
  retryCount?: number
  lastFailureAt?: string
  traceId?: string
}

type TransactionPage = {
  content: Transaction[]
  totalElements: number
}

type StatusHistory = {
  id: string
  status: string
  changedAt: string
  failureReason?: string
}

type ActivityPoint = { label: string; count: number }

const currencies = [
  ['USD', 'US Dollar'], ['EUR', 'Euro'], ['GBP', 'British Pound'], ['CAD', 'Canadian Dollar'],
  ['AUD', 'Australian Dollar'], ['JPY', 'Japanese Yen'], ['CHF', 'Swiss Franc'], ['INR', 'Indian Rupee'],
] as const

function StatusDistribution({ distribution }: { distribution: Record<string, number> }) {
  const items = [['COMPLETED', 'Completed', 'bg-emerald-500'], ['PROCESSING', 'Processing', 'bg-violet-500'], ['RECEIVED', 'Received', 'bg-blue-500'], ['VALIDATING', 'Validating', 'bg-amber-500'], ['FAILED', 'Failed', 'bg-rose-500']]
  const total = Object.values(distribution).reduce((sum, count) => sum + count, 0)
  return <section className="fintech-card p-6"><div className="flex items-start justify-between"><div><h2 className="font-semibold text-slate-950">Status distribution</h2><p className="mt-1 text-xs text-slate-400">Current transaction status counts</p></div><span className="text-2xl font-bold text-slate-950">{total}</span></div>{total === 0 ? <ChartEmpty message="No status data available yet." /> : <div className="mt-6 space-y-4">{items.map(([key, label, color]) => { const count = distribution[key] ?? 0; return <div key={key}><div className="mb-1.5 flex justify-between text-xs"><span className="font-medium text-slate-600">{label}</span><span className="text-slate-400">{count} · {Math.round((count / total) * 100)}%</span></div><div className="h-2 rounded-full bg-slate-100"><div className={`h-2 rounded-full ${color}`} style={{ width: `${(count / total) * 100}%` }} /></div></div>})}</div>}</section>
}

function ProcessingActivity({ activity }: { activity: ActivityPoint[] }) { const max = Math.max(...activity.map(({ count }) => count), 0); const total = activity.reduce((sum, { count }) => sum + count, 0); return <section className="fintech-card p-6"><div><h2 className="font-semibold text-slate-950">Processing activity</h2><p className="mt-1 text-xs text-slate-400">Transactions created over the last 7 days</p></div>{total === 0 ? <ChartEmpty message="No activity recorded in this period." /> : <div className="chart-grid mt-7 flex h-40 items-end gap-2 rounded-lg px-2 pt-3 sm:gap-4">{activity.map(({ label, count }) => <div key={label} className="flex h-full flex-1 flex-col items-center justify-end gap-2"><span className="text-[10px] font-semibold text-slate-500">{count || ''}</span><div className="w-full rounded-t-md bg-indigo-500 transition-all hover:bg-indigo-600" style={{ height: `${Math.max((count / max) * 100, count ? 8 : 2)}%` }} /><span className="text-[10px] font-medium text-slate-400">{label}</span></div>)}</div>}</section> }
function ChartEmpty({ message }: { message: string }) { return <div className="flex h-40 items-center justify-center text-center text-sm text-slate-400">{message}</div> }

function Transactions() {
  const { token } = useAuth()
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)
    const headers: HeadersInit = token ? { Authorization: `Bearer ${token}` } : {}

    const statusQuery = status ? `&status=${encodeURIComponent(status)}` : ''
    fetch(`/api/v1/transactions?page=${page}&size=20${statusQuery}`, { headers, signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) throw new Error(response.status === 401 ? 'Sign in to view transactions.' : 'Could not load transactions.')
        return response.json() as Promise<TransactionPage>
      })
      .then((data) => {
        setTransactions(data.content)
        setTotal(data.totalElements)
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return
        setError(userFriendlyError(requestError, 'Could not load transactions.'))
      })
      .finally(() => setLoading(false))

    return () => controller.abort()
  }, [page, status, retryKey])

  const visibleTransactions = transactions.filter((transaction) => {
    const query = search.trim().toLowerCase()
    return !query || transaction.id.toLowerCase().includes(query) || transaction.currency.toLowerCase().includes(query) || transaction.status.toLowerCase().includes(query)
  })
  const totalPages = Math.max(1, Math.ceil(total / 20))
  const hasFilters = Boolean(search.trim() || status)

  return <PageIntro eyebrow="Workspace" title="Transactions" description="Review and monitor every transaction in your operation." action={{ label: 'Create transaction', to: '/transactions/create' }}><div className="fintech-card overflow-hidden"><div className="border-b border-slate-100 px-5 py-5"><div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"><div><p className="section-kicker">Transaction ledger</p><h2 className="mt-1 font-semibold text-slate-950">All transactions</h2><p className="mt-1 text-xs text-slate-400">{total} persisted records · page {page + 1} of {totalPages}</p></div><div className="flex flex-col gap-3 sm:flex-row"><label className="relative"><span className="sr-only">Search transactions</span><SearchIcon /><input className="w-full rounded-xl border border-slate-200 bg-slate-50/70 py-2.5 pl-9 pr-3 text-sm outline-none focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 sm:w-72" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search ID, status, currency" /></label><label><span className="sr-only">Filter by status</span><select className="w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3 py-2.5 text-sm outline-none focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 sm:w-44" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0) }}><option value="">All statuses</option><option value="RECEIVED">Received</option><option value="VALIDATING">Validating</option><option value="PROCESSING">Processing</option><option value="COMPLETED">Completed</option><option value="FAILED">Failed</option></select></label>{hasFilters && <button type="button" className="rounded-xl px-3 py-2 text-sm font-semibold text-indigo-600 hover:bg-indigo-50" onClick={() => { setSearch(''); setStatus(''); setPage(0) }}>Clear</button>}</div></div>{hasFilters && <div className="mt-4 flex items-center gap-2 text-xs text-slate-500"><span className="h-2 w-2 rounded-full bg-indigo-500" />Showing {visibleTransactions.length} matching records on this page</div>}</div>{loading ? <TableSkeleton /> : error ? <RetryPanel message={error} onRetry={() => setRetryKey((value) => value + 1)} /> : visibleTransactions.length === 0 ? <EmptyTable message={transactions.length === 0 ? 'No transactions yet.' : 'No matching transactions on this page.'} /> : <div className="overflow-x-auto"><table className="responsive-table data-table min-w-[920px] w-full text-left"><thead className="bg-slate-50/80 text-xs font-semibold uppercase tracking-wider text-slate-400"><tr><th className="px-5 py-3">Transaction</th><th className="px-5 py-3">Status</th><th className="px-5 py-3 text-right">Amount</th><th className="px-5 py-3">Currency</th><th className="px-5 py-3">Created</th><th className="px-5 py-3">Updated</th><th className="px-5 py-3">Failure / retry</th></tr></thead><tbody className="divide-y divide-slate-100">{visibleTransactions.map((transaction) => <tr key={transaction.id} className="text-sm hover:bg-indigo-50/30"><td data-label="Transaction" className="px-5 py-4 font-mono text-xs text-slate-500"><Link className="font-semibold text-indigo-600 hover:underline" to={`/transactions/${transaction.id}`}>{transaction.id}</Link><p className="mt-1 font-sans text-[11px] text-slate-400">Open operations trace →</p></td><td data-label="Status" className="px-5 py-4"><StatusBadge status={transaction.status} /></td><td data-label="Amount" className="px-5 py-4 text-right font-semibold text-slate-900">{formatAmount(transaction.amount)}</td><td data-label="Currency" className="px-5 py-4 text-slate-500">{transaction.currency}</td><td data-label="Created" className="whitespace-nowrap px-5 py-4 text-slate-500">{formatDate(transaction.createdAt)}</td><td data-label="Updated" className="whitespace-nowrap px-5 py-4 text-slate-500">{formatDate(transaction.updatedAt ?? transaction.createdAt)}</td><td data-label="Failure / retry" className="max-w-[220px] px-5 py-4 text-xs text-slate-500">{transaction.status === 'FAILED' ? <><span className="block truncate text-rose-700" title={transaction.failureReason}>{transaction.failureReason ?? 'Failure reason unavailable'}</span><span className="mt-1 block">{transaction.retryCount ?? 0} retries · {transaction.lastFailureAt ? formatDate(transaction.lastFailureAt) : '—'}</span></> : <span className="text-slate-300">—</span>}</td></tr>)}</tbody></table></div>}<div className="flex flex-col gap-3 border-t border-slate-100 px-5 py-4 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between"><span>Showing {visibleTransactions.length} of {total} records</span><div className="flex gap-2"><button className="rounded-lg border border-slate-200 px-3 py-2 font-medium hover:border-indigo-200 hover:bg-indigo-50 hover:text-indigo-700 disabled:cursor-not-allowed disabled:opacity-40" disabled={page === 0 || loading} onClick={() => setPage((current) => current - 1)}>Previous</button><button className="rounded-lg border border-slate-200 px-3 py-2 font-medium hover:border-indigo-200 hover:bg-indigo-50 hover:text-indigo-700 disabled:cursor-not-allowed disabled:opacity-40" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((current) => current + 1)}>Next</button></div></div></div></PageIntro>
}

function TransactionDetail() {
  const { token } = useAuth()
  const { id } = useParams<{ id: string }>()
  const [transaction, setTransaction] = useState<Transaction | null>(null)
  const [history, setHistory] = useState<StatusHistory[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retrying, setRetrying] = useState(false)
  const [retryFeedback, setRetryFeedback] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    const headers: HeadersInit = token ? { Authorization: `Bearer ${token}` } : {}
    if (!id) { setError('Transaction ID is missing.'); setLoading(false); return }

    Promise.all([
      fetch(`/api/v1/transactions/${id}`, { headers, signal: controller.signal }),
      fetch(`/api/v1/transactions/${id}/status`, { headers, signal: controller.signal }),
    ]).then(async ([transactionResponse, historyResponse]) => {
      if (!transactionResponse.ok || !historyResponse.ok) throw new Error(transactionResponse.status === 401 ? 'Sign in to view this transaction.' : 'Could not load transaction details.')
      return [await transactionResponse.json() as Transaction, await historyResponse.json() as StatusHistory[]] as const
    }).then(([transactionData, historyData]) => { setTransaction(transactionData); setHistory(historyData) }).catch((requestError: unknown) => {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') return
      setError(userFriendlyError(requestError, 'Could not load transaction details.'))
    }).finally(() => setLoading(false))

    return () => controller.abort()
  }, [id])

  async function retryTransaction() {
    if (!id) return
    setRetrying(true)
    setRetryFeedback(null)
    try {
      const response = await fetch(`/api/v1/transactions/${id}/retry`, { method: 'POST', headers: token ? { Authorization: `Bearer ${token}` } : {} })
      const body = await response.json().catch(() => null) as { error?: string } | null
      if (!response.ok) throw new Error(body?.error ?? 'Could not retry transaction.')
      setRetryFeedback('Retry accepted. The transaction was returned to the Kafka processing pipeline.')
      setTransaction((current) => current ? { ...current, status: 'RECEIVED', failureReason: undefined, updatedAt: new Date().toISOString(), retryCount: (current.retryCount ?? 0) + 1 } : current)
    } catch (requestError) {
      setRetryFeedback(userFriendlyError(requestError, 'Could not retry transaction.'))
    } finally {
      setRetrying(false)
    }
  }

  useEffect(() => {
    if (!id) return
    const controller = new AbortController()
    fetch(`/api/v1/transactions/${id}/status/stream`, { headers: token ? { Authorization: `Bearer ${token}` } : {}, signal: controller.signal })
      .then(async (response) => {
        if (!response.ok || !response.body) throw new Error('Live status updates are unavailable.')
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        while (!controller.signal.aborted) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const events = buffer.split('\n\n')
          buffer = events.pop() ?? ''
          for (const event of events) {
            const data = event.split('\n').find((line) => line.startsWith('data:'))?.slice(5).trim()
            if (!data) continue
            const update = JSON.parse(data) as StatusHistory
            setHistory((current) => current.some((entry) => entry.id === update.id) ? current : [...current, update])
            setTransaction((current) => current ? { ...current, status: update.status, updatedAt: update.changedAt } : current)
          }
        }
      })
      .catch(() => { /* The detail view remains usable if the stream disconnects. */ })
    return () => controller.abort()
  }, [id])

  if (loading) return <PageIntro eyebrow="Transaction" title="Transaction details" description="Loading transaction details…"><TableMessage>Loading transaction…</TableMessage></PageIntro>
  if (error || !transaction) return <PageIntro eyebrow="Transaction" title="Transaction details" description="Review the transaction record and its processing history."><div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700" role="alert">{error ?? 'Transaction not found.'}</div><Link className="mt-5 inline-block text-sm font-semibold text-indigo-600 hover:underline" to="/transactions">← Back to transactions</Link></PageIntro>

  const orderedHistory = [...history].sort((a, b) => new Date(a.changedAt).getTime() - new Date(b.changedAt).getTime())
  const startedAt = orderedHistory[0]?.changedAt ?? transaction.createdAt
  const finishedAt = orderedHistory.at(-1)?.changedAt ?? transaction.updatedAt ?? transaction.createdAt
  const processingTime = formatDuration(startedAt, finishedAt)
  const hasKafkaProgress = orderedHistory.some((entry) => ['VALIDATING', 'PROCESSING', 'COMPLETED'].includes(entry.status))
  const isDeadLettered = transaction.failureReason?.toLowerCase().includes('exhausted retries') ?? false
  const retryEligible = transaction.status === 'FAILED' && (transaction.retryCount ?? 0) < 3

  return <PageIntro eyebrow="Operations trace" title="Transaction detail" description="Follow this transaction across intake, Kafka processing, and its persisted status lifecycle.">
    <Link className="mb-5 inline-flex items-center text-sm font-semibold text-indigo-600 hover:text-indigo-800" to="/transactions">← Back to transactions</Link>
    <section className="detail-hero overflow-hidden rounded-2xl p-6 text-white shadow-xl shadow-indigo-900/20 sm:p-8"><div className="flex flex-col justify-between gap-6 md:flex-row md:items-end"><div><p className="text-xs font-bold uppercase tracking-[.18em] text-indigo-200">Transaction operations trace</p><div className="mt-4 flex flex-wrap items-center gap-3"><StatusBadge status={transaction.status} /><span className="text-sm text-indigo-100">{processingTime} total processing time</span></div></div><p className="max-w-full break-all font-mono text-xs text-indigo-100">{transaction.id}</p></div></section>
    {retryFeedback && <div role="status" className={`mt-5 rounded-xl border p-4 text-sm ${retryFeedback.startsWith('Retry accepted') ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-rose-200 bg-rose-50 text-rose-700'}`}>{retryFeedback}</div>}
    {transaction.status === 'FAILED' && <section className="failure-panel fintech-card mt-5 p-6"><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start"><div><p className="section-kicker text-rose-600">Failure operations</p><h2 className="mt-1 text-lg font-semibold text-slate-950">Transaction failed</h2><p className="mt-2 max-w-2xl text-sm text-rose-700">{transaction.failureReason ?? 'Failure reason unavailable.'}</p></div><StatusBadge status={isDeadLettered ? 'DEAD_LETTERED' : 'FAILED'} /></div><dl className="mt-6 grid gap-5 sm:grid-cols-3"><DetailItem label="Retry count" value={String(transaction.retryCount ?? 0)} /><DetailItem label="Last failure" value={transaction.lastFailureAt ? formatDate(transaction.lastFailureAt) : '—'} /><DetailItem label="Retry eligibility" value={retryEligible ? 'Eligible' : 'Limit reached'} /></dl>{retryEligible && <button type="button" disabled={retrying} onClick={retryTransaction} className="mt-6 inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-rose-600/20 hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-60">{retrying ? <><Spinner />Retrying…</> : 'Retry transaction'}</button>}</section>}
    <div className="mt-5 grid gap-5 lg:grid-cols-[1.1fr_.9fr]"><section className="fintech-card p-6"><div className="flex items-center justify-between"><div><p className="section-kicker">Record</p><h2 className="mt-1 text-lg font-semibold text-slate-950">Transaction information</h2></div><span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">Persisted record</span></div><dl className="mt-7 grid gap-x-6 gap-y-7 sm:grid-cols-2"><DetailItem label="Amount" value={formatAmount(transaction.amount)} /><DetailItem label="Currency" value={transaction.currency} /><DetailItem label="Trace ID" value={transaction.traceId ?? 'Unavailable'} /><DetailItem label="Created" value={formatDate(transaction.createdAt)} /><DetailItem label="Last updated" value={formatDate(transaction.updatedAt ?? transaction.createdAt)} /><DetailItem label="First event" value={formatDate(startedAt)} /><DetailItem label="Final event" value={formatDate(finishedAt)} /><DetailItem label="Total processing" value={processingTime} /></dl></section><section className="fintech-card p-6"><p className="section-kicker">Event transport</p><h2 className="mt-1 text-lg font-semibold text-slate-950">Kafka processing</h2><div className="mt-6 rounded-xl border border-slate-200 bg-slate-50 p-4"><div className="flex items-center gap-3"><span className={`flex h-9 w-9 items-center justify-center rounded-lg ${isDeadLettered ? 'bg-rose-100 text-rose-700' : hasKafkaProgress ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}><PulseIcon /></span><div><p className="text-sm font-semibold text-slate-900">{isDeadLettered ? 'Dead-lettered after Kafka retries' : hasKafkaProgress ? 'Event consumed and processed' : 'Awaiting event processing'}</p><p className="mt-1 text-xs text-slate-500">Topic <span className="font-mono">transaction.created</span></p></div></div><p className="mt-4 border-t border-slate-200 pt-4 text-xs leading-5 text-slate-500">{isDeadLettered ? 'The backend recorded exhausted Kafka retries for this transaction.' : 'Kafka metadata such as partition and offset is not exposed by the backend for this transaction.'}</p></div></section></div>
    <section className="fintech-card mt-5 p-6"><div className="flex flex-wrap items-end justify-between gap-3"><div><p className="section-kicker">Lifecycle trace</p><h2 className="mt-1 text-lg font-semibold text-slate-950">Status timeline</h2><p className="mt-1 text-sm text-slate-500">Persisted status-history events for this transaction.</p></div><span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">{history.length} events · {processingTime}</span></div>{orderedHistory.length === 0 ? <p className="mt-6 text-sm text-slate-500">No status history recorded.</p> : <ol className="trace-timeline mt-8 grid gap-4 md:grid-cols-5">{orderedHistory.map((entry, index) => <li key={entry.id} className="relative"><div className="flex items-center gap-3 md:block"><span className={`trace-node relative z-10 flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold ${index === orderedHistory.length - 1 ? 'bg-indigo-600 text-white ring-4 ring-indigo-100' : 'bg-emerald-500 text-white ring-4 ring-emerald-50'}`}>{index + 1}</span><div className="md:mt-4"><StatusBadge status={entry.status} /><p className="mt-2 text-xs text-slate-400">{formatDate(entry.changedAt)}</p>{index > 0 && <p className="mt-1 text-[11px] font-medium text-slate-400">+{formatDuration(orderedHistory[index - 1].changedAt, entry.changedAt)}</p>}</div></div>{index < orderedHistory.length - 1 && <span className="trace-line" />}</li>)}</ol>}</section>
  </PageIntro>
}

function DetailItem({ label, value }: { label: string; value: string }) { return <div><dt className="text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</dt><dd className="mt-2 text-sm font-semibold text-slate-900">{value}</dd></div> }

function TableMessage({ children }: { children: React.ReactNode }) { return <div className="px-5 py-16 text-center text-sm text-slate-500">{children}</div> }
function MetricSkeleton({ label }: { label: string }) { return <div className="rounded-2xl border border-slate-200 bg-white p-5"><p className="text-sm font-medium text-slate-500">{label}</p><div className="mt-5 h-9 w-20 animate-pulse rounded-lg bg-slate-100" /><div className="mt-3 h-3 w-32 animate-pulse rounded bg-slate-100" /></div> }
function TableSkeleton() { return <div className="space-y-4 px-5 py-6">{[1, 2, 3, 4, 5].map((row) => <div key={row} className="flex animate-pulse gap-5"><div className="h-4 flex-1 rounded bg-slate-100" /><div className="h-4 w-20 rounded bg-slate-100" /><div className="h-4 w-24 rounded bg-slate-100" /><div className="h-4 w-16 rounded bg-slate-100" /></div>)}</div> }
function RetryPanel({ message, onRetry }: { message: string; onRetry: () => void }) { return <div className="sm:col-span-2 xl:col-span-4 rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center" role="alert"><p className="text-sm text-rose-700">{message}</p><button className="mt-4 rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-700" onClick={onRetry}>Retry</button></div> }
function EmptyTable({ message }: { message: string }) { return <div className="px-5 py-16 text-center"><div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-500"><ReceiptIcon /></div><p className="mt-4 text-sm font-semibold text-slate-700">{message}</p><p className="mt-1 text-sm text-slate-400">Try changing your filters or create a new transaction.</p></div> }
function StatusBadge({ status }: { status: string }) { const styles: Record<string, string> = { COMPLETED: 'bg-emerald-50 text-emerald-700', RECEIVED: 'bg-blue-50 text-blue-700', VALIDATING: 'bg-amber-50 text-amber-700', PROCESSING: 'bg-violet-50 text-violet-700', FAILED: 'bg-rose-50 text-rose-700', DEAD_LETTERED: 'bg-slate-200 text-slate-700' }; return <span className={`status-pill inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${styles[status] ?? 'bg-slate-100 text-slate-600'}`}><span className="mr-1.5 mt-1 h-1.5 w-1.5 rounded-full bg-current" />{status}</span> }
function formatAmount(amount: number | string) { return Number(amount).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function formatDate(value: string) { const date = new Date(value); return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) }
function formatDuration(start: string, end: string) { const elapsed = Math.max(0, new Date(end).getTime() - new Date(start).getTime()); if (!Number.isFinite(elapsed)) return '—'; if (elapsed < 1000) return `${elapsed} ms`; const seconds = Math.floor(elapsed / 1000); if (seconds < 60) return `${seconds}.${Math.floor((elapsed % 1000) / 100)} s`; const minutes = Math.floor(seconds / 60); return `${minutes}m ${seconds % 60}s` }
function formatLatency(value: number) { return value < 1000 ? `${Math.round(value)} ms` : `${(value / 1000).toFixed(2)} s` }
function formatBucket(value: string) { const date = new Date(value); return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) }
function userFriendlyError(error: unknown, fallback: string) { return error instanceof TypeError && error.message.toLowerCase().includes('fetch') ? 'We could not reach the transaction service. Check your connection and try again.' : error instanceof Error && error.message ? error.message : fallback }

function NotFound() { return <div className="flex min-h-[70vh] items-center justify-center"><div className="max-w-md text-center"><p className="text-sm font-bold uppercase tracking-[0.2em] text-indigo-600">404</p><h1 className="mt-3 text-4xl font-bold tracking-tight text-slate-950">Page not found</h1><p className="mt-3 text-sm leading-6 text-slate-500">The page you’re looking for doesn’t exist or may have moved.</p><Link className="mt-6 inline-flex rounded-xl bg-indigo-600 px-4 py-3 text-sm font-semibold text-white hover:bg-indigo-700" to="/">Back to dashboard</Link></div></div> }

function CreateTransaction() {
  const { token } = useAuth()
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [description, setDescription] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState<string | null>(null)
  const [createdId, setCreatedId] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function validate() {
    const nextErrors: Record<string, string> = {}
    const numericAmount = Number(amount)
    if (!amount.trim() || !Number.isFinite(numericAmount) || numericAmount <= 0) nextErrors.amount = 'Enter an amount greater than zero.'
    if (!/^[A-Za-z]{3}$/.test(currency)) nextErrors.currency = 'Use a 3-letter currency code.'
    if (description.length > 255) nextErrors.description = 'Description must be 255 characters or fewer.'
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setApiError(null)
    setCreatedId(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      const response = await fetch('/api/v1/transactions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), 'Idempotency-Key': crypto.randomUUID() },
        body: JSON.stringify({ amount: Number(amount), currency: currency.toUpperCase(), description: description.trim() || null }),
      })
      if (!response.ok) {
        const body = await response.json().catch(() => null) as { error?: string; message?: string } | null
        throw new Error(body?.error ?? body?.message ?? (response.status === 401 ? 'Sign in to create transactions.' : response.status === 429 ? 'Rate limit exceeded. Try again shortly.' : 'Could not create transaction.'))
      }
      const transaction = await response.json() as { id: string }
      setCreatedId(transaction.id)
      setAmount('')
      setDescription('')
      setErrors({})
    } catch (requestError: unknown) {
      setApiError(userFriendlyError(requestError, 'Could not create transaction.'))
    } finally {
      setSubmitting(false)
    }
  }

  return <PageIntro eyebrow="Workspace" title="Create transaction" description="Start a new transaction from the secure operations workspace."><div className="fintech-card max-w-3xl p-6 md:p-8"><div className="mb-8 border-b border-slate-100 pb-6"><p className="text-xs font-bold uppercase tracking-[.16em] text-indigo-600">New transaction</p><h2 className="mt-2 text-lg font-semibold text-slate-950">Transaction details</h2><p className="mt-1 text-sm text-slate-500">Enter the amount and settlement currency to begin processing.</p></div>{createdId && <div role="status" className="mb-6 flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"><div className="mt-0.5 font-bold">✓</div><div><p className="font-semibold">Transaction created successfully.</p><p className="mt-1">ID: <span className="font-mono text-xs">{createdId}</span></p><NavLink className="mt-2 inline-block font-semibold underline" to="/transactions">View transactions</NavLink></div></div>}{apiError && <div role="alert" className="mb-6 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{apiError}</div>}<form className="space-y-6" onSubmit={submit} noValidate><div className="grid gap-6 md:grid-cols-2"><Field label="Amount" error={errors.amount}><div className="relative"><span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">$</span><input className={inputClass(Boolean(errors.amount), 'pl-8')} type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="0.00" aria-invalid={Boolean(errors.amount)} /></div></Field><Field label="Currency" error={errors.currency}><CurrencyCombobox value={currency} onChange={setCurrency} hasError={Boolean(errors.currency)} /></Field></div><Field label="Description" hint="Optional"><textarea className="min-h-28 w-full resize-y rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10" value={description} maxLength={255} onChange={(event) => setDescription(event.target.value)} placeholder="Add a note for this transaction" /><p className="mt-1 text-right text-xs text-slate-400">{description.length}/255</p></Field><div className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-6 sm:flex-row sm:justify-end"><button type="button" className="rounded-xl px-4 py-3 text-sm font-semibold text-slate-500 hover:bg-slate-50" onClick={() => { setAmount(''); setCurrency('USD'); setDescription(''); setErrors({}); setApiError(null); setCreatedId(null) }}>Clear</button><button disabled={submitting} className="inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-600/20 transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60" type="submit">{submitting ? <><Spinner />Creating…</> : <><PlusIcon />Create transaction</>}</button></div></form></div></PageIntro>
}

function CurrencyCombobox({ value, onChange, hasError }: { value: string; onChange: (code: string) => void; hasError: boolean }) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [activeIndex, setActiveIndex] = useState(0)
  const selected = currencies.find(([code]) => code === value) ?? currencies[0]
  const filtered = currencies.filter(([code, name]) => `${code} ${name}`.toLowerCase().includes(query.toLowerCase()))
  const select = (code: string) => { onChange(code); setQuery(''); setOpen(false) }
  function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') { event.preventDefault(); setOpen(true); setActiveIndex((index) => Math.min(index + 1, Math.max(filtered.length - 1, 0))) }
    if (event.key === 'ArrowUp') { event.preventDefault(); setActiveIndex((index) => Math.max(index - 1, 0)) }
    if (event.key === 'Enter' && open && filtered[activeIndex]) { event.preventDefault(); select(filtered[activeIndex][0]) }
    if (event.key === 'Escape') { setOpen(false); setQuery('') }
  }
  return <div className="relative"><input className={inputClass(hasError, 'pr-10')} role="combobox" aria-expanded={open} aria-controls="currency-options" aria-autocomplete="list" value={open ? query : `${selected[0]} — ${selected[1]}`} onFocus={() => { setOpen(true); setQuery(''); setActiveIndex(0) }} onChange={(event) => { setQuery(event.target.value.toUpperCase()); setOpen(true); setActiveIndex(0) }} onKeyDown={handleKeyDown} aria-invalid={hasError} placeholder="Search currency" /><span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">⌄</span>{open && <div id="currency-options" role="listbox" className="absolute z-30 mt-2 max-h-60 w-full overflow-auto rounded-xl border border-slate-200 bg-white p-1 shadow-xl shadow-slate-900/10">{filtered.length === 0 ? <p className="px-3 py-3 text-sm text-slate-500">No matching currencies.</p> : filtered.map(([code, name], index) => <button type="button" role="option" aria-selected={code === value} key={code} className={`flex w-full items-center justify-between rounded-lg px-3 py-2.5 text-left text-sm ${index === activeIndex ? 'bg-indigo-50 text-indigo-700' : 'text-slate-700 hover:bg-slate-50'}`} onMouseDown={(event) => event.preventDefault()} onClick={() => select(code)}><span><strong>{code}</strong><span className="ml-2 text-slate-500">— {name}</span></span>{code === value && <span className="text-xs font-bold">✓</span>}</button>)}</div>}</div>
}

function Field({ label, error, hint, children }: { label: string; error?: string; hint?: string; children: React.ReactNode }) { return <label className="block text-sm font-medium text-slate-700"><span>{label}</span>{hint && <span className="ml-2 text-xs font-normal text-slate-400">{hint}</span>}<div className="mt-2">{children}</div>{error && <p className="mt-1 text-xs text-rose-600">{error}</p>}</label> }
function inputClass(hasError: boolean, extra = '') { return `w-full rounded-xl border px-3 py-3 text-sm uppercase outline-none transition placeholder:normal-case placeholder:text-slate-400 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 ${hasError ? 'border-rose-300' : 'border-slate-200'} ${extra}` }
function Spinner() { return <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" aria-hidden="true" /> }

function PageIntro({ eyebrow, title, description, action, children }: { eyebrow: string; title: string; description: string; action?: { label: string; to: string }; children: React.ReactNode }) {
  return <div className="mx-auto max-w-7xl"><div className="mb-9 flex flex-col justify-between gap-6 md:flex-row md:items-end"><div><p className="mb-3 text-[11px] font-bold uppercase tracking-[0.2em] text-indigo-600">{eyebrow}</p><h2 className="page-heading text-[2rem] font-bold leading-tight text-slate-950 md:text-4xl">{title}</h2><p className="mt-3 max-w-2xl text-sm leading-6 text-slate-500">{description}</p></div>{action && <NavLink to={action.to} className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-3 text-sm font-semibold text-white shadow-[0_8px_20px_-8px_rgba(79,70,229,0.8)] transition hover:-translate-y-0.5 hover:bg-indigo-700"><PlusIcon />{action.label}</NavLink>}</div>{children}</div>
}

function MetricCard({ label, value, detail, tone = 'indigo' }: { label: string; value: string; detail: string; tone?: 'indigo' | 'emerald' | 'violet' | 'rose' }) { const accents = { indigo: 'bg-indigo-50 text-indigo-600', emerald: 'bg-emerald-50 text-emerald-600', violet: 'bg-violet-50 text-violet-600', rose: 'bg-rose-50 text-rose-600' }; return <div className="fintech-card kpi-card p-5"><div className="relative z-10 flex items-center justify-between"><p className="text-sm font-medium text-slate-500">{label}</p><span className={`flex h-8 w-8 items-center justify-center rounded-xl ${accents[tone]}`}><span className="h-2 w-2 rounded-full bg-current" /></span></div><p className="relative z-10 mt-5 text-3xl font-bold tracking-[-0.04em] text-slate-950">{value}</p><p className="relative z-10 mt-2 text-xs text-slate-400">{detail}</p></div> }
function EmptyPanel({ title, description }: { title: string; description: string }) { return <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-10 text-center"><div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-500"><PulseIcon /></div><h2 className="mt-4 text-lg font-semibold">{title}</h2><p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">{description}</p></div> }

function Icon({ children }: { children: React.ReactNode }) { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5" aria-hidden="true">{children}</svg> }
function GridIcon() { return <Icon><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></Icon> }
function ChartIcon() { return <Icon><path d="M4 19V5M4 19h16M8 16v-5M12 16V7M16 16v-3M20 16V9" /></Icon> }
function ReceiptIcon() { return <Icon><path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3Z" /><path d="M9 8h6M9 12h6" /></Icon> }
function PlusIcon() { return <Icon><path d="M12 5v14M5 12h14" /></Icon> }
function MenuIcon() { return <Icon><path d="M4 6h16M4 12h16M4 18h16" /></Icon> }
function BellIcon() { return <Icon><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></Icon> }
function SearchIcon() { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></svg> }
function PulseIcon() { return <Icon><path d="M3 12h4l2-7 4 14 2-7h6" /></Icon> }
function SparkIcon() { return <Icon><path d="m12 3-1.5 6.5L4 12l6.5 1.5L12 20l1.5-6.5L20 12l-6.5-2.5L12 3Z" /></Icon> }

export default App

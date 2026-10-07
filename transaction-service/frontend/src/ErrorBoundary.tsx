import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode }
type State = { hasError: boolean }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State { return { hasError: true } }

  componentDidCatch(error: Error, info: ErrorInfo) { console.error('PulseOps UI error', error, info) }

  render() {
    if (!this.state.hasError) return this.props.children
    return <div className="flex min-h-screen items-center justify-center bg-slate-950 px-5"><main className="w-full max-w-md rounded-2xl bg-white p-8 text-center shadow-2xl"><div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-rose-600">!</div><h1 className="mt-5 text-2xl font-bold tracking-tight text-slate-950">Something went wrong</h1><p className="mt-2 text-sm leading-6 text-slate-500">PulseOps could not display this page. Refresh and try again.</p><button className="mt-6 rounded-xl bg-indigo-600 px-4 py-3 text-sm font-semibold text-white hover:bg-indigo-700" onClick={() => window.location.reload()}>Refresh page</button></main></div>
  }
}

import { useState } from 'react'
import RoleSelector from './pages/RoleSelector'
import EmitterDashboard from './pages/EmitterDashboard'
import BuyerDashboard from './pages/BuyerDashboard'

export default function App() {
  const [user, setUser] = useState(null)

  if (!user) {
    return <RoleSelector onRegistered={setUser} />
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b border-gray-200 bg-white px-6 py-3 flex justify-between items-center">
        <div className="flex items-center gap-3">
          <span className="text-lg font-bold text-emerald-700">CarbonLink</span>
          <span className="text-sm text-gray-500">
            {user.companyName} · <span className="text-xs font-semibold uppercase text-gray-400">{user.role}</span>
          </span>
        </div>
        <button
          type="button"
          onClick={() => setUser(null)}
          className="text-sm text-emerald-700 hover:text-emerald-800 underline"
        >
          Switch User
        </button>
      </header>
      {user.role === 'EMITTER' ? <EmitterDashboard user={user} /> : <BuyerDashboard user={user} />}
    </div>
  )
}

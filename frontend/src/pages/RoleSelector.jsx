import { useEffect, useState } from 'react'
import { createUser, getUsersByRole, getCities, extractErrorMessage } from '../api/api'

export default function RoleSelector({ onRegistered }) {
  const [role, setRole] = useState(null)
  const [mode, setMode] = useState('login') // 'login' | 'create'
  const [existingUsers, setExistingUsers] = useState([])
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [usersError, setUsersError] = useState(null)

  const [cities, setCities] = useState([])
  const [citiesError, setCitiesError] = useState(null)

  const [name, setName] = useState('')
  const [companyName, setCompanyName] = useState('')
  const [city, setCity] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    getCities()
      .then(setCities)
      .catch((err) => setCitiesError(extractErrorMessage(err)))
  }, [])

  function chooseRole(chosenRole) {
    setRole(chosenRole)
    setUsersError(null)
    setLoadingUsers(true)
    getUsersByRole(chosenRole)
      .then((users) => {
        setExistingUsers(users)
        setMode(users.length > 0 ? 'login' : 'create')
      })
      .catch((err) => {
        setUsersError(extractErrorMessage(err))
        setExistingUsers([])
        setMode('create')
      })
      .finally(() => setLoadingUsers(false))
  }

  function goBack() {
    setRole(null)
    setExistingUsers([])
    setUsersError(null)
  }

  async function handleCreateSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const user = await createUser({
        name,
        companyName,
        role,
        city,
      })
      onRegistered(user)
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (!role) {
    return (
      <div className="max-w-md mx-auto mt-24 text-center space-y-6">
        <h1 className="text-2xl font-bold text-gray-800">CarbonLink</h1>
        <p className="text-gray-600">Carbon capture-to-product matchmaking</p>
        <div className="flex gap-4 justify-center">
          <button
            type="button"
            onClick={() => chooseRole('EMITTER')}
            className="bg-emerald-600 text-white px-5 py-3 rounded"
          >
            I'm an Emitter
          </button>
          <button
            type="button"
            onClick={() => chooseRole('BUYER')}
            className="bg-teal-600 text-white px-5 py-3 rounded"
          >
            I'm a Buyer
          </button>
        </div>
      </div>
    )
  }

  const roleLabel = role === 'EMITTER' ? 'Emitter' : 'Buyer'

  return (
    <div className="max-w-md mx-auto mt-16">
      <button type="button" onClick={goBack} className="text-sm text-gray-500 mb-4">
        ← back
      </button>
      <h1 className="text-xl font-bold mb-4 text-gray-800">Continue as {roleLabel}</h1>

      {loadingUsers && <p className="text-sm text-gray-500 mb-3">Loading existing users...</p>}
      {usersError && <p className="text-sm text-red-600 mb-3">{usersError}</p>}

      {mode === 'login' && (
        <div className="bg-white p-5 rounded-lg border border-gray-300 space-y-2">
          <p className="text-sm text-gray-600 mb-2">Log in as an existing {roleLabel.toLowerCase()}:</p>
          {existingUsers.map((user) => (
            <button
              key={user.id}
              type="button"
              onClick={() => onRegistered(user)}
              className="w-full text-left border border-gray-200 rounded px-3 py-2 hover:bg-gray-50"
            >
              {user.companyName} ({roleLabel})
            </button>
          ))}
          <button type="button" onClick={() => setMode('create')} className="text-sm text-teal-700 underline pt-1">
            or create a new user
          </button>
        </div>
      )}

      {mode === 'create' && (
        <form onSubmit={handleCreateSubmit} className="space-y-4 bg-white p-5 rounded-lg border border-gray-300">
          <label className="block text-sm text-gray-700">
            Name
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
            />
          </label>

          <label className="block text-sm text-gray-700">
            Company name
            <input
              type="text"
              required
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
            />
          </label>

          <label className="block text-sm text-gray-700">
            City
            <select
              required
              value={city}
              onChange={(e) => setCity(e.target.value)}
              className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
            >
              <option value="" disabled>
                Select your city
              </option>
              {cities.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          {citiesError && <p className="text-red-600 text-xs">{citiesError}</p>}

          {error && <p className="text-red-600 text-sm">{error}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="bg-emerald-600 text-white px-4 py-2 rounded disabled:opacity-50 w-full"
          >
            {submitting ? 'Registering...' : 'Continue'}
          </button>

          {existingUsers.length > 0 && (
            <button type="button" onClick={() => setMode('login')} className="text-sm text-teal-700 underline">
              or choose an existing user
            </button>
          )}
        </form>
      )}
    </div>
  )
}

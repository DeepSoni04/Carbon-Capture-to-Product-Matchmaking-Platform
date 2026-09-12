import { useEffect, useState } from 'react'
import RequestForm from '../components/RequestForm'
import MatchCard from '../components/MatchCard'
import {
  createRequest,
  getRequestsByBuyerId,
  getMatchesForRequest,
  requestMatch,
  getListingsByIds,
  getUsersByIds,
  extractErrorMessage,
} from '../api/api'

export default function BuyerDashboard({ user }) {
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)

  const [myRequests, setMyRequests] = useState([])
  const [loadingMatches, setLoadingMatches] = useState(false)
  const [matchesError, setMatchesError] = useState(null)
  const [matches, setMatches] = useState([])
  const [hasSearched, setHasSearched] = useState(false)
  const [activeRequestId, setActiveRequestId] = useState(null)
  const [listingsById, setListingsById] = useState({})
  const [emittersById, setEmittersById] = useState({})
  const [actionState, setActionState] = useState({})

  // Log in as an existing buyer with prior requests -> immediately show their most
  // recent request's matches, instead of an empty dashboard requiring a new submission.
  useEffect(() => {
    let cancelled = false
    getRequestsByBuyerId(user.id)
      .then((requests) => {
        if (cancelled) return
        setMyRequests(requests)
        if (requests.length > 0) {
          const mostRecent = requests.reduce((a, b) => (a.id > b.id ? a : b));
          loadMatches(mostRecent.id)
        }
      })
      .catch((err) => {
        if (!cancelled) setMatchesError(extractErrorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [user.id])

  async function loadMatches(requestId) {
    setLoadingMatches(true)
    setMatchesError(null)
    try {
      const rawMatches = await getMatchesForRequest(requestId)
      const listings = await getListingsByIds(rawMatches.map((m) => m.listingId))
      const emitters = await getUsersByIds(Object.values(listings).map((l) => l.emitterId))
      setListingsById(listings)
      setEmittersById(emitters)
      setMatches(rawMatches)
      setHasSearched(true)
      setActiveRequestId(requestId)
    } catch (err) {
      setMatchesError(extractErrorMessage(err))
    } finally {
      setLoadingMatches(false)
    }
  }

  async function handleCreateRequest(payload) {
    setSubmitting(true)
    setFormError(null)
    try {
      const request = await createRequest({ ...payload, buyerId: user.id })
      setMyRequests((prev) => [...prev, request])
      await loadMatches(request.id)
    } catch (err) {
      setFormError(extractErrorMessage(err))
      throw err
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRequestMatch(matchId) {
    setActionState((prev) => ({ ...prev, [matchId]: { busy: true, error: null } }))
    try {
      const updated = await requestMatch(matchId)
      setMatches((prev) => prev.map((m) => (m.id === matchId ? updated : m)))
      setActionState((prev) => ({ ...prev, [matchId]: { busy: false, error: null } }))
    } catch (err) {
      setActionState((prev) => ({ ...prev, [matchId]: { busy: false, error: extractErrorMessage(err) } }))
    }
  }

  return (
    <div className="max-w-3xl mx-auto p-6 space-y-8">
      <h1 className="text-xl font-bold text-gray-800">Buyer Dashboard — {user.companyName}</h1>

      <RequestForm onSubmit={handleCreateRequest} submitting={submitting} error={formError} />

      <div>
        <div className="flex items-center justify-between mb-2 gap-3">
          <h2 className="font-semibold text-lg text-gray-800">Matches</h2>
          <div className="flex items-center gap-3">
            {myRequests.length > 1 && (
              <select
                value={activeRequestId ?? ''}
                onChange={(e) => loadMatches(Number(e.target.value))}
                className="text-sm border border-gray-300 rounded px-2 py-1.5"
              >
                {myRequests.map((request) => (
                  <option key={request.id} value={request.id}>
                    Request #{request.id} — {request.intendedUse}
                  </option>
                ))}
              </select>
            )}
            {activeRequestId && (
              <button
                type="button"
                onClick={() => loadMatches(activeRequestId)}
                disabled={loadingMatches}
                className="text-sm text-teal-700 underline disabled:opacity-50"
              >
                Refresh
              </button>
            )}
          </div>
        </div>
        {loadingMatches && <p className="text-gray-500 text-sm">Loading...</p>}
        {matchesError && <p className="text-red-600 text-sm">{matchesError}</p>}
        {!loadingMatches && !matchesError && hasSearched && matches.length === 0 && (
          <p className="text-gray-500 text-sm">No matches found for this request (score below 50).</p>
        )}
        {!hasSearched && !loadingMatches && (
          <p className="text-gray-500 text-sm">Create a request above to see ranked matches.</p>
        )}

        <div className="space-y-3">
          {matches.map((match) => {
            const listing = listingsById[match.listingId]
            const emitter = listing ? emittersById[listing.emitterId] : undefined
            const action = actionState[match.id] || {}
            return (
              <MatchCard
                key={match.id}
                match={match}
                listing={listing}
                counterpartyRoleLabel="Emitter"
                counterpartyName={emitter?.companyName}
                onRequest={handleRequestMatch}
                busy={action.busy}
                actionError={action.error}
              />
            )
          })}
        </div>
      </div>
    </div>
  )
}

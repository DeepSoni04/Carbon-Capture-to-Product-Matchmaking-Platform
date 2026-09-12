import { useCallback, useEffect, useState } from 'react'
import ListingForm from '../components/ListingForm'
import MatchCard from '../components/MatchCard'
import {
  createListing,
  getListings,
  getMatchesForListing,
  acceptMatch,
  rejectMatch,
  getRequestsByIds,
  getUsersByIds,
  extractErrorMessage,
} from '../api/api'

// Listings can be ACTIVE, MATCHED or CLOSED; the marketplace-browsing endpoint defaults to
// ACTIVE only, so "my listings" is assembled from all three statuses, client-side.
const ALL_LISTING_STATUSES = ['ACTIVE', 'MATCHED', 'CLOSED']

export default function EmitterDashboard({ user }) {
  const [listings, setListings] = useState([])
  const [loadingListings, setLoadingListings] = useState(true)
  const [listingsError, setListingsError] = useState(null)

  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)

  const [matches, setMatches] = useState([])
  const [requestsById, setRequestsById] = useState({})
  const [buyersById, setBuyersById] = useState({})
  const [loadingMatches, setLoadingMatches] = useState(false)
  const [matchesError, setMatchesError] = useState(null)
  const [actionState, setActionState] = useState({})

  const loadMyListings = useCallback(async () => {
    setLoadingListings(true)
    setListingsError(null)
    try {
      const results = await Promise.all(ALL_LISTING_STATUSES.map((status) => getListings({ status })))
      const mine = results.flat().filter((listing) => listing.emitterId === user.id)
      setListings(mine)
      return mine
    } catch (err) {
      setListingsError(extractErrorMessage(err))
      return []
    } finally {
      setLoadingListings(false)
    }
  }, [user.id])

  const loadIncomingMatches = useCallback(async (myListings) => {
    if (myListings.length === 0) {
      setMatches([])
      return
    }
    setLoadingMatches(true)
    setMatchesError(null)
    try {
      const perListing = await Promise.all(myListings.map((listing) => getMatchesForListing(listing.id)))
      const flatMatches = perListing.flat()
      const requests = await getRequestsByIds(flatMatches.map((m) => m.requestId))
      const buyers = await getUsersByIds(Object.values(requests).map((r) => r.buyerId))
      setRequestsById(requests)
      setBuyersById(buyers)
      setMatches(flatMatches)
    } catch (err) {
      setMatchesError(extractErrorMessage(err))
    } finally {
      setLoadingMatches(false)
    }
  }, [])

  const refreshAll = useCallback(async () => {
    const mine = await loadMyListings()
    await loadIncomingMatches(mine)
  }, [loadMyListings, loadIncomingMatches])

  useEffect(() => {
    refreshAll()
  }, [refreshAll])

  async function handleCreateListing(payload) {
    setSubmitting(true)
    setFormError(null)
    try {
      await createListing({ ...payload, emitterId: user.id })
      await refreshAll()
    } catch (err) {
      setFormError(extractErrorMessage(err))
      throw err
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDecision(matchId, action) {
    setActionState((prev) => ({ ...prev, [matchId]: { busy: true, error: null } }))
    try {
      const updated = action === 'accept' ? await acceptMatch(matchId) : await rejectMatch(matchId)
      setMatches((prev) => prev.map((m) => (m.id === matchId ? updated : m)))
      setActionState((prev) => ({ ...prev, [matchId]: { busy: false, error: null } }))
      loadMyListings() // accepting flips this listing's status to MATCHED
    } catch (err) {
      setActionState((prev) => ({ ...prev, [matchId]: { busy: false, error: extractErrorMessage(err) } }))
    }
  }

  return (
    <div className="max-w-3xl mx-auto p-6 space-y-8">
      <h1 className="text-xl font-bold text-gray-800">Emitter Dashboard — {user.companyName}</h1>

      <ListingForm onSubmit={handleCreateListing} submitting={submitting} error={formError} />

      <div>
        <h2 className="font-semibold text-lg mb-2 text-gray-800">My Listings</h2>
        {loadingListings && <p className="text-gray-500 text-sm">Loading...</p>}
        {listingsError && <p className="text-red-600 text-sm">{listingsError}</p>}
        {!loadingListings && !listingsError && listings.length === 0 && (
          <p className="text-gray-500 text-sm">No listings yet — create one above.</p>
        )}
        {listings.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm bg-white border border-gray-300 rounded-lg">
              <thead className="bg-gray-100">
                <tr>
                  <th className="text-left p-3">Remaining / Total (tons)</th>
                  <th className="text-left p-3">Purity</th>
                  <th className="text-left p-3">Price/ton</th>
                  <th className="text-left p-3">Status</th>
                </tr>
              </thead>
              <tbody>
                {listings.map((listing) => (
                  <tr key={listing.id} className="border-t border-gray-200">
                    <td className="p-3">
                      {listing.remainingVolumeTons} / {listing.totalVolumeTons}
                    </td>
                    <td className="p-3">{listing.purityPercent}%</td>
                    <td className="p-3">₹{listing.pricePerTon}</td>
                    <td className="p-3">{listing.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div>
        <div className="flex items-center justify-between mb-2">
          <h2 className="font-semibold text-lg text-gray-800">Incoming Matches</h2>
          <button
            type="button"
            onClick={() => loadIncomingMatches(listings)}
            disabled={loadingMatches}
            className="text-sm text-teal-700 underline disabled:opacity-50"
          >
            Refresh
          </button>
        </div>
        {loadingMatches && <p className="text-gray-500 text-sm">Loading...</p>}
        {matchesError && <p className="text-red-600 text-sm">{matchesError}</p>}
        {!loadingMatches && !matchesError && matches.length === 0 && (
          <p className="text-gray-500 text-sm">No matches yet.</p>
        )}

        <div className="space-y-3">
          {matches.map((match) => {
            const request = requestsById[match.requestId]
            const buyer = request ? buyersById[request.buyerId] : undefined
            const listing = listings.find((l) => l.id === match.listingId)
            const action = actionState[match.id] || {}
            return (
              <MatchCard
                key={match.id}
                match={match}
                listing={listing}
                counterpartyRoleLabel="Buyer"
                counterpartyName={buyer?.companyName}
                intendedUse={request?.intendedUse}
                onAccept={(id) => handleDecision(id, 'accept')}
                onReject={(id) => handleDecision(id, 'reject')}
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

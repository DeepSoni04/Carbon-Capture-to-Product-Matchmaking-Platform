function scoreBarColor(score) {
  if (score > 75) return 'bg-green-500'
  if (score >= 50) return 'bg-yellow-500'
  return 'bg-red-500'
}

function statusBadgeClasses(status) {
  switch (status) {
    case 'ACCEPTED':
      return 'bg-green-100 text-green-800'
    case 'REJECTED':
      return 'bg-red-100 text-red-800'
    case 'REQUESTED':
      return 'bg-teal-100 text-teal-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

function scoreTextColor(score) {
  if (score > 75) return 'text-green-700'
  if (score >= 50) return 'text-yellow-700'
  return 'text-red-700'
}

export default function MatchCard({
  match,
  listing,
  counterpartyRoleLabel,
  counterpartyName,
  intendedUse,
  onRequest,
  onAccept,
  onReject,
  busy,
  actionError,
}) {
  const score = match.compatibilityScore
  const isSuggested = match.status === 'SUGGESTED'
  const isActionable = match.status === 'SUGGESTED' || match.status === 'REQUESTED'

  return (
    <div data-testid="match-card" className="border border-gray-300 rounded-lg p-5 bg-white space-y-4">
      <div className="flex justify-between items-start gap-3">
        <div>
          <p className="font-semibold text-gray-800">
            {counterpartyRoleLabel}: {counterpartyName || 'Loading...'}
          </p>
          {listing && (
            <p className="text-sm text-gray-600">
              {listing.captureMethod} · {listing.totalVolumeTons} t total · {listing.purityPercent}% purity · ₹{listing.pricePerTon}/ton
            </p>
          )}
          {intendedUse && <p className="text-sm text-gray-600">Intended use: {intendedUse}</p>}
        </div>
        <span className={`text-xs font-medium px-2 py-1 rounded whitespace-nowrap ${statusBadgeClasses(match.status)}`}>
          {match.status}
        </span>
      </div>

      <div>
        <div className="flex justify-between text-sm text-gray-700 mb-1">
          <span>Compatibility</span>
          <span className={`font-semibold ${scoreTextColor(score)}`}>{score.toFixed(0)}%</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2.5">
          <div
            className={`h-2.5 rounded-full ${scoreBarColor(score)}`}
            style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
          />
        </div>
      </div>

      <p className="text-sm text-gray-700">
        Distance: {match.distanceKm.toFixed(1)} km (
        {match.distanceSource === 'OPENROUTESERVICE' ? 'OpenRouteService' : 'Haversine Fallback'})
      </p>

      {match.transactedVolumeTons != null && listing && (
        <p className="text-sm font-medium text-gray-800">
          Matching {match.transactedVolumeTons} tons of this listing's {listing.totalVolumeTons} total
        </p>
      )}

      <div className="text-sm text-gray-700 border-t border-gray-200 pt-3 space-y-0.5">
        <p>Base price: ₹{match.costBreakdown.basePrice.toLocaleString()}</p>
        <p>Transport cost: ₹{match.costBreakdown.transportCost.toLocaleString()}</p>
        <p className="font-semibold">Total: ₹{match.costBreakdown.totalCost.toLocaleString()}</p>
      </div>

      {actionError && <p className="text-red-600 text-sm">{actionError}</p>}

      {(onRequest || onAccept || onReject) && (
        <div className="flex gap-2 pt-1">
          {onRequest && isSuggested && (
            <button
              type="button"
              onClick={() => onRequest(match.id)}
              disabled={busy}
              className="bg-teal-600 text-white text-sm px-3 py-1.5 rounded disabled:opacity-50"
            >
              {busy ? 'Requesting...' : 'Request Match'}
            </button>
          )}
          {onAccept && isActionable && (
            <button
              type="button"
              onClick={() => onAccept(match.id)}
              disabled={busy}
              className="bg-green-600 text-white text-sm px-3 py-1.5 rounded disabled:opacity-50"
            >
              {busy ? 'Working...' : 'Accept'}
            </button>
          )}
          {onReject && isActionable && (
            <button
              type="button"
              onClick={() => onReject(match.id)}
              disabled={busy}
              className="bg-red-600 text-white text-sm px-3 py-1.5 rounded disabled:opacity-50"
            >
              {busy ? 'Working...' : 'Reject'}
            </button>
          )}
        </div>
      )}
    </div>
  )
}

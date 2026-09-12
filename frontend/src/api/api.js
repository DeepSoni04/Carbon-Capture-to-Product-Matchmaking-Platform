import axios from 'axios'

const client = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

function unwrap(promise) {
  return promise.then((res) => res.data)
}

// --- Users ---
export function createUser(payload) {
  return unwrap(client.post('/users', payload))
}

export function getCities() {
  return unwrap(client.get('/cities'))
}

export function getUser(id) {
  return unwrap(client.get(`/users/${id}`))
}

export function getUsersByRole(role) {
  return unwrap(client.get('/users', { params: { role } }))
}

// --- Listings ---
export function createListing(payload) {
  return unwrap(client.post('/listings', payload))
}

export function getListings(params = {}) {
  return unwrap(client.get('/listings', { params }))
}

export function getListing(id) {
  return unwrap(client.get(`/listings/${id}`))
}

export function getMatchesForListing(id) {
  return unwrap(client.get(`/listings/${id}/matches`))
}

// --- Carbon requests ---
export function createRequest(payload) {
  return unwrap(client.post('/requests', payload))
}

export function getRequest(id) {
  return unwrap(client.get(`/requests/${id}`))
}

export function getRequestsByBuyerId(buyerId) {
  return unwrap(client.get('/requests', { params: { buyerId } }))
}

export function getMatchesForRequest(id) {
  return unwrap(client.get(`/requests/${id}/matches`))
}

// --- Matches ---
export function requestMatch(id) {
  return unwrap(client.post(`/matches/${id}/request`))
}

export function acceptMatch(id) {
  return unwrap(client.post(`/matches/${id}/accept`))
}

export function rejectMatch(id) {
  return unwrap(client.post(`/matches/${id}/reject`))
}

// --- Enrichment helpers: match responses only carry ids, so dashboards batch-fetch
// the listing/user/request rows they need for display, deduped, in parallel. ---
function uniqueIds(ids) {
  return [...new Set(ids.filter((id) => id !== undefined && id !== null))]
}

export async function getListingsByIds(ids) {
  const listings = await Promise.all(uniqueIds(ids).map((id) => getListing(id)))
  return Object.fromEntries(listings.map((listing) => [listing.id, listing]))
}

export async function getUsersByIds(ids) {
  const users = await Promise.all(uniqueIds(ids).map((id) => getUser(id)))
  return Object.fromEntries(users.map((user) => [user.id, user]))
}

export async function getRequestsByIds(ids) {
  const requests = await Promise.all(uniqueIds(ids).map((id) => getRequest(id)))
  return Object.fromEntries(requests.map((request) => [request.id, request]))
}

export function extractErrorMessage(error) {
  return error?.response?.data?.error || error?.message || 'Something went wrong'
}

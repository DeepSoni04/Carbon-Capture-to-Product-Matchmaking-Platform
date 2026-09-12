import { useEffect, useState } from 'react'
import { getCities, extractErrorMessage } from '../api/api'

const CAPTURE_METHODS = ['Post-combustion', 'Direct Air Capture', 'Pre-combustion', 'Oxy-fuel']

const initialValues = {
  totalVolumeTons: '',
  purityPercent: '',
  captureMethod: CAPTURE_METHODS[0],
  pricePerTon: '',
  city: '',
}

export default function ListingForm({ onSubmit, submitting, error }) {
  const [values, setValues] = useState(initialValues)
  const [cities, setCities] = useState([])
  const [citiesError, setCitiesError] = useState(null)

  useEffect(() => {
    getCities()
      .then(setCities)
      .catch((err) => setCitiesError(extractErrorMessage(err)))
  }, [])

  function handleChange(field) {
    return (e) => setValues((prev) => ({ ...prev, [field]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    try {
      await onSubmit({
        totalVolumeTons: Number(values.totalVolumeTons),
        purityPercent: Number(values.purityPercent),
        captureMethod: values.captureMethod,
        pricePerTon: Number(values.pricePerTon),
        city: values.city,
      })
      setValues(initialValues)
    } catch {
      // parent surfaces the error via the `error` prop
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 bg-white p-5 rounded-lg border border-gray-300">
      <h2 className="font-semibold text-lg text-gray-800">Create Listing</h2>

      <div className="grid grid-cols-2 gap-4">
        <label className="text-sm text-gray-700">
          Total volume (tons)
          <input
            type="number"
            step="any"
            min="0.0001"
            required
            value={values.totalVolumeTons}
            onChange={handleChange('totalVolumeTons')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          />
        </label>

        <label className="text-sm text-gray-700">
          Purity (%)
          <input
            type="number"
            step="any"
            min="0"
            max="100"
            required
            value={values.purityPercent}
            onChange={handleChange('purityPercent')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          />
        </label>

        <label className="text-sm text-gray-700">
          Capture method
          <select
            value={values.captureMethod}
            onChange={handleChange('captureMethod')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          >
            {CAPTURE_METHODS.map((method) => (
              <option key={method} value={method}>
                {method}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm text-gray-700">
          Price per ton (₹)
          <input
            type="number"
            step="any"
            min="0.0001"
            required
            value={values.pricePerTon}
            onChange={handleChange('pricePerTon')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          />
        </label>

        <label className="text-sm text-gray-700 col-span-2">
          City
          <select
            required
            value={values.city}
            onChange={handleChange('city')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          >
            <option value="" disabled>
              Select the listing's city
            </option>
            {cities.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>
      </div>

      {citiesError && <p className="text-red-600 text-xs">{citiesError}</p>}
      {error && <p className="text-red-600 text-sm">{error}</p>}

      <button
        type="submit"
        disabled={submitting}
        className="bg-emerald-600 text-white px-4 py-2 rounded text-sm disabled:opacity-50"
      >
        {submitting ? 'Creating...' : 'Create Listing'}
      </button>
    </form>
  )
}

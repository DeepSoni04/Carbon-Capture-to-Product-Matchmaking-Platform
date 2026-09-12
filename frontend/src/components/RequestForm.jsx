import { useState } from 'react'

const INTENDED_USES = ['Fuel Synthesis', 'Building Materials', 'Greenhouse', 'Algae Farming', 'Other']

const initialValues = {
  minVolumeNeeded: '',
  minPurityRequired: '',
  maxDistanceKm: '',
  maxBudgetPerTon: '',
  intendedUse: INTENDED_USES[0],
}

export default function RequestForm({ onSubmit, submitting, error }) {
  const [values, setValues] = useState(initialValues)

  function handleChange(field) {
    return (e) => setValues((prev) => ({ ...prev, [field]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    try {
      await onSubmit({
        minVolumeNeeded: Number(values.minVolumeNeeded),
        minPurityRequired: Number(values.minPurityRequired),
        maxDistanceKm: Number(values.maxDistanceKm),
        maxBudgetPerTon: Number(values.maxBudgetPerTon),
        intendedUse: values.intendedUse,
      })
      setValues(initialValues)
    } catch {
      // parent surfaces the error via the `error` prop
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 bg-white p-5 rounded-lg border border-gray-300">
      <h2 className="font-semibold text-lg text-gray-800">Create Request</h2>

      <div className="grid grid-cols-2 gap-4">
        <label className="text-sm text-gray-700">
          Min volume needed (tons)
          <input
            type="number"
            step="any"
            min="0.0001"
            required
            value={values.minVolumeNeeded}
            onChange={handleChange('minVolumeNeeded')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          />
        </label>

        <label className="text-sm text-gray-700">
          Min purity required (%)
          <input
            type="number"
            step="any"
            min="0"
            max="100"
            required
            value={values.minPurityRequired}
            onChange={handleChange('minPurityRequired')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          />
        </label>

        <label className="text-sm text-gray-700">
          Max distance (km)
          <input
            type="number"
            step="any"
            min="0.0001"
            required
            value={values.maxDistanceKm}
            onChange={handleChange('maxDistanceKm')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          />
        </label>

        <label className="text-sm text-gray-700">
          Max budget per ton (₹)
          <input
            type="number"
            step="any"
            min="0.0001"
            required
            value={values.maxBudgetPerTon}
            onChange={handleChange('maxBudgetPerTon')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          />
        </label>

        <label className="text-sm text-gray-700 col-span-2">
          Intended use
          <select
            value={values.intendedUse}
            onChange={handleChange('intendedUse')}
            className="mt-1 w-full border border-gray-300 rounded px-2 py-1.5"
          >
            {INTENDED_USES.map((use) => (
              <option key={use} value={use}>
                {use}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && <p className="text-red-600 text-sm">{error}</p>}

      <button
        type="submit"
        disabled={submitting}
        className="bg-emerald-600 text-white px-4 py-2 rounded text-sm disabled:opacity-50"
      >
        {submitting ? 'Creating...' : 'Create Request & Find Matches'}
      </button>
    </form>
  )
}

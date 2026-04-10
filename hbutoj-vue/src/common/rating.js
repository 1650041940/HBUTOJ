export const DEFAULT_DIFFICULTY_RATING = 1200
export const DIFFICULTY_BANDS = [
  { key: 'entry', min: 800, max: 1100, labelKey: 'Difficulty_Band_Entry' },
  { key: 'easy', min: 1200, max: 1500, labelKey: 'Difficulty_Band_Easy' },
  { key: 'medium', min: 1600, max: 1900, labelKey: 'Difficulty_Band_Medium' },
  { key: 'hard', min: 2000, max: 2300, labelKey: 'Difficulty_Band_Hard' },
  { key: 'extreme', min: 2400, max: 3500, labelKey: 'Difficulty_Band_Extreme' },
]

const RATING_STAGES = [
  { max: 1199, color: '#808080', background: '#909399' },
  { max: 1399, color: '#0f7a1f', background: '#2ea043' },
  { max: 1599, color: '#1f8f99', background: '#0ea5a4' },
  { max: 1899, color: '#1d4ed8', background: '#2563eb' },
  { max: 2099, color: '#7c3aed', background: '#8b5cf6' },
  { max: 2399, color: '#d97706', background: '#f59e0b' },
  { max: Infinity, color: '#dc2626', background: '#ef4444' },
]

export function normalizeDisplayRating(value, fallback = DEFAULT_DIFFICULTY_RATING) {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? Math.round(num) : fallback
}

export function getRatingStage(value) {
  const rating = normalizeDisplayRating(value)
  return RATING_STAGES.find((stage) => rating <= stage.max) || RATING_STAGES[RATING_STAGES.length - 1]
}

export function getRatingTextStyle(value) {
  const stage = getRatingStage(value)
  return {
    color: stage.color,
    fontWeight: 700,
  }
}

export function getRatingTagStyle(value) {
  const stage = getRatingStage(value)
  return {
    color: '#fff',
    backgroundColor: stage.background,
    borderColor: stage.background,
    fontWeight: 700,
  }
}

export function getRatingCardStyle(value) {
  const stage = getRatingStage(value)
  return {
    background: `linear-gradient(135deg, ${stage.background} 0%, ${stage.color} 100%)`,
    color: '#fff',
  }
}

export function getDifficultyBandByRating(value) {
  const rating = normalizeDisplayRating(value)
  return DIFFICULTY_BANDS.find((band) => rating >= band.min && rating <= band.max) || DIFFICULTY_BANDS[DIFFICULTY_BANDS.length - 1]
}

export function getDifficultyBandKey(value) {
  return getDifficultyBandByRating(value).key
}

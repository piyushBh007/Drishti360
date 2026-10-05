const db = require('../db/connection');
const { buildFeatures } = require('./featureBuilder');
const { activeRiskModel } = require('./riskModel');

/**
 * Calculate risk score for a specific NGO, persist historical record to DB,
 * and update NGO summary fields.
 */
async function calculateAndSaveRisk(ngoId) {
  // 1. Build risk features
  const features = await buildFeatures(ngoId);

  // 2. Predict risk using active risk model (XGBoost with Fallback)
  const prediction = (typeof activeRiskModel.predictAsync === 'function')
    ? await activeRiskModel.predictAsync(features)
    : activeRiskModel.predict(features);

  const { score, riskLevel, factors, model } = prediction;
  const now = new Date();

  // Combine features and factors into snapshot metadata for DB JSONB storage
  const featuresSnapshot = {
    ...features,
    factors: factors
  };

  const scoreVal = Math.round(Number(score) || 0);

  // 3. Persist new historical record to PostgreSQL risk_scores table
  try {
    const insertSql = `
      INSERT INTO risk_scores (ngo_id, score, level, model_version, features, created_at)
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING id, created_at;
    `;
    await db.query(insertSql, [
      ngoId,
      scoreVal,
      riskLevel,
      model.version,
      JSON.stringify(featuresSnapshot),
      now
    ]);

    // 4. Update ngos summary record
    const updateNgoSql = `
      UPDATE ngos
      SET risk_score = $1, risk_level = $2, updated_at = $3
      WHERE LOWER(id) = LOWER($4);
    `;
    await db.query(updateNgoSql, [scoreVal, riskLevel, now, ngoId]);
  } catch (err) {
    console.warn(`[RiskService] DB persistence warning for ${ngoId}:`, err.message);
  }

  return {
    ngoId: ngoId,
    ngoName: features.ngoName,
    score: scoreVal,
    riskLevel: riskLevel,
    features: features,
    factors: factors,
    model: model,
    generatedAt: now.toISOString()
  };
}

/**
 * Calculate and return latest risk scores for all registered NGOs.
 */
async function getAllRiskScores() {
  let ngoIds = [];

  try {
    const res = await db.query('SELECT id FROM ngos ORDER BY id ASC');
    if (res.rows.length > 0) {
      ngoIds = res.rows.map(r => r.id);
    }
  } catch (err) {
    console.warn('[RiskService] Failed to fetch NGO IDs from DB:', err.message);
  }

  if (ngoIds.length === 0) {
    ngoIds = ['ngo-001', 'ngo-002', 'ngo-003', 'ngo-004', 'ngo-005'];
  }

  const results = [];
  for (const ngoId of ngoIds) {
    try {
      const riskResult = await calculateAndSaveRisk(ngoId);
      results.push(riskResult);
    } catch (err) {
      console.error(`[RiskService] Failed risk calculation for ${ngoId}:`, err.message);
    }
  }
  return results;
}

module.exports = {
  calculateAndSaveRisk,
  getAllRiskScores
};

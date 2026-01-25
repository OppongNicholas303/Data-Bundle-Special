const { VALID_BUNDLES } = require('../utils/constants');

function validatePurchase(req, res, next) {
  const { bundle, beneficiary } = req.body;

  if (!bundle) {
    return res.status(400).json({ error: 'Missing bundle parameter' });
  }
  if (!beneficiary) {
    return res.status(400).json({ error: 'Missing beneficiary parameter' });
  }
  if (!/^\d{10}$/.test(beneficiary)) {
    return res.status(400).json({ error: 'Beneficiary must be 10 digits' });
  }
  if (!VALID_BUNDLES.includes(bundle.toLowerCase())) {
    return res.status(400).json({ 
      error: 'Invalid bundle',
      validBundles: VALID_BUNDLES 
    });
  }

  next();
}

module.exports = { validatePurchase };
const purchaseService = require('../services/purchase.service');

exports.purchase = async (req, res) => {
  const { bundle, beneficiary } = req.body;

  try {
    const result = await purchaseService.purchase(bundle, beneficiary);
    
    if (result.success) {
      res.json(result);
    } else {
      res.status(400).json(result);
    }
  } catch (error) {
    res.status(500).json({ 
      success: false,
      error: error.message 
    });
  }
};
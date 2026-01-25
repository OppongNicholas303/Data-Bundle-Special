const express = require('express');
const purchaseController = require('../controllers/purchase.controller');
const { ensureAuthenticated } = require('../middlewares/auth.middleware');
const { validatePurchase } = require('../middlewares/validation.middleware');

const router = express.Router();

router.post('/', ensureAuthenticated, validatePurchase, purchaseController.purchase);

module.exports = router;
const express = require('express');
const authRoutes = require('./auth.routes');
const navigationRoutes = require('./navigation.routes');
const purchaseRoutes = require('./purchase.routes');

const router = express.Router();

router.use('/auth', authRoutes);
router.use('/navigation', navigationRoutes);
router.use('/purchase', purchaseRoutes);

module.exports = router;
const express = require('express');
const navigationController = require('../controllers/navigation.controller');
const { ensureAuthenticated } = require('../middlewares/auth.middleware');

const router = express.Router();

router.get('/goto', ensureAuthenticated, navigationController.navigate);
router.get('/content', ensureAuthenticated, navigationController.getContent);
router.get('/dashboard', ensureAuthenticated, navigationController.dashboard);
router.get('/extract', ensureAuthenticated, navigationController.extract);
router.get('/screenshot', ensureAuthenticated, navigationController.screenshot);

module.exports = router;
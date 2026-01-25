const express = require('express');
const authController = require('../controllers/auth.controller');

const router = express.Router();

router.get('/login', authController.login);
router.get('/status', authController.status);

module.exports = router;
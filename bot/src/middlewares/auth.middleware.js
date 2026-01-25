const authService = require('../services/auth.service');

async function ensureAuthenticated(req, res, next) {
  try {
    await authService.ensureLoggedIn();
    next();
  } catch (error) {
    res.status(401).json({ error: 'Authentication required', message: error.message });
  }
}

module.exports = { ensureAuthenticated };
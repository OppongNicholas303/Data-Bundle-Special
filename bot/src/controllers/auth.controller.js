const authService = require('../services/auth.service');
const { browserService } = require('../services/browser.service');

exports.login = async (req, res) => {
  try {
    const result = await authService.login();
    res.json({ 
      ...result, 
      url: browserService.getCurrentUrl() 
    });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
};

exports.status = (req, res) => {
  res.json({
    loggedIn: browserService.getLoginStatus(),
    currentUrl: browserService.getCurrentUrl()
  });
};
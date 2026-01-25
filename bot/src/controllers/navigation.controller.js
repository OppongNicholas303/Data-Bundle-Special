const { browserService } = require('../services/browser.service');
const authService = require('../services/auth.service');
const { BASE_URL } = require('../config/env.config');
const { URLS } = require('../utils/constants');

exports.navigate = async (req, res) => {
  const { url } = req.query;

  if (!url) {
    return res.status(400).json({ error: 'Missing url parameter' });
  }

  try {
    await authService.ensureLoggedIn();
    const page = await browserService.getPage();
    
    await page.goto(url, { waitUntil: 'networkidle' });

    res.json({
      success: true,
      url: page.url(),
      title: await page.title()
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getContent = async (req, res) => {
  try {
    await authService.ensureLoggedIn();
    const page = await browserService.getPage();
    const content = await page.content();
    res.send(content);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.dashboard = async (req, res) => {
  try {
    await authService.ensureLoggedIn();
    const page = await browserService.getPage();
    
    await page.goto(`${BASE_URL}${URLS.DASHBOARD}`, { waitUntil: 'networkidle' });
    const content = await page.content();

    res.json({
      success: true,
      url: page.url(),
      title: await page.title(),
      html: content
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.extract = async (req, res) => {
  const { selector } = req.query;

  if (!selector) {
    return res.status(400).json({ error: 'Missing selector parameter' });
  }

  try {
    await authService.ensureLoggedIn();
    const page = await browserService.getPage();
    const elements = await page.locator(selector).allTextContents();

    res.json({
      success: true,
      selector,
      count: elements.length,
      data: elements
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.screenshot = async (req, res) => {
  try {
    await authService.ensureLoggedIn();
    const page = await browserService.getPage();
    const screenshot = await page.screenshot({ fullPage: true });
    
    res.set('Content-Type', 'image/png');
    res.send(screenshot);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};
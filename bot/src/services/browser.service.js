const { chromium } = require('playwright');
const logger = require('../utils/logger');

class BrowserService {
  constructor() {
    this.browser = null;
    this.context = null;
    this.page = null;
    this.isLoggedIn = false;
  }

  async launch() {
    if (this.browser && this.page) {
      logger.info('Browser already launched');
      return;
    }

    logger.info('Launching browser...');
    
    this.browser = await chromium.launch({
      headless: false,
      slowMo: 60,
    });

    this.context = await this.browser.newContext({
      viewport: { width: 1280, height: 800 },
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      locale: 'en-US',
      timezoneId: 'Africa/Accra',
    });

    this.page = await this.context.newPage();
    logger.info('Browser launched successfully');
  }

  async getPage() {
    if (!this.page) {
      await this.launch();
    }
    return this.page;
  }

  async saveState() {
    if (this.context) {
      await this.context.storageState({ path: './storage/mydatagigs-state.json' });
      logger.info('Browser state saved');
    }
  }

  async shutdown() {
    logger.info('Shutting down browser...');
    if (this.browser) {
      await this.browser.close();
      this.browser = null;
      this.context = null;
      this.page = null;
      this.isLoggedIn = false;
    }
  }

  getLoginStatus() {
    return this.isLoggedIn;
  }

  setLoginStatus(status) {
    this.isLoggedIn = status;
  }

  getCurrentUrl() {
    return this.page ? this.page.url() : null;
  }
}

const browserService = new BrowserService();

module.exports = {
  browserService,
  shutdown: () => browserService.shutdown()
};
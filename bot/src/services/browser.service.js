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
    
    // Use headless mode for better performance in production
    const isProduction = process.env.NODE_ENV === 'production';
    
    this.browser = await chromium.launch({
      headless: isProduction, // headless in production, headed in dev
      slowMo: 0, // Remove slowMo for better performance
      args: [
        '--disable-blink-features=AutomationControlled',
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
      ]
    });

    this.context = await this.browser.newContext({
      viewport: { width: 1280, height: 800 },
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
      locale: 'en-US',
      timezoneId: 'Africa/Accra',
      // Enable faster navigation
      acceptDownloads: false,
      javaScriptEnabled: true,
    });

    // Set default timeout for all operations
    this.context.setDefaultTimeout(15000);
    this.context.setDefaultNavigationTimeout(15000);

    this.page = await this.context.newPage();
    
    // Optimize page loading
    await this.page.route('**/*.{png,jpg,jpeg,gif,svg,ico,webp}', route => {
      // Block images for faster loading (optional - remove if images are needed)
      // route.abort();
      route.continue();
    });

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
      try {
        await this.context.storageState({ path: './storage/mydatagigs-state.json' });
        logger.info('Browser state saved');
      } catch (error) {
        logger.error('Failed to save browser state:', error.message);
      }
    }
  }

  async loadState() {
    try {
      const fs = require('fs');
      if (fs.existsSync('./storage/mydatagigs-state.json')) {
        logger.info('Loading saved browser state...');
        // Load state would go here if implementing session restoration
        return true;
      }
    } catch (error) {
      logger.error('Failed to load browser state:', error.message);
    }
    return false;
  }

  async shutdown() {
    logger.info('Shutting down browser...');
    if (this.browser) {
      try {
        await this.browser.close();
        this.browser = null;
        this.context = null;
        this.page = null;
        this.isLoggedIn = false;
        logger.info('Browser closed successfully');
      } catch (error) {
        logger.error('Error closing browser:', error.message);
      }
    }
  }

  getLoginStatus() {
    return this.isLoggedIn;
  }

  setLoginStatus(status) {
    this.isLoggedIn = status;
    logger.info(`Login status set to: ${status}`);
  }

  getCurrentUrl() {
    return this.page ? this.page.url() : null;
  }

  async takeScreenshot(filename) {
    if (!this.page) {
      throw new Error('No page available for screenshot');
    }
    
    try {
      const path = `./screenshots/${filename}`;
      await this.page.screenshot({ path, fullPage: true });
      logger.info(`Screenshot saved: ${path}`);
      return path;
    } catch (error) {
      logger.error('Failed to take screenshot:', error.message);
      throw error;
    }
  }
}

const browserService = new BrowserService();

module.exports = {
  browserService,
  shutdown: () => browserService.shutdown()
};
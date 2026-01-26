const { browserService } = require('./browser.service');
const { BASE_URL, MYDATAGIGS_EMAIL, MYDATAGIGS_PASSWORD } = require('../config/env.config');
const { URLS, SELECTORS, TIMEOUTS } = require('../utils/constants');
const logger = require('../utils/logger');

class AuthService {
  async login() {
    if (browserService.getLoginStatus() && browserService.page) {
      logger.info('Already logged in, reusing session');
      return { 
        success: true, 
        message: 'Already logged in',
        url: browserService.getCurrentUrl()
      };
    }

    const page = await browserService.getPage();
    
    logger.info('Navigating to login page...');
    await page.goto(`${BASE_URL}${URLS.LOGIN}`, { 
      waitUntil: 'domcontentloaded', // Changed from 'networkidle' for speed
      timeout: TIMEOUTS.NAVIGATION 
    });
    await page.waitForTimeout(1500); // Reduced from 2000

    // Check if already logged in
    const alreadyLoggedIn = await page.locator('.wppb-alert, text="You are currently logged in"')
      .isVisible({ timeout: 2000 }) // Reduced from 3000
      .catch(() => false);
    
    if (alreadyLoggedIn) {
      logger.info('✓ Already logged in (session active)');
      browserService.setLoginStatus(true);
      await this.navigateToProductPage(page);
      await browserService.saveState();
      return { 
        success: true, 
        message: 'Already logged in (session active)',
        url: page.url()
      };
    }

    // Wait for login form
    await page.waitForSelector('input[type="text"], input[type="email"]', { timeout: 10000 });
    
    // Fill credentials with reduced delays
    logger.info('Filling login credentials...');
    await page.fill('input[type="text"], input[type="email"], input[name="username"], input[name="email"]', MYDATAGIGS_EMAIL);
    await page.waitForTimeout(500 + Math.random() * 300); // Reduced

    await page.fill('input[type="password"]', MYDATAGIGS_PASSWORD);
    await page.waitForTimeout(700 + Math.random() * 400); // Reduced

    // Click login
    logger.info('Clicking login button...');
    await page.click('button[type="submit"], button:has-text("Login"), button:has-text("Sign in"), input[type="submit"]')
      .catch(() => page.click('a:has-text("Login")'));
    
    await page.waitForTimeout(3500); // Reduced from 4000

    // Verify login with parallel checks
    const currentUrl = page.url();
    logger.info(`Current URL: ${currentUrl}`);

    const [buyDataMenuExists, myAccountExists, logoutExists, alreadyLoggedInMsg] = await Promise.all([
      page.locator('#menu-item-7155').isVisible({ timeout: 4000 }).catch(() => false),
      page.locator('text="MY ACCOUNT", text="My account"').isVisible({ timeout: 2000 }).catch(() => false),
      page.locator('#menu-item-4802, a:has-text("Logout")').isVisible({ timeout: 2000 }).catch(() => false),
      page.locator('.wppb-alert').isVisible({ timeout: 1500 }).catch(() => false)
    ]);
    
    logger.info('Login checks:', { buyDataMenuExists, myAccountExists, logoutExists, alreadyLoggedInMsg });

    const isLoginSuccessful = buyDataMenuExists || myAccountExists || logoutExists || alreadyLoggedInMsg;

    if (!isLoginSuccessful) {
      logger.error('Login failed - could not verify successful login');
      await page.screenshot({ path: './screenshots/login-failed.png', fullPage: true });
      throw new Error('Login failed - could not verify successful login');
    }

    logger.info('✓ Login successful!');
    browserService.setLoginStatus(true);
    
    // Navigate to product page
    await this.navigateToProductPage(page);
    await browserService.saveState();

    return { 
      success: true, 
      message: 'Login successful',
      url: page.url()
    };
  }

  async navigateToProductPage(page) {
    // Navigate to buy-data page
    logger.info('Navigating to buy-data page...');
    
    try {
      await page.locator(SELECTORS.MENU_BUY_DATA).click({ timeout: 4000 }); // Reduced from 5000
      logger.info('Clicked Buy Data menu item');
      await page.waitForTimeout(2000); // Reduced from 3000
      await page.waitForLoadState('domcontentloaded', { timeout: 8000 }).catch(() => {});
    } catch (error) {
      logger.info('Menu click failed, using direct navigation');
      await page.goto(`${BASE_URL}${URLS.BUY_DATA}`, { 
        waitUntil: 'domcontentloaded',
        timeout: TIMEOUTS.NAVIGATION 
      }).catch(e => logger.error('Navigation error:', e.message));
      await page.waitForTimeout(1500);
    }

    logger.info(`✓ Now on buy-data page: ${page.url()}`);

    // Navigate to MTN product page
    logger.info('Navigating to MTN Data Bundle Special...');
    
    try {
      const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
      const linkExists = await productLink.isVisible({ timeout: 4000 }).catch(() => false); // Reduced from 5000
      
      if (linkExists) {
        await productLink.click();
        logger.info('Clicked MTN Data Bundle Special product link');
        await page.waitForTimeout(2000); // Reduced from 3000
        await page.waitForLoadState('domcontentloaded', { timeout: 8000 }).catch(() => {});
      } else {
        logger.info('Product link not found, using direct navigation');
        await page.goto(`${BASE_URL}${URLS.MTN_PRODUCT}`, { 
          waitUntil: 'domcontentloaded',
          timeout: TIMEOUTS.NAVIGATION 
        });
        await page.waitForTimeout(1500);
      }
      
      logger.info(`✓ Now on product page: ${page.url()}`);
    } catch (error) {
      logger.error('Error navigating to product page:', error.message);
      throw error;
    }
  }

  async ensureLoggedIn() {
    if (!browserService.getLoginStatus()) {
      await this.login();
    }
  }
}

module.exports = new AuthService();
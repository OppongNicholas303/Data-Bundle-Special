const { browserService } = require('./browser.service');
const authService = require('./auth.service');
const { BASE_URL } = require('../config/env.config');
const { URLS, SELECTORS, TIMEOUTS, VALID_BUNDLES } = require('../utils/constants');
const logger = require('../utils/logger');

class PurchaseService {
  validatePurchaseData(bundle, beneficiary) {
    if (!bundle) {
      throw new Error('Missing bundle parameter');
    }
    if (!beneficiary) {
      throw new Error('Missing beneficiary parameter');
    }
    if (!/^\d{10}$/.test(beneficiary)) {
      throw new Error('Beneficiary number must be 10 digits');
    }
    if (!VALID_BUNDLES.includes(bundle.toLowerCase())) {
      throw new Error(`Invalid bundle. Valid options: ${VALID_BUNDLES.join(', ')}`);
    }
  }

  async purchase(bundle, beneficiary) {
    this.validatePurchaseData(bundle, beneficiary);
    
    await authService.ensureLoggedIn();
    const page = await browserService.getPage();

    logger.info(`Processing purchase: ${bundle.toUpperCase()} for ${beneficiary}`);

    // Navigate to product page
    const currentUrl = page.url();
    if (!currentUrl.includes('mtn-data-bundle-special')) {
      await page.goto(`${BASE_URL}${URLS.MTN_PRODUCT}`, { 
        waitUntil: 'domcontentloaded', 
        timeout: TIMEOUTS.NAVIGATION 
      });
      await page.waitForTimeout(2000);
    }

    // Select bundle
    logger.info(`Selecting ${bundle.toUpperCase()} package...`);
    await page.selectOption(SELECTORS.BUNDLE_SELECT, bundle.toLowerCase());
    await page.waitForTimeout(1500);

    // Fill beneficiary
    logger.info(`Entering beneficiary: ${beneficiary}`);
    await page.fill(SELECTORS.BENEFICIARY_INPUT, beneficiary);
    await page.waitForTimeout(TIMEOUTS.SHORT);

    // Get price
    const price = await page.locator('.woocommerce-variation-price .amount').textContent().catch(() => 'N/A');
    logger.info(`Price: ${price}`);

    // Add to cart
    logger.info('Adding to cart...');
    await page.click(SELECTORS.ADD_TO_CART);
    await page.waitForTimeout(TIMEOUTS.MEDIUM);

    // Navigate to checkout
    let checkoutUrl = page.url();
    if (!checkoutUrl.includes('checkout')) {
      await page.goto(`${BASE_URL}${URLS.CHECKOUT}`, { 
        waitUntil: 'domcontentloaded', 
        timeout: TIMEOUTS.NAVIGATION 
      });
      await page.waitForTimeout(2000);
      checkoutUrl = page.url();
    }

    logger.info('Now on checkout page');

    // Select wallet payment
    await this.selectWalletPayment(page);

    // Place order
    return await this.placeOrder(page, bundle, beneficiary, price, checkoutUrl);
  }

  async selectWalletPayment(page) {
    try {
      await page.waitForSelector(SELECTORS.PAYMENT_WALLET, { timeout: TIMEOUTS.LONG });
      
      const isWalletSelected = await page.locator(SELECTORS.PAYMENT_WALLET).isChecked();
      
      if (!isWalletSelected) {
        await page.click(SELECTORS.PAYMENT_WALLET);
        logger.info('Wallet payment selected');
        await page.waitForTimeout(TIMEOUTS.SHORT);
      }
      
      const balance = await page.locator('label[for="payment_method_wallet"] strong .amount').textContent().catch(() => 'N/A');
      logger.info(`Wallet balance: ${balance}`);
    } catch (error) {
      logger.error('Error selecting wallet payment:', error.message);
    }
  }

  async placeOrder(page, bundle, beneficiary, price, checkoutUrl) {
    try {
      await page.waitForSelector(SELECTORS.PLACE_ORDER, { timeout: TIMEOUTS.LONG });
      await page.click(SELECTORS.PLACE_ORDER);
      logger.info('Place order button clicked');
      
      await page.waitForTimeout(TIMEOUTS.LONG);
      await page.waitForLoadState('domcontentloaded', { timeout: TIMEOUTS.NAVIGATION }).catch(() => {});
      
      const finalUrl = page.url();
      const orderReceived = finalUrl.includes('order-received');
      const hasError = await page.locator('.woocommerce-error').isVisible({ timeout: 3000 }).catch(() => false);
      
      if (hasError) {
        const errorMessage = await page.locator('.woocommerce-error').textContent().catch(() => 'Unknown error');
        await page.screenshot({ path: './screenshots/order-error.png', fullPage: true });
        
        return {
          success: false,
          message: 'Order placement failed',
          error: errorMessage,
          bundle: bundle.toUpperCase(),
          beneficiary,
          price,
          screenshot: 'order-error.png'
        };
      }

      let orderNumber = null;
      if (orderReceived) {
        const orderMatch = finalUrl.match(/order-received\/(\d+)/);
        if (orderMatch) {
          orderNumber = orderMatch[1];
        }
        logger.info(`Order successful! Order #${orderNumber}`);
      }

      await page.screenshot({ path: './screenshots/order-confirmation.png', fullPage: true });

      return {
        success: true,
        message: 'Order placed successfully!',
        orderNumber,
        bundle: bundle.toUpperCase(),
        beneficiary,
        price,
        checkoutUrl,
        finalUrl,
        screenshot: 'order-confirmation.png'
      };
    } catch (error) {
      logger.error('Error placing order:', error.message);
      await page.screenshot({ path: './screenshots/checkout-error.png', fullPage: true });
      
      return {
        success: false,
        message: 'Failed to place order',
        error: error.message,
        bundle: bundle.toUpperCase(),
        beneficiary,
        price,
        screenshot: 'checkout-error.png'
      };
    }
  }
}

module.exports = new PurchaseService();
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

    // Navigate to product page if not already there
    const currentUrl = page.url();
    if (!currentUrl.includes('mtn-data-bundle-special')) {
      logger.info('Navigating to MTN product page...');
      await page.goto(`${BASE_URL}${URLS.MTN_PRODUCT}`, { 
        waitUntil: 'domcontentloaded', 
        timeout: TIMEOUTS.NAVIGATION 
      });
      await page.waitForTimeout(1500);
    }

    // Select bundle
    logger.info(`Selecting ${bundle.toUpperCase()} package...`);
    await page.selectOption(SELECTORS.BUNDLE_SELECT, bundle.toLowerCase());
    await page.waitForTimeout(1200); // Reduced from 1500

    // Fill beneficiary
    logger.info(`Entering beneficiary: ${beneficiary}`);
    await page.fill(SELECTORS.BENEFICIARY_INPUT, beneficiary);
    await page.waitForTimeout(800); // Reduced from 1000

    // Get price
    const price = await page.locator('.woocommerce-variation-price .amount')
      .textContent()
      .catch(() => 'N/A');
    logger.info(`Price: ${price}`);

    // Add to cart
    logger.info('Adding to cart...');
    await page.click(SELECTORS.ADD_TO_CART);
    await page.waitForTimeout(2500); // Reduced from 3000

    // Wait for navigation or cart update
    await page.waitForLoadState('domcontentloaded', { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(1500);

    // Navigate to checkout
    let checkoutUrl = page.url();
    if (!checkoutUrl.includes('checkout')) {
      logger.info('Navigating to checkout page...');
      await page.goto(`${BASE_URL}${URLS.CHECKOUT}`, { 
        waitUntil: 'domcontentloaded', 
        timeout: TIMEOUTS.NAVIGATION 
      });
      await page.waitForTimeout(1500); // Reduced from 2000
      checkoutUrl = page.url();
    }

    logger.info('Now on checkout page');

    // Select wallet payment (with better error handling)
    await this.selectWalletPayment(page);

    // Place order
    return await this.placeOrder(page, bundle, beneficiary, price, checkoutUrl);
  }

  async selectWalletPayment(page) {
    logger.info('Selecting Wallet payment method...');
    
    try {
      // Wait for payment methods section with shorter timeout
      const paymentSection = await page.waitForSelector('#payment', { 
        timeout: 3000,
        state: 'visible'
      }).catch(() => null);

      if (!paymentSection) {
        logger.warn('Payment section not found, wallet might be pre-selected');
        return;
      }

      // Wait for wallet payment radio button
      await page.waitForSelector(SELECTORS.PAYMENT_WALLET, { 
        timeout: 3000,
        state: 'visible'
      });
      
      // Check if wallet is already selected
      const isWalletSelected = await page.locator(SELECTORS.PAYMENT_WALLET).isChecked();
      
      if (!isWalletSelected) {
        await page.click(SELECTORS.PAYMENT_WALLET);
        logger.info('✓ Wallet payment selected');
        await page.waitForTimeout(800); // Reduced from 1000
      } else {
        logger.info('✓ Wallet payment already selected');
      }
      
      // Get wallet balance
      const balance = await page.locator('label[for="payment_method_wallet"] strong .amount')
        .textContent()
        .catch(() => 'N/A');
      logger.info(`Current wallet balance: ${balance}`);
      
    } catch (error) {
      logger.error('Error selecting wallet payment:', error.message);
      // Take screenshot for debugging
      await page.screenshot({ 
        path: './screenshots/wallet-selection-error.png', 
        fullPage: true 
      }).catch(() => {});
      
      // Don't throw - wallet might be pre-selected by default
      logger.warn('Continuing despite wallet selection error - it may be pre-selected');
    }
  }

  async placeOrder(page, bundle, beneficiary, price, checkoutUrl) {
    logger.info('Clicking "Place order" button...');
    
    try {
      // Wait for place order button with shorter timeout
      await page.waitForSelector(SELECTORS.PLACE_ORDER, { 
        timeout: 4000,
        state: 'visible'
      });
      
      // Click place order
      await page.click(SELECTORS.PLACE_ORDER);
      logger.info('✓ Place order button clicked');
      
      // Wait for order processing (reduced timeout)
      await page.waitForTimeout(4000); // Reduced from 5000
      
      // Wait for navigation to order confirmation
      await page.waitForLoadState('domcontentloaded', { timeout: 12000 })
        .catch(() => logger.warn('Timeout waiting for order confirmation page'));
      
      await page.waitForTimeout(1500);
      
      const finalUrl = page.url();
      logger.info(`Order placed, current page: ${finalUrl}`);
      
      // Check if order was successful
      const orderReceived = finalUrl.includes('order-received');
      const hasError = await page.locator('.woocommerce-error, .woocommerce-notice--error')
        .isVisible({ timeout: 2000 })
        .catch(() => false);
      
      if (hasError) {
        const errorMessage = await page.locator('.woocommerce-error, .woocommerce-notice--error')
          .textContent()
          .catch(() => 'Unknown error');
        
        logger.error(`Order failed: ${errorMessage}`);
        await page.screenshot({ 
          path: './screenshots/order-error.png', 
          fullPage: true 
        });
        
        return {
          success: false,
          message: 'Order placement failed',
          error: errorMessage,
          bundle: bundle.toUpperCase(),
          beneficiary,
          price,
          finalUrl,
          screenshot: 'order-error.png'
        };
      }

      let orderNumber = null;
      let orderStatus = 'unknown';
      
      if (orderReceived) {
        // Extract order number from URL
        const orderMatch = finalUrl.match(/order-received\/(\d+)/);
        if (orderMatch) {
          orderNumber = orderMatch[1];
        }
        orderStatus = 'success';
        logger.info(`✓ Order successful! Order #${orderNumber}`);
      } else {
        logger.warn('Order submitted but confirmation unclear');
        orderStatus = 'submitted';
      }

      await page.screenshot({ 
        path: './screenshots/order-confirmation.png', 
        fullPage: true 
      });

      return {
        success: true,
        message: orderStatus === 'success' ? 'Order placed successfully!' : 'Order submitted',
        orderNumber,
        orderStatus,
        bundle: bundle.toUpperCase(),
        beneficiary,
        price,
        checkoutUrl,
        finalUrl,
        screenshot: 'order-confirmation.png'
      };
      
    } catch (error) {
      logger.error('Error placing order:', error.message);
      await page.screenshot({ 
        path: './screenshots/checkout-error.png', 
        fullPage: true 
      }).catch(() => {});
      
      return {
        success: false,
        message: 'Failed to place order',
        error: error.message,
        bundle: bundle.toUpperCase(),
        beneficiary,
        price,
        checkoutUrl: page.url(),
        screenshot: 'checkout-error.png'
      };
    }
  }
}

module.exports = new PurchaseService();
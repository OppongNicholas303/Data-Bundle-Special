// require('dotenv').config();
// const express = require('express');
// const { chromium } = require('playwright');

// const app = express();
// const port = process.env.PORT || 3000;

// let browser;
// let context;
// let page;
// let isLoggedIn = false;

// async function launchAndLogin() {
//   if (isLoggedIn && browser && page) {
//     console.log('✓ Already logged in, reusing session');
//     console.log('Current page:', page.url());
//     return;
//   }

//   console.log('Launching browser and logging in...');

//   browser = await chromium.launch({
//     headless: false,
//     slowMo: 60,
//   });

//   context = await browser.newContext({
//     viewport: { width: 1280, height: 800 },
//     userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
//     locale: 'en-US',
//     timezoneId: 'Africa/Accra',
//   });

//   page = await context.newPage();

//   await page.goto('https://mydatagigs.com/login/', { waitUntil: 'networkidle' });

//   // Check if already logged in on the page (session still active)
//   await page.waitForTimeout(2000);
//   const alreadyLoggedInOnPage = await page.locator('.wppb-alert, text="You are currently logged in"').isVisible({ timeout: 3000 }).catch(() => false);
  
//   if (alreadyLoggedInOnPage) {
//     console.log('✓ Already logged in (session still active)');
//     isLoggedIn = true;
    
//     // Navigate to buy-data page
//     console.log('Navigating to buy-data page...');
//     try {
//       await page.locator('#menu-item-7155 a').click({ timeout: 5000 });
//       console.log('Clicked Buy Data menu item');
//       await page.waitForTimeout(3000);
//       await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//     } catch (error) {
//       console.log('Menu click failed, using direct navigation');
//       await page.goto('https://mydatagigs.com/buy-data/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       }).catch(e => console.log('Navigation error:', e.message));
//       await page.waitForTimeout(2000);
//     }
    
//     console.log('✓ Now on buy-data page:', page.url());
    
//     // Navigate to MTN Data Bundle Special product page
//     console.log('Navigating to MTN Data Bundle Special...');
    
//     try {
//       const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
//       const linkExists = await productLink.isVisible({ timeout: 5000 }).catch(() => false);
      
//       if (linkExists) {
//         await productLink.click();
//         console.log('Clicked MTN Data Bundle Special product link');
//         await page.waitForTimeout(3000);
//         await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//       } else {
//         console.log('Product link not found, using direct navigation');
//         await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
//           waitUntil: 'domcontentloaded',
//           timeout: 15000 
//         });
//         await page.waitForTimeout(2000);
//       }
      
//       console.log('✓ Now on product page:', page.url());
//     } catch (error) {
//       console.log('Error navigating to product page:', error.message);
//     }
    
//     await context.storageState({ path: 'mydatagigs-state.json' });
//     return;
//   }

//   // Wait for form to be visible
//   await page.waitForSelector('input[type="text"], input[type="email"]', { timeout: 15000 });

//   // Fill credentials with human-like delays
//   console.log('Filling login credentials...');
//   await page.fill('input[type="text"], input[type="email"], input[name="username"], input[name="email"]', process.env.MYDATAGIGS_EMAIL);
//   await page.waitForTimeout(800 + Math.random() * 600);

//   await page.fill('input[type="password"]', process.env.MYDATAGIGS_PASSWORD);
//   await page.waitForTimeout(1000 + Math.random() * 800);

//   // Click submit button
//   console.log('Clicking login button...');
//   await page.click('button[type="submit"], button:has-text("Login"), button:has-text("Sign in"), input[type="submit"]').catch(() => 
//     page.click('a:has-text("Login")')
//   );

//   // Wait for navigation after login
//   console.log('Waiting for navigation after login...');
//   await page.waitForTimeout(4000);
  
//   const alreadyLoggedIn = await page.locator('.wppb-alert').isVisible({ timeout: 2000 }).catch(() => false);
  
//   if (alreadyLoggedIn) {
//     console.log('✓ Already logged in!');
//   } else {
//     console.log('Checking login status...');
//   }
  
//   const currentUrl = page.url();
//   console.log('Current URL:', currentUrl);

//   // Check if we're logged in - look for menu items that only appear when logged in
//   const buyDataMenuExists = await page.locator('#menu-item-7155').isVisible({ timeout: 5000 }).catch(() => false);
//   const myAccountExists = await page.locator('text="MY ACCOUNT", text="My account"').isVisible({ timeout: 3000 }).catch(() => false);
//   const logoutExists = await page.locator('#menu-item-4802, a:has-text("Logout")').isVisible({ timeout: 3000 }).catch(() => false);
//   const alreadyLoggedInMsg = await page.locator('.wppb-alert').isVisible({ timeout: 2000 }).catch(() => false);
  
//   console.log('Login checks:', { 
//     buyDataMenuExists, 
//     myAccountExists, 
//     logoutExists, 
//     alreadyLoggedInMsg,
//     currentUrl 
//   });

//   const isLoginSuccessful = buyDataMenuExists || myAccountExists || logoutExists || alreadyLoggedInMsg;

//   if (!isLoginSuccessful) {
//     console.log('Login appears to have FAILED');
//     await page.screenshot({ path: 'login-failed.png', fullPage: true });
//     const pageContent = await page.content();
//     console.log('Page title:', await page.title());
//     throw new Error('Login failed - could not verify successful login');
//   }

//   console.log('✓ Login successful!');

//   // Navigate to buy-data page after login
//   console.log('Navigating to buy-data page...');
  
//   try {
//     // Try clicking the menu item first (more natural)
//     await page.locator('#menu-item-7155 a').click({ timeout: 5000 });
//     console.log('Clicked Buy Data menu item');
//     await page.waitForTimeout(3000);
//     await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//   } catch (error) {
//     // Fallback to direct navigation
//     console.log('Menu click failed, using direct navigation');
//     await page.goto('https://mydatagigs.com/buy-data/', { 
//       waitUntil: 'domcontentloaded',
//       timeout: 15000 
//     }).catch(e => console.log('Navigation error:', e.message));
//     await page.waitForTimeout(2000);
//   }
  
//   const finalUrl = page.url();
//   console.log('✓ Now on buy-data page:', finalUrl);
  
//   // Verify we're on the buy-data page
//   if (!finalUrl.includes('buy-data')) {
//     console.log('Warning: Not on buy-data page, current URL:', finalUrl);
//   }

//   // Navigate to MTN Data Bundle Special product page
//   console.log('Navigating to MTN Data Bundle Special...');
  
//   try {
//     // Try clicking the product link first
//     const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
//     const linkExists = await productLink.isVisible({ timeout: 5000 }).catch(() => false);
    
//     if (linkExists) {
//       await productLink.click();
//       console.log('Clicked MTN Data Bundle Special product link');
//       await page.waitForTimeout(3000);
//       await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//     } else {
//       // Fallback to direct navigation
//       console.log('Product link not found, using direct navigation');
//       await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       });
//       await page.waitForTimeout(2000);
//     }
    
//     const productUrl = page.url();
//     console.log('✓ Now on product page:', productUrl);
    
//     // Verify we're on the product page
//     if (!productUrl.includes('mtn-data-bundle-special')) {
//       console.log('Warning: Not on MTN product page, current URL:', productUrl);
//     }
//   } catch (error) {
//     console.log('Error navigating to product page:', error.message);
//   }

//   // Save authentication state for future use
//   await context.storageState({ path: 'mydatagigs-state.json' });

//   isLoggedIn = true;
// }

// // Helper to ensure we're logged in before any operation
// async function ensureLoggedIn() {
//   if (!isLoggedIn) {
//     await launchAndLogin();
//   }
// }

// app.get('/status', (req, res) => {
//   res.json({ 
//     loggedIn: isLoggedIn,
//     currentUrl: page ? page.url() : null
//   });
// });

// app.get('/login', async (req, res) => {
//   try {
//     await launchAndLogin();
//     res.json({ success: true, message: 'Logged in', url: page.url() });
//   } catch (err) {
//     res.status(500).json({ success: false, error: err.message });
//   }
// });

// // Navigate to any authenticated page
// app.get('/navigate', async (req, res) => {
//   const { url } = req.query;

//   if (!url) {
//     return res.status(400).json({ error: 'Missing url parameter' });
//   }

//   try {
//     await ensureLoggedIn();
    
//     await page.goto(url, { waitUntil: 'networkidle' });
//     const content = await page.content();
//     const currentUrl = page.url();

//     res.json({ 
//       success: true, 
//       url: currentUrl,
//       title: await page.title(),
//       contentLength: content.length
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Get current page content
// app.get('/content', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     const content = await page.content();
//     res.send(content);
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Dashboard endpoint (example)
// app.get('/dashboard', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     await page.goto('https://mydatagigs.com/dashboard/', { waitUntil: 'networkidle' });
//     const content = await page.content();
    
//     res.json({
//       success: true,
//       url: page.url(),
//       title: await page.title(),
//       html: content
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Extract data from current page (example)
// app.get('/extract', async (req, res) => {
//   const { selector } = req.query;

//   try {
//     await ensureLoggedIn();
    
//     if (!selector) {
//       return res.status(400).json({ error: 'Missing selector parameter' });
//     }

//     const elements = await page.locator(selector).allTextContents();
    
//     res.json({ 
//       success: true, 
//       selector,
//       count: elements.length,
//       data: elements 
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Screenshot current page
// app.get('/screenshot', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     const screenshot = await page.screenshot({ fullPage: true });
//     res.set('Content-Type', 'image/png');
//     res.send(screenshot);
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// async function shutdown() {
//   console.log('Shutting down...');
//   if (browser) await browser.close();
//   process.exit(0);
// }

// process.on('SIGINT', shutdown);
// process.on('SIGTERM', shutdown);

// app.listen(port, async () => {
//   console.log(`Server running on http://localhost:${port}`);
//   console.log(`
// Available endpoints:
//   GET /login              - Manually trigger login
//   GET /status             - Check login status
//   GET /navigate?url=...   - Navigate to any URL (auto-login)
//   GET /dashboard          - Go to dashboard
//   GET /content            - Get current page HTML
//   GET /extract?selector=... - Extract elements by CSS selector
//   GET /screenshot         - Get screenshot of current page
//   `);
  
//   // Optional: Auto-login on startup
//   // await launchAndLogin().catch(console.error);
// });








// require('dotenv').config();
// const express = require('express');
// const { chromium } = require('playwright');

// const app = express();
// const port = process.env.PORT || 3000;

// let browser;
// let context;
// let page;
// let isLoggedIn = false;

// async function launchAndLogin() {
//   if (isLoggedIn && browser && page) {
//     console.log('✓ Already logged in, reusing session');
//     console.log('Current page:', page.url());
//     return;
//   }

//   console.log('Launching browser and logging in...');

//   browser = await chromium.launch({
//     headless: false,
//     slowMo: 60,
//   });

//   context = await browser.newContext({
//     viewport: { width: 1280, height: 800 },
//     userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
//     locale: 'en-US',
//     timezoneId: 'Africa/Accra',
//   });

//   page = await context.newPage();

//   await page.goto('https://mydatagigs.com/login/', { waitUntil: 'networkidle' });

//   // Check if already logged in on the page (session still active)
//   await page.waitForTimeout(2000);
//   const alreadyLoggedInOnPage = await page.locator('.wppb-alert, text="You are currently logged in"').isVisible({ timeout: 3000 }).catch(() => false);
  
//   if (alreadyLoggedInOnPage) {
//     console.log('✓ Already logged in (session still active)');
//     isLoggedIn = true;
    
//     // Navigate to buy-data page
//     console.log('Navigating to buy-data page...');
//     try {
//       await page.locator('#menu-item-7155 a').click({ timeout: 5000 });
//       console.log('Clicked Buy Data menu item');
//       await page.waitForTimeout(3000);
//       await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//     } catch (error) {
//       console.log('Menu click failed, using direct navigation');
//       await page.goto('https://mydatagigs.com/buy-data/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       }).catch(e => console.log('Navigation error:', e.message));
//       await page.waitForTimeout(2000);
//     }
    
//     console.log('✓ Now on buy-data page:', page.url());
    
//     // Navigate to MTN Data Bundle Special product page
//     console.log('Navigating to MTN Data Bundle Special...');
    
//     try {
//       const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
//       const linkExists = await productLink.isVisible({ timeout: 5000 }).catch(() => false);
      
//       if (linkExists) {
//         await productLink.click();
//         console.log('Clicked MTN Data Bundle Special product link');
//         await page.waitForTimeout(3000);
//         await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//       } else {
//         console.log('Product link not found, using direct navigation');
//         await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
//           waitUntil: 'domcontentloaded',
//           timeout: 15000 
//         });
//         await page.waitForTimeout(2000);
//       }
      
//       console.log('✓ Now on product page:', page.url());
//     } catch (error) {
//       console.log('Error navigating to product page:', error.message);
//     }
    
//     await context.storageState({ path: 'mydatagigs-state.json' });
//     return;
//   }

//   // Wait for form to be visible
//   await page.waitForSelector('input[type="text"], input[type="email"]', { timeout: 15000 });

//   // Fill credentials with human-like delays
//   console.log('Filling login credentials...');
//   await page.fill('input[type="text"], input[type="email"], input[name="username"], input[name="email"]', process.env.MYDATAGIGS_EMAIL);
//   await page.waitForTimeout(800 + Math.random() * 600);

//   await page.fill('input[type="password"]', process.env.MYDATAGIGS_PASSWORD);
//   await page.waitForTimeout(1000 + Math.random() * 800);

//   // Click submit button
//   console.log('Clicking login button...');
//   await page.click('button[type="submit"], button:has-text("Login"), button:has-text("Sign in"), input[type="submit"]').catch(() => 
//     page.click('a:has-text("Login")')
//   );

//   // Wait for navigation after login
//   console.log('Waiting for navigation after login...');
//   await page.waitForTimeout(4000);
  
//   const alreadyLoggedIn = await page.locator('.wppb-alert').isVisible({ timeout: 2000 }).catch(() => false);
  
//   if (alreadyLoggedIn) {
//     console.log('✓ Already logged in!');
//   } else {
//     console.log('Checking login status...');
//   }
  
//   const currentUrl = page.url();
//   console.log('Current URL:', currentUrl);

//   // Check if we're logged in - look for menu items that only appear when logged in
//   const buyDataMenuExists = await page.locator('#menu-item-7155').isVisible({ timeout: 5000 }).catch(() => false);
//   const myAccountExists = await page.locator('text="MY ACCOUNT", text="My account"').isVisible({ timeout: 3000 }).catch(() => false);
//   const logoutExists = await page.locator('#menu-item-4802, a:has-text("Logout")').isVisible({ timeout: 3000 }).catch(() => false);
//   const alreadyLoggedInMsg = await page.locator('.wppb-alert').isVisible({ timeout: 2000 }).catch(() => false);
  
//   console.log('Login checks:', { 
//     buyDataMenuExists, 
//     myAccountExists, 
//     logoutExists, 
//     alreadyLoggedInMsg,
//     currentUrl 
//   });

//   const isLoginSuccessful = buyDataMenuExists || myAccountExists || logoutExists || alreadyLoggedInMsg;

//   if (!isLoginSuccessful) {
//     console.log('Login appears to have FAILED');
//     await page.screenshot({ path: 'login-failed.png', fullPage: true });
//     const pageContent = await page.content();
//     console.log('Page title:', await page.title());
//     throw new Error('Login failed - could not verify successful login');
//   }

//   console.log('✓ Login successful!');

//   // Navigate to buy-data page after login
//   console.log('Navigating to buy-data page...');
  
//   try {
//     // Try clicking the menu item first (more natural)
//     await page.locator('#menu-item-7155 a').click({ timeout: 5000 });
//     console.log('Clicked Buy Data menu item');
//     await page.waitForTimeout(3000);
//     await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//   } catch (error) {
//     // Fallback to direct navigation
//     console.log('Menu click failed, using direct navigation');
//     await page.goto('https://mydatagigs.com/buy-data/', { 
//       waitUntil: 'domcontentloaded',
//       timeout: 15000 
//     }).catch(e => console.log('Navigation error:', e.message));
//     await page.waitForTimeout(2000);
//   }
  
//   const finalUrl = page.url();
//   console.log('✓ Now on buy-data page:', finalUrl);
  
//   // Verify we're on the buy-data page
//   if (!finalUrl.includes('buy-data')) {
//     console.log('Warning: Not on buy-data page, current URL:', finalUrl);
//   }

//   // Navigate to MTN Data Bundle Special product page
//   console.log('Navigating to MTN Data Bundle Special...');
  
//   try {
//     // Try clicking the product link first
//     const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
//     const linkExists = await productLink.isVisible({ timeout: 5000 }).catch(() => false);
    
//     if (linkExists) {
//       await productLink.click();
//       console.log('Clicked MTN Data Bundle Special product link');
//       await page.waitForTimeout(3000);
//       await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//     } else {
//       // Fallback to direct navigation
//       console.log('Product link not found, using direct navigation');
//       await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       });
//       await page.waitForTimeout(2000);
//     }
    
//     const productUrl = page.url();
//     console.log('✓ Now on product page:', productUrl);
    
//     // Verify we're on the product page
//     if (!productUrl.includes('mtn-data-bundle-special')) {
//       console.log('Warning: Not on MTN product page, current URL:', productUrl);
//     }
//   } catch (error) {
//     console.log('Error navigating to product page:', error.message);
//   }

//   // Save authentication state for future use
//   await context.storageState({ path: 'mydatagigs-state.json' });

//   isLoggedIn = true;
// }

// // Helper to ensure we're logged in before any operation
// async function ensureLoggedIn() {
//   if (!isLoggedIn) {
//     await launchAndLogin();
//   }
// }

// app.get('/status', (req, res) => {
//   res.json({ 
//     loggedIn: isLoggedIn,
//     currentUrl: page ? page.url() : null
//   });
// });

// app.get('/login', async (req, res) => {
//   try {
//     await launchAndLogin();
//     res.json({ success: true, message: 'Logged in', url: page.url() });
//   } catch (err) {
//     res.status(500).json({ success: false, error: err.message });
//   }
// });

// // Navigate to any authenticated page
// app.get('/navigate', async (req, res) => {
//   const { url } = req.query;

//   if (!url) {
//     return res.status(400).json({ error: 'Missing url parameter' });
//   }

//   try {
//     await ensureLoggedIn();
    
//     await page.goto(url, { waitUntil: 'networkidle' });
//     const content = await page.content();
//     const currentUrl = page.url();

//     res.json({ 
//       success: true, 
//       url: currentUrl,
//       title: await page.title(),
//       contentLength: content.length
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Get current page content
// app.get('/content', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     const content = await page.content();
//     res.send(content);
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Dashboard endpoint (example)
// app.get('/dashboard', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     await page.goto('https://mydatagigs.com/dashboard/', { waitUntil: 'networkidle' });
//     const content = await page.content();
    
//     res.json({
//       success: true,
//       url: page.url(),
//       title: await page.title(),
//       html: content
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Extract data from current page (example)
// app.get('/extract', async (req, res) => {
//   const { selector } = req.query;

//   try {
//     await ensureLoggedIn();
    
//     if (!selector) {
//       return res.status(400).json({ error: 'Missing selector parameter' });
//     }

//     const elements = await page.locator(selector).allTextContents();
    
//     res.json({ 
//       success: true, 
//       selector,
//       count: elements.length,
//       data: elements 
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Screenshot current page
// app.get('/screenshot', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     const screenshot = await page.screenshot({ fullPage: true });
//     res.set('Content-Type', 'image/png');
//     res.send(screenshot);
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Purchase MTN data bundle
// app.get('/purchase', async (req, res) => {
//   const { bundle, beneficiary } = req.query;

//   // Validate parameters
//   if (!bundle) {
//     return res.status(400).json({ error: 'Missing bundle parameter (e.g., 2gb, 5gb, 10gb)' });
//   }
//   if (!beneficiary) {
//     return res.status(400).json({ error: 'Missing beneficiary parameter (phone number)' });
//   }

//   // Validate beneficiary number (should be 10 digits)
//   if (!/^\d{10}$/.test(beneficiary)) {
//     return res.status(400).json({ error: 'Beneficiary number must be 10 digits' });
//   }

//   // Validate bundle package
//   const validBundles = ['1gb', '2gb', '3gb', '4gb', '5gb', '6gb', '8gb', '10gb', '15gb', '20gb', '25gb', '30gb', '40gb', '50gb', '100gb'];
//   if (!validBundles.includes(bundle.toLowerCase())) {
//     return res.status(400).json({ 
//       error: 'Invalid bundle package',
//       validBundles 
//     });
//   }

//   try {
//     await ensureLoggedIn();
    
//     console.log(`Processing purchase: ${bundle.toUpperCase()} for ${beneficiary}`);
    
//     // Navigate to product page if not already there
//     const currentUrl = page.url();
//     if (!currentUrl.includes('mtn-data-bundle-special')) {
//       console.log('Navigating to MTN product page...');
//       await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       });
//       await page.waitForTimeout(2000);
//     }

//     // Select data bundle package
//     console.log(`Selecting ${bundle.toUpperCase()} package...`);
//     await page.selectOption('#pa_data-bundle-packages', bundle.toLowerCase());
//     await page.waitForTimeout(1500); // Wait for price to update

//     // Fill beneficiary number
//     console.log(`Entering beneficiary number: ${beneficiary}...`);
//     await page.fill('input[name="beneficiary"]', beneficiary);
//     await page.waitForTimeout(1000);

//     // Get the price before adding to cart
//     const priceElement = await page.locator('.woocommerce-variation-price .amount').textContent().catch(() => 'N/A');
//     console.log(`Price: ${priceElement}`);

//     // Click add to cart button
//     console.log('Adding to cart...');
//     await page.click('button.single_add_to_cart_button');
//     await page.waitForTimeout(3000);

//     // Wait for cart update or navigation
//     await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//     await page.waitForTimeout(2000);

//     // Check if we're on checkout page or need to navigate
//     let checkoutUrl = page.url();
//     if (!checkoutUrl.includes('checkout')) {
//       console.log('Navigating to checkout page...');
//       await page.goto('https://mydatagigs.com/checkout/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       });
//       await page.waitForTimeout(2000);
//       checkoutUrl = page.url();
//     }

//     console.log(`✓ Now on checkout page: ${checkoutUrl}`);

//     // Take screenshot of checkout page
//     await page.screenshot({ path: 'checkout-page.png', fullPage: true });

//     res.json({ 
//       success: true,
//       message: 'Product added to cart and navigated to checkout',
//       bundle: bundle.toUpperCase(),
//       beneficiary,
//       price: priceElement,
//       checkoutUrl,
//       screenshot: 'checkout-page.png'
//     });

//   } catch (err) {
//     console.error('Purchase error:', err);
//     await page.screenshot({ path: 'purchase-error.png', fullPage: true });
//     res.status(500).json({ 
//       error: err.message,
//       screenshot: 'purchase-error.png'
//     });
//   }
// });

// async function shutdown() {
//   console.log('Shutting down...');
//   if (browser) await browser.close();
//   process.exit(0);
// }

// process.on('SIGINT', shutdown);
// process.on('SIGTERM', shutdown);

// app.listen(port, async () => {
//   console.log(`Server running on http://localhost:${port}`);
//   console.log(`
// Available endpoints:
//   GET /login                          - Manually trigger login
//   GET /status                         - Check login status
//   GET /navigate?url=...               - Navigate to any URL (auto-login)
//   GET /dashboard                      - Go to dashboard
//   GET /content                        - Get current page HTML
//   GET /extract?selector=...           - Extract elements by CSS selector
//   GET /screenshot                     - Get screenshot of current page
//   GET /purchase?bundle=2gb&beneficiary=0241234567 - Purchase MTN data bundle
  
// Purchase endpoint parameters:
//   - bundle: 1gb, 2gb, 3gb, 4gb, 5gb, 6gb, 8gb, 10gb, 15gb, 20gb, 25gb, 30gb, 40gb, 50gb, 100gb
//   - beneficiary: 10-digit phone number (e.g., 0241234567)
//   `);
  
//   // Optional: Auto-login on startup
//   // await launchAndLogin().catch(console.error);
// });



// require('dotenv').config();
// const express = require('express');
// const { chromium } = require('playwright');

// const app = express();
// const port = process.env.PORT || 3000;

// let browser;
// let context;
// let page;
// let isLoggedIn = false;

// async function launchAndLogin() {
//   if (isLoggedIn && browser && page) {
//     console.log('✓ Already logged in, reusing session');
//     console.log('Current page:', page.url());
//     return;
//   }

//   console.log('Launching browser and logging in...');

//   browser = await chromium.launch({
//     headless: false,
//     slowMo: 60,
//   });

//   context = await browser.newContext({
//     viewport: { width: 1280, height: 800 },
//     userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
//     locale: 'en-US',
//     timezoneId: 'Africa/Accra',
//   });

//   page = await context.newPage();

//   await page.goto('https://mydatagigs.com/login/', { waitUntil: 'networkidle' });

//   // Check if already logged in on the page (session still active)
//   await page.waitForTimeout(2000);
//   const alreadyLoggedInOnPage = await page.locator('.wppb-alert, text="You are currently logged in"').isVisible({ timeout: 3000 }).catch(() => false);
  
//   if (alreadyLoggedInOnPage) {
//     console.log('✓ Already logged in (session still active)');
//     isLoggedIn = true;
    
//     // Navigate to buy-data page
//     console.log('Navigating to buy-data page...');
//     try {
//       await page.locator('#menu-item-7155 a').click({ timeout: 5000 });
//       console.log('Clicked Buy Data menu item');
//       await page.waitForTimeout(3000);
//       await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//     } catch (error) {
//       console.log('Menu click failed, using direct navigation');
//       await page.goto('https://mydatagigs.com/buy-data/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       }).catch(e => console.log('Navigation error:', e.message));
//       await page.waitForTimeout(2000);
//     }
    
//     console.log('✓ Now on buy-data page:', page.url());
    
//     // Navigate to MTN Data Bundle Special product page
//     console.log('Navigating to MTN Data Bundle Special...');
    
//     try {
//       const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
//       const linkExists = await productLink.isVisible({ timeout: 5000 }).catch(() => false);
      
//       if (linkExists) {
//         await productLink.click();
//         console.log('Clicked MTN Data Bundle Special product link');
//         await page.waitForTimeout(3000);
//         await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//       } else {
//         console.log('Product link not found, using direct navigation');
//         await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
//           waitUntil: 'domcontentloaded',
//           timeout: 15000 
//         });
//         await page.waitForTimeout(2000);
//       }
      
//       console.log('✓ Now on product page:', page.url());
//     } catch (error) {
//       console.log('Error navigating to product page:', error.message);
//     }
    
//     await context.storageState({ path: 'mydatagigs-state.json' });
//     return;
//   }

//   // Wait for form to be visible
//   await page.waitForSelector('input[type="text"], input[type="email"]', { timeout: 15000 });

//   // Fill credentials with human-like delays
//   console.log('Filling login credentials...');
//   await page.fill('input[type="text"], input[type="email"], input[name="username"], input[name="email"]', process.env.MYDATAGIGS_EMAIL);
//   await page.waitForTimeout(800 + Math.random() * 600);

//   await page.fill('input[type="password"]', process.env.MYDATAGIGS_PASSWORD);
//   await page.waitForTimeout(1000 + Math.random() * 800);

//   // Click submit button
//   console.log('Clicking login button...');
//   await page.click('button[type="submit"], button:has-text("Login"), button:has-text("Sign in"), input[type="submit"]').catch(() => 
//     page.click('a:has-text("Login")')
//   );

//   // Wait for navigation after login
//   console.log('Waiting for navigation after login...');
//   await page.waitForTimeout(4000);
  
//   const alreadyLoggedIn = await page.locator('.wppb-alert').isVisible({ timeout: 2000 }).catch(() => false);
  
//   if (alreadyLoggedIn) {
//     console.log('✓ Already logged in!');
//   } else {
//     console.log('Checking login status...');
//   }
  
//   const currentUrl = page.url();
//   console.log('Current URL:', currentUrl);

//   // Check if we're logged in - look for menu items that only appear when logged in
//   const buyDataMenuExists = await page.locator('#menu-item-7155').isVisible({ timeout: 5000 }).catch(() => false);
//   const myAccountExists = await page.locator('text="MY ACCOUNT", text="My account"').isVisible({ timeout: 3000 }).catch(() => false);
//   const logoutExists = await page.locator('#menu-item-4802, a:has-text("Logout")').isVisible({ timeout: 3000 }).catch(() => false);
//   const alreadyLoggedInMsg = await page.locator('.wppb-alert').isVisible({ timeout: 2000 }).catch(() => false);
  
//   console.log('Login checks:', { 
//     buyDataMenuExists, 
//     myAccountExists, 
//     logoutExists, 
//     alreadyLoggedInMsg,
//     currentUrl 
//   });

//   const isLoginSuccessful = buyDataMenuExists || myAccountExists || logoutExists || alreadyLoggedInMsg;

//   if (!isLoginSuccessful) {
//     console.log('Login appears to have FAILED');
//     await page.screenshot({ path: 'login-failed.png', fullPage: true });
//     const pageContent = await page.content();
//     console.log('Page title:', await page.title());
//     throw new Error('Login failed - could not verify successful login');
//   }

//   console.log('✓ Login successful!');

//   // Navigate to buy-data page after login
//   console.log('Navigating to buy-data page...');
  
//   try {
//     // Try clicking the menu item first (more natural)
//     await page.locator('#menu-item-7155 a').click({ timeout: 5000 });
//     console.log('Clicked Buy Data menu item');
//     await page.waitForTimeout(3000);
//     await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//   } catch (error) {
//     // Fallback to direct navigation
//     console.log('Menu click failed, using direct navigation');
//     await page.goto('https://mydatagigs.com/buy-data/', { 
//       waitUntil: 'domcontentloaded',
//       timeout: 15000 
//     }).catch(e => console.log('Navigation error:', e.message));
//     await page.waitForTimeout(2000);
//   }
  
//   const finalUrl = page.url();
//   console.log('✓ Now on buy-data page:', finalUrl);
  
//   // Verify we're on the buy-data page
//   if (!finalUrl.includes('buy-data')) {
//     console.log('Warning: Not on buy-data page, current URL:', finalUrl);
//   }

//   // Navigate to MTN Data Bundle Special product page
//   console.log('Navigating to MTN Data Bundle Special...');
  
//   try {
//     // Try clicking the product link first
//     const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
//     const linkExists = await productLink.isVisible({ timeout: 5000 }).catch(() => false);
    
//     if (linkExists) {
//       await productLink.click();
//       console.log('Clicked MTN Data Bundle Special product link');
//       await page.waitForTimeout(3000);
//       await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//     } else {
//       // Fallback to direct navigation
//       console.log('Product link not found, using direct navigation');
//       await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       });
//       await page.waitForTimeout(2000);
//     }
    
//     const productUrl = page.url();
//     console.log('✓ Now on product page:', productUrl);
    
//     // Verify we're on the product page
//     if (!productUrl.includes('mtn-data-bundle-special')) {
//       console.log('Warning: Not on MTN product page, current URL:', productUrl);
//     }
//   } catch (error) {
//     console.log('Error navigating to product page:', error.message);
//   }

//   // Save authentication state for future use
//   await context.storageState({ path: 'mydatagigs-state.json' });

//   isLoggedIn = true;
// }

// // Helper to ensure we're logged in before any operation
// async function ensureLoggedIn() {
//   if (!isLoggedIn) {
//     await launchAndLogin();
//   }
// }

// app.get('/status', (req, res) => {
//   res.json({ 
//     loggedIn: isLoggedIn,
//     currentUrl: page ? page.url() : null
//   });
// });

// app.get('/login', async (req, res) => {
//   try {
//     await launchAndLogin();
//     res.json({ success: true, message: 'Logged in', url: page.url() });
//   } catch (err) {
//     res.status(500).json({ success: false, error: err.message });
//   }
// });

// // Navigate to any authenticated page
// app.get('/navigate', async (req, res) => {
//   const { url } = req.query;

//   if (!url) {
//     return res.status(400).json({ error: 'Missing url parameter' });
//   }

//   try {
//     await ensureLoggedIn();
    
//     await page.goto(url, { waitUntil: 'networkidle' });
//     const content = await page.content();
//     const currentUrl = page.url();

//     res.json({ 
//       success: true, 
//       url: currentUrl,
//       title: await page.title(),
//       contentLength: content.length
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Get current page content
// app.get('/content', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     const content = await page.content();
//     res.send(content);
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Dashboard endpoint (example)
// app.get('/dashboard', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     await page.goto('https://mydatagigs.com/dashboard/', { waitUntil: 'networkidle' });
//     const content = await page.content();
    
//     res.json({
//       success: true,
//       url: page.url(),
//       title: await page.title(),
//       html: content
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Extract data from current page (example)
// app.get('/extract', async (req, res) => {
//   const { selector } = req.query;

//   try {
//     await ensureLoggedIn();
    
//     if (!selector) {
//       return res.status(400).json({ error: 'Missing selector parameter' });
//     }

//     const elements = await page.locator(selector).allTextContents();
    
//     res.json({ 
//       success: true, 
//       selector,
//       count: elements.length,
//       data: elements 
//     });
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Screenshot current page
// app.get('/screenshot', async (req, res) => {
//   try {
//     await ensureLoggedIn();
    
//     const screenshot = await page.screenshot({ fullPage: true });
//     res.set('Content-Type', 'image/png');
//     res.send(screenshot);
//   } catch (err) {
//     res.status(500).json({ error: err.message });
//   }
// });

// // Purchase MTN data bundle
// app.get('/purchase', async (req, res) => {
//   const { bundle, beneficiary } = req.query;

//   // Validate parameters
//   if (!bundle) {
//     return res.status(400).json({ error: 'Missing bundle parameter (e.g., 2gb, 5gb, 10gb)' });
//   }
//   if (!beneficiary) {
//     return res.status(400).json({ error: 'Missing beneficiary parameter (phone number)' });
//   }

//   // Validate beneficiary number (should be 10 digits)
//   if (!/^\d{10}$/.test(beneficiary)) {
//     return res.status(400).json({ error: 'Beneficiary number must be 10 digits' });
//   }

//   // Validate bundle package
//   const validBundles = ['1gb', '2gb', '3gb', '4gb', '5gb', '6gb', '8gb', '10gb', '15gb', '20gb', '25gb', '30gb', '40gb', '50gb', '100gb'];
//   if (!validBundles.includes(bundle.toLowerCase())) {
//     return res.status(400).json({ 
//       error: 'Invalid bundle package',
//       validBundles 
//     });
//   }

//   try {
//     await ensureLoggedIn();
    
//     console.log(`Processing purchase: ${bundle.toUpperCase()} for ${beneficiary}`);
    
//     // Navigate to product page if not already there
//     const currentUrl = page.url();
//     if (!currentUrl.includes('mtn-data-bundle-special')) {
//       console.log('Navigating to MTN product page...');
//       await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       });
//       await page.waitForTimeout(2000);
//     }

//     // Select data bundle package
//     console.log(`Selecting ${bundle.toUpperCase()} package...`);
//     await page.selectOption('#pa_data-bundle-packages', bundle.toLowerCase());
//     await page.waitForTimeout(1500); // Wait for price to update

//     // Fill beneficiary number
//     console.log(`Entering beneficiary number: ${beneficiary}...`);
//     await page.fill('input[name="beneficiary"]', beneficiary);
//     await page.waitForTimeout(1000);

//     // Get the price before adding to cart
//     const priceElement = await page.locator('.woocommerce-variation-price .amount').textContent().catch(() => 'N/A');
//     console.log(`Price: ${priceElement}`);

//     // Click add to cart button
//     console.log('Adding to cart...');
//     await page.click('button.single_add_to_cart_button');
//     await page.waitForTimeout(3000);

//     // Wait for cart update or navigation
//     await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
//     await page.waitForTimeout(2000);

//     // Check if we're on checkout page or need to navigate
//     let checkoutUrl = page.url();
//     if (!checkoutUrl.includes('checkout')) {
//       console.log('Navigating to checkout page...');
//       await page.goto('https://mydatagigs.com/checkout/', { 
//         waitUntil: 'domcontentloaded',
//         timeout: 15000 
//       });
//       await page.waitForTimeout(2000);
//       checkoutUrl = page.url();
//     }

//     console.log(`✓ Now on checkout page: ${checkoutUrl}`);

//     // Click "Place order" button
//     console.log('Clicking "Place order" button...');
    
//     try {
//       // Wait for the place order button to be visible
//       await page.waitForSelector('#place_order', { timeout: 5000 });
      
//       // Click the place order button
//       await page.click('#place_order');
//       console.log('Place order button clicked');
      
//       // Wait for order processing
//       await page.waitForTimeout(5000);
      
//       // Wait for navigation to order confirmation or error
//       await page.waitForLoadState('domcontentloaded', { timeout: 15000 }).catch(() => {});
//       await page.waitForTimeout(2000);
      
//       const finalUrl = page.url();
//       console.log(`✓ Order placed, current page: ${finalUrl}`);
      
//       // Check if order was successful
//       const orderReceived = finalUrl.includes('order-received');
//       const hasError = await page.locator('.woocommerce-error, .woocommerce-notice--error').isVisible({ timeout: 3000 }).catch(() => false);
      
//       let orderNumber = null;
//       let orderStatus = 'unknown';
      
//       if (orderReceived) {
//         // Extract order number from URL or page
//         const orderMatch = finalUrl.match(/order-received\/(\d+)/);
//         if (orderMatch) {
//           orderNumber = orderMatch[1];
//         }
//         orderStatus = 'success';
//         console.log(`✓ Order successful! Order #${orderNumber}`);
//       } else if (hasError) {
//         const errorMessage = await page.locator('.woocommerce-error, .woocommerce-notice--error').textContent().catch(() => 'Unknown error');
//         orderStatus = 'failed';
//         console.log(`✗ Order failed: ${errorMessage}`);
        
//         // Take screenshot of error
//         await page.screenshot({ path: 'order-error.png', fullPage: true });
        
//         return res.json({
//           success: false,
//           message: 'Order placement failed',
//           error: errorMessage,
//           bundle: bundle.toUpperCase(),
//           beneficiary,
//           price: priceElement,
//           finalUrl,
//           screenshot: 'order-error.png'
//         });
//       }
      
//       // Take screenshot of final page
//       await page.screenshot({ path: 'order-confirmation.png', fullPage: true });
      
//       res.json({ 
//         success: true,
//         message: orderStatus === 'success' ? 'Order placed successfully!' : 'Order submitted',
//         orderNumber,
//         orderStatus,
//         bundle: bundle.toUpperCase(),
//         beneficiary,
//         price: priceElement,
//         checkoutUrl,
//         finalUrl,
//         screenshot: 'order-confirmation.png'
//       });
      
//     } catch (placeOrderError) {
//       console.error('Error clicking place order:', placeOrderError.message);
      
//       // Take screenshot of checkout page
//       await page.screenshot({ path: 'checkout-page.png', fullPage: true });
      
//       res.json({ 
//         success: false,
//         message: 'Failed to place order',
//         error: placeOrderError.message,
//         bundle: bundle.toUpperCase(),
//         beneficiary,
//         price: priceElement,
//         checkoutUrl,
//         screenshot: 'checkout-page.png'
//       });
//     }

//   } catch (err) {
//     console.error('Purchase error:', err);
//     await page.screenshot({ path: 'purchase-error.png', fullPage: true });
//     res.status(500).json({ 
//       error: err.message,
//       screenshot: 'purchase-error.png'
//     });
//   }
// });

// async function shutdown() {
//   console.log('Shutting down...');
//   if (browser) await browser.close();
//   process.exit(0);
// }

// process.on('SIGINT', shutdown);
// process.on('SIGTERM', shutdown);

// app.listen(port, async () => {
//   console.log(`Server running on http://localhost:${port}`);
//   console.log(`
// Available endpoints:
//   GET /login                          - Manually trigger login
//   GET /status                         - Check login status
//   GET /navigate?url=...               - Navigate to any URL (auto-login)
//   GET /dashboard                      - Go to dashboard
//   GET /content                        - Get current page HTML
//   GET /extract?selector=...           - Extract elements by CSS selector
//   GET /screenshot                     - Get screenshot of current page
//   GET /purchase?bundle=2gb&beneficiary=0241234567 - Purchase MTN data bundle
  
// Purchase endpoint parameters:
//   - bundle: 1gb, 2gb, 3gb, 4gb, 5gb, 6gb, 8gb, 10gb, 15gb, 20gb, 25gb, 30gb, 40gb, 50gb, 100gb
//   - beneficiary: 10-digit phone number (e.g., 0241234567)
//   `);
  
//   // Optional: Auto-login on startup
//   // await launchAndLogin().catch(console.error);
// });





require('dotenv').config();
const express = require('express');
const { chromium } = require('playwright');

const app = express();
const port = process.env.PORT || 3000;

let browser;
let context;
let page;
let isLoggedIn = false;

async function launchAndLogin() {
  if (isLoggedIn && browser && page) {
    console.log('✓ Already logged in, reusing session');
    console.log('Current page:', page.url());
    return;
  }

  console.log('Launching browser and logging in...');

  browser = await chromium.launch({
    headless: false,
    slowMo: 60,
  });

  context = await browser.newContext({
    viewport: { width: 1280, height: 800 },
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    locale: 'en-US',
    timezoneId: 'Africa/Accra',
  });

  page = await context.newPage();

  await page.goto('https://mydatagigs.com/login/', { waitUntil: 'networkidle' });

  // Check if already logged in on the page (session still active)
  await page.waitForTimeout(2000);
  const alreadyLoggedInOnPage = await page.locator('.wppb-alert, text="You are currently logged in"').isVisible({ timeout: 3000 }).catch(() => false);
  
  if (alreadyLoggedInOnPage) {
    console.log('✓ Already logged in (session still active)');
    isLoggedIn = true;
    
    // Navigate to buy-data page
    console.log('Navigating to buy-data page...');
    try {
      await page.locator('#menu-item-7155 a').click({ timeout: 5000 });
      console.log('Clicked Buy Data menu item');
      await page.waitForTimeout(3000);
      await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
    } catch (error) {
      console.log('Menu click failed, using direct navigation');
      await page.goto('https://mydatagigs.com/buy-data/', { 
        waitUntil: 'domcontentloaded',
        timeout: 15000 
      }).catch(e => console.log('Navigation error:', e.message));
      await page.waitForTimeout(2000);
    }
    
    console.log('✓ Now on buy-data page:', page.url());
    
    // Navigate to MTN Data Bundle Special product page
    console.log('Navigating to MTN Data Bundle Special...');
    
    try {
      const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
      const linkExists = await productLink.isVisible({ timeout: 5000 }).catch(() => false);
      
      if (linkExists) {
        await productLink.click();
        console.log('Clicked MTN Data Bundle Special product link');
        await page.waitForTimeout(3000);
        await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
      } else {
        console.log('Product link not found, using direct navigation');
        await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
          waitUntil: 'domcontentloaded',
          timeout: 15000 
        });
        await page.waitForTimeout(2000);
      }
      
      console.log('✓ Now on product page:', page.url());
    } catch (error) {
      console.log('Error navigating to product page:', error.message);
    }
    
    await context.storageState({ path: 'mydatagigs-state.json' });
    return;
  }

  // Wait for form to be visible
  await page.waitForSelector('input[type="text"], input[type="email"]', { timeout: 15000 });

  // Fill credentials with human-like delays
  console.log('Filling login credentials...');
  await page.fill('input[type="text"], input[type="email"], input[name="username"], input[name="email"]', process.env.MYDATAGIGS_EMAIL);
  await page.waitForTimeout(800 + Math.random() * 600);

  await page.fill('input[type="password"]', process.env.MYDATAGIGS_PASSWORD);
  await page.waitForTimeout(1000 + Math.random() * 800);

  // Click submit button
  console.log('Clicking login button...');
  await page.click('button[type="submit"], button:has-text("Login"), button:has-text("Sign in"), input[type="submit"]').catch(() => 
    page.click('a:has-text("Login")')
  );

  // Wait for navigation after login
  console.log('Waiting for navigation after login...');
  await page.waitForTimeout(4000);
  
  const alreadyLoggedIn = await page.locator('.wppb-alert').isVisible({ timeout: 2000 }).catch(() => false);
  
  if (alreadyLoggedIn) {
    console.log('✓ Already logged in!');
  } else {
    console.log('Checking login status...');
  }
  
  const currentUrl = page.url();
  console.log('Current URL:', currentUrl);

  // Check if we're logged in - look for menu items that only appear when logged in
  const buyDataMenuExists = await page.locator('#menu-item-7155').isVisible({ timeout: 5000 }).catch(() => false);
  const myAccountExists = await page.locator('text="MY ACCOUNT", text="My account"').isVisible({ timeout: 3000 }).catch(() => false);
  const logoutExists = await page.locator('#menu-item-4802, a:has-text("Logout")').isVisible({ timeout: 3000 }).catch(() => false);
  const alreadyLoggedInMsg = await page.locator('.wppb-alert').isVisible({ timeout: 2000 }).catch(() => false);
  
  console.log('Login checks:', { 
    buyDataMenuExists, 
    myAccountExists, 
    logoutExists, 
    alreadyLoggedInMsg,
    currentUrl 
  });

  const isLoginSuccessful = buyDataMenuExists || myAccountExists || logoutExists || alreadyLoggedInMsg;

  if (!isLoginSuccessful) {
    console.log('Login appears to have FAILED');
    await page.screenshot({ path: 'login-failed.png', fullPage: true });
    const pageContent = await page.content();
    console.log('Page title:', await page.title());
    throw new Error('Login failed - could not verify successful login');
  }

  console.log('✓ Login successful!');

  // Navigate to buy-data page after login
  console.log('Navigating to buy-data page...');
  
  try {
    // Try clicking the menu item first (more natural)
    await page.locator('#menu-item-7155 a').click({ timeout: 5000 });
    console.log('Clicked Buy Data menu item');
    await page.waitForTimeout(3000);
    await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
  } catch (error) {
    // Fallback to direct navigation
    console.log('Menu click failed, using direct navigation');
    await page.goto('https://mydatagigs.com/buy-data/', { 
      waitUntil: 'domcontentloaded',
      timeout: 15000 
    }).catch(e => console.log('Navigation error:', e.message));
    await page.waitForTimeout(2000);
  }
  
  const finalUrl = page.url();
  console.log('✓ Now on buy-data page:', finalUrl);
  
  // Verify we're on the buy-data page
  if (!finalUrl.includes('buy-data')) {
    console.log('Warning: Not on buy-data page, current URL:', finalUrl);
  }

  // Navigate to MTN Data Bundle Special product page
  console.log('Navigating to MTN Data Bundle Special...');
  
  try {
    // Try clicking the product link first
    const productLink = await page.locator('a[href*="mtn-data-bundle-special"]').first();
    const linkExists = await productLink.isVisible({ timeout: 5000 }).catch(() => false);
    
    if (linkExists) {
      await productLink.click();
      console.log('Clicked MTN Data Bundle Special product link');
      await page.waitForTimeout(3000);
      await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
    } else {
      // Fallback to direct navigation
      console.log('Product link not found, using direct navigation');
      await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
        waitUntil: 'domcontentloaded',
        timeout: 15000 
      });
      await page.waitForTimeout(2000);
    }
    
    const productUrl = page.url();
    console.log('✓ Now on product page:', productUrl);
    
    // Verify we're on the product page
    if (!productUrl.includes('mtn-data-bundle-special')) {
      console.log('Warning: Not on MTN product page, current URL:', productUrl);
    }
  } catch (error) {
    console.log('Error navigating to product page:', error.message);
  }

  // Save authentication state for future use
  await context.storageState({ path: 'mydatagigs-state.json' });

  isLoggedIn = true;
}

// Helper to ensure we're logged in before any operation
async function ensureLoggedIn() {
  if (!isLoggedIn) {
    await launchAndLogin();
  }
}

app.get('/status', (req, res) => {
  res.json({ 
    loggedIn: isLoggedIn,
    currentUrl: page ? page.url() : null
  });
});

app.get('/login', async (req, res) => {
  try {
    await launchAndLogin();
    res.json({ success: true, message: 'Logged in', url: page.url() });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Navigate to any authenticated page
app.get('/navigate', async (req, res) => {
  const { url } = req.query;

  if (!url) {
    return res.status(400).json({ error: 'Missing url parameter' });
  }

  try {
    await ensureLoggedIn();
    
    await page.goto(url, { waitUntil: 'networkidle' });
    const content = await page.content();
    const currentUrl = page.url();

    res.json({ 
      success: true, 
      url: currentUrl,
      title: await page.title(),
      contentLength: content.length
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get current page content
app.get('/content', async (req, res) => {
  try {
    await ensureLoggedIn();
    
    const content = await page.content();
    res.send(content);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Dashboard endpoint (example)
app.get('/dashboard', async (req, res) => {
  try {
    await ensureLoggedIn();
    
    await page.goto('https://mydatagigs.com/dashboard/', { waitUntil: 'networkidle' });
    const content = await page.content();
    
    res.json({
      success: true,
      url: page.url(),
      title: await page.title(),
      html: content
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Extract data from current page (example)
app.get('/extract', async (req, res) => {
  const { selector } = req.query;

  try {
    await ensureLoggedIn();
    
    if (!selector) {
      return res.status(400).json({ error: 'Missing selector parameter' });
    }

    const elements = await page.locator(selector).allTextContents();
    
    res.json({ 
      success: true, 
      selector,
      count: elements.length,
      data: elements 
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Screenshot current page
app.get('/screenshot', async (req, res) => {
  try {
    await ensureLoggedIn();
    
    const screenshot = await page.screenshot({ fullPage: true });
    res.set('Content-Type', 'image/png');
    res.send(screenshot);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Purchase MTN data bundle
app.get('/purchase', async (req, res) => {
  const { bundle, beneficiary } = req.query;

  // Validate parameters
  if (!bundle) {
    return res.status(400).json({ error: 'Missing bundle parameter (e.g., 2gb, 5gb, 10gb)' });
  }
  if (!beneficiary) {
    return res.status(400).json({ error: 'Missing beneficiary parameter (phone number)' });
  }

  // Validate beneficiary number (should be 10 digits)
  if (!/^\d{10}$/.test(beneficiary)) {
    return res.status(400).json({ error: 'Beneficiary number must be 10 digits' });
  }

  // Validate bundle package
  const validBundles = ['1gb', '2gb', '3gb', '4gb', '5gb', '6gb', '8gb', '10gb', '15gb', '20gb', '25gb', '30gb', '40gb', '50gb', '100gb'];
  if (!validBundles.includes(bundle.toLowerCase())) {
    return res.status(400).json({ 
      error: 'Invalid bundle package',
      validBundles 
    });
  }

  try {
    await ensureLoggedIn();
    
    console.log(`Processing purchase: ${bundle.toUpperCase()} for ${beneficiary}`);
    
    // Navigate to product page if not already there
    const currentUrl = page.url();
    if (!currentUrl.includes('mtn-data-bundle-special')) {
      console.log('Navigating to MTN product page...');
      await page.goto('https://mydatagigs.com/product/mtn-data-bundle-special/', { 
        waitUntil: 'domcontentloaded',
        timeout: 15000 
      });
      await page.waitForTimeout(2000);
    }

    // Select data bundle package
    console.log(`Selecting ${bundle.toUpperCase()} package...`);
    await page.selectOption('#pa_data-bundle-packages', bundle.toLowerCase());
    await page.waitForTimeout(1500); // Wait for price to update

    // Fill beneficiary number
    console.log(`Entering beneficiary number: ${beneficiary}...`);
    await page.fill('input[name="beneficiary"]', beneficiary);
    await page.waitForTimeout(1000);

    // Get the price before adding to cart
    const priceElement = await page.locator('.woocommerce-variation-price .amount').textContent().catch(() => 'N/A');
    console.log(`Price: ${priceElement}`);

    // Click add to cart button
    console.log('Adding to cart...');
    await page.click('button.single_add_to_cart_button');
    await page.waitForTimeout(3000);

    // Wait for cart update or navigation
    await page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(2000);

    // Check if we're on checkout page or need to navigate
    let checkoutUrl = page.url();
    if (!checkoutUrl.includes('checkout')) {
      console.log('Navigating to checkout page...');
      await page.goto('https://mydatagigs.com/checkout/', { 
        waitUntil: 'domcontentloaded',
        timeout: 15000 
      });
      await page.waitForTimeout(2000);
      checkoutUrl = page.url();
    }

    console.log(`✓ Now on checkout page: ${checkoutUrl}`);

    // Select Wallet payment method
    console.log('Selecting Wallet payment method...');
    
    try {
      // Wait for payment methods to be visible
      await page.waitForSelector('#payment_method_wallet', { timeout: 5000 });
      
      // Check if wallet payment is already selected
      const isWalletSelected = await page.locator('#payment_method_wallet').isChecked();
      
      if (!isWalletSelected) {
        // Click the wallet payment radio button
        await page.click('#payment_method_wallet');
        console.log('Wallet payment method selected');
        await page.waitForTimeout(1000);
      } else {
        console.log('Wallet payment already selected');
      }
      
      // Get wallet balance
      const walletBalance = await page.locator('label[for="payment_method_wallet"] strong .amount').textContent().catch(() => 'N/A');
      console.log(`Current wallet balance: ${walletBalance}`);
      
    } catch (walletError) {
      console.error('Error selecting wallet payment:', walletError.message);
      // Continue anyway as wallet might be pre-selected
    }

    // Click "Place order" button
    console.log('Clicking "Place order" button...');
    
    try {
      // Wait for the place order button to be visible
      await page.waitForSelector('#place_order', { timeout: 5000 });
      
      // Click the place order button
      await page.click('#place_order');
      console.log('Place order button clicked');
      
      // Wait for order processing
      await page.waitForTimeout(5000);
      
      // Wait for navigation to order confirmation or error
      await page.waitForLoadState('domcontentloaded', { timeout: 15000 }).catch(() => {});
      await page.waitForTimeout(2000);
      
      const finalUrl = page.url();
      console.log(`✓ Order placed, current page: ${finalUrl}`);
      
      // Check if order was successful
      const orderReceived = finalUrl.includes('order-received');
      const hasError = await page.locator('.woocommerce-error, .woocommerce-notice--error').isVisible({ timeout: 3000 }).catch(() => false);
      
      let orderNumber = null;
      let orderStatus = 'unknown';
      
      if (orderReceived) {
        // Extract order number from URL or page
        const orderMatch = finalUrl.match(/order-received\/(\d+)/);
        if (orderMatch) {
          orderNumber = orderMatch[1];
        }
        orderStatus = 'success';
        console.log(`✓ Order successful! Order #${orderNumber}`);
      } else if (hasError) {
        const errorMessage = await page.locator('.woocommerce-error, .woocommerce-notice--error').textContent().catch(() => 'Unknown error');
        orderStatus = 'failed';
        console.log(`✗ Order failed: ${errorMessage}`);
        
        // Take screenshot of error
        await page.screenshot({ path: 'order-error.png', fullPage: true });
        
        return res.json({
          success: false,
          message: 'Order placement failed',
          error: errorMessage,
          bundle: bundle.toUpperCase(),
          beneficiary,
          price: priceElement,
          finalUrl,
          screenshot: 'order-error.png'
        });
      }
      
      // Take screenshot of final page
      await page.screenshot({ path: 'order-confirmation.png', fullPage: true });
      
      res.json({ 
        success: true,
        message: orderStatus === 'success' ? 'Order placed successfully!' : 'Order submitted',
        orderNumber,
        orderStatus,
        bundle: bundle.toUpperCase(),
        beneficiary,
        price: priceElement,
        checkoutUrl,
        finalUrl,
        screenshot: 'order-confirmation.png'
      });
      
    } catch (placeOrderError) {
      console.error('Error clicking place order:', placeOrderError.message);
      
      // Take screenshot of checkout page
      await page.screenshot({ path: 'checkout-page.png', fullPage: true });
      
      res.json({ 
        success: false,
        message: 'Failed to place order',
        error: placeOrderError.message,
        bundle: bundle.toUpperCase(),
        beneficiary,
        price: priceElement,
        checkoutUrl,
        screenshot: 'checkout-page.png'
      });
    }

  } catch (err) {
    console.error('Purchase error:', err);
    await page.screenshot({ path: 'purchase-error.png', fullPage: true });
    res.status(500).json({ 
      error: err.message,
      screenshot: 'purchase-error.png'
    });
  }
});

async function shutdown() {
  console.log('Shutting down...');
  if (browser) await browser.close();
  process.exit(0);
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

app.listen(port, async () => {
  console.log(`Server running on http://localhost:${port}`);
  console.log(`
Available endpoints:
  GET /login                          - Manually trigger login
  GET /status                         - Check login status
  GET /navigate?url=...               - Navigate to any URL (auto-login)
  GET /dashboard                      - Go to dashboard
  GET /content                        - Get current page HTML
  GET /extract?selector=...           - Extract elements by CSS selector
  GET /screenshot                     - Get screenshot of current page
  GET /purchase?bundle=2gb&beneficiary=0241234567 - Purchase MTN data bundle
  
Purchase endpoint parameters:
  - bundle: 1gb, 2gb, 3gb, 4gb, 5gb, 6gb, 8gb, 10gb, 15gb, 20gb, 25gb, 30gb, 40gb, 50gb, 100gb
  - beneficiary: 10-digit phone number (e.g., 0241234567)
  `);
  
  // Optional: Auto-login on startup
  // await launchAndLogin().catch(console.error);
});
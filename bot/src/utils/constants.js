module.exports = {
  VALID_BUNDLES: [
    '1gb', '2gb', '3gb', '4gb', '5gb', '6gb', '8gb', 
    '10gb', '15gb', '20gb', '25gb', '30gb', '40gb', '50gb', '100gb'
  ],
  
  URLS: {
    LOGIN: '/login/',
    BUY_DATA: '/buy-data/',
    MTN_PRODUCT: '/product/mtn-data-bundle-special/',
    CHECKOUT: '/checkout/',
    DASHBOARD: '/dashboard/'
  },
  
  SELECTORS: {
    MENU_BUY_DATA: '#menu-item-7155 a',
    PAYMENT_WALLET: '#payment_method_wallet',
    PLACE_ORDER: '#place_order',
    BUNDLE_SELECT: '#pa_data-bundle-packages',
    BENEFICIARY_INPUT: 'input[name="beneficiary"]',
    ADD_TO_CART: 'button.single_add_to_cart_button',
    WALLET_BALANCE: 'label[for="payment_method_wallet"] strong .amount' // ADD THIS
  },
  
  TIMEOUTS: {
    SHORT: 1000,
    MEDIUM: 3000,
    LONG: 5000,
    NAVIGATION: 15000
  }
};
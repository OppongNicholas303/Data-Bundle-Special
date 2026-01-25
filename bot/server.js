require('dotenv').config();
const fs = require('fs');
const path = require('path');
const app = require('./src/app');
const { PORT, NODE_ENV } = require('./src/config/env.config');
const logger = require('./src/utils/logger');
const { shutdown } = require('./src/services/browser.service');

const port = PORT || 3000;

// Create necessary directories
const dirs = ['./screenshots', './storage'];
dirs.forEach(dir => {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
    logger.info(`Created directory: ${dir}`);
    console.log(`✓ Created directory: ${dir}`);
  }
});

// Start server
const server = app.listen(port, () => {
  console.log('\n╔════════════════════════════════════════════════════════════╗');
  console.log('║        MyDataGigs Automation Server                       ║');
  console.log('╚════════════════════════════════════════════════════════════╝\n');
  
  logger.info(`Server running on http://localhost:${port}`);
  logger.info(`Environment: ${NODE_ENV || 'development'}`);
  
  console.log(`🚀 Server running on: http://localhost:${port}`);
  console.log(`📊 Environment: ${NODE_ENV || 'development'}`);
  console.log(`📁 Logs: ./error.log, ./combined.log`);
  console.log(`📸 Screenshots: ./screenshots/`);
  console.log(`💾 Storage: ./storage/\n`);
  
  console.log('════════════════════════════════════════════════════════════');
  console.log('📡 Available API Endpoints:');
  console.log('════════════════════════════════════════════════════════════\n');
  
  console.log('🔐 Authentication:');
  console.log('  GET  /api/auth/login     - Login to MyDataGigs');
  console.log('  GET  /api/auth/status    - Check login status\n');
  
  console.log('🧭 Navigation:');
  console.log('  GET  /api/navigation/goto?url=<url>       - Navigate to URL');
  console.log('  GET  /api/navigation/dashboard            - Go to dashboard');
  console.log('  GET  /api/navigation/content              - Get page HTML');
  console.log('  GET  /api/navigation/extract?selector=... - Extract elements');
  console.log('  GET  /api/navigation/screenshot           - Get screenshot\n');
  
  console.log('💰 Purchase:');
  console.log('  POST /api/purchase                        - Purchase data bundle');
  console.log('       Body: { "bundle": "2gb", "beneficiary": "0241234567" }\n');
  
  console.log('🔍 Health Check:');
  console.log('  GET  /health                              - Server health status\n');
  
  console.log('════════════════════════════════════════════════════════════');
  console.log('📦 Valid Bundle Options:');
  console.log('════════════════════════════════════════════════════════════');
  console.log('1gb, 2gb, 3gb, 4gb, 5gb, 6gb, 8gb, 10gb, 15gb,');
  console.log('20gb, 25gb, 30gb, 40gb, 50gb, 100gb\n');
  
  console.log('════════════════════════════════════════════════════════════');
  console.log('📚 Usage Examples:');
  console.log('════════════════════════════════════════════════════════════\n');
  
  console.log('# 1. Login');
  console.log(`curl http://localhost:${port}/api/auth/login\n`);
  
  console.log('# 2. Check Status');
  console.log(`curl http://localhost:${port}/api/auth/status\n`);
  
  console.log('# 3. Purchase Data Bundle');
  console.log(`curl -X POST http://localhost:${port}/api/purchase \\`);
  console.log(`  -H "Content-Type: application/json" \\`);
  console.log(`  -d '{"bundle":"2gb","beneficiary":"0241234567"}'\n`);
  
  console.log('# 4. Get Screenshot');
  console.log(`curl http://localhost:${port}/api/navigation/screenshot > page.png\n`);
  
  console.log('# 5. Health Check');
  console.log(`curl http://localhost:${port}/health\n`);
  
  console.log('════════════════════════════════════════════════════════════');
  console.log('⚠️  Important Notes:');
  console.log('════════════════════════════════════════════════════════════');
  console.log('• Make sure your .env file is configured with credentials');
  console.log('• Purchase endpoint uses POST method (not GET)');
  console.log('• Beneficiary must be exactly 10 digits');
  console.log('• Screenshots are saved to ./screenshots/ directory');
  console.log('• Press Ctrl+C to gracefully shutdown the server\n');
  
  console.log('════════════════════════════════════════════════════════════\n');
  console.log('✅ Server is ready and waiting for requests...\n');
});

// Handle server errors
server.on('error', (error) => {
  if (error.code === 'EADDRINUSE') {
    logger.error(`Port ${port} is already in use`);
    console.error(`\n❌ Error: Port ${port} is already in use`);
    console.error('Please either:');
    console.error('  1. Stop the other process using this port');
    console.error('  2. Change the PORT in your .env file\n');
    process.exit(1);
  } else {
    logger.error('Server error:', error);
    console.error('\n❌ Server error:', error.message);
    process.exit(1);
  }
});

// Graceful shutdown handler
const gracefulShutdown = async (signal) => {
  console.log(`\n\n⚠️  ${signal} received. Shutting down gracefully...`);
  logger.info(`${signal} received. Shutting down gracefully...`);
  
  console.log('📦 Closing browser...');
  await shutdown();
  
  console.log('🔌 Closing server...');
  server.close(() => {
    logger.info('Server closed');
    console.log('✅ Server closed successfully');
    console.log('👋 Goodbye!\n');
    process.exit(0);
  });
  
  // Force close after 10 seconds
  setTimeout(() => {
    logger.error('Forced shutdown after timeout');
    console.error('❌ Could not close connections in time, forcefully shutting down');
    process.exit(1);
  }, 10000);
};

// Listen for termination signals
process.on('SIGINT', () => gracefulShutdown('SIGINT'));
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
  logger.error('Uncaught Exception:', error);
  console.error('\n❌ Uncaught Exception:', error.message);
  console.error(error.stack);
  gracefulShutdown('UNCAUGHT_EXCEPTION');
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Rejection at:', promise, 'reason:', reason);
  console.error('\n❌ Unhandled Promise Rejection:', reason);
  gracefulShutdown('UNHANDLED_REJECTION');
});

module.exports = server;
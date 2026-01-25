module.exports = {
  PORT: process.env.PORT,
  NODE_ENV: process.env.NODE_ENV || 'development',
  MYDATAGIGS_EMAIL: process.env.MYDATAGIGS_EMAIL,
  MYDATAGIGS_PASSWORD: process.env.MYDATAGIGS_PASSWORD,
  BASE_URL: process.env.BASE_URL || 'https://mydatagigs.com',
};
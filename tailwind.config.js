/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './resources/public/index.html',
    './src/cljs/**/*.cljs'
  ],

  // Safelist for dynamically generated classes
  safelist: [
    // Add any classes that are generated dynamically
    // e.g., 'bg-red-500', 'text-green-700'
  ],

  theme: {
    extend: {
      colors: {
        'lastfm-red': '#d51007',
        'spotify-green': '#1db954',
      },
    },
  },

  plugins: [],

  // Production optimizations
  ...(process.env.NODE_ENV === 'production' && {
    // Additional production-specific config if needed
  }),
}

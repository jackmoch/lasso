/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './resources/public/index.html',
    './src/cljs/**/*.cljs'
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
}

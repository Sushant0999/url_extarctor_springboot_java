/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        indigo: {
          900: '#1e1b4b',
          800: '#312e81',
          700: '#4338ca',
          500: '#6366f1',
          400: '#818cf8',
        },
        emerald: {
          900: '#064e3b',
          500: '#10b981',
        },
        slate: {
          950: '#020617',
          900: '#0f172a',
        }
      }
    },
  },
  plugins: [],
}

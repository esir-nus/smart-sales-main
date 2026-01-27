/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'prism-bg': '#F7F7F7', // V12 --gray-50
        'prism-accent': '#007AFF', // V12 --blue-500
        'prism-knot': '#34C759',   // V12 --green-500
        'prism-danger': '#FF3B30', // V12 --red-500
        'prism-surface': '#FFFFFF',
        'prism-surface-muted': '#F2F2F7', // V12 --gray-100
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      boxShadow: {
        'glass': '0 8px 32px 0 rgba(0, 0, 0, 0.08)', // V12 --glass-shadow
        'floating': '0 8px 32px 0 rgba(0, 122, 255, 0.15)', // V12 Input Capsule shadow
      },
      animation: {
        'breathing': 'breathing 4s ease-in-out infinite',
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-down': 'slideDown 0.3s ease-out',
        'pulse-fast': 'pulseFast 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'scan-shine': 'shine 3s linear infinite', // V12 Scan Shine
        'shimmer': 'shimmer 2s infinite',
      },
      keyframes: {
        breathing: {
          '0%, 100%': { transform: 'scale(1)', opacity: '0.9' },
          '50%': { transform: 'scale(1.15)', opacity: '1' },
        },
        shine: {
          'to': { 'background-position': '200% center' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(100%)' },
          '100%': { transform: 'translateY(0)' },
        },
        slideDown: {
          '0%': { transform: 'translateY(-20px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        pulseFast: {
            '0%, 100%': { opacity: '1' },
            '50%': { opacity: '0.5' },
        },
        shimmer: {
          '0%': { transform: 'translateX(-150%) skewX(-12deg)' },
          '100%': { transform: 'translateX(150%) skewX(-12deg)' },
        }
      }
    },
  },
  plugins: [],
}


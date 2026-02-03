/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'prism-bg': 'var(--bg-app)', 
        'prism-accent': 'var(--color-accent)', 
        'prism-knot': '#34C759',   // Keep static for now or map?
        'prism-danger': '#FF3B30', 
        'prism-surface': 'var(--bg-surface)',
        'prism-surface-muted': 'var(--bg-surface-muted)',
        'prism-primary': 'var(--color-text-primary)',
        'prism-secondary': 'var(--color-text-secondary)',
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      borderColor: {
        'prism-border': 'var(--border-base)',
      },
      borderWidth: {
        'retina': 'var(--border-width)',
      },
      boxShadow: {
        'glass': 'var(--shadow-glass)', 
        'floating': '0 8px 32px 0 rgba(0, 122, 255, 0.15)', // Keep for active inputs
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


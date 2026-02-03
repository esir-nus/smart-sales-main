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
        'prism-surface': 'var(--bg-surface)',
        'prism-accent': 'var(--color-accent)',
        'prism-primary': 'var(--color-text-primary)',
        'prism-secondary': 'var(--color-text-secondary)',
      },
      borderColor: {
        'prism-border': 'var(--border-base)',
      },
      borderWidth: {
        'retina': 'var(--border-width)',
      },
      boxShadow: {
        'glass': 'var(--shadow-glass)',
      },
      borderRadius: {
        'card': 'var(--radius-card)',
      },
      letterSpacing: {
        'tight-tech': 'var(--font-spacing)',
      }
    },
  },
  plugins: [],
}

const { createGlobPatternsForDependencies } = require('@nx/angular/tailwind');
const { join } = require('path');

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    join(__dirname, 'src/**/!(*.stories|*.spec).{ts,html}'),
    ...createGlobPatternsForDependencies(__dirname),
  ],
  theme: {
    extend: {
      colors: {
        // Vermelho Spartan — vibrante, com escala completa
        spartan: {
          DEFAULT: '#E11D2A',
          50: '#FFE5E7',
          100: '#FFB8BD',
          200: '#FF8A92',
          300: '#FF5C67',
          400: '#FF2E3C',
          500: '#E11D2A',
          600: '#B71722',
          700: '#8C111A',
          800: '#620C12',
          900: '#380609',
        },
        // Fundos escuros (dark mode nativo)
        ink: {
          950: '#08080A',
          900: '#0A0A0B',
          800: '#111114',
          700: '#18181B',
          600: '#1F1F23',
          500: '#27272B',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        display: ['"Orbitron"', 'Inter', 'sans-serif'],
      },
      boxShadow: {
        'neon-red': '0 0 12px rgba(225, 29, 42, 0.55), 0 0 32px rgba(225, 29, 42, 0.25)',
        'neon-soft': '0 0 8px rgba(225, 29, 42, 0.35)',
      },
      backgroundImage: {
        'spartan-grid':
          'radial-gradient(circle at 50% 0%, rgba(225,29,42,0.12), transparent 60%)',
      },
      keyframes: {
        'pulse-glow': {
          '0%, 100%': { boxShadow: '0 0 8px rgba(225,29,42,0.4)' },
          '50%': { boxShadow: '0 0 20px rgba(225,29,42,0.8)' },
        },
        rise: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'pulse-glow': 'pulse-glow 1.8s ease-in-out infinite',
        rise: 'rise 0.35s ease-out both',
      },
    },
  },
  plugins: [],
};

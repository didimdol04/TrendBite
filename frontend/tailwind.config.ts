import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        navy: { DEFAULT: '#1A2A6C', dark: '#0F1A4D', light: '#243B7E' },
        gold: { DEFAULT: '#E0A800', dark: '#B88800' },
      },
      fontFamily: {
        sans: ['"Pretendard"', '"Apple SD Gothic Neo"', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};

export default config;

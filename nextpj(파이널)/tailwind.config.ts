import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        'sumo-primary': '#5F5FD3',
        'sumo-secondary': '#2C2CA0',
      },
      zIndex: {
        'side': '1000',
        'map': '1',
        'canvas': '2',
      },
      width: {
        'side': '250px',
      },
      height: {
        'controls': 'calc(100% - 55px)',
      }
    },
  },
  plugins: [],
}

export default config
/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eef4ff",
          100: "#dce9ff",
          200: "#bfd7ff",
          300: "#93bcff",
          400: "#6098ff",
          500: "#3b82f6",
          600: "#2563eb",
          700: "#1d4ed8",
          800: "#1e40af",
          900: "#1e3a8a"
        },
        app: "#f8fafd"
      },
      boxShadow: {
        card: "0 8px 24px rgba(15, 23, 42, 0.08)"
      },
      borderRadius: {
        xl2: "1rem"
      }
    }
  },
  plugins: []
};

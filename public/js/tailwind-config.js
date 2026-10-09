// Cấu hình Tailwind dùng chung cho các màn MB (token màu từ mockup S1/S2).
// Nạp sau thẻ <script src="cdn.tailwindcss.com"> và trước phần nội dung.
window.tailwind = window.tailwind || {};
window.tailwind.config = {
  theme: {
    extend: {
      colors: {
        mb: {
          blue: '#1b4bcf',
          'blue-light': '#2f65f6',
          'blue-hover': '#153eb2',
          navy: '#001a70',
          red: '#ea1e2e',
          green: '#24a148',
          'green-bg': '#e6f7ec',
          orange: '#ff9900',
          bg: '#f3f5f9',
        },
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'Helvetica', 'Arial', 'sans-serif'],
      },
    },
  },
};

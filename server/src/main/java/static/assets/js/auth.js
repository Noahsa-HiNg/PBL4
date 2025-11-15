// auth.js
// "Vệ sĩ" kiểm tra đăng nhập

const token = localStorage.getItem('authToken');

// Lấy tên trang hiện tại (ví dụ: "login.html")
const currentPage = window.location.pathname.split('/').pop();

if (!token && currentPage !== 'login.html') {
    console.log("Chưa đăng nhập, chuyển về login.html");
    window.location.href = 'login.html';
} else if (token && currentPage === 'login.html') {
    // Nếu đã đăng nhập VÀ đang ở trang login
    // -> Chuyển vào trang chủ (live_feed.html)
    console.log("Đã đăng nhập, chuyển vào trang chủ.");
    window.location.href = 'live_feed.html';
}
// Chờ HTML tải xong
document.addEventListener('DOMContentLoaded', () => {

    const loginForm = document.getElementById('login-form');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginButton = document.getElementById('login-button');
    const errorMessageDiv = document.getElementById('error-message');

    if (!loginForm) return; // Chỉ chạy code nếu đây là trang login

    // Bắt sự kiện khi người dùng submit form
    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault(); // Ngăn trình duyệt tự gửi form

        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();

        // Ẩn lỗi cũ, vô hiệu hóa nút
        errorMessageDiv.style.display = 'none';
        loginButton.disabled = true;
        loginButton.textContent = 'Đang xử lý...';

        try {
            // 1. Gọi hàm loginUser từ api.js
            const loginData = await loginUser(username, password);

            // 2. Xử lý đăng nhập thành công
            if (loginData.token) { // Giả sử server trả về token
                console.log("Lưu token:", loginData.token);
                // Lưu token vào localStorage để dùng cho các request sau
                localStorage.setItem('authToken', loginData.token); 
                
                // Lưu thông tin user (nếu có) để biết role
                if (loginData.username) {
                    console.log("Lưu user info:", loginData.username);
                    localStorage.setItem('userInfo', JSON.stringify(loginData.username));
                }

                
            } else {
                 // Trường hợp server không trả về token như mong đợi
                 showError('Phản hồi từ server không hợp lệ.');
            }

        } catch (error) {
            // 3. Xử lý lỗi đăng nhập (sai pass, lỗi mạng...)
            showError(error.message || 'Đăng nhập thất bại. Vui lòng thử lại.');
        } finally {
            // Luôn bật lại nút sau khi xử lý xong
            loginButton.disabled = false;
            loginButton.textContent = 'Đăng nhập';
        }
    });

    // Hàm hiển thị lỗi
    function showError(message) {
        errorMessageDiv.textContent = message;
        errorMessageDiv.style.display = 'block';
    }
});
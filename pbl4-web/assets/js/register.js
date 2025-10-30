// register.js

const API_URL = 'http://localhost:8080/api/auth/register'; 

document.getElementById('registrationForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Ngăn chặn form submit theo cách truyền thống

    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const messageElement = document.getElementById('message');

    // 1. Chuẩn bị dữ liệu JSON
    const data = {
        username: username,
        email: email,
        password: password,
        // Role được đặt mặc định trong Service, không cần gửi lên
        role: "viewer" 
    };

    // 2. Gọi API POST
    fetch(API_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data)
    })
    .then(response => {
        // Kiểm tra status code để biết request có thành công hay không
        if (response.ok) {
            return response.json(); // Phản hồi thành công (201)
        }
        // Ném lỗi để bắt ở khối catch
        return response.json().then(errorData => {
            throw new Error(errorData.message || 'Registration failed');
        });
    })
    .then(data => {
        // Xử lý thành công
        messageElement.textContent = `Đăng ký thành công! Chào mừng ${data.username}.`;
        messageElement.style.color = 'green';
        // Có thể chuyển hướng người dùng đến trang đăng nhập
        // window.location.href = 'login.html'; 
    })
    .catch(error => {
        // Xử lý lỗi (ví dụ: Username đã tồn tại)
        messageElement.textContent = `Lỗi: ${error.message}`;
        messageElement.style.color = 'red';
        console.error('Registration Error:', error);
    });
});
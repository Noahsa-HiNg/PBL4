/**
 * Gửi yêu cầu đăng nhập đến server.
 * @param {string} username Tên đăng nhập.
 * @param {string} password Mật khẩu.
 * @returns {Promise<object>} Promise chứa dữ liệu từ server (vd: token, user info).
 */
async function loginUser(username, password) {
    // API_BASE_URL từ config.js
    const url = `${API_BASE_URL}/auth/login`; 

    // Dữ liệu phải ở dạng x-www-form-urlencoded
    const formData = new URLSearchParams();
    formData.append('username', username); // Key phải khớp usernameParameter trong SecurityConfig
    formData.append('password', password); // Key phải khớp passwordParameter trong SecurityConfig

    console.log(`Gửi yêu cầu đăng nhập đến: ${url} với data:`, formData.toString());

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                // KIỂU DỮ LIỆU QUAN TRỌNG cho formLogin
                'Content-Type': 'application/x-www-form-urlencoded', 
            },
            body: formData // Gửi đối tượng URLSearchParams
        });

        const data = await response.json(); // Đọc JSON response từ success/failure handler

        if (!response.ok) { // Kiểm tra nếu server trả về lỗi (vd: 401)
            throw new Error(data.message || `Lỗi HTTP: ${response.status}`);
        }

        console.log('Đăng nhập thành công:', data);
        return data; // Trả về dữ liệu (vd: { message: "...", username: "..." })

    } catch (error) {
        console.error('Lỗi API đăng nhập:', error);
        throw error; // Ném lỗi ra để login.js xử lý
    }
}
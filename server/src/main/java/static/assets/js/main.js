document.addEventListener('DOMContentLoaded', () => {

    // --- Cấu hình ---
    const testButton = document.getElementById('test-connection-btn');
    const statusDisplay = document.getElementById('connection-status');
    const healthCheckUrl = 'http://localhost:8080/api/health'; // URL của API health check

    // --- Hàm kiểm tra kết nối ---
    async function testServerConnection() {
        statusDisplay.textContent = 'Testing connection...'; // Hiển thị đang kiểm tra
        statusDisplay.style.color = 'orange';

        try {
            // 1. Gọi API GET /api/health
            const response = await fetch(healthCheckUrl);

            // 2. Kiểm tra kết quả
            if (response.ok) {
                // Nếu server trả về status 2xx (thành công)
                const data = await response.json(); // Đọc nội dung JSON
                statusDisplay.textContent = `✅ Connection Successful! Server status: ${data.status}`;
                statusDisplay.style.color = 'green';
                console.log('Server response:', data);
            } else {
                // Nếu server trả về lỗi (4xx, 5xx)
                statusDisplay.textContent = `❌ Connection Failed! Server returned status: ${response.status}`;
                statusDisplay.style.color = 'red';
            }
        } catch (error) {
            // 3. Xử lý lỗi mạng (không thể kết nối)
            console.error('Network error:', error);
            statusDisplay.textContent = '❌ Connection Error! Could not reach the server. Is it running?';
            statusDisplay.style.color = 'red';
        }
    }

    // --- Gắn sự kiện click cho button ---
    if (testButton) {
        testButton.addEventListener('click', testServerConnection);
    }

    // --- (Optional) Code cũ để tải ảnh ---
    // const galleryContainer = document.getElementById('image-gallery-container');
    // const apiUrl = 'http://localhost:8080/api/images';
    // async function fetchAndDisplayImages() { ... }
    // fetchAndDisplayImages();
});
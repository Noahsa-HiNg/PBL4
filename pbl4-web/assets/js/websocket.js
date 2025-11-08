let socket = null;
function connectWebSocket() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        console.error("Không có Token. Không thể thiết lập WebSocket.");
        return;
    }
    
    // Đảm bảo không kết nối lại nếu đã có
    if (socket && socket.readyState === WebSocket.OPEN) return;

    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
        console.log("WebSocket: Đã kết nối. Đang gửi xác thực...");
        
        // BƯỚC 1: Gửi Token xác thực
        const authMessage = JSON.stringify({
            type: "AUTH",
            token: token
        });
        socket.send(authMessage);
    };

    socket.onmessage = (event) => {
        // BƯỚC 2: Xử lý các tin nhắn từ Server
        handleWebSocketMessage(event.data);
    };

    socket.onclose = (event) => {
        console.warn("WebSocket: Kết nối đã đóng. Mã:", event.code, event.reason);
        // Cố gắng kết nối lại sau 5 giây nếu lỗi không phải do người dùng
        if (event.code !== 1000) { 
             setTimeout(connectWebSocket, 5000);
        }
    };

    socket.onerror = (error) => {
        console.error("WebSocket: Lỗi kết nối", error);
    };
}
/**
 * Phân tích tin nhắn từ Server và cập nhật UI.
 * @param {string} jsonPayload - Chuỗi JSON từ Server.
 */
function handleWebSocketMessage(jsonPayload) {
    console.log("DEBUG: RECEIVED PAYLOAD:", jsonPayload);
    if (typeof jsonPayload === 'string' && jsonPayload.startsWith("connect")) {
        console.log("WebSocket: Trạng thái kết nối đã được Server xác nhận.");
        return; // Bỏ qua việc parse và dừng xử lý
    }
    
    // Bạn cũng nên kiểm tra tin nhắn rỗng hoặc không phải chuỗi
    if (typeof jsonPayload !== 'string' || jsonPayload.trim().length === 0) {
        return;
    }
    try {
        const message = JSON.parse(jsonPayload);
        const grid = document.getElementById('gallery-grid'); // Giả sử bạn muốn cập nhật trang thư viện

        switch (message.type) {
            case 'AUTH_SUCCESS':
                console.log("WebSocket: Xác thực thành công!");
                break;

            case 'NEW_SNAPSHOT':
                // Tin nhắn NEW_SNAPSHOT được gửi khi ảnh mới được upload
                console.log("Thông báo: Ảnh mới đã đến.");
                const newImage = message.image; 

                // Tùy chọn: 
                // 1. Nếu bạn đang ở trang 'snapshot_management', gọi lại loadImages(0)
                if (window.location.pathname.includes('snapshot_management')) {
                    // Gọi hàm callback đã được gán trên trang view
                    if (typeof window.snapshotUpdateCallback === 'function') {
                        window.snapshotUpdateCallback(); 
                    }
                }
                break;
                
            case 'CLIENT_STATUS_UPDATE':
                // Thông báo từ ClientMonitorService khi Client chuyển sang OFFLINE
                console.warn(`Client ID ${message.clientId} đã chuyển sang trạng thái: ${message.status}`);
                // Bạn có thể cập nhật trạng thái Client trên trang Dashboard Admin tại đây.
                break;
                
            default:
                console.log("WebSocket: Loại tin nhắn không xác định:", message.type);
        }

    } catch (e) {
        console.error("Lỗi parse tin nhắn WebSocket:", e, jsonPayload);
    }
}
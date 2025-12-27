let socket = null;
function connectWebSocket() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        console.error("Không có Token. Không thể thiết lập WebSocket.");
        return;
    }
    if (socket && socket.readyState === WebSocket.OPEN) return;

    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
        console.log("WebSocket: Đã kết nối. Đang gửi xác thực...");
        
       
        const authMessage = JSON.stringify({
            type: "AUTH",
            token: token,
			clientType: "WEB"
        });
        socket.send(authMessage);
    };

    socket.onmessage = (event) => {
        handleWebSocketMessage(event.data);
    };

    socket.onclose = (event) => {
        console.warn("WebSocket: Kết nối đã đóng. Mã:", event.code, event.reason);
       
        if (event.code !== 1000) { 
             setTimeout(connectWebSocket, 5000);
        }
    };

    socket.onerror = (error) => {
        console.error("WebSocket: Lỗi kết nối", error);
    };
}

function handleWebSocketMessage(jsonPayload) {
    console.log("DEBUG: RECEIVED PAYLOAD:", jsonPayload);
    if (typeof jsonPayload === 'string' && jsonPayload.startsWith("connect")) {
        console.log("WebSocket: Trạng thái kết nối đã được Server xác nhận.");
        return; 
    }
    
    
    if (typeof jsonPayload !== 'string' || jsonPayload.trim().length === 0) {
        return;
    }
    try {
        const message = JSON.parse(jsonPayload);
        const grid = document.getElementById('gallery-grid'); 

        switch (message.type) {
            case 'AUTH_SUCCESS':
                console.log("WebSocket: Xác thực thành công!");
                break;

            case 'NEW_SNAPSHOT':
                
                console.log("Thông báo: Ảnh mới đã đến.");
                const newImage = message.image; 

               
                if (window.location.pathname.includes('snapshot_management')) {
                    
                    if (typeof window.snapshotUpdateCallback === 'function') {
                        window.snapshotUpdateCallback(); 
                    }
                } else if (typeof window.webSocketMessageCallback === 'function') {
                   
                    window.webSocketMessageCallback(message);
                }
                break;
                
            case 'CLIENT_STATUS_UPDATE':
                console.log("WebSocket: Cấu hình Client đã được cập nhật từ Server.");
                if (window.location.pathname.includes('client_setup.html')) {
                    window.clientUpdateCallback();
                }
                break;
                
            default:
                console.log("WebSocket: Loại tin nhắn không xác định:", message.type);
        }

    } catch (e) {
        console.error("Lỗi parse tin nhắn WebSocket:", e, jsonPayload);
    }
}
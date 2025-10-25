// /**
//  * Gửi yêu cầu đăng nhập đến server.
//  * @param {string} username Tên đăng nhập.
//  * @param {string} password Mật khẩu.
//  * @returns {Promise<object>} Promise chứa dữ liệu từ server (vd: token, user info).
//  */
// async function loginUser(username, password) {
//     // API_BASE_URL từ config.js
//     const url = `${API_BASE_URL}/auth/login`; 

//     // Dữ liệu phải ở dạng x-www-form-urlencoded
//     const formData = new URLSearchParams();
//     formData.append('username', username); // Key phải khớp usernameParameter trong SecurityConfig
//     formData.append('password', password); // Key phải khớp passwordParameter trong SecurityConfig

//     console.log(`Gửi yêu cầu đăng nhập đến: ${url} với data:`, formData.toString());

//     try {
//         const response = await fetch(url, {
//             method: 'POST',
//             headers: {
//                 // KIỂU DỮ LIỆU QUAN TRỌNG cho formLogin
//                 'Content-Type': 'application/x-www-form-urlencoded', 
//             },
//             body: formData // Gửi đối tượng URLSearchParams
//         });

//         const data = await response.json(); // Đọc JSON response từ success/failure handler

//         if (!response.ok) { // Kiểm tra nếu server trả về lỗi (vd: 401)
//             throw new Error(data.message || `Lỗi HTTP: ${response.status}`);
//         }

//         console.log('Đăng nhập thành công:', data);
//         return data; // Trả về dữ liệu (vd: { message: "...", username: "..." })

//     } catch (error) {
//         console.error('Lỗi API đăng nhập:', error);
//         throw error; // Ném lỗi ra để login.js xử lý
//     }
// }
// /**
//  * Lấy danh sách camera từ API server.
//  * @returns {Promise<Array>} Promise chứa mảng các đối tượng camera.
//  */
// async function getCameraList() {
//     const url = `${API_BASE_URL}/cameras`; // Giả sử endpoint là /api/cameras
//     console.log("Đang gọi API lấy danh sách camera:", url);

//     try {
//         const response = await fetch(url, {
//              // Thêm header Authorization nếu API yêu cầu đăng nhập
//              // headers: { 'Authorization': `Bearer ${localStorage.getItem('authToken')}` } 
//         });

//         if (!response.ok) {
//             throw new Error(`Lỗi HTTP: ${response.status}`);
//         }

//         const data = await response.json();
//         console.log("Đã nhận dữ liệu camera:", data);
//         return data;

//     } catch (error) {
//         console.error("Không thể lấy danh sách camera:", error);
//         throw error;
//     }
// }
/**
 * =================================================================
 * TỆP API GIẢ LẬP (MOCK)
 * * Mô phỏng chính xác các API server (Java) đã cung cấp.
 * * Mô phỏng luồng xác thực (Authentication) bằng Token.
 * =================================================================
 */

// === DỮ LIỆU GIẢ (MOCK DATABASE) ===
const FAKE_TOKEN = "fake-jwt-token-abc.123.xyz-for-testing";

const MOCK_DB = {
    users: {
        "1": { id: 1, username: "admin", email: "admin@example.com", role: "ADMIN" },
        "2": { id: 2, username: "viewer", email: "viewer@example.com", role: "VIEWER" }
    },
    clients: [
        { id: 1, clientName: "Lobby-PC", ipAddress: "192.168.1.100", status: "online", lastHeartbeat: "2025-10-25T14:30:00Z", imageWidth: 1920, imageHeight: 1080, captureIntervalSeconds: 5, compressionQuality: 90 },
        { id: 2, clientName: "Warehouse-PC", ipAddress: "192.168.1.101", status: "offline", lastHeartbeat: "2025-10-24T10:00:00Z", imageWidth: 1280, imageHeight: 720, captureIntervalSeconds: 10, compressionQuality: 80 }
    ],
    cameras: [
        { id: 1, clientId: 1, cameraName: "Lobby - Entrance", ipAddress: "192.168.1.201", onvifUrl: "/onvif", username: "cam_admin", password: "123", active: true },
        { id: 2, clientId: 1, cameraName: "Lobby - Elevator (Offline)", ipAddress: "192.168.1.202", onvifUrl: "/onvif", username: "cam_admin", password: "123", active: false },
        { id: 3, clientId: 2, cameraName: "Warehouse - Bay 1", ipAddress: "192.168.2.201", onvifUrl: "/onvif", username: "cam_admin", password: "456", active: true }
    ],
    images: [
        { id: 101, cameraId: 1, imageName: "sanh_001.jpg", filePath: "https://picsum.photos/id/101/600/400", capturedAt: "2025-10-25T14:30:00Z", fileSizeKb: 450.75 },
        { id: 102, cameraId: 3, imageName: "kho_001.jpg", filePath: "https://picsum.photos/id/102/600/400", capturedAt: "2025-10-25T14:31:00Z", fileSizeKb: 312.50 },
        { id: 103, cameraId: 1, imageName: "sanh_002.jpg", filePath: "https://picsum.photos/id/103/600/400", capturedAt: "2025-10-25T14:32:00Z", fileSizeKb: 455.10 }
    ]
};

// Hàm tiện ích mô phỏng độ trễ mạng
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Hàm tiện ích mô phỏng việc server kiểm tra token
const checkAuth = () => {
    const token = localStorage.getItem('authToken');
    if (!token || token !== FAKE_TOKEN) {
        // Mô phỏng lỗi 401
        throw { status: 401, message: "Mock: Bạn chưa đăng nhập hoặc token không hợp lệ." };
    }
    console.log("Mock Auth: Token hợp lệ.");
};

// === API: /api/auth ===

async function loginUser(username, password) {
    console.log(`Mock Login: Thử đăng nhập với user: ${username}`);
    await delay(500);

    // Mật khẩu giả lập: "123"
    if (password === "123" && (username === "admin" || username === "viewer")) {
        console.log("Mock Login: Thành công!");
        localStorage.setItem('authToken', FAKE_TOKEN);
        // Lưu ID user giả
        localStorage.setItem('mockUserId', username === "admin" ? "1" : "2");
        return { token: FAKE_TOKEN }; // Server thật chỉ trả về token
    } else {
        console.error("Mock Login: Thất bại!");
        throw { status: 401, message: "Tên đăng nhập hoặc mật khẩu không đúng" };
    }
}

async function getCurrentUser() {
    console.log("Mock API: Đang gọi /api/auth/me");
    await delay(200);
    checkAuth(); 
    const userId = localStorage.getItem('mockUserId');
    return MOCK_DB.users[userId];
}

async function logoutUser() {
    console.log("Mock Logout: Đang đăng xuất...");
    await delay(100);
    localStorage.removeItem('authToken');
    localStorage.removeItem('mockUserId');
    console.log("Mock Logout: Đã xóa token.");
    return { message: "Logout successful" };
}

// === API: /api/clients ===

async function getClientList() {
    console.log("Mock API: Đang gọi /api/clients (GET)");
    await delay(300);
    checkAuth();
    return MOCK_DB.clients;
}

async function getClientById(id) {
    console.log(`Mock API: Đang gọi /api/clients/${id} (GET)`);
    await delay(100);
    checkAuth();
    const client = MOCK_DB.clients.find(c => c.id == id);
    if (!client) throw { status: 404, message: "Mock: Không tìm thấy Client" };
    return client;
}

async function updateClient(id, clientDetails) {
    console.log(`Mock API: Đang gọi /api/clients/${id} (PUT)`);
    await delay(400);
    checkAuth();
    let client = MOCK_DB.clients.find(c => c.id == id);
    if (!client) throw { status: 404, message: "Mock: Không tìm thấy Client" };
    
    // Cập nhật
    Object.assign(client, clientDetails);
    return client;
}

// (Các hàm create và delete tương tự, bạn có thể tự thêm nếu cần)

// === API: /api/cameras ===

async function getCameraList() {
    console.log("Mock API: Đang gọi /api/cameras (GET)");
    await delay(500);
    checkAuth();
    return MOCK_DB.cameras;
}

async function getCameraListByClient(clientId) {
    console.log(`Mock API: Đang gọi /api/cameras/by-client/${clientId} (GET)`);
    await delay(300);
    checkAuth();
    return MOCK_DB.cameras.filter(c => c.clientId == clientId);
}

async function getCameraById(id) {
    console.log(`Mock API: Đang gọi /api/cameras/${id} (GET)`);
    await delay(200);
    checkAuth();
    const camera = MOCK_DB.cameras.find(c => c.id == id);
    if (!camera) throw { status: 404, message: "Mock: Không tìm thấy Camera" };
    return camera;
}

// (Các hàm create, update, delete camera tương tự)

// === API: /api/images ===

async function getImageList(page = 0, size = 10, cameraId = null) {
    console.log(`Mock API: Đang gọi /api/images?page=${page}&size=${size}`);
    await delay(700);
    checkAuth();

    let filteredImages = MOCK_DB.images;
    if (cameraId) {
        filteredImages = MOCK_DB.images.filter(img => img.cameraId == cameraId);
    }
    
    const start = page * size;
    const end = start + size;
    const pagedContent = filteredImages.slice(start, end);

    return {
        content: pagedContent,
        totalPages: Math.ceil(filteredImages.length / size),
        totalElements: filteredImages.length,
        last: (page + 1) * size >= filteredImages.length,
        first: page === 0,
        number: page,
    };
}

async function uploadImage(file, cameraId, capturedAt) {
    console.log(`Mock API: Đang gọi /api/images/upload (POST)`);
    await delay(1000); // Giả lập upload
    checkAuth();
    
    const newImage = {
        id: Math.floor(Math.random() * 1000) + 200,
        cameraId: cameraId,
        imageName: file.name,
        filePath: `https://picsum.photos/id/${Math.floor(Math.random() * 100)}/600/400`, // Ảnh ngẫu nhiên
        capturedAt: capturedAt.toISOString(),
        fileSizeKb: (file.size / 1024).toFixed(2)
    };
    MOCK_DB.images.unshift(newImage); // Thêm vào đầu danh sách
    return newImage;
}


// === API: /api/users ===
async function updateUser(id, userDetails) {
    console.log(`Mock API: Đang gọi /api/users/${id} (PUT)`);
    await delay(400);
    checkAuth();
    let user = MOCK_DB.users[id.toString()];
    if (!user) throw { status: 404, message: "Mock: Không tìm thấy User" };
    
    // Chỉ cập nhật email (không cho đổi username, password)
    user.email = userDetails.email;
    
    // (Server thật sẽ xử lý password riêng)
    console.log("Mock User Updated:", user);
    return user;
}
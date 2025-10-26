// // api.js - Phiên bản THẬT sử dụng Token

// /**
//  * Gửi yêu cầu đăng nhập (sử dụng JSON và nhận về Token).
//  */
// async function loginUser(username, password) {
//     const url = `${API_BASE_URL}/auth/login`;
    
//     // 1. Dữ liệu gửi đi là JSON, khớp với @RequestBody LoginRequest
//     const bodyData = JSON.stringify({
//         username: username,
//         password: password
//     });

//     console.log(`Gọi API đăng nhập: ${url}`);

//     try {
//         const response = await fetch(url, {
//             method: 'POST',
//             headers: {
//                 'Content-Type': 'application/json'
//             },
//             body: bodyData
//         });

//         const data = await response.json();

//         if (!response.ok) {
//             throw new Error(data.message || `Lỗi HTTP: ${response.status}`);
//         }

//         // 2. Đăng nhập thành công, LƯU TOKEN
//         if (data.token) {
//             localStorage.setItem('authToken', data.token);
//             console.log("Đăng nhập thành công, đã lưu token.");
//         } else {
//             throw new Error("Server không trả về token.");
//         }

//         return data; // Trả về { "token": "..." }

//     } catch (error) {
//         console.error('Lỗi API đăng nhập:', error);
//         throw error;
//     }
// }

// /**
//  * Hàm helper: Lấy token từ localStorage và tạo header
//  */
// function getAuthHeaders() {
//     const token = localStorage.getItem('authToken');
//     if (!token) {
//         // Nếu không có token, ném lỗi để chuyển hướng về trang login
//         throw new Error("Chưa đăng nhập (không tìm thấy authToken).");
//     }
    
//     // 3. Tạo Authorization header
//     return {
//         'Authorization': `Bearer ${token}`, //
//         'Content-Type': 'application/json'
//     };
// }

// /**
//  * Lấy danh sách camera (Yêu cầu xác thực bằng Token).
//  */
// async function getCameraList() {
//     const url = `${API_BASE_URL}/cameras`;
//     console.log("Đang gọi API lấy danh sách camera (với Token)...", url);

//     try {
//         const response = await fetch(url, {
//             method: 'GET',
//             headers: getAuthHeaders() // 4. Gửi kèm token
//         });

//         if (!response.ok) {
//             // Nếu token hết hạn (401), hàm này sẽ ném lỗi
//             const errorData = await response.json();
//             throw new Error(errorData.message || `Lỗi HTTP: ${response.status}`);
//         }

//         const data = await response.json();
//         console.log("Đã nhận dữ liệu camera:", data);
//         return data;

//     } catch (error) {
//         console.error("Không thể lấy danh sách camera:", error);
//         // Nếu lỗi là 401 (do token hết hạn), chúng ta nên xóa token cũ và reload
//         if (error.message.includes("401") || error.message.includes("Chưa đăng nhập")) {
//             localStorage.removeItem('authToken');
//             window.location.href = 'login.html'; // Tải lại về trang login
//         }
//         throw error;
//     }
// }

// /**
//  * Lấy thông tin user hiện tại (dựa trên token).
//  */
// async function getCurrentUser() {
//     const url = `${API_BASE_URL}/auth/me`;
//     console.log("Đang gọi API /api/auth/me (với Token)...");

//     try {
//         const response = await fetch(url, {
//             method: 'GET',
//             headers: getAuthHeaders()
//         });

//         if (!response.ok) {
//             const errorData = await response.json();
//             throw new Error(errorData.message || `Lỗi HTTP: ${response.status}`);
//         }
//         return await response.json(); // Trả về { username: "...", role: "..." }

//     } catch (error) {
//         console.error("Không thể lấy thông tin user:", error);
//         throw error;
//     }
// }

// /**
//  * Xóa token khỏi localStorage.
//  */
// async function logoutUser() {
//     console.log("Đang đăng xuất (xóa token)...");
//     localStorage.removeItem('authToken');
//     localStorage.removeItem('username'); // Xóa các thông tin cũ
//     localStorage.removeItem('role');
//     // Không cần gọi API, chỉ cần xóa token là đủ
//     return Promise.resolve({ message: "Đã đăng xuất" });
// }

// // (Thêm các hàm getClientList, getImageList... tương tự như getCameraList)
// // Ví dụ:
// async function getClientList() {
//     const url = `${API_BASE_URL}/clients`;
//     try {
//         const response = await fetch(url, { headers: getAuthHeaders() });
//         if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
//         return await response.json();
//     } catch (error) {
//         console.error("Lỗi khi lấy danh sách client:", error);
//         throw error;
//     }
// }
/**
 * =================================================================
 * TỆP API GIẢ LẬP (MOCK) - PHIÊN BẢN ĐẦY ĐỦ
 * * Mô phỏng tất cả API server cần thiết cho web frontend.
 * * Mô phỏng luồng xác thực (Authentication) bằng Token.
 * * Trả về cấu trúc JSON giống hệt server thật.
 * =================================================================
 */

// === DỮ LIỆU GIẢ (MOCK DATABASE) ===
const FAKE_TOKEN = "fake-jwt-token-abc.123.xyz-for-testing";
let nextId = { user: 3, client: 3, camera: 4, image: 104 }; // Để tạo ID mới

const MOCK_DB = {
    users: {
        "1": { id: 1, username: "admin", email: "admin@example.com", role: "ADMIN" },
        "2": { id: 2, username: "viewer", email: "viewer@example.com", role: "VIEWER" }
    },
    clients: [
        { id: 1, clientName: "Lobby-PC", ipAddress: "192.168.1.100", status: "online", lastHeartbeat: "2025-10-25T14:30:00Z", imageWidth: 1920, imageHeight: 1080, captureIntervalSeconds: 5, compressionQuality: 90, createdAt: "2025-10-24T09:00:00Z", updatedAt: "2025-10-25T14:30:00Z" },
        { id: 2, clientName: "Warehouse-PC", ipAddress: "192.168.1.101", status: "offline", lastHeartbeat: "2025-10-24T10:00:00Z", imageWidth: 1280, imageHeight: 720, captureIntervalSeconds: 10, compressionQuality: 80, createdAt: "2025-10-24T09:05:00Z", updatedAt: "2025-10-24T10:00:00Z" }
    ],
    cameras: [
        { id: 1, clientId: 1, cameraName: "Lobby - Entrance", ipAddress: "192.168.1.201", onvifUrl: "/onvif", username: "cam_admin", password: "123", active: true, createdAt: "2025-10-24T09:10:00Z" },
        { id: 2, clientId: 1, cameraName: "Lobby - Elevator (Offline)", ipAddress: "192.168.1.202", onvifUrl: "/onvif", username: "cam_admin", password: "123", active: false, createdAt: "2025-10-24T09:11:00Z" },
        { id: 3, clientId: 2, cameraName: "Warehouse - Bay 1", ipAddress: "192.168.2.201", onvifUrl: "/onvif", username: "cam_admin", password: "456", active: true, createdAt: "2025-10-24T09:12:00Z" }
    ],
    images: [
        { id: 101, cameraId: 1, imageName: "sanh_001.jpg", filePath: "1/1/2025/10/25/sanh_001_uuid.jpg", fileSizeKb: 450.75, capturedAt: "2025-10-25T14:30:00Z", uploadedAt: "2025-10-25T14:30:05Z", metadata: null },
        { id: 102, cameraId: 3, imageName: "kho_001.jpg", filePath: "2/3/2025/10/25/kho_001_uuid.jpg", fileSizeKb: 312.50, capturedAt: "2025-10-25T14:31:00Z", uploadedAt: "2025-10-25T14:31:05Z", metadata: null },
        { id: 103, cameraId: 1, imageName: "sanh_002.jpg", filePath: "1/1/2025/10/25/sanh_002_uuid.jpg", fileSizeKb: 455.10, capturedAt: "2025-10-25T14:32:00Z", uploadedAt: "2025-10-25T14:32:05Z", metadata: null }
    ]
};

// Hàm tiện ích mô phỏng độ trễ mạng
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Hàm tiện ích mô phỏng việc server kiểm tra token và role (nếu cần)
const checkAuth = (requiredRole = null) => {
    const token = localStorage.getItem('authToken');
    if (!token || token !== FAKE_TOKEN) {
        throw { status: 401, message: "Mock: Bạn chưa đăng nhập hoặc token không hợp lệ." };
    }
    // Giả lập kiểm tra role (nếu API yêu cầu)
    const userRole = localStorage.getItem('mockUserRole'); // Giả sử role được lưu khi login
    if (requiredRole && (!userRole || userRole !== requiredRole)) {
         throw { status: 403, message: `Mock: Yêu cầu quyền ${requiredRole}.` };
    }
    console.log(`Mock Auth: Token hợp lệ. ${requiredRole ? `Role ${userRole} OK.` : ''}`);
};

// === API: /api/auth ===

async function loginUser(username, password) {
    console.log(`Mock Login: Thử đăng nhập với user: ${username}`);
    await delay(500);
    const user = Object.values(MOCK_DB.users).find(u => u.username === username);

    if (user && password === "123") { // Mật khẩu giả lập
        console.log("Mock Login: Thành công!");
        localStorage.setItem('authToken', FAKE_TOKEN);
        localStorage.setItem('mockUserId', user.id.toString());
        localStorage.setItem('mockUserRole', user.role); // Lưu role
        return { token: FAKE_TOKEN }; // Server thật chỉ trả token
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
    const user = MOCK_DB.users[userId];
    if (!user) throw { status: 404, message: "Mock: User không tồn tại" };
    // Trả về giống cấu trúc API thật
    return { username: user.username, role: user.role };
}

async function logoutUser() {
    console.log("Mock Logout: Đang đăng xuất...");
    await delay(100);
    localStorage.removeItem('authToken');
    localStorage.removeItem('mockUserId');
    localStorage.removeItem('mockUserRole');
    console.log("Mock Logout: Đã xóa token và user info.");
    return { message: "Logout successful" };
}

// === API: /api/clients ===

async function createClient(clientData) {
    console.log("Mock API: Đang gọi /api/clients (POST)");
    await delay(400);
    checkAuth("ADMIN"); // Giả sử chỉ Admin được tạo client
    const newClient = {
        ...clientData,
        id: nextId.client++,
        status: 'offline', // Mặc định khi mới tạo
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
    };
    MOCK_DB.clients.push(newClient);
    return newClient;
}

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
    checkAuth(); // Có thể chỉ Admin được cập nhật cấu hình?
    let client = MOCK_DB.clients.find(c => c.id == id);
    if (!client) throw { status: 404, message: "Mock: Không tìm thấy Client" };
    Object.assign(client, { ...clientDetails, updatedAt: new Date().toISOString() });
    return client;
}

async function deleteClient(id) {
    console.log(`Mock API: Đang gọi /api/clients/${id} (DELETE)`);
    await delay(500);
    checkAuth("ADMIN"); // Giả sử chỉ Admin được xóa
    const index = MOCK_DB.clients.findIndex(c => c.id == id);
    if (index === -1) throw { status: 404, message: "Mock: Không tìm thấy Client" };
    MOCK_DB.clients.splice(index, 1);
    // Cũng nên xóa cameras và images liên quan (logic phức tạp hơn)
    return {}; // Trả về {} hoặc null, status 204
}


// === API: /api/cameras ===

async function createCamera(cameraData) {
    console.log("Mock API: Đang gọi /api/cameras (POST)");
    await delay(300);
    checkAuth(); // Ai được tạo camera?
    const clientExists = MOCK_DB.clients.some(c => c.id == cameraData.clientId);
    if (!clientExists) throw { status: 400, message: "Mock: Client ID không tồn tại" };
    const newCamera = {
        ...cameraData,
        id: nextId.camera++,
        createdAt: new Date().toISOString()
    };
    MOCK_DB.cameras.push(newCamera);
    return newCamera;
}

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
    // Trả về bản copy để tránh thay đổi trực tiếp DB giả
    return { ...camera };
}

async function updateCamera(id, cameraDetails) {
    console.log(`Mock API: Đang gọi /api/cameras/${id} (PUT)`);
    await delay(400);
    checkAuth();
    let camera = MOCK_DB.cameras.find(c => c.id == id);
    if (!camera) throw { status: 404, message: "Mock: Không tìm thấy Camera" };
    // Không cho đổi clientId
    const { clientId, ...restOfDetails } = cameraDetails;
    Object.assign(camera, restOfDetails);
    return { ...camera }; // Trả về bản copy
}

async function deleteCamera(id) {
    console.log(`Mock API: Đang gọi /api/cameras/${id} (DELETE)`);
    await delay(400);
    checkAuth();
    const index = MOCK_DB.cameras.findIndex(c => c.id == id);
    if (index === -1) throw { status: 404, message: "Mock: Không tìm thấy Camera" };
    MOCK_DB.cameras.splice(index, 1);
    // Cũng nên xóa images liên quan
    return {};
}

// === API: /api/images ===

async function getImageList(page = 0, size = 10, cameraId = null) {
    console.log(`Mock API: Đang gọi /api/images?page=${page}&size=${size}${cameraId ? `&cameraId=${cameraId}` : ''}`);
    await delay(700);
    checkAuth();

    let filteredImages = MOCK_DB.images;
    if (cameraId != null) {
        filteredImages = MOCK_DB.images.filter(img => img.cameraId == cameraId);
    }
    // Sắp xếp theo capturedAt giảm dần (mô phỏng sort mặc định)
    filteredImages.sort((a, b) => new Date(b.capturedAt) - new Date(a.capturedAt));

    const start = page * size;
    const end = start + size;
    const pagedContent = filteredImages.slice(start, end);
    const totalElements = filteredImages.length;
    const totalPages = Math.ceil(totalElements / size);

    // Trả về đối tượng Page<> giống Spring Boot
    return {
        content: pagedContent.map(img => ({ // Build URL giả lập
            ...img,
            // filePath URL sẽ được controller thật xây dựng, ở đây ta dùng ảnh placeholder
            filePath: `https://picsum.photos/seed/${img.id}/400/225`
        })),
        pageable: { pageNumber: page, pageSize: size },
        totalPages: totalPages,
        totalElements: totalElements,
        last: page >= totalPages - 1,
        first: page === 0,
        size: size,
        number: page,
        numberOfElements: pagedContent.length,
        empty: pagedContent.length === 0
    };
}

async function uploadImage(file, cameraId, capturedAt) {
    console.log(`Mock API: Đang gọi /api/images/upload (POST)`);
    await delay(1000); // Giả lập upload
    checkAuth(); // Ai được upload?

    const camera = MOCK_DB.cameras.find(c => c.id == cameraId);
    if (!camera) throw { status: 400, message: "Mock: Camera ID không tồn tại" };
    const clientId = camera.clientId;
    const date = new Date(capturedAt); // Parse capturedAt
    const datePath = `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`;
    const uniqueFileName = `${UUID.randomUUID().toString()}_${file.name}`; // UUID để tránh trùng
    const relativePath = `${clientId}/${cameraId}/${datePath}/${uniqueFileName}`;

    const newImage = {
        id: nextId.image++,
        cameraId: parseInt(cameraId),
        imageName: file.name,
        filePath: relativePath, // Lưu path tương đối
        capturedAt: date.toISOString(),
        uploadedAt: new Date().toISOString(),
        fileSizeKb: (file.size / 1024).toFixed(2),
        metadata: null
    };
    MOCK_DB.images.unshift(newImage); // Thêm vào đầu
    
    // Trả về DTO với URL giả lập
    return {
        ...newImage,
        filePath: `https://picsum.photos/seed/${newImage.id}/400/225`
    };
}


async function deleteImage(id) {
    console.log(`Mock API: Đang gọi /api/images/${id} (DELETE)`);
    await delay(300);
    checkAuth();
    const index = MOCK_DB.images.findIndex(img => img.id == id);
    if (index === -1) throw { status: 404, message: "Mock: Không tìm thấy Image" };
    MOCK_DB.images.splice(index, 1);
    return {};
}

// === API: /api/users === (Giả lập phân quyền Admin)

async function createUser(userData) {
    console.log("Mock API: Đang gọi /api/users (POST)");
    await delay(300);
    checkAuth("ADMIN"); // Chỉ Admin
    if (Object.values(MOCK_DB.users).some(u => u.username === userData.username)) {
         throw { status: 400, message: "Mock: Username đã tồn tại" };
    }
    const newUser = {
        ...userData,
        id: nextId.user++,
        // Server thật sẽ hash password
    };
    MOCK_DB.users[newUser.id.toString()] = newUser;
    const { password, ...userWithoutPassword } = newUser; // Không trả password
    return userWithoutPassword;
}

async function getUserList() {
    console.log("Mock API: Đang gọi /api/users (GET)");
    await delay(200);
    checkAuth("ADMIN"); // Chỉ Admin
    return Object.values(MOCK_DB.users).map(u => {
        const { password, ...userWithoutPassword } = u;
        return userWithoutPassword;
    });
}

async function getUserById(id) {
    console.log(`Mock API: Đang gọi /api/users/${id} (GET)`);
    await delay(100);
    checkAuth(); // Mọi user đăng nhập đều có thể gọi
    
    const user = MOCK_DB.users[id.toString()];
    if (!user) throw { status: 404, message: "Mock: Không tìm thấy User" };

    // Kiểm tra quyền: Admin xem được hết, User chỉ xem được mình
    const currentUserId = localStorage.getItem('mockUserId');
    const currentUserRole = localStorage.getItem('mockUserRole');
    if (currentUserRole !== 'ADMIN' && currentUserId != id) {
         throw { status: 403, message: "Mock: Bạn không có quyền xem user này" };
    }

    const { password, ...userWithoutPassword } = user;
    return userWithoutPassword;
}

async function updateUser(id, userDetails) {
    console.log(`Mock API: Đang gọi /api/users/${id} (PUT)`);
    await delay(400);
    checkAuth();
    let user = MOCK_DB.users[id.toString()];
    if (!user) throw { status: 404, message: "Mock: Không tìm thấy User" };

    // Kiểm tra quyền
    const currentUserId = localStorage.getItem('mockUserId');
    const currentUserRole = localStorage.getItem('mockUserRole');
     if (currentUserRole !== 'ADMIN' && currentUserId != id) {
         throw { status: 403, message: "Mock: Bạn không có quyền sửa user này" };
    }
    
    // Cập nhật các trường cho phép (ví dụ: chỉ email, không cho đổi username/role)
    if (userDetails.email) user.email = userDetails.email;
    // (Server thật sẽ xử lý đổi password riêng)
    
    console.log("Mock User Updated:", user);
    const { password, ...userWithoutPassword } = user;
    return userWithoutPassword;
}

async function deleteUser(id) {
    console.log(`Mock API: Đang gọi /api/users/${id} (DELETE)`);
    await delay(500);
    checkAuth("ADMIN"); // Chỉ Admin
    if (!MOCK_DB.users[id.toString()]) throw { status: 404, message: "Mock: Không tìm thấy User" };
    if (id == 1) throw { status: 400, message: "Mock: Không thể xóa tài khoản admin gốc" }; // Ví dụ
    delete MOCK_DB.users[id.toString()];
    return {};
}

// (Nếu dùng ES Modules, export tất cả các hàm ở cuối)
// export { loginUser, getCurrentUser, logoutUser, getClientList, ... };
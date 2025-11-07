/**
 * =================================================================
 * TỆP API THẬT - Sử dụng Fetch để gọi Server
 * * Gọi các API server Spring Boot đã được xây dựng.
 * * Xử lý xác thực bằng Token (JWT).
 * =================================================================
 */

// Giả sử config.js đã được tải và có biến API_BASE_URL
// Ví dụ: const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Hàm helper: Lấy token từ localStorage và tạo header Authorization.
 * Cũng thêm Content-Type mặc định là JSON.
 * Ném lỗi nếu không tìm thấy token.
 */
function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        // Ném lỗi rõ ràng để xử lý ở tầng gọi (catch)
        const error = new Error("Chưa đăng nhập (không tìm thấy authToken).");
        error.status = 401; // Thêm status để dễ phân biệt
        throw error;
    }
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json' // Mặc định cho hầu hết API
    };
}

/**
 * Hàm helper xử lý lỗi response chung
 * Đọc JSON lỗi nếu có, ném lỗi kèm status code
 */
async function handleResponseError(response) {
    let errorData = { message: `Lỗi HTTP: ${response.status}` };
    try {
        // Cố gắng đọc JSON lỗi từ server
        errorData = await response.json();
    } catch (e) {
        // Nếu server không trả về JSON (vd: lỗi mạng, server sập)
        console.error("Không thể parse JSON lỗi từ server:", response.statusText);
    }
    const error = new Error(errorData.message || `Lỗi HTTP: ${response.status}`);
    error.status = response.status; // Gắn status vào lỗi
    throw error;
}

/**
 * Hàm helper xử lý lỗi chung (bao gồm lỗi mạng và lỗi 401/403)
 */
function handleApiError(error, functionName = "API call") {
    console.error(`Lỗi khi gọi ${functionName}:`, error);
    // Tự động chuyển về login nếu lỗi 401 hoặc 403
    if (error.status === 401 || error.status === 403 || error.message.includes("Chưa đăng nhập")) {
        console.warn("Token không hợp lệ hoặc hết hạn. Đang chuyển hướng về trang đăng nhập...");
        localStorage.removeItem('authToken'); // Xóa token cũ (nếu có)
        // Đảm bảo bạn đang ở trang web, không phải worker hay môi trường khác
        if (typeof window !== 'undefined') {
            window.location.href = 'login.html';
        }
    }
    // Ném lỗi ra ngoài để UI có thể xử lý (hiển thị thông báo)
    throw error;
}


// === API: /api/auth ===

async function loginUser(username, password) {
    const url = `${API_BASE_URL}/auth/login`;
    const bodyData = JSON.stringify({ username, password });
    console.log(`Gọi API đăng nhập: ${url}`);

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: bodyData
        });

        const data = await response.json(); // Đọc JSON (cả thành công và lỗi)

        if (!response.ok) {
            await handleResponseError(response); // Ném lỗi nếu status không phải 2xx
        }

        if (data.token) {
            localStorage.setItem('authToken', data.token);
            console.log("Đăng nhập thành công, đã lưu token.");

            let userRole = null; // Khởi tạo biến role để sử dụng cho chuyển hướng

            // 1. CỐ GẮNG LẤY THÔNG TIN USER VÀ ROLE
            try {
                const userInfo = await getCurrentUser(); // Gọi API /me
                
                if (userInfo.id) localStorage.setItem('id', userInfo.id);
                if (userInfo.username) localStorage.setItem('username', userInfo.username);
                
                // Lưu role và cập nhật biến cục bộ
                if (userInfo.role) {
                    localStorage.setItem('role', userInfo.role);
                    userRole = userInfo.role; 
                }
                
            } catch (userError) {
                // Nếu /me bị lỗi (ví dụ: token hợp lệ nhưng database lỗi), 
                // ta cảnh báo nhưng vẫn cho phép chuyển hướng dựa trên giả định User thường
                console.warn("Không thể lấy thông tin user sau khi đăng nhập. Chuyển hướng về trang mặc định.", userError);
            }
            
            // 2. LOGIC CHUYỂN HƯỚNG TỔNG QUÁT (ĐƯỢC GỌI TRONG MỌI TRƯỜNG HỢP TOKEN HỢP LỆ)
            // Kiểm tra role đã lấy được, hoặc role đã có trong localStorage
            const finalRole = userRole || localStorage.getItem('role');
           
            if (finalRole === 'ADMIN') {
                console.log("Vai trò Admin được xác nhận. Chuyển hướng đến Admin Dashboard.");
                // Chuyển hướng Admin đến thư mục Admin
              
                 
                window.location.href = '/admin/admin_dashboard.html'; 
            } else {
                console.log("Vai trò User được xác nhận. Chuyển hướng đến Live Feed.");
                // Chuyển hướng Viewer/User đến trang mặc định
                 
                window.location.href = 'live_feed.html'; 
            }
            
            // Dòng này không bao giờ được gọi nếu chuyển hướng thành công
            return data; 

        } else {
            throw new Error("Server không trả về token.");
        }
        

    } catch (error) {
        // Xử lý lỗi đăng nhập (401, 400)
        console.error('Lỗi API đăng nhập:', error);
        throw error;
    }
}

async function getCurrentUser() {
    const url = `${API_BASE_URL}/auth/me`;
    console.log("Đang gọi API /api/auth/me (với Token)...");
    try {
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, "getCurrentUser");
    }
}

async function logoutUser() {
    console.log("Đang đăng xuất (xóa token)...");
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    console.log("Đã xóa token và user info.");
    // Chuyển hướng ngay lập tức
    if (typeof window !== 'undefined') {
        window.location.href = 'login.html';
    }
    return Promise.resolve({ message: "Đã đăng xuất" });
}

// === API: /api/clients ===

async function createClient(clientData) {
    const url = `${API_BASE_URL}/clients`;
    console.log("Gọi API tạo client:", url);
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(clientData)
        });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, "createClient");
    }
}

async function getClientList() {
    const url = `${API_BASE_URL}/clients`;
    console.log("Gọi API lấy danh sách client:", url);
    try {
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, "getClientList");
    }
}

async function getClientById(id) {
    const url = `${API_BASE_URL}/clients/${id}`;
    console.log(`Gọi API lấy client ${id}:`, url);
    try {
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, `getClientById(${id})`);
    }
}

async function getClientList() {
    // URL: API_BASE_URL/clients
    const url = `${API_BASE_URL}/clients`; 
    console.log("Gọi API lấy danh sách client:", url);
    
    try {
        
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() });
        
        if (!response.ok) {
            await handleResponseError(response);
        }
        return await response.json();
    } catch (error) {
        handleApiError(error, "getClientList");
    }
}

async function updateClient(id, clientDetails) {
    const url = `${API_BASE_URL}/clients/${id}`;
    console.log(`Gọi API cập nhật client ${id}:`, url);
    try {
        const response = await fetch(url, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(clientDetails)
        });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, `updateClient(${id})`);
    }
}

async function deleteClient(id) {
    const url = `${API_BASE_URL}/clients/${id}`;
    console.log(`Gọi API xóa client ${id}:`, url);
    try {
        const response = await fetch(url, { method: 'DELETE', headers: getAuthHeaders() });
        // DELETE thành công thường trả về 204 No Content, không có body
        if (!response.ok && response.status !== 204) {
             await handleResponseError(response);
        }
        return {}; // Không có nội dung trả về
    } catch (error) {
        handleApiError(error, `deleteClient(${id})`);
    }
}


// === API: /api/cameras ===
// Trong tệp API JavaScript của bạn
async function getCameraListByClient(clientId) {
    // Gọi API đã được cấu hình trên Server: GET /api/cameras?clientId={id}
    const url = `${API_BASE_URL}/cameras?clientId=${clientId}`; 
    console.log(`Gọi API lấy camera theo Client ID (QUERY PARAM): ${url}`);
    
    try {
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() });
        if (!response.ok) await handleResponseError(response);
        
        // Trả về danh sách Camera
        return await response.json();
    } catch (error) {
        handleApiError(error, `getCameraListByClient(${clientId})`);
    }
}
async function createCamera(cameraData) {
    const url = `${API_BASE_URL}/cameras`;
     console.log("Gọi API tạo camera:", url);
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(cameraData)
        });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, "createCamera");
    }
}

async function getCameraList() {
    const url = `${API_BASE_URL}/cameras`;
     console.log("Gọi API lấy danh sách camera:", url);
    try {
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, "getCameraList");
    }
}

// async function getCameraListByClient(clientId) {
//     const url = `${API_BASE_URL}/cameras/by-client/${clientId}`;
//     console.log(`Gọi API lấy camera theo client ${clientId}:`, url);
//     try {
//         const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() });
//         if (!response.ok) await handleResponseError(response);
//         return await response.json();
//     } catch (error) {
//         handleApiError(error, `getCameraListByClient(${clientId})`);
//     }
// }

async function getCameraById(id) {
    const url = `${API_BASE_URL}/cameras/${id}`;
    console.log(`Gọi API lấy camera ${id}:`, url);
    try {
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, `getCameraById(${id})`);
    }
}

async function updateCamera(id, cameraDetails) {
    const url = `${API_BASE_URL}/cameras/${id}`;
    console.log(`Gọi API cập nhật camera ${id}:`, url);
    // Nhớ rằng server sẽ không cho cập nhật clientId
    const { clientId, ...detailsToSend } = cameraDetails;
    try {
        const response = await fetch(url, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(detailsToSend)
        });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, `updateCamera(${id})`);
    }
}

async function deleteCamera(id) {
    const url = `${API_BASE_URL}/cameras/${id}`;
    console.log(`Gọi API xóa camera ${id}:`, url);
    try {
        const response = await fetch(url, { method: 'DELETE', headers: getAuthHeaders() });
         if (!response.ok && response.status !== 204) {
             await handleResponseError(response);
        }
        return {};
    } catch (error) {
        handleApiError(error, `deleteCamera(${id})`);
    }
}

// === API: /api/images ===

async function getImageList(page = 0, size = 20, cameraId = null, start = null, end = null) {
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', size);

    params.append('sort', 'capturedAt,desc');

    if (cameraId != null) {
        params.append('cameraId', cameraId);
    }
    if (start != null) {
        params.append('start', start); 
    }
    if (end != null) {
        params.append('end', end); 
    }
    const url = `${API_BASE_URL}/images?${params.toString()}`;
    
    console.log(`Gọi API lấy danh sách ảnh: ${url}`);

    // 3. Gọi API (Giữ nguyên logic fetch của bạn)
    try {
        const response = await fetch(url, { 
            method: 'GET', 
            headers: getAuthHeaders() // Giả định hàm này lấy token
        });
        
        if (!response.ok) {
            // Xử lý lỗi (giữ nguyên hàm của bạn)
            await handleResponseError(response); 
        }
        
        return await response.json(); // Server trả về đối tượng Page<>
        
    } catch (error) {
        // Xử lý lỗi (giữ nguyên hàm của bạn)
        handleApiError(error, "getImageList");
        throw error; // Ném lỗi ra để code gọi nó (trong HTML) biết
    }
}
async function uploadImage(file, cameraId, capturedAt) {
    const url = `${API_BASE_URL}/images/upload`;
    console.log(`Gọi API upload ảnh cho camera ${cameraId}:`, url);

    // Sử dụng FormData cho multipart/form-data
    const formData = new FormData();
    formData.append('file', file);
    formData.append('cameraId', cameraId);
    // Gửi Timestamp dưới dạng chuỗi ISO 8601 UTC mà Spring Boot có thể parse
    formData.append('capturedAt', capturedAt.toISOString());

    // Lấy token riêng, không set Content-Type
    const token = localStorage.getItem('authToken');
     if (!token) {
        const error = new Error("Chưa đăng nhập (không tìm thấy authToken).");
        error.status = 401;
        throw error;
    }

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                 // KHÔNG set 'Content-Type', trình duyệt sẽ tự làm cho FormData
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });

        const data = await response.json(); // Đọc JSON trả về (ảnh mới hoặc lỗi)
        if (!response.ok) { // Kiểm tra status (vd: 201 Created là OK)
             throw new Error(data.message || `Lỗi HTTP: ${response.status}`);
        }
        return data; // Trả về ảnh mới từ server (đã có URL đầy đủ)

    } catch (error) {
         handleApiError(error, "uploadImage"); // Xử lý lỗi chung (401,...)
    }
}


async function deleteImage(id) {
    const url = `${API_BASE_URL}/images/${id}`;
    console.log(`Gọi API xóa ảnh ${id}:`, url);
    try {
        const response = await fetch(url, { method: 'DELETE', headers: getAuthHeaders() });
        if (!response.ok && response.status !== 204) {
             await handleResponseError(response);
        }
        return {};
    } catch (error) {
        handleApiError(error, `deleteImage(${id})`);
    }
}
async function deleteBatchImages(imageIds = []) {
    if (!imageIds || imageIds.length === 0) {
        return { message: "Không có ảnh nào được chọn" };
    }

    // Endpoint xóa hàng loạt (DELETE /api/images)
    const url = `${API_BASE_URL}/images`;
    console.log(`Gọi API xóa hàng loạt ${imageIds.length} ảnh`);
    const body = JSON.stringify({
        photoIds: imageIds
    });

    try {
        const headers = getAuthHeaders();
        headers.set('Content-Type', 'application/json'); // QUAN TRỌNG

        const response = await fetch(url, {
            method: 'DELETE',
            headers: headers,
            body: body 
        });

        if (!response.ok) {
            await handleResponseError(response);
        }
        
        return await response.json(); // Server trả về { message: "..." }
        
    } catch (error) {
        handleApiError(error, "deleteBatchImages");
        throw error;
    }
}
// === API: /api/users ===

async function createUser(userData) {
    const url = `${API_BASE_URL}/users`;
     console.log("Gọi API tạo user:", url);
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: getAuthHeaders(), // Yêu cầu quyền Admin (server kiểm tra)
            body: JSON.stringify(userData)
        });
        if (!response.ok) await handleResponseError(response);
        return await response.json(); // Trả về user mới (không pass)
    } catch (error) {
        handleApiError(error, "createUser");
    }
}

async function getUserList() {
    const url = `${API_BASE_URL}/users`;
    console.log("Gọi API lấy danh sách user:", url);
    try {
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() }); // Yêu cầu Admin
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, "getUserList");
    }
}

async function getUserById(id) {
    const url = `${API_BASE_URL}/users/${id}`;
     console.log(`Gọi API lấy user ${id}:`, url);
    try {
        const response = await fetch(url, { method: 'GET', headers: getAuthHeaders() }); // Yêu cầu Admin hoặc chính user
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, `getUserById(${id})`);
    }
}

async function updateUser(id, userDetails) {
    const url = `${API_BASE_URL}/users/${id}`;
     console.log(`Gọi API cập nhật user ${id}:`, url);
    try {
        const response = await fetch(url, {
            method: 'PUT',
            headers: getAuthHeaders(), // Yêu cầu Admin hoặc chính user
            body: JSON.stringify(userDetails) // Chỉ gửi các trường được phép cập nhật
        });
        if (!response.ok) await handleResponseError(response);
        return await response.json();
    } catch (error) {
        handleApiError(error, `updateUser(${id})`);
    }
}

async function deleteUser(id) {
    const url = `${API_BASE_URL}/users/${id}`;
     console.log(`Gọi API xóa user ${id}:`, url);
    try {
        const response = await fetch(url, { method: 'DELETE', headers: getAuthHeaders() }); // Yêu cầu Admin
        if (!response.ok && response.status !== 204) {
             await handleResponseError(response);
        }
        return {};
    } catch (error) {
        handleApiError(error, `deleteUser(${id})`);
    }
}

// (Nếu dùng ES Modules, export các hàm ở cuối)
// export { loginUser, getAuthHeaders, handleApiError, getCameraList, ... };
async function registerUser(userData) {
    // SỬ DỤNG ENDPOINT CÔNG KHAI /auth/register
    const url = `${API_BASE_URL}/auth/register`; 
    console.log(`Gọi API đăng ký: ${url}`);

    try {
        const response = await fetch(url, {
            method: 'POST',
            // API công khai KHÔNG cần Authorization header
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userData)
        });

        const data = await response.json(); // Đọc JSON (cả thành công và lỗi)

        if (!response.ok) {
            // Xử lý lỗi từ server (vd: 400 Bad Request, username đã tồn tại)
            // Lỗi này sẽ được ném ra và catch ở UI (register.html)
            throw new Error(data.message || `Đăng ký thất bại với mã lỗi: ${response.status}`);
        }

        console.log("Đăng ký thành công:", data.username);
        return data; // Trả về { message: "...", username: "...", id: ... }

    } catch (error) {
        // Xử lý lỗi mạng hoặc lỗi không rõ khác
        console.error('Lỗi API đăng ký:', error);
        // Ném lỗi ra ngoài để UI hiển thị thông báo
        throw error; 
    }
}
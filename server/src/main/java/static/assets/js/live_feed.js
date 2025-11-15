// Chờ HTML tải xong
document.addEventListener('DOMContentLoaded', () => {

    const liveFeedGrid = document.getElementById('live-feed-grid');
    // Có thể lấy thêm div gallery ảnh chụp gần đây nếu cần
    // const recentCapturesGallery = document.getElementById('recent-captures-gallery');

    /**
     * Hàm tải và hiển thị danh sách camera live.
     */
    async function loadLiveCameras() {
        if (!liveFeedGrid) return; // Kiểm tra

        liveFeedGrid.innerHTML = '<p class="status-message">Đang tải danh sách camera...</p>';

        try {
            // 1. Gọi API lấy danh sách camera
            const cameras = await getCameraList();

            liveFeedGrid.innerHTML = ''; // Xóa thông báo loading

            // 2. Kiểm tra kết quả
            if (!cameras || cameras.length === 0) {
                liveFeedGrid.innerHTML = '<p class="status-message">Không tìm thấy camera nào.</p>';
                return;
            }

            // 3. Lặp qua từng camera và tạo thẻ HTML
            cameras.forEach(camera => {
                const cameraCard = createCameraCard(camera);
                liveFeedGrid.appendChild(cameraCard);
            });

        } catch (error) {
            // 4. Hiển thị lỗi
            liveFeedGrid.innerHTML = '<p class="status-message error">Lỗi khi tải danh sách camera.</p>';
        }
    }

    /**
     * Hàm tạo thẻ HTML (card) cho một camera.
     * @param {object} camera Đối tượng camera từ API (ví dụ: { id: 1, cameraName: "Sân trước", active: true }).
     * @returns {HTMLElement} Phần tử div.card.
     */
    function createCameraCard(camera) {
        const card = document.createElement('div');
        card.className = 'card';

        // Lấy trạng thái camera (ví dụ từ trường 'active')
        const isOnline = camera.active; // Giả sử API trả về trường 'active'
        const statusClass = isOnline ? '' : 'offline';
        const statusTitle = isOnline ? 'Online' : 'Offline';
        const isButtonDisabled = !isOnline;

        // Tạo nội dung HTML
        // Lưu ý: Phần src của img cần được cập nhật động (ví dụ: dùng MJPEG stream)
        // Hiện tại chỉ để placeholder
        card.innerHTML = `
            <img src="placeholder-live-${camera.id}.jpg" alt="Feed from ${camera.cameraName}">
            <div class="card-content">
                <div>
                    <strong>${camera.cameraName}</strong>
                    <span class="status-indicator ${statusClass}" title="${statusTitle}"></span>
                </div>
                <button class="btn btn-primary btn-sm capture-button" data-camera-id="${camera.id}" ${isButtonDisabled ? 'disabled' : ''}>
                    Capture Snapshot
                </button>
            </div>
        `;

        // (Tùy chọn) Thêm sự kiện click cho nút "Capture Snapshot"
        const captureButton = card.querySelector('.capture-button');
        if (captureButton) {
            captureButton.addEventListener('click', () => {
                handleCaptureSnapshot(camera.id); // Gọi hàm xử lý chụp ảnh
            });
        }

        return card;
    }

    /**
     * Hàm xử lý khi nhấn nút Capture Snapshot (cần viết thêm API call).
     * @param {number} cameraId ID của camera cần chụp.
     */
    function handleCaptureSnapshot(cameraId) {
        console.log(`Chụp ảnh từ camera ID: ${cameraId}`);
        // TODO: Gọi API server để yêu cầu client chụp ảnh từ camera này
        // Ví dụ: await captureSnapshotApiCall(cameraId);
        alert(`Đã yêu cầu chụp ảnh từ camera ${cameraId} (chức năng chưa hoàn thiện)`);
    }

    // --- (Tùy chọn) Tải ảnh chụp gần đây ---
    async function loadRecentCaptures() {
        // Tương tự như loadLiveCameras, gọi API lấy ảnh gần đây
        // const recentImages = await getRecentImageList(); // Hàm này cần tạo trong api.js
        // ... xử lý và hiển thị vào #recent-captures-gallery ...
    }

    // Bắt đầu tải dữ liệu khi trang sẵn sàng
    loadLiveCameras();
    // loadRecentCaptures(); // Gọi nếu có

});
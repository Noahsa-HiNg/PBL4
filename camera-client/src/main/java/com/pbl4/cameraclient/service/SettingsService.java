package com.pbl4.cameraclient.service;

import javafx.stage.DirectoryChooser; // Dùng để mở cửa sổ chọn thư mục
import javafx.stage.Window; // Dùng để lấy cửa sổ cha

import java.io.File;
import java.util.prefs.Preferences; // Dùng để lưu cài đặt

public class SettingsService {

    // Lấy một "nút" lưu trữ riêng cho package của ứng dụng
    // (Java sẽ tự xử lý việc lưu ở đâu, ví dụ: Windows Registry)
    private final Preferences prefs;
    
    // Tên của khóa (key) dùng để lưu đường dẫn
    private static final String KEY_SNAPSHOT_SAVE_PATH = "snapshotSavePath";

    public SettingsService() {
        // Lấy node lưu trữ cho lớp này
        this.prefs = Preferences.userNodeForPackage(SettingsService.class);
    }

    /**
     * Lấy đường dẫn thư mục lưu ảnh đã được người dùng chọn trước đó.
     * @return Đường dẫn (String), hoặc null nếu chưa chọn.
     */
    public String getSnapshotSavePath() {
        // Lấy giá trị từ Preferences, trả về null nếu không tìm thấy
        return prefs.get(KEY_SNAPSHOT_SAVE_PATH, null);
    }

    /**
     * Lưu đường dẫn thư mục mới vào Preferences.
     * @param path Đường dẫn thư mục.
     */
    public void setSnapshotSavePath(String path) {
        if (path != null) {
            prefs.put(KEY_SNAPSHOT_SAVE_PATH, path);
            System.out.println("Đã lưu đường dẫn snapshot: " + path);
        } else {
            prefs.remove(KEY_SNAPSHOT_SAVE_PATH); // Xóa nếu path là null
        }
    }

    /**
     * Mở cửa sổ JavaFX cho phép người dùng chọn một thư mục.
     * Tự động lưu đường dẫn nếu người dùng chọn.
     * @param ownerWindow Cửa sổ hiện tại (lấy từ Scene) để làm cửa sổ cha.
     * @return Đường dẫn đã chọn, hoặc null nếu người dùng hủy.
     */
    public String askForSnapshotSavePath(Window ownerWindow) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Chọn thư mục để lưu ảnh chụp (Snapshots)");

        // Thử đặt thư mục mặc định là thư mục đã lưu trước đó
        String currentPath = getSnapshotSavePath();
        if (currentPath != null && new File(currentPath).isDirectory()) {
            directoryChooser.setInitialDirectory(new File(currentPath));
        }

        // Hiển thị cửa sổ chọn
        File selectedDirectory = directoryChooser.showDialog(ownerWindow);

        if (selectedDirectory != null) {
            // Nếu người dùng chọn "OK", lưu đường dẫn mới
            String newPath = selectedDirectory.getAbsolutePath();
            setSnapshotSavePath(newPath);
            return newPath;
        } else {
            // Người dùng nhấn "Cancel"
            return null;
        }
    }
}
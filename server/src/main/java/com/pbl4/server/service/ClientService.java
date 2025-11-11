package com.pbl4.server.service;

import com.pbl4.server.dto.CameraDTO;
import com.pbl4.server.dto.ClientDTO;
import com.pbl4.server.dto.ClientRegisterRequest;
import com.pbl4.server.dto.ClientRegisterResponse;
import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ClientRepository;
import com.pbl4.server.repository.UserRepository;

import io.jsonwebtoken.lang.Collections;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import pbl4.common.model.Client; // DTO

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.UserRepository;@Service
public class ClientService {
	
    private final ClientRepository clientRepository;

    private final UserRepository userRepository;
    private final CameraRepository cameraRepository;
    private static final long SECONDS_IN_DAY = 86400;
    private static final long MAX_PING_INTERVAL_SECONDS = 90000; // 1 ngày + 1 giờ
    private static final long MIN_PING_INTERVAL_SECONDS = 30;
    public ClientService(ClientRepository clientRepository, UserRepository userRepository,CameraRepository cameraRepository) {

        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.cameraRepository = cameraRepository;
    }
    public ClientRegisterResponse registerOrGetClient(ClientRegisterRequest request, String username, String remoteIpAddress) {

        // 1. Tìm UserEntity dựa vào username từ token (đảm bảo user tồn tại)
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy User với username: " + username));

        // 2. Tìm ClientEntity dựa trên machineId VÀ user (unique constraint)
        Optional<ClientEntity> existingClientOpt = clientRepository.findByMachineIdAndUser(request.getMachineId(), user);

        if (existingClientOpt.isPresent()) {
            // ----- TRƯỜNG HỢP 1: Client ĐÃ TỒN TẠI -----
            ClientEntity client = existingClientOpt.get();

            // Cập nhật thông tin mới nhất cho client
            client.setIpAddress(remoteIpAddress);       // Cập nhật IP hiện tại
            client.setStatus("online");                 // Đánh dấu là đang online
            client.setLastHeartbeat(Timestamp.from(Instant.now())); // Cập nhật thời gian kết nối cuối
            client.setClientName(request.getClientName()); // Cập nhật tên nếu client có đổi
            clientRepository.save(client);              // Lưu thay đổi vào DB

            // Lấy danh sách camera liên kết với client này
            // Do dùng FetchType.EAGER nên cameras đã được tải cùng client
            List<CameraDTO> cameras = client.getCameras().stream()
                    .map(CameraDTO::new) // Chuyển từ CameraEntity sang CameraDTO
                    .collect(Collectors.toList());

            System.out.println("Client '" + client.getClientName() + "' (ID: " + client.getId() + ") đã kết nối lại. Tìm thấy " + cameras.size() + " camera.");

            // Trả về thông tin client ID, thông báo và danh sách camera
            ClientDTO clientDTO = new ClientDTO(client);
            return new ClientRegisterResponse(clientDTO, "Client đã đăng ký. Tải lại danh sách camera.", cameras);

        } 
        else {
            // ----- TRƯỜNG HỢP 2: Client CHƯA TỒN TẠI -----
            ClientEntity newClient = new ClientEntity();
            newClient.setUser(user);                      // Liên kết với user
            newClient.setMachineId(request.getMachineId()); // ID duy nhất của máy
            newClient.setClientName(request.getClientName()); // Tên client gửi lên
            newClient.setIpAddress(remoteIpAddress);      // IP hiện tại

            // Thiết lập các giá trị mặc định theo CSDL
            newClient.setStatus("online");                // Trạng thái ban đầu
            newClient.setImageWidth(1280);                // Giá trị mặc định
            newClient.setImageHeight(720);                // Giá trị mặc định
            newClient.setCaptureIntervalSeconds(5);       // Giá trị mặc định
            newClient.setCompressionQuality(85);          // Giá trị mặc định
            newClient.setCreatedAt(Timestamp.from(Instant.now())); // Thời gian tạo
            newClient.setLastHeartbeat(Timestamp.from(Instant.now())); // Thời gian kết nối đầu tiên

            // Lưu client mới vào DB
            ClientEntity savedClient = clientRepository.save(newClient);

            System.out.println("Client mới '" + savedClient.getClientName() + "' (ID: " + savedClient.getId() + ") đã được đăng ký cho user '" + username + "'.");

            // (Tùy chọn) Cập nhật active_client_id trong bảng Users nếu user chưa có client nào đang active
            if (user.getActiveClient() == null) {
                user.setActiveClient(savedClient);
                userRepository.save(user);
                System.out.println("Đã đặt client ID " + savedClient.getId() + " làm active client cho user '" + username + "'.");
            }
            ClientDTO clientDTO = new ClientDTO(savedClient);

            // Trả về thông tin client ID mới, thông báo và danh sách camera RỖNG
            return new ClientRegisterResponse(clientDTO, "Client mới đã được đăng ký thành công.", Collections.emptyList());
        }
    }

    
    /**
     * TẠO MỚI CLIENT - Đã sửa để nhận ID người dùng đang đăng nhập.
     * @param clientDto Dữ liệu Client từ Client.
     * @param userId ID của người dùng đang đăng nhập (lấy từ JWT Token).
     * @return Client DTO đã được tạo.
     */
    public Client createClient(Client clientDto, Long userId) {
        
        // 1. Tìm UserEntity (BẮT BUỘC)
        UserEntity userEntity = userRepository.findById(userId.intValue()) // Dùng findById
            .orElseThrow(() -> new RuntimeException("Authenticated user not found."));

        // 2. Chuyển đổi DTO sang Entity
        ClientEntity clientEntity = toEntity(clientDto);
        
        // 3. GÁN KHÓA NGOẠI BẮT BUỘC (clientEntity.user)
        clientEntity.setUser(userEntity); 
        
        // 4. Lưu và trả về
        ClientEntity savedEntity = clientRepository.save(clientEntity);
        return toDto(savedEntity);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    /**
     * Phương thức mới: Lấy danh sách Client theo User ID sở hữu.
     * Phương thức này thay thế cho logic cũ getAllClients().
     * @param userId ID của người dùng đang đăng nhập (Long).
     * @return Danh sách Client DTO.
     */
    public List<Client> getClientsByUserId(Long userId) {
        // Chuyển đổi Long userId sang int
        int userIdInt = userId.intValue();
        
        // Gọi phương thức Repository đã lọc theo User ID
        List<ClientEntity> entities = clientRepository.findByUserId(userIdInt);
        
        // Chuyển đổi Entity sang DTO và trả về
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    // [Tùy chọn] Sửa đổi phương thức GET ONE để đảm bảo phân quyền
    public Client getClientById(int id, Long currentUserId) {
        ClientEntity entity = clientRepository.findByIdAndUserId(id, currentUserId.intValue());
        if (entity == null) {
            // Ném lỗi nếu Client không tồn tại HOẶC không thuộc sở hữu của người dùng
            throw new RuntimeException("Client not found or access denied.");
        }
        return toDto(entity);
    }

//    public Client updateClient(int id, Client clientDto) {
//        ClientEntity existingEntity = clientRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
//
//        existingEntity.setClientName(clientDto.getClientName());
//        existingEntity.setStatus(clientDto.getStatus());
//
//        ClientEntity updatedEntity = clientRepository.save(existingEntity);
//        return toDto(updatedEntity);
//    }
//
    public void deleteClient(int id) {
        if (!clientRepository.existsById(id)) {
            throw new RuntimeException("Client not found with id: " + id);
        }
        clientRepository.deleteById(id);
    }

    public String getUsernameByClientId(int clientId) {
        return clientRepository.findUsernameByClientId(clientId)
                               .orElse(null); // Trả về null nếu không tìm thấy
    }
//    public Client updateClient(int id,Client clientDto,Long currentUserId) {
//    	ClientEntity existingEntity = clientRepository.findByIdAndUserId(id, currentUserId.intValue());
//    	if (existingEntity == null) {
//            // Ném ra lỗi. Controller sẽ bắt lỗi này và trả về 404 hoặc 403
//            throw new EntityNotFoundException("Access Denied: Client not found with id: " + id + " or user " + currentUserId + " does not own it.");
//        }
//    	if (clientDto.getClientName()!=null) {
//    		existingEntity.setClientName(clientDto.getClientName());
//    	}
//    	if (clientDto.getImageHeight()!=null) {
//    		existingEntity.setImageHeight(clientDto.getImageHeight());
//    	}
//    	if (clientDto.getImageWidth()!=null) {
//    		existingEntity.setImageWidth(clientDto.getImageWidth());
//    	}
//    	
//        if (clientDto.getCompressionQuality() != null) {
//            existingEntity.setCompressionQuality(clientDto.getCompressionQuality());
//        }
//        if (clientDto.getCaptureIntervalSeconds() != null) {
//            existingEntity.setCaptureIntervalSeconds(clientDto.getCaptureIntervalSeconds());
//        }
//        ClientEntity updatedEntity = clientRepository.save(existingEntity);
//    	
//    	return toDto(updatedEntity);            
//    }
    
    // --- Helper Methods for Mapping ---
    private Client toDto(ClientEntity entity) {
    	Client dto = new Client();
        dto.setId(entity.getId());
        dto.setClientName(entity.getClientName());
        dto.setStatus(entity.getStatus());
        dto.setIpAddress(entity.getIpAddress());
        dto.setCaptureIntervalSeconds(entity.getCaptureIntervalSeconds());
        dto.setImageWidth(entity.getImageWidth());
        dto.setImageHeight(entity.getImageHeight());
        dto.setCompressionQuality(entity.getCompressionQuality());
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
        }
        return dto;
    }

    private ClientEntity toEntity(Client dto) {
        ClientEntity entity = new ClientEntity();
        entity.setClientName(dto.getClientName());
        entity.setIpAddress(dto.getIpAddress());
        entity.setStatus(dto.getStatus());
        entity.setIpAddress(dto.getIpAddress());
        entity.setCaptureIntervalSeconds(dto.getCaptureIntervalSeconds());
        entity.setImageWidth(dto.getImageWidth());
        entity.setImageHeight(dto.getImageHeight());
        entity.setCompressionQuality(dto.getCompressionQuality());
        return entity;
    }
    
    public void clientPingResponded(int clientId) {
        clientRepository.findById(clientId).ifPresent(client -> {
            // Đặt lại mốc thời gian và trạng thái chờ
            client.setLastImageReceived(new Timestamp(System.currentTimeMillis())); 
            client.setLastPingAttempt(null); // Reset Ping Attempt
            client.setStatus("SUSPENDED"); // Giữ trạng thái đang treo
            clientRepository.save(client);
        });
    }
    public long calculateDynamicPingInterval(int clientId) {
        ClientEntity client = clientRepository.findById(clientId).orElse(null);
        if (client == null ||  client.getCaptureIntervalSeconds() <= 0) {
            return 180; // Mặc định 3 phút
        }

        long captureInterval = (long) client.getCaptureIntervalSeconds();
        long calculatedInterval;

        if (captureInterval > SECONDS_IN_DAY) {
            calculatedInterval = MAX_PING_INTERVAL_SECONDS;
        } else {
            calculatedInterval = captureInterval * 2;
        }

        return Math.max(calculatedInterval, MIN_PING_INTERVAL_SECONDS);
    }
    public void setClientOfflineAndTurnOffCameras(int clientId) {
        clientRepository.findById(clientId).ifPresent(client -> {
            // 1. Tắt tất cả Cameras
            cameraRepository.updateAllByClientId(clientId, false); 
            
            // 2. Đặt trạng thái Client
            client.setStatus("OFFLINE"); 
            client.setLastPingAttempt(null);
            clientRepository.save(client);
        });
        
    }
    public int checkStatus(int clientId) {
    	return clientRepository.findStatusById(clientId);
    }
    
}
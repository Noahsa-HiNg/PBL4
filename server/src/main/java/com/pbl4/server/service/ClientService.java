package com.pbl4.server.service;

import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.repository.ClientRepository;
import org.springframework.stereotype.Service;
import pbl4.common.model.Client; // DTO

import java.util.List;
import java.util.stream.Collectors;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.UserRepository;@Service
public class ClientService {
	private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
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

    public Client updateClient(int id, Client clientDto) {
        ClientEntity existingEntity = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
        
        // Cập nhật các trường
        existingEntity.setClientName(clientDto.getClientName());
        existingEntity.setStatus(clientDto.getStatus());
        // ... cập nhật các trường khác nếu cần

        ClientEntity updatedEntity = clientRepository.save(existingEntity);
        return toDto(updatedEntity);
    }

    public void deleteClient(int id) {
        if (!clientRepository.existsById(id)) {
            throw new RuntimeException("Client not found with id: " + id);
        }
        clientRepository.deleteById(id);
    }

    // --- Helper Methods for Mapping ---
    private Client toDto(ClientEntity entity) {
        Client dto = new Client();
        dto.setId(entity.getId());
        dto.setClientName(entity.getClientName());
        dto.setIpAddress(entity.getIpAddress());
        dto.setStatus(entity.getStatus());
        // ... sao chép các trường khác
        return dto;
    }

    private ClientEntity toEntity(Client dto) {
        ClientEntity entity = new ClientEntity();
        entity.setClientName(dto.getClientName());
        entity.setIpAddress(dto.getIpAddress());
        entity.setStatus(dto.getStatus());
        // ... sao chép các trường khác
        return entity;
    }
}
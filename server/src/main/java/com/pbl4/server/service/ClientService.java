package com.pbl4.server.service;

import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.repository.ClientRepository;
import org.springframework.stereotype.Service;
import pbl4.common.model.Client; // DTO

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client createClient(Client clientDto) {
        ClientEntity clientEntity = toEntity(clientDto);
        ClientEntity savedEntity = clientRepository.save(clientEntity);
        return toDto(savedEntity);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Client getClientById(int id) {
        ClientEntity entity = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
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
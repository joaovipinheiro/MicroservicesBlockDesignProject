package br.com.arenamanager.player_service.service;


import br.com.arenamanager.player_service.dto.PlayerRequestDTO;
import br.com.arenamanager.player_service.dto.PlayerResponseDTO;
import br.com.arenamanager.player_service.entity.Player;
import br.com.arenamanager.player_service.exception.BusinessException;
import br.com.arenamanager.player_service.exception.ResourceNotFoundException;
import br.com.arenamanager.player_service.mapper.PlayerMapper;
import br.com.arenamanager.player_service.repository.PlayerRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository repository;
    private final PlayerMapper mapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public PlayerResponseDTO createPlayer(PlayerRequestDTO dto) {
        validateUniqueFields(dto.email(), dto.nickname(), null);

        Player player = mapper.toEntity(dto);
        player = repository.save(player);

        meterRegistry.counter("players.created.total", "service", "player-service").increment();
        log.info("Jogador criado: id={}, email={}", player.getId(), player.getEmail());

        return mapper.toResponseDTO(player);
    }

    @Transactional(readOnly = true)
    public List<PlayerResponseDTO> listAll() {
        return repository.findAll().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponseDTO getById(Long id) {
        Player player = findEntityById(id);
        return mapper.toResponseDTO(player);
    }

    @Transactional
    public PlayerResponseDTO updatePlayer(Long id, PlayerRequestDTO dto) {
        Player player = findEntityById(id);

        validateUniqueFields(dto.email(), dto.nickname(), id);

        mapper.updateEntityFromDto(dto, player);
        player = repository.save(player);

        meterRegistry.counter("players.updated.total", "service", "player-service").increment();
        log.info("Jogador atualizado: id={}", player.getId());

        return mapper.toResponseDTO(player);
    }

    @Transactional
    public void deletePlayer(Long id) {
        Player player = findEntityById(id);
        repository.delete(player);

        meterRegistry.counter("players.deleted.total", "service", "player-service").increment();
        log.info("Jogador removido: id={}", id);
    }

    private Player findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jogador não encontrado com ID: " + id));
    }
    private void validateUniqueFields(String email, String nickname, Long currentId) {
        // Lógica para evitar falsos positivos no Update (verificar se o email/nick já existe, mas pertence a OUTRO usuário)
        boolean emailExists = repository.existsByEmail(email);
        boolean nicknameExists = repository.existsByNickname(nickname);

        if (emailExists && currentId == null) {
            throw new BusinessException("Este email já está em uso.");
        }
        if (nicknameExists && currentId == null) {
            throw new BusinessException("Este nickname já está em uso.");
        }
    }
}

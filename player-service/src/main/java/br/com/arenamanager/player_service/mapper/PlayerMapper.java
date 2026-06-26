package br.com.arenamanager.player_service.mapper;

import br.com.arenamanager.player_service.dto.PlayerRequestDTO;
import br.com.arenamanager.player_service.dto.PlayerResponseDTO;
import br.com.arenamanager.player_service.entity.Player;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlayerMapper {
    Player toEntity(PlayerRequestDTO requestDTO);
    PlayerResponseDTO toResponseDTO(Player entity);
    void updateEntityFromDto(PlayerRequestDTO dto, @MappingTarget Player entity);
}

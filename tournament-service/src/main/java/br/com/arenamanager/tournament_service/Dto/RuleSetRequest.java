package br.com.arenamanager.tournament_service.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RuleSetRequest {
    private String formato;
    private Integer maxParticipantes;
    private Integer melhorDe;
}
package br.com.arenamanager.tournament_service.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rule_sets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String formato;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipantes;

    @Column(name = "best_of")
    private Integer melhorDe;
}
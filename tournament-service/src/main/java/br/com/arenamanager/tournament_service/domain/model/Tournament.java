package br.com.arenamanager.tournament_service.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tournaments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime data_inicio;

    @Column(name = "end_date")
    private LocalDateTime data_fim;

    // Futuramente, adicionaremos aqui os relacionamentos:
    // @OneToMany(mappedBy = "tournament")
    // private List<Match> matches;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "rule_set_id")
    private RuleSet regras;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Match> partidas;
}
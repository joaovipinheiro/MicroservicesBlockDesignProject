package br.com.arenamanager.tournament_service.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relação com o Torneio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "participant1_id")
    private String participant1Id;

    @Column(name = "participant2_id")
    private String participant2Id;

    @Column(name = "participant1_score")
    private Integer participant1Score = 0;

    @Column(name = "participant2_score")
    private Integer participant2Score = 0;

    @Column(name = "winner_id")
    private String winnerId;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;
}

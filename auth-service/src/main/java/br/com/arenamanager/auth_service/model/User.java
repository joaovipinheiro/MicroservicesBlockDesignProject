package br.com.arenamanager.auth_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_usuarios", schema = "auth")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String role;
}

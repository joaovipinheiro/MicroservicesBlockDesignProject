package br.com.arenamanager.player_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerRequestDTO(
    @NotBlank(message = "O nome não pode estar em branco")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    String nome,

    @NotBlank(message = "O nickname é obrigatório")
    @Size(min = 3, max = 50, message = "O nickname deve ter entre 3 e 50 caracteres")
    String nickname,

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Formato de email inválido")
    String email
) {
}


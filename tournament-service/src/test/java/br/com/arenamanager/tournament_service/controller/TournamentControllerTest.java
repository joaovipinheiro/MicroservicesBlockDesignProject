package br.com.arenamanager.tournament_service.controller;

import br.com.arenamanager.tournament_service.Dto.TournamentRequest;
import br.com.arenamanager.tournament_service.Dto.TournamentResponse;
import br.com.arenamanager.tournament_service.domain.model.Tournament;
import br.com.arenamanager.tournament_service.domain.model.TournamentStatus;
import br.com.arenamanager.tournament_service.service.TournamentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TournamentController.class)
class TournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TournamentService tournamentService;

    @Test
    void deveCriarTorneioERetornar201() throws Exception {
        TournamentRequest request = new TournamentRequest();
        request.setNome("Torneio Teste");
        request.setDescricao("Descrição do torneio");
        request.setData_inicio(LocalDateTime.of(2026, 7, 1, 10, 0));
        request.setData_fim(LocalDateTime.of(2026, 7, 10, 18, 0));

        TournamentResponse response = new TournamentResponse();
        response.setId(1L);
        response.setNome("Torneio Teste");
        response.setDescricao("Descrição do torneio");
        response.setStatus(TournamentStatus.CRIADO);
        response.setData_inicio(LocalDateTime.of(2026, 7, 1, 10, 0));
        response.setData_fim(LocalDateTime.of(2026, 7, 10, 18, 0));

        when(tournamentService.createTournament(any(TournamentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/tournament")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("Torneio Teste"))
                .andExpect(jsonPath("$.status").value("CRIADO"));
    }

    @Test
    void deveRetornarListaDeTorneios() throws Exception {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setNome("Torneio A");
        tournament.setStatus(TournamentStatus.CRIADO);

        when(tournamentService.getAllTournaments()).thenReturn(List.of(tournament));

        mockMvc.perform(get("/tournament"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nome").value("Torneio A"));
    }

    @Test
    void deveRetornarTorneioQuandoIdExiste() throws Exception {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setNome("Torneio A");
        tournament.setStatus(TournamentStatus.CRIADO);

        when(tournamentService.getTournamentById(1L)).thenReturn(Optional.of(tournament));

        mockMvc.perform(get("/tournament/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("Torneio A"));
    }

    @Test
    void deveRetornar404QuandoTorneioNaoExiste() throws Exception {
        when(tournamentService.getTournamentById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/tournament/99"))
                .andExpect(status().isNotFound());
    }
}

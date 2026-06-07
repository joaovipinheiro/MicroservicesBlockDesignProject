package br.com.arenamanager.analytics_service.service;

import br.com.arenamanager.analytics_service.dto.MatchHistoryRequest;
import br.com.arenamanager.analytics_service.model.MatchHistory;
import br.com.arenamanager.analytics_service.repository.MatchHistoryRepository;
import br.com.arenamanager.analytics_service.exception.AnalyticsException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AnalyticsService {

    private final MatchHistoryRepository repository;

    public AnalyticsService(MatchHistoryRepository repository) {
        this.repository = repository;
    }

    public MatchHistory processarESalvarPartida(MatchHistoryRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new AnalyticsException("O nome do torneio nao pode estar vazio.");
        }

        MatchHistory history = new MatchHistory();
        history.setIdTorneio(request.getIdTorneio());
        history.setNome(request.getNome());
        history.setFormato(request.getFormato());

        return repository.save(history);
    }

    public List<MatchHistory> obterHistoricoPorJogador(String nome) {
        List<MatchHistory> resultados = repository.findByNomeContaining(nome);
        if (resultados.isEmpty()) {
            throw new AnalyticsException("Nenhum historico encontrado para o torneio: " + nome);
        }
        return resultados;
    }
}
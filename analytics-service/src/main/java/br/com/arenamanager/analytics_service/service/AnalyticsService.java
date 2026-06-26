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
        if (request.getName() == null || request.getName().isBlank()) {
            throw new AnalyticsException("O nome do torneio nao pode estar vazio.");
        }

        MatchHistory history = new MatchHistory();
        history.setTournamentId(request.getTournamentId());
        history.setName(request.getName());
        history.setFormat(request.getFormat());

        return repository.save(history);
    }

    public List<MatchHistory> obterHistoricoPorJogador(String name) {
        List<MatchHistory> resultados = repository.findByNameContaining(name);
        if (resultados.isEmpty()) {
            throw new AnalyticsException("Nenhum historico encontrado para o torneio: " + name);
        }
        return resultados;
    }
}
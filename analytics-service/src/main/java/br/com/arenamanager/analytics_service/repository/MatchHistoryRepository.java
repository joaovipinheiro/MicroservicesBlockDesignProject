package br.com.arenamanager.analytics_service.repository;
import br.com.arenamanager.analytics_service.model.MatchHistory;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface MatchHistoryRepository extends ElasticsearchRepository<MatchHistory, String> {
    // Busca automática que o Spring Data gera para nós pesquisarmos por Nickname
    List<MatchHistory> findByNameContaining(String name);
}
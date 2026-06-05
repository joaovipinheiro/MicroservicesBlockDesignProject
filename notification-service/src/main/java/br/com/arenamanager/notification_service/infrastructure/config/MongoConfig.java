package br.com.arenamanager.notification_service.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

import org.bson.Document;

/**
 * MongoDB configuration that programmatically ensures indexes are created on startup.
 *
 * <p>Creates the compound index {@code {playerId: 1, sentAt: -1}} on the
 * {@code notification_logs} collection to support paginated queries by player.
 *
 * <p>Valida: Requisito 5.2</p>
 */
@Configuration
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void ensureIndexes() {
        try {
            IndexOperations indexOps = mongoTemplate.indexOps("notification_logs");
            Document indexDef = new Document("playerId", 1).append("sentAt", -1);
            CompoundIndexDefinition compoundIndex = new CompoundIndexDefinition(indexDef);
            indexOps.ensureIndex(compoundIndex);
        } catch (Exception ex) {
            // Log and continue — indexes will be created when authentication is available
            org.slf4j.LoggerFactory.getLogger(MongoConfig.class)
                .warn("Could not ensure MongoDB indexes on startup: {}", ex.getMessage());
        }
    }
}

package br.com.arenamanager.analytics_service.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "match_history")
public class MatchHistory {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long tournamentId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String format;

    public MatchHistory() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
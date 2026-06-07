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
    private Long idTorneio;

    @Field(type = FieldType.Text)
    private String nome;

    @Field(type = FieldType.Text)
    private String formato;

    public MatchHistory() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getIdTorneio() { return idTorneio; }
    public void setIdTorneio(Long idTorneio) { this.idTorneio = idTorneio; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
}
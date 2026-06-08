package br.com.arenamanager.analytics_service.dto;

public class MatchHistoryRequest {
    private Long idTorneio;
    private String nome;
    private String formato;

    public MatchHistoryRequest() {}

    public Long getIdTorneio() { return idTorneio; }
    public void setIdTorneio(Long idTorneio) { this.idTorneio = idTorneio; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
}
package br.com.arenamanager.analytics_service.dto;

public class MatchHistoryRequest {
    private Long tournamentId;
    private String name;
    private String format;

    public MatchHistoryRequest() {}

    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
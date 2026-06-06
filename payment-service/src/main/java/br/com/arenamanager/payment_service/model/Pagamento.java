import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;  // Apenas o ID, guardando a independência
    private Long torneioId;  // Apenas o ID
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private StatusPagamento status;

    private LocalDateTime dataCriacao;

    public Pagamento() {}

    public Pagamento(Long usuarioId, Long torneioId, BigDecimal valor) {
        this.usuarioId = usuarioId;
        this.torneioId = torneioId;
        this.valor = valor;
        this.status = StatusPagamento.PENDENTE;
        this.dataCriacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getTorneioId() { return torneioId; }
    public BigDecimal getValor() { return valor; }
    public StatusPagamento getStatus() { return status; }
    public void setStatus(StatusPagamento status) { this.status = status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
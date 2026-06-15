package br.com.arenamanager.payment_service.controller;

import br.com.arenamanager.payment_service.dto.PagamentoResponseDTO;
import br.com.arenamanager.payment_service.dto.SolicitacaoPagamentoDTO;
import br.com.arenamanager.payment_service.model.Pagamento;
import br.com.arenamanager.payment_service.service.PagamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private static final Logger log = LoggerFactory.getLogger(PagamentoController.class);
    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping
    public ResponseEntity<PagamentoResponseDTO> efetuarPagamento(
            @RequestBody SolicitacaoPagamentoDTO requisicao,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        String cid = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        try {
            log.info("Requisição recebida: POST /api/pagamentos, usuarioId={}, torneioId={}, correlationId={}",
                    requisicao.usuarioId(), requisicao.torneioId(), cid);
            Pagamento pagamento = pagamentoService.criarPagamento(
                    requisicao.usuarioId(),
                    requisicao.torneioId(),
                    requisicao.valor()
            );
            log.info("Resposta enviada: POST /api/pagamentos, status=200, pagamentoId={}, correlationId={}",
                    pagamento.getId(), cid);
            return ResponseEntity.ok(toResponseDTO(pagamento));
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagamentoResponseDTO> obterPorId(@PathVariable Long id) {
        Pagamento pagamento = pagamentoService.buscarPorId(id);
        return ResponseEntity.ok(toResponseDTO(pagamento));
    }

    private PagamentoResponseDTO toResponseDTO(Pagamento pagamento) {
        return new PagamentoResponseDTO(
                pagamento.getId(),
                pagamento.getUsuarioId(),
                pagamento.getTorneioId(),
                pagamento.getValor(),
                pagamento.getStatus().name(),
                pagamento.getDataCriacao()
        );
    }
}

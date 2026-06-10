package br.com.arenamanager.payment_service.controller;

import br.com.arenamanager.payment_service.dto.PagamentoResponseDTO;
import br.com.arenamanager.payment_service.dto.SolicitacaoPagamentoDTO;
import br.com.arenamanager.payment_service.model.Pagamento;
import br.com.arenamanager.payment_service.service.PagamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping
    public ResponseEntity<PagamentoResponseDTO> efetuarPagamento(@RequestBody SolicitacaoPagamentoDTO requisicao) {
        Pagamento pagamento = pagamentoService.criarPagamento(
                requisicao.usuarioId(),
                requisicao.torneioId(),
                requisicao.valor()
        );
        return ResponseEntity.ok(toResponseDTO(pagamento));
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
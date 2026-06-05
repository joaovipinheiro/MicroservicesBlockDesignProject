package br.com.arenamanager.payment_service.controller;

import br.com.arenamanager.payment_service.model.Pagamento;
import br.com.arenamanager.payment_service.service.PagamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }


    @PostMapping
    public ResponseEntity<Pagamento> efetuarPagamento(@RequestBody SolicitacaoPagamentoDTO requisicao) {
        Pagamento novoPagamento = pagamentoService.criarPagamento(
                requisicao.usuarioId(),
                requisicao.torneioId(),
                requisicao.valor()
        );
        return ResponseEntity.ok(novoPagamento);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pagamento> obterPorId(@PathVariable Long id) {
        Pagamento pagamento = pagamentoService.buscarPorId(id);
        return ResponseEntity.ok(pagamento);
    }
}

record SolicitacaoPagamentoDTO(Long usuarioId, Long torneioId, BigDecimal valor) {}
package br.com.arenamanager.payment_service.service;

import br.com.arenamanager.payment_service.model.Pagamento;
import br.com.arenamanager.payment_service.model.StatusPagamento;
import br.com.arenamanager.payment_service.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = pagamentoRepository;
    }

    public Pagamento criarPagamento(Long usuarioId, Long torneioId, BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero");
        }

        Pagamento pagamento = new Pagamento(usuarioId, torneioId, valor);

        pagamento.setStatus(StatusPagamento.APROVADO);

        return pagamentoRepository.save(pagamento);
    }

    public Pagamento buscarPorId(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado com o ID: " + id));
    }
}
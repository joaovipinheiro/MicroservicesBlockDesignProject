package br.com.arenamanager.payment_service.service;

import br.com.arenamanager.payment_service.client.PlayerClient;
import br.com.arenamanager.payment_service.client.PlayerDTO;
import br.com.arenamanager.payment_service.dto.EventoPagamentoAprovado;
import br.com.arenamanager.payment_service.model.Pagamento;
import br.com.arenamanager.payment_service.model.StatusPagamento;
import br.com.arenamanager.payment_service.repository.PagamentoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository pagamentoRepository;
    private final PlayerClient playerClient;
    private final PagamentoPublisherService pagamentoPublisherService;
    private final MeterRegistry meterRegistry;

    public PagamentoService(PagamentoRepository pagamentoRepository,
                            PlayerClient playerClient,
                            PagamentoPublisherService pagamentoPublisherService,
                            MeterRegistry meterRegistry) {
        this.pagamentoRepository = pagamentoRepository;
        this.playerClient = playerClient;
        this.pagamentoPublisherService = pagamentoPublisherService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public Pagamento criarPagamento(Long usuarioId, Long torneioId, BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero!");
        }

        log.info("Processando pagamento: usuarioId={}, torneioId={}, valor={}", usuarioId, torneioId, valor);

        Pagamento pagamento = new Pagamento(usuarioId, torneioId, valor);
        pagamento.setStatus(StatusPagamento.APROVADO);
        pagamento = pagamentoRepository.save(pagamento);

        try {
            PlayerDTO jogador = playerClient.obterJogadorPorId(usuarioId);

            EventoPagamentoAprovado evento = new EventoPagamentoAprovado(
                    pagamento.getId(),
                    jogador.nome(),
                    jogador.email(),
                    torneioId,
                    valor
            );

            pagamentoPublisherService.publicarPagamentoAprovado(evento);

            meterRegistry.counter("payments.approved.total",
                    "service", "payment-service").increment();
            meterRegistry.counter("payments.published.kafka.total",
                    "service", "payment-service").increment();

            log.info("Pagamento aprovado: id={}, usuarioId={}, torneioId={}", pagamento.getId(), usuarioId, torneioId);

        } catch (Exception e) {
            meterRegistry.counter("payments.failed.total",
                    "service", "payment-service").increment();
            log.error("Falha ao processar pagamento: usuarioId={}, torneioId={}, erro={}", usuarioId, torneioId, e.getMessage());
            throw e;
        }

        return pagamento;
    }

    public Pagamento buscarPorId(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado com o ID: " + id));
    }
}
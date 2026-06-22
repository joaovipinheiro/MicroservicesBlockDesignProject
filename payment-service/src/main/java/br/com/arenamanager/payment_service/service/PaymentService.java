package br.com.arenamanager.payment_service.service;

import br.com.arenamanager.payment_service.client.PlayerClient;
import br.com.arenamanager.payment_service.client.PlayerDTO;
import br.com.arenamanager.payment_service.dto.PaymentApprovedEvent;
import br.com.arenamanager.payment_service.model.Payment;
import br.com.arenamanager.payment_service.model.PaymentStatus;
import br.com.arenamanager.payment_service.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PlayerClient playerClient;
    private final PaymentPublisherService paymentPublisherService;
    private final MeterRegistry meterRegistry;

    public PaymentService(PaymentRepository paymentRepository,
                            PlayerClient playerClient,
                            PaymentPublisherService paymentPublisherService,
                            MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.playerClient = playerClient;
        this.paymentPublisherService = paymentPublisherService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public Payment createPayment(Long playerId, Long tournamentId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do payment deve ser maior que zero!");
        }

        log.info("Processando payment: playerId={}, tournamentId={}, amount={}", playerId, tournamentId, amount);

        Payment payment = new Payment(playerId, tournamentId, amount);
        payment.setStatus(PaymentStatus.APPROVED);
        payment = paymentRepository.save(payment);

        try {
            PlayerDTO jogador = playerClient.obterJogadorPorId(playerId);

            PaymentApprovedEvent event = new PaymentApprovedEvent(
                    payment.getId(),
                    jogador.name(),
                    jogador.email(),
                    tournamentId,
                    amount
            );

            paymentPublisherService.publishPaymentApproved(event);

            meterRegistry.counter("payments.approved.total",
                    "service", "payment-service").increment();
            meterRegistry.counter("payments.published.kafka.total",
                    "service", "payment-service").increment();

            log.info("Payment aprovado: id={}, playerId={}, tournamentId={}", payment.getId(), playerId, tournamentId);

        } catch (Exception e) {
            meterRegistry.counter("payments.failed.total",
                    "service", "payment-service").increment();
            log.error("Falha ao processar payment: playerId={}, tournamentId={}, erro={}", playerId, tournamentId, e.getMessage());
            throw e;
        }

        return payment;
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment não encontrado com o ID: " + id));
    }
}
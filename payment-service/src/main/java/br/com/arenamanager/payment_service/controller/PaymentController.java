package br.com.arenamanager.payment_service.controller;

import br.com.arenamanager.payment_service.dto.PaymentResponseDTO;
import br.com.arenamanager.payment_service.dto.PaymentRequestDTO;
import br.com.arenamanager.payment_service.model.Payment;
import br.com.arenamanager.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @RequestBody PaymentRequestDTO requisicao,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        String cid = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        try {
            log.info("Requisição recebida: POST /api/payments, playerId={}, tournamentId={}, correlationId={}",
                    requisicao.playerId(), requisicao.tournamentId(), cid);
            Payment payment = paymentService.createPayment(
                    requisicao.playerId(),
                    requisicao.tournamentId(),
                    requisicao.amount()
            );
            log.info("Resposta enviada: POST /api/payments, status=200, paymentId={}, correlationId={}",
                    payment.getId(), cid);
            return ResponseEntity.ok(toResponseDTO(payment));
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getById(@PathVariable Long id) {
        Payment payment = paymentService.findById(id);
        return ResponseEntity.ok(toResponseDTO(payment));
    }

    private PaymentResponseDTO toResponseDTO(Payment payment) {
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getPlayerId(),
                payment.getTournamentId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getCreatedAt()
        );
    }
}

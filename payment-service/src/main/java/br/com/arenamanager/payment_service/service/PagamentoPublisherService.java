package br.com.arenamanager.payment_service.service;

import br.com.arenamanager.payment_service.dto.EventoPagamentoAprovado;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PagamentoPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PagamentoPublisherService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarPagamentoAprovado(EventoPagamentoAprovado evento) {
        String topico = "pagamentos-aprovados";
        kafkaTemplate.send(topico, evento);
        System.out.println("Evento de pagamento aprovado enviado ao Kafka! ID: " + evento.pagamentoId());
    }
}
package br.com.arenamanager.payment_service.service;

import br.com.arenamanager.payment_service.model.Pagamento;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PagamentoPublisherService {


    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PagamentoPublisherService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarPagamentoAprovado(Pagamento pagamento) {
        String topico = "pagamentos-aprovados";

        kafkaTemplate.send(topico, pagamento);

        System.out.println("Evento de pagamento aprovado! ID: " + pagamento.getId());
    }
}
package br.com.arenamanager.registration_service.client;

import br.com.arenamanager.registration_service.dto.PaymentRequest;
import br.com.arenamanager.registration_service.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/pagamentos")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}

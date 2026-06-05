package br.com.arenamanager.notification_service.application.port.out;

import br.com.arenamanager.notification_service.domain.model.EmailMessage;

public interface EmailSenderPort {

    void send(EmailMessage message);
}

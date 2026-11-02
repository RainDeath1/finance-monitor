package com.globus.finance.service.auth;

import com.globus.finance.service.mail.MailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailVerificationMailListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    EmailVerificationMailListener.class
            );

    private final MailService mailService;
    private final String baseUrl;

    public EmailVerificationMailListener(
            MailService mailService,
            @Value("${app.base-url}") String baseUrl) {

        this.mailService = mailService;
        this.baseUrl = baseUrl;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void sendVerificationEmail(
            EmailVerificationRequestedEvent event) {

        String confirmationLink =
                baseUrl.replaceAll("/+$", "")
                        + "/verify-email?token="
                        + event.token();

        String message = """
                Здравствуйте!

                Благодарим за регистрацию в Finance Monitor.

                Для подтверждения электронной почты
                перейдите по ссылке:

                %s

                Ссылка действительна в течение 24 часов.

                Если вы не регистрировались в Finance Monitor,
                проигнорируйте это письмо.
                """.formatted(confirmationLink);

        try {
            mailService.sendEmail(
                    event.email(),
                    "Finance Monitor — подтверждение email",
                    message
            );
        } catch (MailException e) {
            log.error(
                    "Не удалось отправить письмо подтверждения email",
                    e
            );
        }
    }
}
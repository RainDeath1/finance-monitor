package com.globus.finance.service.mail;

import com.globus.finance.service.auth.PasswordResetRequestedEvent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetMailListener {

    private final MailService mailService;
    private final String baseUrl;

    public PasswordResetMailListener(
            MailService mailService,
            @Value("${app.base-url}") String baseUrl) {

        this.mailService = mailService;
        this.baseUrl = baseUrl;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void sendPasswordResetEmail(
            PasswordResetRequestedEvent event) {

        String link = baseUrl.replaceAll("/+$", "")
                + "/reset-password?token="
                + event.rawToken();

        String subject =
                "Finance Monitor — восстановление пароля";

        String text = """
                Здравствуйте!

                Мы получили запрос на восстановление
                пароля от вашей учётной записи Finance Monitor.

                Для установки нового пароля перейдите по ссылке:

                %s

                Ссылка действительна в течение 30 минут.

                Если вы не запрашивали восстановление пароля,
                просто проигнорируйте это письмо.

                С уважением,
                Finance Monitor
                """.formatted(link);

        mailService.sendEmail(
                event.email(),
                subject,
                text
        );
    }
}
package com.globus.finance.service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(
            String to,
            String subject,
            String text) {

        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException(
                    "Не указан адрес получателя"
            );
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "Не указана тема письма"
            );
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Не указан текст письма"
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
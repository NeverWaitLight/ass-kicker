package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    private SmtpEmailSenderProperty smtpProperties;
    private SmtpEmailSender smtpEmailSender;

    @BeforeEach
    void setUp() {
        smtpProperties = new SmtpEmailSenderProperty();
        smtpProperties.setHost("smtp.example.com");
        smtpProperties.setPort(465);
        smtpProperties.setUsername("test@example.com");
        smtpProperties.setPassword("password");
        smtpProperties.setProtocol("smtp");
        smtpProperties.setSslEnabled(true);
        smtpProperties.setFrom("notify@example.com");
        smtpProperties.setConnectionTimeout(Duration.ofSeconds(5));
        smtpProperties.setReadTimeout(Duration.ofSeconds(10));
        smtpProperties.setMaxRetries(3);
        smtpProperties.setRetryDelay(Duration.ofMillis(100));

        smtpEmailSender = new SmtpEmailSender(javaMailSender, smtpProperties);
    }

    @Test
    void shouldSendEmailSuccessfully() throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = smtpEmailSender.send(request);

        assertThat(response.isSuccess()).isTrue();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldReturnFailureWhenRequestIsNull() {
        MessageResponse response = smtpEmailSender.send(null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVALID_REQUEST");
        verifyNoInteractions(javaMailSender);
    }

    @Test
    void shouldRetryOnMailException() throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("Connection failed") {})
                .doThrow(new MailException("Connection failed") {})
                .doNothing()
                .when(javaMailSender).send(any(MimeMessage.class));

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = smtpEmailSender.send(request);

        assertThat(response.isSuccess()).isTrue();
        verify(javaMailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    void shouldFailAfterMaxRetries() throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("Connection failed") {})
                .when(javaMailSender).send(any(MimeMessage.class));

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = smtpEmailSender.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("MAIL_SEND_FAILED");
        verify(javaMailSender, times(4)).send(any(MimeMessage.class));
    }

    @Test
    void shouldUseFromFieldWhenProvided() throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        smtpProperties.setFrom("custom@example.com");
        smtpEmailSender = new SmtpEmailSender(javaMailSender, smtpProperties);

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = smtpEmailSender.send(request);

        assertThat(response.isSuccess()).isTrue();
    }
}

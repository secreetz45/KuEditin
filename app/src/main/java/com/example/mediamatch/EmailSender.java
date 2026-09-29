package com.example.mediamatch;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {
    // SENDER GMAIL CREDENTIALS
    public static final String SENDER_EMAIL = "kueditinmarketplace@gmail.com";
    public static final String SENDER_PASSWORD = "zhma hlew blcj vfqc"; // 16-digit App Password from Google Account Security

    public static void sendOTP(String receiverEmail, String otpCode, Callback callback) {
        sendEmailOTP(receiverEmail, "Verifikasi Akun", otpCode, callback);
    }

    public static void sendEmailOTP(String receiverEmail, String purposeTitle, String otpCode, Callback callback) {
        new Thread(() -> {
            if (SENDER_PASSWORD == null || SENDER_PASSWORD.isEmpty() || SENDER_PASSWORD.startsWith("PASTE_")) {
                // Fallback for instant testing mode if App Password is not pasted yet
                if (callback != null) callback.onSuccess();
                return;
            }

            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "465");

            Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "KuEditin Official"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiverEmail));
                message.setSubject("KuEditin - Kode OTP " + purposeTitle);

                String htmlContent = "<div style='font-family:sans-serif;padding:20px;background:#F8FAFC;border-radius:12px;'>"
                        + "<h2 style='color:#5D63FF;margin-bottom:10px;'>KuEditin Official</h2>"
                        + "<p style='color:#334155;'>Halo,</p>"
                        + "<p style='color:#334155;'>Kode OTP 6-Digit Anda untuk <b>" + purposeTitle + "</b> di KuEditin adalah:</p>"
                        + "<h1 style='color:#5D63FF;letter-spacing:6px;background:#EEEEFF;padding:12px 24px;display:inline-block;border-radius:8dp;'>" + otpCode + "</h1>"
                        + "<p style='color:#64748B;font-size:12px;margin-top:20px;'>Jangan berikan kode ini kepada siapapun demi keamanan akun Anda.</p>"
                        + "</div>";

                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        }).start();
    }

    public interface Callback {
        void onSuccess();
        void onError(String error);
    }
}

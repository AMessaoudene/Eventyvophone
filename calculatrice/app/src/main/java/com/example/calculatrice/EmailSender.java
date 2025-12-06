package com.example.calculatrice;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    // Credentials provided by user
    public static final String SENDER_EMAIL = "amessaoudenecontact@gmail.com"; 
    public static final String SENDER_PASSWORD = "ciblgmkyoyphwsny"; 

    public interface EmailCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void sendEmail(Context context, String recipientEmail, String subject, String body, EmailCallback callback) {
        if (SENDER_EMAIL.contains("your_email")) {
            Toast.makeText(context, "Developer: Please set Email/Password in EmailSender.java", Toast.LENGTH_LONG).show();
            if (callback != null) callback.onFailure(new Exception("Credentials not set"));
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);

                handler.post(() -> {
                    if (callback != null) callback.onSuccess();
                });

            } catch (MessagingException e) {
                e.printStackTrace();
                handler.post(() -> {
                    if (callback != null) callback.onFailure(e);
                });
            }
        });
    }
}

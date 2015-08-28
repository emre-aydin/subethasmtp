package org.subethamail.forwarder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ForwarderServer {
    private static final Logger LOG = LoggerFactory.getLogger(ForwarderServer.class);

    public static void main(String[] args) {
        validateArgs(args);

        int port = checkAndGetPortNumber();

        String username = getUsername(args);
        String password = getPassword(args);

        SMTPServer smtpServer = createSmtpServer(username, password, port);
        smtpServer.start();
    }

    private static SMTPServer createSmtpServer(final String username, final String password, int port) {
        SMTPServer server = new SMTPServer(new SimpleMessageListenerAdapter(new SimpleMessageListener() {
            @Override
            public boolean accept(String from, String recipient) {
                return true;
            }

            @Override
            public void deliver(String from, String recipient, InputStream data) throws IOException {
                try {
                    MimeMessage mimeMessage = new MimeMessage(createSession(username, password), data);
                    sendMail(mimeMessage);
                } catch (MessagingException | RuntimeException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }));
        server.setPort(port);
        return server;
    }

    private static String getPassword(String[] args) {
        return args[0];
    }

    private static String getUsername(String[] args) {
        return args[1];
    }

    private static int checkAndGetPortNumber() {
        int defaultPort = 25;

        String portString = System.getProperty("port");
        if (portString == null) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            System.out.println("port needs to be a number [" + portString + "], defaulting to 25");
            return defaultPort;
        }
    }

    private static void validateArgs(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage 'java [-Dport=25] -jar subethasmtp-3.1.7.jar <gmail username> <gmail password>'");
            System.exit(1);
        }
    }

    private static void sendMail(MimeMessage message) {
        try {
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Session createSession(String username, String password) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
}

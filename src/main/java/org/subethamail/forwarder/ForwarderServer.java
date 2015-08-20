package org.subethamail.forwarder;

import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ForwarderServer {
    public static void main(String[] args) {
        SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(new SimpleMessageListener() {
            @Override
            public boolean accept(String from, String recipient) {
                return true;
            }

            @Override
            public void deliver(String from, String recipient, InputStream data) throws IOException {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(data))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }
        }));
        smtpServer.setPort(25);
        smtpServer.start();
    }
}

package org.subethamail.smtp.server;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.client.SMTPException;
import org.subethamail.smtp.client.SmartClient;
import org.subethamail.smtp.util.TextUtils;

import javax.mail.MessagingException;
import java.io.IOException;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

/**
 * This class tests whether the event handler methods defined in MessageHandler 
 * are called at the appropriate times and in good order.  
 */
public class MessageHandlerTest {
	private MessageHandlerFactory messageHandlerFactory;

	private MessageHandler messageHandler;

	private MessageHandler messageHandler2;

	private SMTPServer smtpServer;

	@Before
	public void setup() {
		messageHandlerFactory = EasyMock.createMock(MessageHandlerFactory.class);
		messageHandler = EasyMock.createMock(MessageHandler.class);
		messageHandler2 = EasyMock.createMock(MessageHandler.class);

		smtpServer = new SMTPServer(messageHandlerFactory);
		smtpServer.setPort(2566);
		smtpServer.start();
	}

	@Test
	public void testCompletedMailTransaction() throws Exception {
		expect(messageHandlerFactory.create(anyObject())).andReturn(messageHandler).once();

		messageHandler.from(anyString());
		expectLastCall().once();

		messageHandler.recipient(anyString());
		expectLastCall().once();

		messageHandler.data(anyObject());
		expectLastCall().once();

		messageHandler.done();
		expectLastCall().once();

		replayAll();

		SmartClient client = new SmartClient("localhost", smtpServer.getPort(),
				"localhost");
		client.from("john@example.com");
		client.to("jane@example.com");
		client.dataStart();
		client.dataWrite(TextUtils.getAsciiBytes("body"), 4);
		client.dataEnd();
		client.quit();
		smtpServer.stop(); // wait for the server to catch up

		verifyAll();
	}

	@Test
	public void testDisconnectImmediately() throws Exception {
		expect(messageHandlerFactory.create(anyObject())).andStubThrow(new AssertionError("Should not be called"));
//		new Expectations() {
//			{
//				messageHandlerFactory.create((MessageContext) any);
//				times = 0;
//			}
//		};
		replayAll();
		SmartClient client = new SmartClient("localhost", smtpServer.getPort(),
				"localhost");
		client.quit();
		smtpServer.stop(); // wait for the server to catch up
		verifyAll();
	}

	@Test
	public void testAbortedMailTransaction() throws Exception {
		expect(messageHandlerFactory.create(anyObject())).andReturn(messageHandler).once();

		messageHandler.from(anyString());
		expectLastCall().once();

		messageHandler.done();
		expectLastCall().once();

		replayAll();

		SmartClient client = new SmartClient("localhost", smtpServer.getPort(),
				"localhost");
		client.from("john@example.com");
		client.quit();
		smtpServer.stop(); // wait for the server to catch up

		verifyAll();
	}

	@Test
	public void testTwoMailsInOneSession() throws Exception {
        expect(messageHandlerFactory.create(anyObject())).andReturn(messageHandler).once();

        messageHandler.from("john1@example.com");
        expectLastCall().once();

        messageHandler.recipient("jane1@example.com");
        expectLastCall().once();

        messageHandler.data(anyObject());
        expectLastCall().once();

        messageHandler.done();
        expectLastCall().once();

        expect(messageHandlerFactory.create(anyObject())).andReturn(messageHandler2).once();

        messageHandler2.from("john2@example.com");
        expectLastCall().once();

        messageHandler2.recipient("jane2@example.com");
        expectLastCall().once();

        messageHandler2.data(anyObject());
        expectLastCall().once();

        messageHandler2.done();
        expectLastCall().once();

        replayAll();

        SmartClient client = new SmartClient("localhost", smtpServer.getPort(),
                "localhost");

        client.from("john1@example.com");
        client.to("jane1@example.com");
        client.dataStart();
        client.dataWrite(TextUtils.getAsciiBytes("body1"), 5);
        client.dataEnd();

        client.from("john2@example.com");
        client.to("jane2@example.com");
        client.dataStart();
        client.dataWrite(TextUtils.getAsciiBytes("body2"), 5);
        client.dataEnd();

        client.quit();

        smtpServer.stop(); // wait for the server to catch up

        verifyAll();
    }

    /**
	 * Test for issue 56: rejecting a Mail From causes IllegalStateException in
	 * the next Mail From attempt.
	 * @see <a href=http://code.google.com/p/subethasmtp/issues/detail?id=56>Issue 56</a>
	 */
	@Test
	public void testMailFromRejectedFirst() throws IOException, MessagingException {
		expect(messageHandlerFactory.create(anyObject())).andReturn(messageHandler).anyTimes();

		messageHandler.from("john1@example.com");
		expectLastCall().andStubThrow(new RejectException("Test MAIL FROM rejection"));

		messageHandler.from("john2@example.com");
		expectLastCall().once();

		messageHandler.done();
		expectLastCall().times(2);

		replayAll();

		SmartClient client = new SmartClient("localhost", smtpServer.getPort(),
				"localhost");

		boolean expectedRejectReceived = false;
		try {
			client.from("john1@example.com");
		} catch (SMTPException e) {
			expectedRejectReceived = true;
		}
		Assert.assertTrue(expectedRejectReceived);

		client.from("john2@example.com");
		client.quit();

		smtpServer.stop(); // wait for the server to catch up

		verifyAll();

	}

	private void replayAll() {
		EasyMock.replay(messageHandlerFactory, messageHandler, messageHandler2);
	}

	private void verifyAll() {
		EasyMock.verify(messageHandlerFactory, messageHandler, messageHandler2);
	}
	
}

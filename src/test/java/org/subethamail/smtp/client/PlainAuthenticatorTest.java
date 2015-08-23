package org.subethamail.smtp.client;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class PlainAuthenticatorTest {

	private SmartClient smartClient;

	private final Map<String, String> extensions = new HashMap<>();

	@Before
	public void setUp() throws Exception {
		smartClient = createMock(SmartClient.class);
	}

	@Test
	public void testSuccess() throws IOException {
		extensions.put("AUTH", "GSSAPI DIGEST-MD5 PLAIN");
		PlainAuthenticator authenticator = new PlainAuthenticator(smartClient, "test", "1234");

		expect(smartClient.getExtensions()).andReturn(extensions).once();

		expect(smartClient.sendAndCheck("AUTH PLAIN AHRlc3QAMTIzNA==")).andReturn(null).once();

		replay(smartClient);

		authenticator.authenticate();

		verify(smartClient);
	}
}

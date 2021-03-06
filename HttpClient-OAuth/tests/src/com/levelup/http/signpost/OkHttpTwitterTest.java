package com.levelup.http.signpost;

import com.levelup.http.HttpClient;
import com.levelup.http.okhttp.OkHttpClient;

public class OkHttpTwitterTest extends AbstractTwitterTest {
	protected void setUp() throws Exception {
		super.setUp();
		HttpClient.setConnectionFactory(OkHttpClient.instance);
	};

	public void testIdentityEncoding() throws Exception {
		HttpRequestSignedGet search = getSearchRequest();
		search.setHeader("Accept-Encoding", "identity");
		String response = HttpClient.getStringResponse(search);
		assertNotNull(response);
		assertTrue(response.length() > 0);
		assertEquals('{', response.charAt(0));
	}

	public void testGzipEncoding() throws Exception {
		HttpRequestSignedGet search = getSearchRequest();
		search.setHeader("Accept-Encoding", "gzip");
		String response = HttpClient.getStringResponse(search);
		assertNotNull(response);
		assertTrue(response.length() > 0);
		assertEquals('{', response.charAt(0));
	}

	public void testDirectTransport() throws Exception {
		HttpRequestSignedGet search = getSearchRequest();
		search.setHeader("X-Android-Transports", "http/1.1");
		String response = HttpClient.getStringResponse(search);
		assertNotNull(response);
		assertTrue(response.length() > 0);
		assertEquals('{', response.charAt(0));
	}

	public void testBlacklistTransport() throws Exception {
		HttpRequestSignedGet search = getSearchRequest();
		OkHttpClient.addUrlBlacklist(search.getUri().toString());
		try {
			String response = HttpClient.getStringResponse(search);
			assertNotNull(response);
			assertTrue(response.length() > 0);
			assertEquals('{', response.charAt(0));
		} finally {
			OkHttpClient.removeUrlBlacklist(search.getUri().toString());
		}
	}
}

package com.levelup.http.signpost;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.protocol.HTTP;

import android.text.TextUtils;

import com.levelup.http.Header;
import com.levelup.http.HttpRequest;
import com.levelup.http.HttpRequestPost;

/**
 * Wrap a {@link HttpRequest HttpClient HttpRequest} to match the {@link oauth.signpost.http.HttpRequest signpost HttpRequest} interface
 */
public class OAuthRequestAdapter implements oauth.signpost.http.HttpRequest {
	private final HttpRequest req;

	public OAuthRequestAdapter(HttpRequest request) {
		this.req = request;
	}
	
	@Override
	public void setHeader(String name, String value) {
		req.setHeader(name, value);
	}
	
	@Override
	public String getHeader(String name) {
		return req.getHeader(name);
	}
	
	@Override
	public Map<String, String> getAllHeaders() {
		Header[] allHeaders = req.getAllHeaders();
		Map<String, String> result = new HashMap<String, String>(allHeaders.length);
		for (Header header : allHeaders) {
			result.put(header.getName(), header.getValue());
		}
		return result;
	}

	@Override
	public InputStream getMessagePayload() throws IOException {
		final String contentType = getContentType();  
		if (null != contentType && contentType.startsWith("application/x-www-form-urlencoded")) {
			String contentLength = req.getHeader(HTTP.CONTENT_LEN);
			ByteArrayOutputStream output = new ByteArrayOutputStream(TextUtils.isEmpty(contentLength) ? 32 : Integer.parseInt(contentLength));
			((HttpRequestPost) req).outputBody(output);
			return new ByteArrayInputStream(output.toByteArray());
		}
		return null;
	}

	@Override
	public String getContentType() {
		return req.getHeader(HTTP.CONTENT_TYPE);
	}

	@Override
	public String getMethod() {
		return (req instanceof HttpRequestPost) ? "POST" : "GET";
	}

	@Override
	public String getRequestUrl() {
		return req.getUri().toString();
	}

	@Override
	public void setRequestUrl(String url) {
        // can't do
	}

	@Override
	public Object unwrap() {
		return req;
	}
}
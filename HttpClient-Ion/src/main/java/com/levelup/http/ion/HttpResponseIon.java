package com.levelup.http.ion;

import java.util.List;
import java.util.Map;

import org.apache.http.protocol.HTTP;

import android.text.TextUtils;

import com.koushikdutta.ion.Response;
import com.levelup.http.HttpResponse;

/**
 * Created by Steve Lhomme on 09/07/2014.
 */
public class HttpResponseIon<T> implements HttpResponse {
	private final Response<T> response;

	public HttpResponseIon(Response<T> response) {
		this.response = response;
	}

	@Override
	public String getContentType() {
		return getHeaderField(HTTP.CONTENT_TYPE);
	}

	@Override
	public int getResponseCode() {
		return response.getHeaders().code();
	}

	@Override
	public Map<String, List<String>> getHeaderFields() {
		return response.getHeaders().getHeaders().getMultiMap();
	}

	@Override
	public String getHeaderField(String name) {
		return response.getHeaders().getHeaders().get(name);
	}

	@Override
	public int getContentLength() {
		String contentLength = getHeaderField(HTTP.CONTENT_LEN);
		if (TextUtils.isEmpty(contentLength))
			return -1;
		return Integer.parseInt(contentLength);
	}

	@Override
	public String getResponseMessage() {
		return response.getHeaders().message();
	}

	@Override
	public String getContentEncoding() {
		return getHeaderField(HTTP.CONTENT_ENCODING);
	}

	@Override
	public void disconnect() {
		// TODO see if we can cancel a Ion response while it's processing
	}

	T getResult() {
		return response.getResult();
	}

	Exception getException() {
		return response.getException();
	}
}

package com.levelup.http.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.protocol.HTTP;

import android.annotation.SuppressLint;
import android.os.Build;

import com.levelup.http.BaseHttpRequest;
import com.levelup.http.DataErrorException;
import com.levelup.http.HttpClient;
import com.levelup.http.HttpException;
import com.levelup.http.LogManager;
import com.levelup.http.LoggerTagged;
import com.levelup.http.ParserException;
import com.levelup.http.ResponseHandler;
import com.levelup.http.TypedHttpRequest;

/**
 * Basic HTTP request to be passed to {@link com.levelup.http.HttpClient}
 *
 * @param <T> type of the data read from the HTTP response
 * @see com.levelup.http.HttpRequestGet for a more simple API
 * @see com.levelup.http.HttpRequestPost for a more simple POST API
 */
public class HttpEngineUrlConnection<T> extends BaseHttpEngine<T,HttpResponseUrlConnection> {
	final HttpURLConnection urlConnection;

	public HttpEngineUrlConnection(BaseHttpRequest.AbstractBuilder<T, ?> builder) {
		super(builder);

		try {
			this.urlConnection = (HttpURLConnection) new URL(getUri().toString()).openConnection();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Bad uri: " + getUri(), e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Process the HTTP request on the network and return the HttpURLConnection
	 * @param request
	 * @return an {@link java.net.HttpURLConnection} with the network response
	 * @throws com.levelup.http.HttpException
	 */
	private void getQueryResponse(TypedHttpRequest<T> request, boolean allowGzip) throws HttpException {
		try {
			if (allowGzip && request.getHeader(HttpClient.ACCEPT_ENCODING)==null) {
				request.setHeader(HttpClient.ACCEPT_ENCODING, "gzip,deflate");
			}

			prepareRequest(request);

			final LoggerTagged logger = request.getLogger();
			if (null != logger) {
				logger.v(request.getHttpMethod() + ' ' + request.getUri());
				for (Map.Entry<String, List<String>> header : urlConnection.getRequestProperties().entrySet()) {
					logger.v(header.getKey()+": "+header.getValue());
				}
			}

			doConnection();

			if (null != logger) {
				logger.v(urlConnection.getResponseMessage());
				for (Map.Entry<String, List<String>> header : urlConnection.getHeaderFields().entrySet()) {
					logger.v(header.getKey()+": "+header.getValue());
				}
			}

		} catch (SecurityException e) {
			throw exceptionToHttpException(request, e).build();

		} catch (IOException e) {
			throw exceptionToHttpException(request, e).build();

		} finally {
			try {
				setRequestResponse(request, new HttpResponseUrlConnection(this));
			} catch (IllegalStateException e) {
				// okhttp 2.0.0 issue https://github.com/square/okhttp/issues/689
				LogManager.getLogger().d("connection closed ? for "+request+' '+e);
				HttpException.Builder builder = request.newException();
				builder.setErrorMessage("Connection closed "+e.getMessage());
				builder.setCause(e);
				builder.setErrorCode(HttpException.ERROR_NETWORK);
				throw builder.build();
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void settleHttpHeaders(TypedHttpRequest<T> request) throws HttpException {
		try {
			urlConnection.setRequestMethod(getHttpMethod());

		} catch (ProtocolException e) {
			throw exceptionToHttpException(request, e).build();
		}

		final long contentLength;
		if (null != bodyParams) {
			setHeader(HTTP.CONTENT_TYPE, bodyParams.getContentType());
			contentLength = bodyParams.getContentLength();
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
		} else {
			contentLength = 0L;
		}
		setHeader(HTTP.CONTENT_LEN, Long.toString(contentLength));

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			urlConnection.setFixedLengthStreamingMode((int) contentLength);
		else
			urlConnection.setFixedLengthStreamingMode(contentLength);

		super.settleHttpHeaders(request);

		for (Entry<String, String> entry : mRequestSetHeaders.entrySet()) {
			urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
		}
		for (Entry<String, HashSet<String>> entry : mRequestAddHeaders.entrySet()) {
			for (String value : entry.getValue()) {
				urlConnection.addRequestProperty(entry.getKey(), value);
			}
		}

		if (null != followRedirect) {
			urlConnection.setInstanceFollowRedirects(followRedirect);
		}

		if (null != getHttpConfig()) {
			int readTimeout = getHttpConfig().getReadTimeout(request);
			if (readTimeout >= 0)
				urlConnection.setReadTimeout(readTimeout);
		}
	}

	@Override
	public final void setupBody() {
		// do nothing
	}

	@Override
	public final void doConnection() throws IOException {
		urlConnection.connect();

		if (null != bodyParams) {
			OutputStream output = urlConnection.getOutputStream();
			try {
				outputBody(output, this);
			} finally {
				output.close();
			}
		}
	}

	@Override
	protected HttpResponseUrlConnection queryResponse(TypedHttpRequest<T> request, ResponseHandler<T> responseHandler) throws HttpException {
		getQueryResponse(request, true);
		HttpResponseUrlConnection response = getHttpResponse();
		try {
			response.getInputStream();
			return response;
		} catch (FileNotFoundException e) {
			try {
				DataErrorException exceptionWithData = responseHandler.errorHandler.handleError(response, this);

				HttpException.Builder exceptionBuilder = exceptionToHttpException(request, exceptionWithData);
				throw exceptionBuilder.build();

			} catch (ParserException ee) {
				throw exceptionToHttpException(request, ee).build();

			} catch (IOException ee) {
				throw exceptionToHttpException(request, ee).build();
			}
		} catch (IOException e) {
			throw exceptionToHttpException(request, e).build();

		}
	}

	@Override
	protected T responseToResult(HttpResponseUrlConnection response, ResponseHandler<T> responseHandler) throws ParserException, IOException {
		return responseHandler.contentParser.transformData(response, this);
	}

}

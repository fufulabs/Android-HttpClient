package com.levelup.http.ion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.http.protocol.HTTP;

import android.net.Uri;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.ConnectionClosedException;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.ion.Response;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.LoadBuilder;
import com.koushikdutta.ion.future.ResponseFuture;
import com.koushikdutta.ion.gson.GsonSerializer;
import com.koushikdutta.ion.loader.AsyncHttpRequestFactory;
import com.levelup.http.BaseHttpRequest;
import com.levelup.http.HttpBodyJSON;
import com.levelup.http.HttpBodyMultiPart;
import com.levelup.http.HttpBodyParameters;
import com.levelup.http.HttpBodyString;
import com.levelup.http.HttpBodyUrlEncoded;
import com.levelup.http.HttpException;
import com.levelup.http.HttpExceptionCreator;
import com.levelup.http.HttpRequest;
import com.levelup.http.InputStreamParser;
import com.levelup.http.UploadProgressListener;
import com.levelup.http.gson.InputStreamGsonParser;
import com.levelup.http.internal.BaseHttpEngine;
import com.levelup.http.ion.internal.IonBody;
import com.levelup.http.ion.internal.IonHttpBodyJSON;
import com.levelup.http.ion.internal.IonHttpBodyMultiPart;
import com.levelup.http.ion.internal.IonHttpBodyString;
import com.levelup.http.ion.internal.IonHttpBodyUrlEncoded;

/**
 * Basic HTTP request to be passed to {@link com.levelup.http.HttpClient}
 *
 * @param <T> type of the data read from the HTTP response
 * @see com.levelup.http.HttpRequestGet for a more simple API
 * @see com.levelup.http.HttpRequestPost for a more simple POST API
 */
public class HttpEngineIon<T> extends BaseHttpEngine<T, HttpResponseIon<T>> {
	public final Builders.Any.B requestBuilder;

	public HttpEngineIon(BaseHttpRequest.AbstractBuilder<T, ?> builder) {
		super(wrapBuilderBodyParams(builder));

		if (builder.getContext() == null) {
			throw new NullPointerException("Ion HTTP request with no Context, try calling HttpClient.setup() first or a constructor with a Context");
		}

		final Ion ion = Ion.getDefault(builder.getContext());
		// until https://github.com/koush/AndroidAsync/issues/210 is fixed
		ion.getConscryptMiddleware().enable(false);

		ion.configure().setAsyncHttpRequestFactory(new AsyncHttpRequestFactory() {
			@Override
			public AsyncHttpRequest createAsyncHttpRequest(Uri uri, String method, Headers headers) {
				AsyncHttpRequest request = new AsyncHttpRequest(uri, method, headers) {
					@Override
					public void logd(String message) {
						if (getLogger() != null)
							getLogger().d(message);
						else
							super.logd(message);
					}

					@Override
					public void logd(String message, Exception e) {
						if (getLogger() != null)
							getLogger().d(message, e);
						else
							super.logd(message, e);
					}

					@Override
					public void logi(String message) {
						if (getLogger() != null)
							getLogger().i(message);
						else
							super.logi(message);
					}

					@Override
					public void logv(String message) {
						if (getLogger() != null)
							getLogger().v(message);
						else
							super.logv(message);
					}

					@Override
					public void logw(String message) {
						if (getLogger() != null)
							getLogger().w(message);
						else
							super.logw(message);
					}

					@Override
					public void loge(String message) {
						if (getLogger() != null)
							getLogger().e(message);
						else
							super.loge(message);
					}

					@Override
					public void loge(String message, Exception e) {
						if (getLogger() != null)
							getLogger().e(message, e);
						else
							super.loge(message, e);
					}

					@Override
					public void setBody(AsyncHttpRequestBody body) {
						if (body instanceof MultipartFormDataBody) {
							MultipartFormDataBody multipartFormDataBody = (MultipartFormDataBody) body;
							multipartFormDataBody.setBoundary(HttpBodyMultiPart.boundary);
						}

						super.setBody(body);
					}
				};
				return request;
			}
		});

		final LoadBuilder<Builders.Any.B> ionLoadBuilder = ion.build(builder.getContext());
		this.requestBuilder = ionLoadBuilder.load(getHttpMethod(), getUri().toString());
	}

	private static <T> BaseHttpRequest.AbstractBuilder<T, ?> wrapBuilderBodyParams(BaseHttpRequest.AbstractBuilder<T, ?> builder) {
		final HttpBodyParameters sourceBody = builder.getBodyParams();
		if (sourceBody instanceof HttpBodyMultiPart)
			builder.setBody(builder.getHttpMethod(), new IonHttpBodyMultiPart((HttpBodyMultiPart) sourceBody));
		else if (sourceBody instanceof HttpBodyJSON)
			builder.setBody(builder.getHttpMethod(), new IonHttpBodyJSON((HttpBodyJSON) sourceBody));
		else if (sourceBody instanceof HttpBodyUrlEncoded)
			builder.setBody(builder.getHttpMethod(), new IonHttpBodyUrlEncoded((HttpBodyUrlEncoded) sourceBody));
		else if (sourceBody instanceof HttpBodyString)
			builder.setBody(builder.getHttpMethod(), new IonHttpBodyString((HttpBodyString) sourceBody));
		else if (sourceBody != null)
			throw new IllegalStateException("Unknown body type "+sourceBody);

		return builder;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	public void settleHttpHeaders(HttpRequest request) throws HttpException {
		if (!isMethodWithBody(getHttpMethod())) {
			setHeader(HTTP.CONTENT_LEN, "0");
		}

		super.settleHttpHeaders(request);

		for (Entry<String, String> entry : mRequestSetHeaders.entrySet()) {
			requestBuilder.setHeader(entry.getKey(), entry.getValue());
		}
		for (Entry<String, HashSet<String>> entry : mRequestAddHeaders.entrySet()) {
			for (String value : entry.getValue()) {
				requestBuilder.addHeader(entry.getKey(), value);
			}
		}

		if (null!=followRedirect) {
			requestBuilder.followRedirect(followRedirect);
		}

		if (null != getHttpConfig()) {
			int readTimeout = getHttpConfig().getReadTimeout(request);
			if (readTimeout >= 0)
				requestBuilder.setTimeout(readTimeout);
		}
	}

	@Override
	public final void setupBody() {
		if (null == requestBuilder) throw new IllegalStateException("is this a streaming request?");
		if (null != bodyParams) {
			((IonBody) bodyParams).setOutputData(requestBuilder);

			final UploadProgressListener progressListener = getProgressListener();
			if (null != progressListener) {
				requestBuilder.progress(new ProgressCallback() {
					@Override
					public void onProgress(long downloaded, long total) {
						progressListener.onParamUploadProgress(HttpEngineIon.this, null, (int) ((100 * downloaded) / total));
					}
				});
			}
		}
	}

	@Override
	public final void doConnection() throws IOException {
		// do nothing
	}

	@Override
	public InputStream getInputStream(HttpRequest request) throws HttpException {
		prepareRequest(request);
		ResponseFuture<InputStream> req = requestBuilder.asInputStream();
		Future<Response<InputStream>> withResponse = req.withResponse();
		return getServerResponse(withResponse, request);
	}

	@Override
	public <P> P parseRequest(InputStreamParser<P> parser, HttpRequest request) throws HttpException {
		// special case: Gson data handling with HttpRequestIon
		if (parser instanceof InputStreamGsonParser) {
			InputStreamGsonParser gsonParser = (InputStreamGsonParser) parser;
			final GsonSerializer<P> gsonSerializer;
			if (gsonParser.typeToken != null) {
				gsonSerializer = new GsonSerializer<P>(gsonParser.gson, gsonParser.typeToken);
			} else if (gsonParser.type instanceof Class) {
				Class<P> clazz = (Class<P>) gsonParser.type;
				gsonSerializer = new GsonSerializer<P>(gsonParser.gson, clazz);
			} else {
				gsonSerializer = null;
			}
			if (null != gsonSerializer) {
				prepareRequest(request);
				ResponseFuture<P> req = requestBuilder.as(gsonSerializer);
				Future<Response<P>> withResponse = req.withResponse();
				return getServerResponse(withResponse, request);
			}
		}

		return super.parseRequest(parser, request);
	}

	private <P> P getServerResponse(Future<Response<P>> req, HttpRequest request) throws HttpException {
		try {
			Response<P> response = req.get();
			setRequestResponse(request, new HttpResponseIon(response));

			if (getHttpResponse().getResponseCode() < 200 || getHttpResponse().getResponseCode() >= 300) {
				HttpException.Builder builder = request.newExceptionFromResponse(null);
				throw builder.build();
			}

			Exception e = getHttpResponse().getException();
			if (null!=e) {
				throw exceptionToHttpException(request, e).build();
			}

			return (P) response.getResult();

		} catch (InterruptedException e) {
			throw exceptionToHttpException(request, e).build();

		} catch (ExecutionException e) {
			throw exceptionToHttpException(request, e).build();

		}
	}

	@Override
	protected InputStream getParseableErrorStream() throws IOException {
		Object result = getHttpResponse().getResult();
		if (result instanceof InputStream) {
			return (InputStream) result;
		}
		if (result == null)
			throw new IOException("error stream not supported");

		return new ByteArrayInputStream(result.toString().getBytes());
	}

	@Override
	protected HttpException.Builder exceptionToHttpException(HttpExceptionCreator request, Exception e) throws HttpException {
		if (e instanceof ConnectionClosedException && e.getCause() instanceof Exception) {
			return exceptionToHttpException(request, (Exception) e.getCause());
		}

		return super.exceptionToHttpException(request, e);
	}
}

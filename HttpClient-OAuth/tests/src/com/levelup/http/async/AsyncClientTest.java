package com.levelup.http.async;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.levelup.http.HttpClient;
import com.levelup.http.HttpException;
import com.levelup.http.HttpRequest;
import com.levelup.http.HttpRequestGet;
import com.levelup.http.InputStreamStringParser;

public class AsyncClientTest extends TestCase {

	private static final String BASIC_URL = "http://www.levelupstudio.com/";
	private static final String BASIC_URL_TAG = "test1";
	private static final String SLOW_URL = "http://httpbin.org/delay/10";
	private static final String BASIC_URL_HTTPS = "https://www.google.com/";
	private static final String SLOW_URL_HTTPS = "https://httpbin.org/delay/10";

	// TODO test with streaming connection (chunked over HTTPS with sometimes no data sent for 1 minute)
	// TODO test with streaming connection with SPDY
	// TODO test with long POST
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		HttpClient.setConnectionFactory(null); // make sure we don't use Okhttp
	}

	public void testAsyncSimpleQuery() {
		AsyncHttpClient.getString(BASIC_URL, BASIC_URL_TAG, null);
	}
	
	private static class TestAsyncCallback extends BaseNetworkCallback<String> {
		@Override
		public void onNetworkFailed(Throwable t) {
			if (t instanceof IOException) {
				// shit happens
			} else if (t instanceof HttpException && t.getCause() instanceof IOException) {
				// shit happens
			} else {
				fail(t.getMessage());
			}
		}
	}
	
	private static class TestLongAsyncCallback extends TestAsyncCallback {
		@Override
		public void onNetworkSuccess(String response) {
			fail("We're not supposed to have received this");
		}
	}

	public void testAsyncSimpleQueryResult() {
		final CountDownLatch latch = new CountDownLatch(1);

		AsyncHttpClient.getString(BASIC_URL, BASIC_URL_TAG, new TestAsyncCallback() {
			@Override
			public void onNetworkSuccess(String response) {
				latch.countDown();
			}
		});
		try {
			latch.await(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("unreasonably slow");
		}
	}


	public void testCancelShort() {
		HttpRequest request = new HttpRequestGet(BASIC_URL);
		Future<String> downloadTask = AsyncHttpClient.doRequest(request, InputStreamStringParser.instance, new TestLongAsyncCallback());

		downloadTask.cancel(true);

		try {
			downloadTask.get();
		} catch(CancellationException e) {
			// fine
		} catch (InterruptedException e) {
			// fine
		} catch (ExecutionException e) {
			fail("the task did not exit correctly "+e);
		}
	}

	public void testCancelShortHttps() {
		HttpRequest request = new HttpRequestGet(BASIC_URL_HTTPS);
		Future<String> downloadTask = AsyncHttpClient.doRequest(request, InputStreamStringParser.instance, new TestLongAsyncCallback());

		downloadTask.cancel(true);

		try {
			downloadTask.get();
		} catch(CancellationException e) {
			// fine
		} catch (InterruptedException e) {
			// fine
		} catch (ExecutionException e) {
			fail("the task did not exit correctly "+e);
		}
	}

	public void testCancelLong() {
		HttpRequest request = new HttpRequestGet(SLOW_URL);
		Future<String> downloadTask = AsyncHttpClient.doRequest(request, InputStreamStringParser.instance, new TestLongAsyncCallback());
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}

		downloadTask.cancel(true);

		try {
			downloadTask.get();
		} catch(CancellationException e) {
			// fine
		} catch (InterruptedException e) {
			// fine
		} catch (ExecutionException e) {
			fail("the task did not exit correctly "+e);
		}
	}
	
	public void testCancelLongHttps() {
		HttpRequest request = new HttpRequestGet(SLOW_URL_HTTPS);
		Future<String> downloadTask = AsyncHttpClient.doRequest(request, InputStreamStringParser.instance, new TestLongAsyncCallback());
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}

		downloadTask.cancel(true);

		try {
			downloadTask.get();
		} catch(CancellationException e) {
			// fine
		} catch (InterruptedException e) {
			// fine
		} catch (ExecutionException e) {
			fail("the task did not exit correctly "+e);
		}
	}
}

package com.levelup.http.ion.async;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.levelup.http.HttpException;
import com.levelup.http.HttpRequestGet;
import com.levelup.http.async.AsyncHttpClient;
import com.levelup.http.async.BaseHttpAsyncCallback;
import com.levelup.http.ion.IonClient;
import com.levelup.http.parser.BodyToString;

public class AsyncClientTest extends AndroidTestCase {

	private static final String BASIC_URL = "http://www.levelupstudio.com/";
	private static final String BASIC_URL_TAG = "test1";
	private static final String SLOW_URL = "http://httpbin.org/delay/10";
	private static final String BASIC_URL_HTTPS = "https://www.google.com/";
	private static final String SLOW_URL_HTTPS = "https://httpbin.org/delay/10";

	// TODO test with streaming connection (chunked over HTTPS with sometimes no data sent for 1 minute)
	// TODO test with streaming connection with SPDY
	// TODO test with long POST

	private static final HttpRequestGet<String> BASIC_REQUEST = new HttpRequestGet(BASIC_URL, BodyToString.RESPONSE_HANDLER);

	@Override
	public void setContext(Context context) {
		super.setContext(context);
		IonClient.setup(context);
	}

	public void testAsyncSimpleQuery() {
		AsyncHttpClient.postTagRequest(BASIC_REQUEST, BASIC_URL_TAG, null);
	}

	private static class TestAsyncCallback extends BaseHttpAsyncCallback<String> {
		@Override
		public void onHttpFailed(Throwable t) {
			if (t instanceof IOException || t instanceof TimeoutException) {
				// shit happens
			} else if (t instanceof HttpException && t.getCause() instanceof IOException) {
				// shit happens
			} else if (t instanceof HttpException && ((HttpException) t).getErrorCode()==HttpException.ERROR_TIMEOUT) {
				// shit happens
			} else {
				fail(t.getMessage());
			}
		}
	}

	private static class TestLongAsyncCallback extends TestAsyncCallback {
		@Override
		public void onHttpResult(String result) {
			fail("We're not supposed to have received this");
		}
	}

	@MediumTest
	public void testAsyncSimpleQueryResult() {
		final CountDownLatch latch = new CountDownLatch(1);

		AsyncHttpClient.postTagRequest(BASIC_REQUEST, BASIC_URL_TAG, new TestAsyncCallback() {
					@Override
					public void onHttpResult(String result) {
						// we received the result successfully
						latch.countDown();
					}
				}
		);
		try {
			latch.await();
		} catch (InterruptedException e) {
			fail("unreasonably slow");
		}
	}


	public void testCancelShort() {
		Future<String> downloadTask = AsyncHttpClient.postRequest(BASIC_REQUEST, new TestLongAsyncCallback());

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
		HttpRequestGet<String> request = new HttpRequestGet(BASIC_URL_HTTPS, BodyToString.RESPONSE_HANDLER);
		Future<String> downloadTask = AsyncHttpClient.postRequest(request, new TestLongAsyncCallback());

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
		HttpRequestGet<String> request = new HttpRequestGet(SLOW_URL, BodyToString.RESPONSE_HANDLER);
		Future<String> downloadTask = AsyncHttpClient.postRequest(request, new TestLongAsyncCallback());
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
		HttpRequestGet<String> request = new HttpRequestGet(SLOW_URL_HTTPS, BodyToString.RESPONSE_HANDLER);
		Future<String> downloadTask = AsyncHttpClient.postRequest(request, new TestLongAsyncCallback());
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

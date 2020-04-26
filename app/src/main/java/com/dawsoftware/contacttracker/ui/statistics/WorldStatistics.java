package com.dawsoftware.contacttracker.ui.statistics;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.dawsoftware.contacttracker.BuildConfig;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.analytics.Analytics;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;

public class WorldStatistics extends Fragment {
	
	private static final String URL = "https://google.com/covid19-map/"; //?hl=
	
	private static final String FOOTER_CLASS = "nHiRk";
	private static final String HEADER_CLASS = "pGxpHc";
	private static final String HEADER_PADDING_CLASS = "VjFXz";
	
	private static final String TEMP_DIR_NAME = "docs";
	private static final String PAGE_NAME = "/page.html";
	
	private WebView webView;
	private WebViewClient client;
	
	private SwipeRefreshLayout swipeLayout;
	private FrameLayout noDataLayout;
	
	private LoadPage loadPageHandler = null;
	
	private boolean isLoading = false;
	
	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		loadPageHandler = new LoadPageImpl();
		client = new WebViewClientImpl();
	}
	
//	private String parseLocale() {
//		String current = "en";
//
//		try {
//			current = getResources().getConfiguration().locale.getLanguage();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return current;
//	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Analytics.statisticsScreenOpened(getActivity());
		
		webView.loadUrl(URL);
		
//		if (loadPageHandler == null) {
//			loadPageHandler = new LoadPageImpl();
//		}
		
//		getHtmlFromWeb(parseLocale(), loadPageHandler);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
//		loadPageHandler = null;
	}
	
	private void getHtmlFromWeb(final String locale, final LoadPage loadPageHandler) {
		new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					final String url = URL + locale;
					
					final Document doc = Jsoup.connect(url).get();
					
					final Elements footer = doc.getElementsByClass(FOOTER_CLASS);
					final Elements header = doc.getElementsByClass(HEADER_CLASS);
					final Elements headerPadding = doc.getElementsByClass(HEADER_PADDING_CLASS);
					
					footer.remove();
					header.remove();
					headerPadding.remove();
					
					final String html = doc.outerHtml();
					
					File cachePath = null;
					
					try {
						cachePath = new File(getActivity().getCacheDir(), TEMP_DIR_NAME);
						cachePath.mkdirs();
						
						FileOutputStream stream = new FileOutputStream(cachePath + PAGE_NAME);
						stream.write(html.getBytes());
						stream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					if (cachePath != null && cachePath.exists() && loadPageHandler != null) {
						loadPageHandler.loadPage(cachePath + PAGE_NAME);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}).start();
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
	                         @Nullable final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_world_stat, container, false);
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		swipeLayout = view.findViewById(R.id.statistics_swipe);
		swipeLayout.setEnabled(false);
		swipeLayout.setOnRefreshListener(() -> {
			
			if (isLoading) {
				return;
			}
			
			if (webView != null) {
				webView.reload();
			}
		});
		
		noDataLayout = view.findViewById(R.id.no_data);
		
		webView = view.findViewById(R.id.world_stat_view);
		if (webView != null) {
			webView.setWebViewClient(client);
			webView.getSettings().setJavaScriptEnabled(true);
			
			if (BuildConfig.DEBUG) {
				webView.setWebChromeClient(new WebChromeClient() {
					public boolean onConsoleMessage(ConsoleMessage cm) {
						Log.i("wwww", cm.message() + " -- From line "
								+ cm.lineNumber() + " of "
								+ cm.sourceId());
						return true;
					}
				});
			}
		}
	}
	
	interface LoadPage {
		void loadPage(final String path);
	}
	
	class LoadPageImpl implements LoadPage {
		
		@Override
		public void loadPage(final String path) {
			new Handler(Looper.getMainLooper()).post(() -> {
//				if (webView != null) {
//					webView.loadUrl("file:///" + path);
//				}
			});
		}
	}
	
	private class WebViewClientImpl extends WebViewClient {
		
		boolean mustClearVisibility = false;
		
		boolean hasError = false;
		
		@Override
		public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			
			isLoading = true;
			
			noDataLayout.setVisibility(View.INVISIBLE);
			webView.setVisibility(View.INVISIBLE);
			
			swipeLayout.setRefreshing(true);
		}
		
		@Override
		public void onPageFinished(final WebView view, final String url) {
			super.onPageFinished(view, url);
			
			isLoading = false;
			
			if (hasError) {
				hasError = false;
				return;
			}

			if (mustClearVisibility) {
				swipeLayout.setRefreshing(false);
				swipeLayout.setEnabled(false);
				
				noDataLayout.setVisibility(View.INVISIBLE);
				webView.setVisibility(View.VISIBLE);
				
				mustClearVisibility = false;
				return;
			}
			
			webView.loadUrl("javascript:(function() { " +
//					                "document.getElementsByTagName(\"c-wiz\")[0].getElementsByClassName(\"nHiRk\")[0].style" +
//					                ".display=\"none\";" +
					                "document.getElementsByClassName('pGxpHc')[0].style.display='none'; " + // header
//					                "document.getElementsByClassName('VjFXz')[0].style.display='none'; " + // header padding
					
//					                "console.log(document.body.getElementsByClassName(\"nHiRk\")[0].childElementCount);" +
//					                "var node = document.getElementsByTagName(\"c-wiz\")[0].getElementsByClassName" +
//					                "(\"nHiRk\")[0].childNodes[0];" +
//
//					                "var div = document.createElement('div');" +
//									"div.className = \"alert\";" +
//									"div.innerHTML = \"Вы прочитали важное сообщение.\";" +
//
//					                "node.replaceWith(div);"+
//					                "console.log(document.body.getElementsByClassName(\"nHiRk\")[0].childElementCount);" +
					
					
					
//					                "var doc = document.getElementsByTagName(\"c-wiz\")[0].getElementsByClassName(\"nHiRk\");" +
//
//					                "console.log(doc[0].childElementCount);" +
//
//					                "var node = doc[0].children[0];" +
//									"doc[0].removeChild(node);" +
//
//					                "console.log(doc[0].childElementCount);" +
					
					                 "})()");
			
			mustClearVisibility = true;
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
			if (url.contains(URL)) {
				view.loadUrl(url);
				return true;
			}
			
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(intent);
			
			return true;
		}
		
		@TargetApi(VERSION_CODES.N)
		@Override
		public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest request) {
			if (request.getUrl().toString().contains(URL)) {
				view.loadUrl(request.getUrl().toString());
				return true;
			}
			
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getUrl().toString()));
			startActivity(intent);
			
			return true;
		}
		
		
		@Override
		public void onReceivedError(final WebView view, final WebResourceRequest request, final WebResourceError error) {
			super.onReceivedError(view, request, error);
			handleError();
		}
		
		@Override
		public void onReceivedHttpError(final WebView view, final WebResourceRequest request,
		                                final WebResourceResponse errorResponse) {
			super.onReceivedHttpError(view, request, errorResponse);
			handleError();
		}
		
		@Override
		public void onReceivedSslError(final WebView view, final SslErrorHandler handler, final SslError error) {
			super.onReceivedSslError(view, handler, error);
			handleError();
		}
		
		private void handleError() {
			hasError = true;
			
			if (noDataLayout != null) {
				webView.setVisibility(View.GONE);
				noDataLayout.setVisibility(View.VISIBLE);
				swipeLayout.setRefreshing(false);
				swipeLayout.setFocusable(true);
				noDataLayout.setFocusable(false);
			}
		}
	}
}

package com.ywesee.amiko;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ExpertInfoView {
	
	private WebView mWebView = null;
	private JSInterface mJSInterface = null;
	private Context mContext = null;
	
	public ExpertInfoView(Context context, WebView webView) {
		mWebView = webView;
		mContext = context;

		// Override web client to open all links in same webview
		// mWebView.setWebChromeClient(new WebChromeClient());
		mWebView.setWebViewClient(new MyWebViewClient());
				
		mWebView.setInitialScale(1);
		mWebView.setPadding(0, 0, 0, 0);		
		mWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);	
		mWebView.setScrollbarFadingEnabled(true);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.requestFocus(WebView.FOCUS_DOWN);
		
		// Activate JavaScriptInterface in given context
		mJSInterface = new JSInterface(context);
		mWebView.addJavascriptInterface(mJSInterface, "jsInterface");				
				
		WebSettings wsettings = mWebView.getSettings();		    		
		// Sets whether WebView loads pages in overview mode
		wsettings.setLoadWithOverviewMode(true);
		// Tells WebView to use a wide viewport
		wsettings.setUseWideViewPort(true);
		// Sets whether WebView should use its built-in zoom mechanisms
		wsettings.setBuiltInZoomControls(true);
		// Sets whether WebView should display on-screen zoom controls
		wsettings.setDisplayZoomControls(false);
		// Sets default zoom density of the page
		// wsettings.setDefaultZoom(WebSettings.ZoomDensity.CLOSE);
		wsettings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);//SINGLE_COLUMN);
		// Enable javascript
		wsettings.setJavaScriptEnabled(true);
		// TODO
		wsettings.setLoadsImagesAutomatically(true);		
	}
	
	public WebView getWebView() {
		return mWebView;
	}
	
	public void adjustZoom() {
		int orientation = mContext.getResources().getConfiguration().orientation;
		if (orientation==Configuration.ORIENTATION_PORTRAIT) {
			mWebView.getSettings().setTextZoom(175);
		} else if (orientation==Configuration.ORIENTATION_LANDSCAPE) {
			mWebView.getSettings().setTextZoom(125);						
		}
	}
	
	public JSInterface getJSInterface() {
		return mJSInterface;
	}
	
	/**
	 * Customizes web view client to open links from your own site in the same web view otherwise
	 * just open the default browser activity with the URL
	 * @author Max
	 * 
	 */
	private class MyWebViewClient extends WebViewClient {
				
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	if (url.startsWith("https://github.com/")) {
	    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
	    		mContext.startActivity(intent);    		
	    		return true;
	    	}
	    	// Otherwise
	   	 	view.loadUrl(url);
	   	 	view.requestFocus();
	    	return false;
	    }
	    
	    @Override
	    public void onPageFinished(WebView view, String url) {
	    	super.onPageFinished(view, url);
	    }
	}
}

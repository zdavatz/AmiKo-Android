package com.ywesee.amiko;

// Note: might have to include the one of the Android support lib!
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.ywesee.amiko.de.R;

public class ReportFragment extends Fragment {
	
	private WebView mWebView = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}	
	
    @Override 
    public void onActivityCreated(Bundle savedInstanceState) { 
        super.onActivityCreated(savedInstanceState); 
    }
	
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState )	{
		View view = inflater.inflate( R.layout.report_view, container, false );		
		view.setPadding(5, 5, 5, 5);	
		
		mWebView = (WebView) view.findViewById(R.id.reportView);
		// Activate JavaScriptInterface
		mWebView.addJavascriptInterface(new JSInterface(this.getActivity()), "jsInterface");		
		// Enable javascript
		mWebView.getSettings().setJavaScriptEnabled(true);
		
		return view;
	}	
}

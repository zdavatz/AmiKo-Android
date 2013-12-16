package com.ywesee.amiko;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class JSInterface {
	Context mContext;

	JSInterface(Context c) {
		mContext = c;
	}
	
	// Annotation as of API 17 (08.Dec.2013)
	@JavascriptInterface		
	public String showToast(String toast) {
		Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
		return toast;
	}
	
	public int highlightKeyword(String key) {
		return key.length();
	}
}

/*
Copyright (c) 2013 Max Lungarella

This file is part of AmiKo for Android.

AmiKo for Android is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.ywesee.amiko;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class JSInterface {
	
	private static final String TAG = "JSInterface"; // Tag for LogCat window
	
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

	@JavascriptInterface
	public void receiveValueFromJS(int searchCnt) {
		Log.d(TAG, "Search result count = " + searchCnt);
	}
	
	public int highlightKeyword(String key) {
		return key.length();
	}
}

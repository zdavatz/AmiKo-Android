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
import android.widget.Toast;

/**
 * TODO: transform into singleton class!
 * @author Max
 *
 */
public class CustomToast {
	
	private Context mContext = null;
	private Toast mToastObject = null;
	
	public CustomToast(Context context) {
		mContext = context;
	}
	
	public void show(String text, int duration) {
		if (mToastObject!=null)
			mToastObject.cancel();
		mToastObject = Toast.makeText(mContext, text, duration);
		mToastObject.show();
	}
}

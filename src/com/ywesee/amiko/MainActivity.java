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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.ywesee.amiko.de.R;

public class MainActivity extends Activity {

	private static final String TAG = "AmiKoActivity";
	private static final boolean DEBUG = false;

	// Main AsyncTask
	private AsyncSearchTask mAsyncSearchTask = null;	
	// SQLite database adapter
	private DBAdapter mMediDataSource;
	// List of medications returned by SQLite query
	private List<Medication> mMedis = null;
	// Html string displayed in show_view
	private String mHtmlString;
	// Current action bar tab
	private String mActionName = "";
	// Minimum number of characters used for SQLite query (default: min 1-chars search)
	private int mMinCharSearch = 0;	
	// Global timer used for benchmarking app
	private long mTimer = 0;	

	// Listview of suggestions returned by SQLite query
	private ListView mListView = null;	
	// ListView of section titles (shortcuts)
	private ListView mSectionView = null;	
	// Webview used to display "Fachinformation"
	private WebView mWebView;	
	// Webview used to display the report (About-File)
	private WebView mReportWebView;
	// Cascading style sheet
	private String mCSS_str = null;
	
	// Actionbar menu items
	private MenuItem mSearchItem = null;
	private EditText mSearch = null;
	private Button mDelete = null;
	
	// Viewholder for fragments
	private ViewGroup mViewHolder = null;	
	private View mSuggestView = null;	
	private View mShowView = null;
	private View mReportView = null;	
	
	// Fragments
	private ReportFragment mReportFragment = null;
	
	/**
	 * Implements listeners for action bar
	 * @author MaxL
	 *
	 */
	private class MyTabListener implements ActionBar.TabListener {
		
		private Fragment mFragment;
		private final Activity mActivity;
		private final String mFragName;
		private final String mTabName;
		
		public MyTabListener(Activity activity, String fragName, String tabName) {
			mActivity = activity;
			mFragName = fragName;
			mTabName = tabName;		
			mActionName = getString(R.string.tab_name_1); // Präparat
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			if (mSearch!=null) {				
				// Reset search
				mSearch.setText("");
				// Set hint
				mSearch.setHint(getString(R.string.search) + " " + mTabName);			
			}
			// Change content view
    		if (mShowView!=null) {
    			mShowView.setVisibility(View.GONE);
    			mReportView.setVisibility(View.GONE);
    			mSuggestView.setVisibility(View.VISIBLE);
    		}		    		
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mFragment = Fragment.instantiate(mActivity, mFragName);
			ft.add(android.R.id.content, mFragment);		
			mActionName = mTabName;
			if (mMedis!=null)
				showResults(mMedis);
			if (mSearch!=null) {
				// Reset search
				mSearch.setText("");
				// Set hint
				mSearch.setHint(getString(R.string.search) + " " + mTabName);
			}
    		// Change content view
    		if (mShowView!=null) {
    			mShowView.setVisibility(View.GONE);
    			mReportView.setVisibility(View.GONE);    			
    			mSuggestView.setVisibility(View.VISIBLE);  			
    		}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {			
			ft.remove( mFragment );
			mFragment = null;
		}
	}	

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mMedis;
	}
	
	/**
	 * Overrides onCreate method
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		// Flag for enabling the Action Bar on top
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		
		// Create action bar
		int mode = ActionBar.NAVIGATION_MODE_TABS;		
		if (savedInstanceState != null) {
			mode = savedInstanceState.getInt("mode", ActionBar.NAVIGATION_MODE_TABS);
		}
		// Setup action bar for tabs
		ActionBar ab = getActionBar();
		// Disable activity title
		ab.setDisplayShowTitleEnabled(true);
		setTabNavigation(ab);
						
		// Sets current content view
		setContentView(R.layout.activity_main);	

		// Initialize views
		mSuggestView = getLayoutInflater().inflate(R.layout.suggest_view, null);
		mShowView = getLayoutInflater().inflate(R.layout.show_view, null);
		mReportView = getLayoutInflater().inflate(R.layout.report_view, null);
		mReportFragment = new ReportFragment();
		// Add views to viewholder
		mViewHolder = (ViewGroup) findViewById(R.id.main_layout);		
		mViewHolder.addView(mSuggestView);		
		mViewHolder.addView(mShowView);	
		mViewHolder.addView(mReportView);
	
		// Set visibility of views
		mShowView.setVisibility(View.GONE);
		mReportView.setVisibility(View.GONE);
		mSuggestView.setVisibility(View.VISIBLE);				

		// Setup webviews
		setupWebView();
		setupReportView();
				
		// Reset action name
		mActionName = getString(R.string.tab_name_1);
		
		// Initialize suggestion listview
		mListView = (ListView) findViewById(R.id.listView1);
		mListView.setClickable(true);
		
		try {
			AsyncInitDBTask initDBTask = new AsyncInitDBTask(this);						
			initDBTask.execute();		
		} catch (Exception e) {
			Log.e(TAG, "AsyncInitDBTask exception caught!");
		}

		// Get search intent
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			// showResults(query);
		}	
	}
	
	private void setupWebView() {
		// Define and load webview
		mWebView = (WebView) findViewById(R.id.webView1);	
		// Override web client to open all links in same webview
		// mWebView.setWebChromeClient(new WebChromeClient());
		mWebView.setWebViewClient(new MyWebViewClient());
		
		mWebView.setInitialScale(1);
		mWebView.setPadding(0, 0, 0, 0);		
		mWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);	
		mWebView.setScrollbarFadingEnabled(true);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.requestFocus(WebView.FOCUS_DOWN);
		// Activate JavaScriptInterface
		mWebView.addJavascriptInterface(new JSInterface(this), "jsInterface");		

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
		wsettings.setLightTouchEnabled(true);
		// Enable javascript
		wsettings.setJavaScriptEnabled(true);
		// TODO
		wsettings.setLoadsImagesAutomatically(true);		
				
		// Load CSS from asset folder
		mCSS_str = loadFromFile("amiko_stylesheet.css", "UTF-8"); // loadCSS();
	}

	private void setupReportView() {
		mReportView.setPadding(5, 5, 5, 5);	
		mReportWebView = (WebView) mReportView.findViewById(R.id.reportView);
		// Activate JavaScriptInterface
		mReportWebView.addJavascriptInterface(new JSInterface(this), "jsInterface");		
		// Enable javascript
		mReportWebView.getSettings().setJavaScriptEnabled(true);
	}
	
	/**
	 * Asynchronous thread launched to initialize the SQLite database
	 * @author Max
	 *
	 */
	private class AsyncInitDBTask extends AsyncTask<Void, Integer, Void> {
		
		// Progressbar
		private ProgressDialog progressBar;
		private Context context;
		
		public AsyncInitDBTask(Context context) {
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			if (DEBUG)
				Log.d(TAG, "onPreExecute(): progressDialog");
	        // initialize the dialog
			progressBar = new ProgressDialog(MainActivity.this);
	        progressBar.setMessage("Initializing database ...");
	        progressBar.setIndeterminate(true);
	        progressBar.setCancelable(true);
	        progressBar.show();
		}
				
		@Override
		protected void onProgressUpdate(Integer... progress) {
			// progressBar.incrementProgressBy(progress[0]);
		}
		
		@Override
		protected Void doInBackground(Void... voids) {
			if (DEBUG)
				Log.d(TAG, "doInBackground: open database");
			// Creates and opens database
			mMediDataSource = new DBAdapter(this.context);
			// Display progressbar ...
			try {
				mMediDataSource.create();
			} catch( IOException e) {
				Log.d(TAG, "unable to create database!");
				throw new Error("Unable to create database");
			}	
			mMediDataSource.open();	
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {			
			if (DEBUG) 
				Log.d(TAG, "mMediDataSource open!");			
			if (progressBar.isShowing())
				progressBar.dismiss();
		}
	}
	
	/**
	 * Asynchronous thread launched to search in the SQLite database
	 * @author Max
	 *
	 */
	private class AsyncSearchTask extends AsyncTask<String, Void, Void> {

		List<Medication> medis = null;
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			mAsyncSearchTask.cancel(true);
		}

		@Override
		protected void onPreExecute() {
			// TODO
		}
		
		@Override
		protected Void doInBackground(String... search_key) {
			// Do the expensive work in the background here
			try {
				// Thread.sleep(1000L);
				if (!isCancelled()) {
					if (search_key[0].length()>mMinCharSearch) {
						medis = mMedis = getResults(search_key[0]);		
					}
				}
			} catch (Exception e) {
				//
			}
			return null;
		}	
		
		@Override
		protected void onPostExecute(Void r) {
			if (medis!=null) {		
				showResults(medis);
			}
		}
		
		@Override
		protected void onProgressUpdate(Void... progress_info) {
			//
		}
	}	
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is killed and restarted.
		savedInstanceState.putInt("mode", getActionBar().getNavigationMode());
	}	

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		if (mWebView!=null) {
			// Checks the orientation of the screen
			if (newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE) {
				mWebView.getSettings().setTextZoom(125);
			} else if (newConfig.orientation==Configuration.ORIENTATION_PORTRAIT) {
				mWebView.getSettings().setTextZoom(175);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu. Add items to the action bar if present.
		getMenuInflater().inflate(R.menu.actionbar, menu);

		menu.findItem(R.id.menu_pref1).setChecked(false);
		
		mSearchItem = menu.findItem(R.id.menu_search);
		mSearchItem.setVisible(true);		
		
		mSearch = (EditText) mSearchItem.getActionView().findViewById(R.id.search);
		if (mSearch != null) {
			mSearch.setHint(getString(R.string.search) + " " + getString(R.string.tab_name_1));
		}
		mSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					getWindow().setSoftInputMode(
							WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
		mSearch.requestFocus();
		
		mDelete = (Button) mSearchItem.getActionView().findViewById(R.id.delete);		
		mDelete.setVisibility( mSearch.getText().length()>0 ? View.VISIBLE : View.GONE );
		
		mSearch.addTextChangedListener(new TextWatcher() {		
			@Override
			public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
				//
			}					
			
			@Override
			public void onTextChanged( CharSequence cs, int start, int before, int count ) {	
				/*
				if (myTask!=null) {
					myTask.cancel(true);
					Log.d(TAG, "onTextChanged: cancel()");
				}
				*/
				// Shifted to afterTextChanged ... 
				/*
				myTask = new MyAsyncTask();						
				myTask.execute(cs.toString());		
				*/
			}	

			@Override
			public void afterTextChanged(Editable s) {	
				String search_key = mSearch.getText().toString();
	
				if (search_key!="") {
					if (mSuggestView.getVisibility()==View.VISIBLE) {
						long t0 = System.currentTimeMillis();				
						mAsyncSearchTask = new AsyncSearchTask();						
						mAsyncSearchTask.execute(search_key);	
						if (DEBUG)
							Log.d(TAG, "Time AsyncTask: "+Long.toString(System.currentTimeMillis()-t0)+"ms");
					} else if (mShowView.getVisibility()==View.VISIBLE) {
						if (mWebView!=null) {
							if (search_key.length()>2) {
						    	mWebView.loadUrl("javascript:MyApp_HighlightAllOccurencesOfString('" + search_key + "')");									
							}
							else {
								mWebView.loadUrl("javascript:MyApp_RemoveAllHighlights()");								
							}
						}
					} else if (mReportView.getVisibility()==View.VISIBLE) {
						if (search_key.length()>2) {
							mReportWebView.loadUrl("javascript:MyApp_HighlightAllOccurencesOfString('" + search_key + "')");									
						}
						else {
							mReportWebView.loadUrl("javascript:MyApp_RemoveAllHighlights()");								
						}
					}
				}
				mDelete.setVisibility( s.length()>0 ? View.VISIBLE : View.GONE );	    		
			}
		} );
		
		mDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSearch.setText("");
				/*
				// Change content view
	    		if (mShowView!=null) {
	    			mShowView.setVisibility(View.GONE);
	    			mSuggestView.setVisibility(View.VISIBLE);
	    		}
	    		*/	
			}
		});
		
		return true;
	}
	
    protected boolean isAlwaysExpanded() {
        return false;
    }    
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case (R.id.menu_pref1): {
			Toast.makeText(getBaseContext(), getString(R.string.menu_pref1), Toast.LENGTH_SHORT).show();
			if (!item.isChecked()) {
				item.setChecked(true);
				mMinCharSearch = 1;
			} else {
				item.setChecked(false);
				mMinCharSearch = 0;
			}			
			return true;
		}
		case (R.id.menu_pref2): {
			if (getString(R.string.app_name).equals("AmiKo"))
				Toast.makeText(getBaseContext(), "About AmiKo", Toast.LENGTH_SHORT).show();	
			else if (getString(R.string.app_name).equals("CoMed"))
				Toast.makeText(getBaseContext(), "à propos de CoMed", Toast.LENGTH_SHORT).show();					
			// Reset and change search hint
			if (mSearch != null) {
				mSearch.setText("");
				mSearch.setHint(getString(R.string.search) + " " + getString(R.string.report_search));
			}
			// Load report from file
			String parse_report = "";
			if (getString(R.string.app_name).equals("AmiKo"))
				parse_report = loadReport("amiko_report_de.html");
			else if (getString(R.string.app_name).equals("CoMed"))
				parse_report = loadReport("amiko_report_fr.html");
			else
				parse_report = loadReport("amiko_report.html");
			mReportWebView.loadDataWithBaseURL("app:myhtml", parse_report, "text/html", "utf-8", null);
			// Display report
			mShowView.setVisibility(View.GONE);
			mSuggestView.setVisibility(View.GONE);
			// mReportView.setVisibility(View.VISIBLE);			
			
			FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            ft.show(mReportFragment);
            ft.commit();
			
			return true;
		}
		/*
		case (R.id.menu_pref3): {
			Toast.makeText(getBaseContext(), "Volltextsuche", Toast.LENGTH_SHORT).show();
			return true;
		}
		*/
		default:
			break;
		}

		return true;
	}    
	
	private List<Medication> getResults(String query) {
    	List<Medication> medis = null;
    	
		mTimer = System.currentTimeMillis();		
		
    	if (mActionName.equals(getString(R.string.tab_name_1)))
	 		medis = mMediDataSource.searchTitle((query!=null ? query.toString() : "@@@@"));
	 	else if (mActionName.equals(getString(R.string.tab_name_2)))
	 		medis = mMediDataSource.searchAuth((query!=null ? query.toString() : "@@@@"));
 		else if (mActionName.equals(getString(R.string.tab_name_3)))	 		
	 		medis = mMediDataSource.searchRegnr((query!=null ? query.toString() : "@@@@"));			
	 	else if (mActionName.equals(getString(R.string.tab_name_4)))
	 		medis = mMediDataSource.searchATC((query!=null ? query.toString() : "@@@@")); 	
		else if (mActionName.equals(getString(R.string.tab_name_5)))
	 		medis = mMediDataSource.searchSubstance((query!=null ? query.toString() : "@@@@"));
		else if (mActionName.equals(getString(R.string.tab_name_6)))
	 		medis = mMediDataSource.searchApplication((query!=null ? query.toString() : "@@@@"));
		
    	if (DEBUG)
    		Log.d(TAG, "getResults() - "+medis.size()+" medis found in "+Long.toString(System.currentTimeMillis()-mTimer)+"ms");	   	
    	
    	return medis;
	}
	
    private void showResults(List<Medication> medis) {
		if (medis!=null) {	
	   		// Create simple cursor adapter
	   		CustomListAdapter<Medication> custom_adapter = 
	   			new CustomListAdapter<Medication>(this, R.layout.medi_result, medis);	
	   		// Set adapter to listview		
	   		mListView.setAdapter(custom_adapter);	
	   		// Give some feedback about the search to the user (could be done better!)
	   		Toast.makeText(getApplicationContext(), + medis.size() + " Suchresultate in " + (System.currentTimeMillis()-mTimer) + "ms", 
	   				Toast.LENGTH_SHORT).show();
	   	}
	}
	   		
    /**
     * Displays a customized list of items
     * @author Max
     * 
     * @param <T>
     */
	private class CustomListAdapter<T> extends ArrayAdapter<T> {		
		// TODO: add starring mechanisms plus list of favorites
		
		private Context mContext;
		private int id;
		private List<T> items ;

		public CustomListAdapter(Context context, int textViewResourceId , List<T> list ) {
		    super(context, textViewResourceId, list);           
		    mContext = context;
		    id = textViewResourceId;
		    items = list ;
		}

		@Override
		public int getCount() {
			return items!=null ? items.size() : 0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
		    View mView = convertView;
		    if (mView==null) {
		    	/*
		    	LayoutInflater vi = getLayoutInflater();
		        mView = vi.inflate(id, parent, false);
		    	*/
		    	LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        mView = vi.inflate(id, null);
		    }
		     
		    final Medication med = (Medication) items.get(position);

		    ImageView image_logo = (ImageView) mView.findViewById(R.id.mlogo);		    
        	image_logo.setImageResource(R.drawable.logo_desitin);	
		    
        	String title_str = "k.A.";		// tab_name_1 = Präparat
        	String auth_str = "k.A.";		// tab_name_2 = Inhaber
        	String regnr_str = "k.A.";		// tab_name_3 = Reg.Nr.
        	String atc_code_str = "k.A.";	// tab_name_4 = ATC Code 
        	String atc_class_str = "k.A.";	//			  = ATC Klasse	
        	String substances_str = "k.A.";	// tab_name_5 = Wirkstoff
        	String therapy_str = "k.A.";	// tab_name_6 = Therapie / Indications
        	String application_str = "";
        	String pack_info_str = "";
        	
		 	if (mActionName.equals(getString(R.string.tab_name_1))) {
		 		// Display "Präparatname" and "Therapie/Kurzbeschrieb"
		 		TextView text_title = (TextView) mView.findViewById(R.id.mtitle);
			    TextView text_auth = (TextView) mView.findViewById(R.id.mauth);
			    
			    if (med!=null ) {    
			    	if (med.getTitle()!=null) 
			    		title_str = med.getTitle();
			    	if (med.getPackInfo()!=null)
			    		pack_info_str = med.getPackInfo();		    	
			    	text_title.setText(title_str);
			    	text_title.setTextColor(Color.argb(255,10,10,10));
			    	// text_auth.setText(pack_info_str);  // --> Original solution
			    	// text_auth.setText(Html.fromHtml(pack_info_str));	 // --> Solution with fromHtml (slow)		
	        		text_auth.setTextColor(Color.argb(255,128,128,128));	

	        		Pattern p_red = Pattern.compile(".*O]");
	        		Pattern p_green = Pattern.compile(".*G]");
			    	Matcher m_red = p_red.matcher(pack_info_str);
			    	Matcher m_green = p_green.matcher(pack_info_str);
			    	SpannableStringBuilder spannable = new SpannableStringBuilder(pack_info_str);
			    	while (m_red.find()) {   		
			    		spannable.setSpan(new ForegroundColorSpan(Color.rgb(205,0,0)), m_red.start(), m_red.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);	    		
			    	}			    	
			    	while (m_green.find()) {
			    		spannable.setSpan(new ForegroundColorSpan(Color.rgb(50,188,50)), m_green.start(), m_green.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			    	}
			    	text_auth.setText(spannable, BufferType.SPANNABLE);
			    	
		        	if (med.getCustomerId()==1) {
			        	image_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		image_logo.setVisibility(View.GONE);
		        	}
			    }		 		
		 	}
		 	else if (mActionName.equals(getString(R.string.tab_name_2))) {
		 		// Display "Präparatname" and "Inhaber"		 		
		 		TextView text_title = (TextView) mView.findViewById(R.id.mtitle);
			    TextView text_auth = (TextView) mView.findViewById(R.id.mauth);	
			    
			    if (med!=null ) {    	 
			    	if (med.getTitle()!=null) 
			    		title_str = med.getTitle();
			    	if (med.getAuth()!=null) 
			    		auth_str = med.getAuth();
		    		text_title.setText(title_str);    
		    		text_auth.setText(auth_str);		    		
			    	text_title.setTextColor(Color.argb(255,10,10,10));
	        		text_auth.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	image_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		image_logo.setVisibility(View.GONE);
		        	}
			    }
		 	}
			else if (mActionName.equals(getString(R.string.tab_name_3))) {
				// Display name, registration number (swissmedicno5) and author
				TextView text_title = (TextView) mView.findViewById(R.id.mtitle);
			    TextView text_regnr = (TextView) mView.findViewById(R.id.mauth);	
			    
			    if (med!=null ) {  
			    	if (med.getTitle()!=null) 
			    		title_str = med.getTitle();
			    	if (med.getRegnrs()!=null)
			    		regnr_str = med.getRegnrs();
			    	if (med.getAuth()!=null)
			    		auth_str = med.getAuth();
			    	
		    		text_title.setText(title_str);			    				    	
			        text_regnr.setText(regnr_str + " - " + auth_str);			        
			        text_title.setTextColor(Color.argb(255,10,10,10));
	        		text_regnr.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	image_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		image_logo.setVisibility(View.GONE);
		        	}
			    }
			}
		 	else if (mActionName.equals(getString(R.string.tab_name_4))) {
		 		// Display "Präparatname" and "ATC Code" (atccode) and "Inhaber" (author)		 		
		 		TextView text_title = (TextView) mView.findViewById(R.id.mtitle);		 		
			    TextView text_auth = (TextView) mView.findViewById(R.id.mauth);
			    if (med!=null) {   
			    	if (med.getTitle()!=null)
			    		title_str = med.getTitle();
				    List<String> atc = Arrays.asList(med.getAtcCode().split("\\s*;\\s*"));			    				        
			    	if (atc.size()>1) {
			    		if (atc.get(0)!=null)
			    			atc_code_str = atc.get(0);
			    		if (atc.get(1)!=null)
			    			atc_class_str = atc.get(1);
			    	}
			    	
			    	text_title.setText(title_str);			     
		        	text_auth.setText(atc_code_str + " - " + atc_class_str);			    	
			    	text_title.setTextColor(Color.argb(255,10,10,10));
	        		text_auth.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	image_logo.setVisibility(View.VISIBLE);			        	
		        	} else {
		        		image_logo.setVisibility(View.GONE);
		        	}
			    }		 		
		 	}		 	
	 		else if (mActionName.equals(getString(R.string.tab_name_5))) {
	 			// Display substances, name and author
	 			TextView text_substances = (TextView) mView.findViewById(R.id.mtitle);
			    TextView text_title = (TextView) mView.findViewById(R.id.mauth);	
			    
			    if (med!=null) {
			    	if (med.getSubstances()!=null)
			    		substances_str = med.getSubstances();
			    	if (med.getTitle()!=null)
			    		title_str = med.getTitle();
			    	if (med.getAuth()!=null)
			    		auth_str = med.getAuth();
			    	
			        text_substances.setText(substances_str);			     
			        text_title.setText(title_str + " - " + auth_str);
			        text_substances.setTextColor(Color.argb(255,10,10,10));
	        		text_title.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	image_logo.setVisibility(View.VISIBLE);			        	
		        	} else {
		        		image_logo.setVisibility(View.GONE);
		        	}
			    }
	 		}
			else if (mActionName.equals(getString(R.string.tab_name_6))) {
				// Display name and "Therapy/Kurzbeschrieb"
				TextView text_title = (TextView) mView.findViewById(R.id.mtitle);
			    TextView text_application = (TextView) mView.findViewById(R.id.mauth);	
			    
			    if (med!=null ) {    	 
			    	if (med.getTitle()!=null)
			    		title_str = med.getTitle();
			    	if (med.getApplication()!=null) {
			    		// 29/09/2013: fix for Heparin bug
			    		application_str = med.getApplication().replaceAll(";","\n");			    		
			    	}
			    	text_title.setText(title_str);			     
			        text_application.setText(application_str);
			        text_title.setTextColor(Color.argb(255,10,10,10));
	        		text_application.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	image_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		image_logo.setVisibility(View.GONE);
		        	}
			    }
			}
		 	
		 	/**
		 	 * If any of the list items is clicked, change to webview ('showview')
		 	 */
		    mView.setOnClickListener(new OnClickListener() {			    	
		    	@Override
		    	public void onClick(View v) {
		    		TextView text_title = (TextView) v.findViewById(R.id.mtitle);
		    		Toast.makeText(getApplicationContext(), 
		    				text_title.getText(),Toast.LENGTH_SHORT).show();  
		    		
		    		// Change content view
		    		// TODO: slide view in!
		    		if (mSuggestView!=null) {
		    			mSuggestView.setVisibility(View.GONE);
		    			mShowView.setVisibility(View.VISIBLE);
		    		}
										
					// If portrait
					int orientation = getResources().getConfiguration().orientation;
					if (orientation==Configuration.ORIENTATION_PORTRAIT) {
						mWebView.getSettings().setTextZoom(175);
					} else if (orientation==Configuration.ORIENTATION_LANDSCAPE) {
						mWebView.getSettings().setTextZoom(125);						
					}
		    		
					// Reset and change search hint
					if (mSearch != null) {
						mSearch.setText("");
						mSearch.setHint(getString(R.string.search) + " " + getString(R.string.full_text_search));
					}					
					
					Medication m = null;
					if (DEBUG)
						Log.d(TAG, "medi id = " + med.getId());					
					m = mMediDataSource.searchId(med.getId());
					
					if (m!=null) {
						// mHtmlString = createHtml(m.getStyle(), m.getContent());						
						mHtmlString = createHtml(mCSS_str, m.getContent());						
						
						if (mWebView!=null) {
							// Checks the orientation of the screen
							Configuration mConfig = mContext.getResources().getConfiguration();
							if (mConfig.orientation==Configuration.ORIENTATION_LANDSCAPE) {
								mWebView.getSettings().setTextZoom(125);
							} else if (mConfig.orientation==Configuration.ORIENTATION_PORTRAIT) {
								mWebView.getSettings().setTextZoom(175);
							}
						}							
						
						mWebView.loadDataWithBaseURL("app:myhtml", mHtmlString, "text/html", "utf-8", null);					
							
			    		/**
			    		 * Add section title view
			    		 */
						/*
			    		String[] id_items = m.getSectionIds().split(",");
			    		List<String> section_ids = Arrays.asList(id_items);		    		
			    		String[] title_items = m.getSectionTitles().split(";");
			    		List<String> section_titles = Arrays.asList(title_items);		    			
						
						mSectionView = (ListView) findViewById(R.id.sectionView1);
		    			mSectionView.setClickable(true);	    	
			    			
		    			SectionTitlesAdapter sectionTitles = new SectionTitlesAdapter(mContext, R.layout.section_item, 
		    					section_ids, section_titles);
		    			mSectionView.setAdapter(sectionTitles);	
		    			*/
					}
		    	}
	    	});
		
			return mView;
		}
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
	   	 	view.loadUrl(url);
	   	 	view.requestFocus();
	    	return true;
	    }
	    
	    @Override
	    public void onPageFinished(WebView view, String url) {
	    	super.onPageFinished(view, url);
	    }
	}	
	
	private String loadFromFile(String file_name, String encoding) {
		String file_str = "";
		
        try {
            InputStream is = getAssets().open(file_name); 
            InputStreamReader isr = new InputStreamReader(is, encoding);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                file_str += line;
            }
            is.close(); 
        }
        catch (Exception e) {}
        
		return file_str;			
	}
	
	private String createHtml( String style_str, String content_str ) {
		String js_str = loadFromFile("jshighlight.js", "UTF-8"); // loadJS("jshighlight.js");
		
		String html_str = "<html><head>"
				+ "<script type=\"text/javascript\">" + js_str + "</script>"				
				+ "<style type=\"text/css\">" + style_str + "</style>"
				+ "</head><body>" + content_str + "</body></html>";

		return html_str;
	}	 	
	
	private String loadReport(String file_name) {	
		String js_str = loadFromFile("jshighlight.js", "UTF-8"); // loadJS("jshighlight.js");
    
		String file_content = loadFromFile(file_name, "ISO-8859-1");

        file_content = "<html><head>"
        		+ "<script type=\"text/javascript\">" + js_str + "</script></head>"
        		+ "<body>" + file_content + "</body></html>";
        
		return file_content;
	}		
		
	private class SectionTitlesAdapter extends ArrayAdapter<String> {
		private Context mContext;
		private int id;
		private List<String> title_items;
		private List<String> id_items;

		public SectionTitlesAdapter(Context context, int textViewResourceId , List<String> s_ids, List<String> s_titles ) {
		    super(context, textViewResourceId, s_titles);   		       
		    mContext = context;
		    id = textViewResourceId;
		    id_items = s_ids;
		    title_items = s_titles;		    
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View mView = convertView;
			
			if (mView==null) {
		        LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        mView = vi.inflate(id, null);
			}
			
		    TextView text_title = (TextView) mView.findViewById(R.id.absTitle); // R.id.textView			
		    final String title = (String) title_items.get(position);
		    final String id = (String) id_items.get(position);
		    
		    if (title != null ) {    	 
		        text_title.setText(title);			     
		        text_title.setTextColor(Color.argb(255,10,10,10));
		    }
		    
		    mView.setOnClickListener(new OnClickListener() {			    	
		    	@Override
		    	public void onClick(View v) {
		    		// Log.d(TAG, "section = "+ id);	
		    		mWebView.loadUrl("app:myhtml#"+id);
		    	}
		    });
		    
			return mView;
		}		
	}
	
	/**
	 * Sets action bar tab click listeners
	 * @param actionBar
	 */
	private void setTabNavigation( ActionBar actionBar ) {
		actionBar.removeAllTabs();
		actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
		actionBar.setTitle(R.string.app_name);

		Tab tab = actionBar.newTab().setText(R.string.tab_name_1)
				.setTabListener(new MyTabListener(this, TabFragment.class.getName(), getString(R.string.tab_name_1)));
		actionBar.addTab(tab);

		tab = actionBar.newTab().setText(R.string.tab_name_2)
				.setTabListener(new MyTabListener(this, TabFragment.class.getName(), getString(R.string.tab_name_2)));
		actionBar.addTab(tab);

		tab = actionBar.newTab().setText(R.string.tab_name_3)
				.setTabListener(new MyTabListener(this, TabFragment.class.getName(), getString(R.string.tab_name_3)));
		actionBar.addTab(tab);
		
		tab = actionBar.newTab().setText(R.string.tab_name_4)
				.setTabListener(new MyTabListener(this, TabFragment.class.getName(), getString(R.string.tab_name_4)));
		actionBar.addTab(tab);
		
		tab = actionBar.newTab().setText(R.string.tab_name_5)
				.setTabListener(new MyTabListener(this, TabFragment.class.getName(), getString(R.string.tab_name_5)));
		actionBar.addTab(tab);
		
		tab = actionBar.newTab().setText(R.string.tab_name_6)
				.setTabListener(new MyTabListener(this, TabFragment.class.getName(), getString(R.string.tab_name_6)));
		actionBar.addTab(tab);		
	}	
	
	/**
	 * Web view has record of all pages visited so you can go back and forth just override 
	 * back button to go back in history if there is page available for display
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mWebView!=null ) {
			if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
				mWebView.goBack();
		        return true;
		    }
		}
		return super.onKeyDown(keyCode, event);
	}	 	
}
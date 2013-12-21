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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.animation.LayoutTransition;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
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
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
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

	private static final String TAG = "MainActivity";	
	
	// German section title abbreviations
	private static final String[] SectionTitle_DE = {"Zusammensetzung", "Galenische Form", "Kontraindikationen", 
		"Indikationen", "Dosierung/Anwendung", "Vorsichtsmassnahmen", "Interaktionen", "Schwangerschaft", 
		"Fahrtüchtigkeit", "Unerwünschte Wirk.", "Überdosierung", "Eig./Wirkung", "Kinetik", "Präklinik", 
		"Sonstige Hinweise", "Zulassungsnummer", "Packungen", "Inhaberin", "Stand der Information"};	
	// French section title abbrevations
	private static final String[] SectionTitle_FR = {"Composition", "Forme galénique", "Contre-indications", 
		"Indications", "Posologie", "Précautions", "Interactions", "Grossesse/All.", 
		"Conduite", "Effets indésir.", "Surdosage", "Propriétés/Effets", "Cinétique", "Préclinique", 
		"Remarques", "Numéro d'autorisation", "Présentation", "Titulaire", "Mise à jour"};	
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
	
	// Hashset containing registration numbers of favorite medications
	private HashSet<String> mFavoriteMedsSet = null;
	// Reference to favorites' datastore
	private DataStore mFavoriteData = null;
	// This is the currently used database
	private String mDatabaseUsed = "aips";	

	// Actionbar menu items
	private MenuItem mSearchItem = null;
	private EditText mSearch = null;
	private Button mDelete = null;	
	
	// Viewholder and views
	private ViewGroup mViewHolder = null;	
	private View mSuggestView = null;	
	private View mShowView = null;
	private View mReportView = null;	
	
	// This is the currently visible view
	private View mCurrentView = null;
	
	/**
	 * Returns the language of the app
	 * @return
	 */
	private String appLanguage() {
		if (Constants.APP_NAME.equals(Constants.AMIKO_NAME)) {
			return "de";
		} else if (Constants.APP_NAME.equals(Constants.COMED_NAME)) {
			return "fr";
		}
		return "";
	}
	
	/**
	 * Sets currently visible view
	 * @param newCurrentView
	 * @param withAnimation
	 */
    private void setCurrentView(View newCurrentView, boolean withAnimation) {
    	if (mCurrentView==newCurrentView)
    		return;
    	// It's important to perform sanity checks on views and viewgroup
    	if (mViewHolder!=null) {
    		// Set direction of transitation old view to new view
    		int direction = 1;
    		if (mCurrentView==mShowView)
    			direction = -1;
    		// Remove current view    		
    		if (mCurrentView!=null) {
	    		if (withAnimation==true) {
		    	    TranslateAnimation animate = 
		    	    		new TranslateAnimation(0, direction*mCurrentView.getWidth(), 0, 0);
		    	    animate.setDuration(500);
		    	    animate.setFillAfter(false);
		    	    mCurrentView.startAnimation(animate);
	    		}
	    		mCurrentView.setVisibility(View.GONE);    
	    	}
    		// Add new view
        	if (newCurrentView!=null) {
        		if (withAnimation==true) {
	    		    TranslateAnimation animate = 
	    		    		new TranslateAnimation(-direction*newCurrentView.getWidth(), 0, 0, 0);
	    		    animate.setDuration(500);
	    		    animate.setFillAfter(false);
	    		    newCurrentView.startAnimation(animate);
        		}
        		newCurrentView.setVisibility(View.VISIBLE);
        	} 	    	
        	// Update currently visible view
        	mCurrentView = newCurrentView;
        	
        	// This handler is used to schedule the runnable to be executed as some point in the future
            Handler handler = new Handler();
        	final Runnable r = new Runnable() {
                @Override
                public void run() {            	
                	// Remove keyboard
            		InputMethodManager imm = (InputMethodManager) getSystemService(Service.INPUT_METHOD_SERVICE);
            		if (imm!=null)
            			imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0); 
                }
            };
            // Runnable is executed in 1000ms
            handler.postDelayed(r, 1000);
    	}
    }
       
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
   			setCurrentView(mSuggestView, true);
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
   			setCurrentView(mSuggestView, true);
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
		// Retrieve reference to the activity's action bar
		ActionBar ab = getActionBar();
		// Disable activity title
		ab.setDisplayShowTitleEnabled(false);
		setTabNavigation(ab);
						
		// Sets current content view
		setContentView(R.layout.activity_main);	

		// Initialize views
		mSuggestView = getLayoutInflater().inflate(R.layout.suggest_view, null);
		mShowView = getLayoutInflater().inflate(R.layout.show_view, null);
		mReportView = getLayoutInflater().inflate(R.layout.report_view, null);
		
		// Add views to viewholder
		mViewHolder = (ViewGroup) findViewById(R.id.main_layout);		
		mViewHolder.addView(mSuggestView);		
		mViewHolder.addView(mShowView);	
		mViewHolder.addView(mReportView);
		
		LayoutTransition lt = new LayoutTransition();		
		lt.enableTransitionType(LayoutTransition.CHANGING);
		lt.setDuration(LayoutTransition.APPEARING, 500);
		lt.setDuration(LayoutTransition.DISAPPEARING, 100);
		mViewHolder.setLayoutTransition(lt);
			
		// Set visibility of views
		mSuggestView.setVisibility(View.VISIBLE);			
		mShowView.setVisibility(View.GONE);			
		mReportView.setVisibility(View.GONE);			

		// Sets initial view
		setCurrentView(mSuggestView, false);
		
		// Setup webviews
		setupReportView();
		// Load CSS from asset folder
		mCSS_str = loadFromFile("amiko_stylesheet.css", "UTF-8"); 
		// Define and load webview
		ExpertInfoView mExpertInfoView = 
				new ExpertInfoView(this, (WebView) findViewById(R.id.fach_info_view));
		mWebView = mExpertInfoView.getWebView();	
		
		// Reset action name
		mActionName = getString(R.string.tab_name_1);
		
		// Initialize suggestion listview
		mListView = (ListView) findViewById(R.id.listView1);
		mListView.setClickable(true);
		
		// Load hashset containing registration numbers from persistent data store
		mFavoriteData = new DataStore(this.getFilesDir().toString());
		mFavoriteMedsSet = new HashSet<String>();
		mFavoriteMedsSet = mFavoriteData.load();
		
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
	
	private void setupReportView() {
		mReportView.setPadding(5, 5, 5, 5);	
		mReportWebView = (WebView) mReportView.findViewById(R.id.report_view);
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
			if (Constants.DEBUG)
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
			if (Constants.DEBUG)
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
			if (Constants.DEBUG) 
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

		private List<Medication> medis = null;
		
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
					if (search_key[0].length()>=mMinCharSearch) {
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
		
		mSearch = (EditText) mSearchItem.getActionView().findViewById(R.id.search_box);
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
		
		// Action listener for search_box
		mSearch.addTextChangedListener(new TextWatcher() {		
			@Override
			public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
				// Do nothing
			}					
			
			@Override
			public void onTextChanged( CharSequence cs, int start, int before, int count ) {	
				// Do nothing
			}	

			@Override
			public void afterTextChanged(Editable s) {	
				performSearch(mSearch.getText().toString());
				mDelete.setVisibility( s.length()>0 ? View.VISIBLE : View.GONE );	    		
			}
		} );
		
		mDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSearch.setText("");
			}
		});
		
		return true;
	}
	
	void performSearch(String search_key) {
		if (search_key!="") {
			if (mCurrentView==mSuggestView) {
				long t0 = System.currentTimeMillis();
				if (mDatabaseUsed.equals("aips")) {
					mAsyncSearchTask = new AsyncSearchTask();						
					mAsyncSearchTask.execute(search_key);	
				} else if (mDatabaseUsed.equals("favorites")){
					
				}
				if (Constants.DEBUG)
					Log.d(TAG, "Time for performing search: "+Long.toString(System.currentTimeMillis()-t0)+"ms");
			} else if (mCurrentView==mShowView) {
				if (mWebView!=null) {
					if (search_key.length()>2) {
				    	mWebView.loadUrl("javascript:MyApp_HighlightAllOccurencesOfString('" + search_key + "')");									
					}
					else {
						mWebView.loadUrl("javascript:MyApp_RemoveAllHighlights()");								
					}
				}
			} else if (mCurrentView==mReportView) {
				if (search_key.length()>2) {
					mReportWebView.loadUrl("javascript:MyApp_HighlightAllOccurencesOfString('" + search_key + "')");									
				}
				else {
					mReportWebView.loadUrl("javascript:MyApp_RemoveAllHighlights()");								
				}
			}
		} else {
			if (mCurrentView==mSuggestView) {
				long t0 = System.currentTimeMillis();
				if (mDatabaseUsed.equals("aips")) {
					mAsyncSearchTask = new AsyncSearchTask();						
					mAsyncSearchTask.execute(search_key);	
				} else if (mDatabaseUsed.equals("favorites")) {
					// Clear the search container
					List<Medication> medis = new ArrayList<Medication>();
					mTimer = System.currentTimeMillis();
					for (String regnr : mFavoriteMedsSet) {
						List<Medication> meds = mMediDataSource.searchRegnr((regnr!=null ? regnr.toString() : "@@@@"));
						if (!meds.isEmpty())
							medis.add(meds.get(0));
					}
					// Sort list of meds
					Collections.sort(medis, new Comparator<Medication>() {
						@Override
						public int compare(final Medication m1, final Medication m2) {
							return m1.getTitle().compareTo(m2.getTitle());
						}
					});
					if (medis!=null)
						showResults(medis);
				}
				if (Constants.DEBUG)
					Log.d(TAG, "Time performing search: "+Long.toString(System.currentTimeMillis()-t0)+"ms");
			}
		}
	}
	
    protected boolean isAlwaysExpanded() {
        return false;
    }    
        
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case (R.id.aips_button): {
			Toast.makeText(getBaseContext(), getString(R.string.aips_button), Toast.LENGTH_SHORT).show();
			// Switch to AIPS database
			mDatabaseUsed = "aips";
			performSearch("");
			return true;
		}
		case (R.id.favorites_button): {
			Toast.makeText(getBaseContext(), getString(R.string.favorites_button), Toast.LENGTH_SHORT).show();
			// Switch to favorites database
			mDatabaseUsed = "favorites";
			performSearch("");
			return true;
		}
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
			Toast.makeText(getBaseContext(), "Error Report", Toast.LENGTH_SHORT).show();	
			// Reset and change search hint
			if (mSearch != null) {
				mSearch.setText("");
				mSearch.setHint(getString(R.string.search) + " " + getString(R.string.report_search));
			}
			// Load report from file
			String parse_report = "";
			if (appLanguage().equals("de"))
				parse_report = loadReport("amiko_report_de.html");
			else if (appLanguage().equals("fr"))
				parse_report = loadReport("amiko_report_fr.html");
			else
				parse_report = loadReport("amiko_report.html");
			mReportWebView.loadDataWithBaseURL("app:myhtml", parse_report, "text/html", "utf-8", null);

			// Display report
			setCurrentView(mReportView, true);	
			
			/*
			FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            ft.show(mReportFragment);
            ft.commit();
			*/
			return true;
		}
		case (R.id.menu_pref3): {
			Toast.makeText(getBaseContext(), "Update", Toast.LENGTH_SHORT).show();
			return true;
		}
		default:
			break;
		}

		return true;
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
		
    	if (Constants.DEBUG)
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
	 * Implements view holder design pattern
	 * @author Max
	 *
	 */
	private static class ViewHolder {
		public ImageView owner_logo;
		public TextView text_title;
		public TextView text_subtitle;
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
				
		/**
		 * Every time ListView needs to show a new row on screen, it will call getView().
		 * Its goal is to return single list row. The row is recreated each time. 
		 * There is a performance issue. Must be optimized.
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// viewHolder is instantiated only once!
			ViewHolder viewHolder;			
			// convertView is a "ScrapView" (non-visible view used for going on-screen)
		    View mView = convertView;
		    // Trick 1: if convertView is null, inflate it, otherwise only update its content!
		    if (mView==null) {
		    	LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        mView = vi.inflate(id, null);
		        // viewHolder is a static variable and is instantiated only here
		        viewHolder = new ViewHolder();
		        viewHolder.owner_logo = (ImageView) mView.findViewById(R.id.mlogo);
		        viewHolder.owner_logo.setImageResource(R.drawable.logo_desitin);
		 		viewHolder.text_title = (TextView) mView.findViewById(R.id.mtitle);
			    viewHolder.text_subtitle = (TextView) mView.findViewById(R.id.mauth);
			    // Store view
		        mView.setTag(viewHolder);
		    } else {
		    	// Recycle existing view. 
		    	// We've just avoided calling findViewById() on resource, just call viewHolder
		    	viewHolder = (ViewHolder) mView.getTag();
		    }
		     
		    final Medication med = (Medication) items.get(position);

		    // Get reference to customer logo
		    // ImageView image_logo = (ImageView) mView.findViewById(R.id.mlogo);		    
        	// viewHolder.image_logo.setImageResource(R.drawable.logo_desitin);	
		    
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
			    if (med!=null ) {    
			    	if (med.getTitle()!=null) 
			    		title_str = med.getTitle();
			    	if (med.getPackInfo()!=null)
			    		pack_info_str = med.getPackInfo();		    	
			    	viewHolder.text_title.setText(title_str);
			    	viewHolder.text_title.setTextColor(Color.argb(255,10,10,10));
			    	// text_auth.setText(pack_info_str);  // --> Original solution
			    	// text_auth.setText(Html.fromHtml(pack_info_str));	 // --> Solution with fromHtml (slow)		
			    	viewHolder.text_subtitle.setTextColor(Color.argb(255,128,128,128));	

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
			    	viewHolder.text_subtitle.setText(spannable, BufferType.SPANNABLE);
			    	
		        	if (med.getCustomerId()==1) {
			        	viewHolder.owner_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		viewHolder.owner_logo.setVisibility(View.GONE);
		        	}
			    }		 		
		 	}
		 	else if (mActionName.equals(getString(R.string.tab_name_2))) {
		 		// Display "Präparatname" and "Inhaber"		 					    
			    if (med!=null ) {    	 
			    	if (med.getTitle()!=null) 
			    		title_str = med.getTitle();
			    	if (med.getAuth()!=null) 
			    		auth_str = med.getAuth();
			    	viewHolder.text_title.setText(title_str);    
			    	viewHolder.text_subtitle.setText(auth_str);		    		
		    		viewHolder.text_title.setTextColor(Color.argb(255,10,10,10));
		    		viewHolder.text_subtitle.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	viewHolder.owner_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		viewHolder.owner_logo.setVisibility(View.GONE);
		        	}
			    }
		 	}
			else if (mActionName.equals(getString(R.string.tab_name_3))) {
				// Display name, registration number (swissmedicno5) and author			    
			    if (med!=null ) {  
			    	if (med.getTitle()!=null) 
			    		title_str = med.getTitle();
			    	if (med.getRegnrs()!=null)
			    		regnr_str = med.getRegnrs();
			    	if (med.getAuth()!=null)
			    		auth_str = med.getAuth();
			    	
			    	viewHolder.text_title.setText(title_str);			    				    	
			    	viewHolder.text_subtitle.setText(regnr_str + " - " + auth_str);			        
			        viewHolder.text_title.setTextColor(Color.argb(255,10,10,10));
			        viewHolder.text_subtitle.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	viewHolder.owner_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		viewHolder.owner_logo.setVisibility(View.GONE);
		        	}
			    }
			}
		 	else if (mActionName.equals(getString(R.string.tab_name_4))) {
		 		// Display "Präparatname" and "ATC Code" (atccode) and "Inhaber" (author)		 		
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
			    	
			    	viewHolder.text_title.setText(title_str);			     
			    	viewHolder.text_subtitle.setText(atc_code_str + " - " + atc_class_str);			    	
		        	viewHolder.text_title.setTextColor(Color.argb(255,10,10,10));
		        	viewHolder.text_subtitle.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	viewHolder.owner_logo.setVisibility(View.VISIBLE);			        	
		        	} else {
		        		viewHolder.owner_logo.setVisibility(View.GONE);
		        	}
			    }		 		
		 	}		 	
	 		else if (mActionName.equals(getString(R.string.tab_name_5))) {
	 			// Display substances, name and author			    
			    if (med!=null) {
			    	if (med.getSubstances()!=null)
			    		substances_str = med.getSubstances();
			    	if (med.getTitle()!=null)
			    		title_str = med.getTitle();
			    	if (med.getAuth()!=null)
			    		auth_str = med.getAuth();
			    	
			    	viewHolder.text_title.setText(substances_str);			     
			    	viewHolder.text_subtitle.setText(title_str + " - " + auth_str);
			        viewHolder.text_title.setTextColor(Color.argb(255,10,10,10));
			        viewHolder.text_subtitle.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	viewHolder.owner_logo.setVisibility(View.VISIBLE);			        	
		        	} else {
		        		viewHolder.owner_logo.setVisibility(View.GONE);
		        	}
			    }
	 		}
			else if (mActionName.equals(getString(R.string.tab_name_6))) {
				// Display name and "Therapy/Kurzbeschrieb"			    
			    if (med!=null ) {    	 
			    	if (med.getTitle()!=null)
			    		title_str = med.getTitle();
			    	if (med.getApplication()!=null) {
			    		// 29/09/2013: fix for Heparin bug
			    		application_str = med.getApplication().replaceAll(";","\n");			    		
			    	}
			    	viewHolder.text_title.setText(title_str);			     
			    	viewHolder.text_subtitle.setText(application_str);
			        viewHolder.text_title.setTextColor(Color.argb(255,10,10,10));
			        viewHolder.text_subtitle.setTextColor(Color.argb(255,128,128,128));		        
		        	if (med.getCustomerId()==1) {
			        	viewHolder.owner_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		viewHolder.owner_logo.setVisibility(View.GONE);
		        	}
			    }
			}
		 	
		    // Get reference to favorite's star
		    final ImageView favorite_star = (ImageView) mView.findViewById(R.id.mfavorite);
		 	// Retrieve information from hash set		 			 	
		 	if (mFavoriteMedsSet.contains(med.getRegnrs())) {
			    favorite_star.setImageResource(R.drawable.star_small_ye);
			    favorite_star.setVisibility(View.VISIBLE);		 		
		 	} else {
			    favorite_star.setImageResource(R.drawable.star_small_gy);
			    favorite_star.setVisibility(View.VISIBLE);
		 	}
		    // Make star clickable
		 	favorite_star.setOnClickListener( new OnClickListener() {
		 		@Override
		 		public void onClick(View v) {
		 			String regnrs = med.getRegnrs();
		 			// Update star
		 			if (mFavoriteMedsSet.contains(regnrs))
		 				mFavoriteMedsSet.remove(regnrs);
	 				else
	 					mFavoriteMedsSet.add(regnrs);
		 			mFavoriteData.save(mFavoriteMedsSet);
		 			// Refreshes the listview
		 			notifyDataSetChanged();
		 		}
		 	});
		 	
		    // ClickListener
		    mView.setOnClickListener( new OnClickListener() {
		    	@Override
		    	public void onClick(View v) {	
		    		// Change content view
		    		if (mSuggestView!=null) {
						setCurrentView(mShowView, true);
						// Get handle to DrawerLayout
						DrawerLayout dl = (DrawerLayout) findViewById(R.id.show_view_container);					
						// Close any open drawers
						if (dl != null)
							dl.closeDrawers();
		    		}
										
					// Adapt the zoom settings depending on the device's orientation
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
					
					if (Constants.DEBUG)
						Log.d(TAG, "medi id = " + med.getId());					
					Medication m = mMediDataSource.searchId(med.getId());
					
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
			    		String[] id_items = m.getSectionIds().split(",");
			    		List<String> section_ids = Arrays.asList(id_items);		    		
			    		String[] title_items = m.getSectionTitles().split(";");
			    		List<String> section_titles = Arrays.asList(title_items);		    			
						
			    		// Get reference to listview in DrawerLayout
						mSectionView = (ListView) findViewById(R.id.section_title_view);
						// Make it clickable
		    			mSectionView.setClickable(true);	    	
			    			
		    			SectionTitlesAdapter sectionTitles = new SectionTitlesAdapter(mContext, R.layout.section_item, 
		    					section_ids, section_titles);
		    			mSectionView.setAdapter(sectionTitles);	
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
		    String title = (String) title_items.get(position);
		    final String id = (String) id_items.get(position);
		    
		    if (title != null ) {
		    	// Use German abbreviation if possible
		    	if (appLanguage().equals("de")) {
		    		for (String s : SectionTitle_DE) {
		    			if (title.contains(s)) {
		    				title = s;
		    				break;
		    			}
		    		}
		    	} else if (appLanguage().equals("fr")) {
		    		// Use French abbreviation if possible
		    		for (String s : SectionTitle_FR) {
			    		if (title.contains(s)) {
			    			title = s;
			    			break;
			    		}
			    	}
		    	}		    			    		
		    	text_title.setText(title);			     
		        // See section_item.xml for settings!!
		        // text_title.setTextColor(Color.argb(255,240,240,240));
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
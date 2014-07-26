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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.PictureDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebView.FindListener;
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
	// Index of most recently clicked medication
	private long mMedIndex = -1;
	// Html string displayed in show_view
	private String mHtmlString;
	// Current action bar tab
	private String mActionName = "";
	// Current search query
	private String mSearchQuery = "";
	// Minimum number of characters used for SQLite query (default: min 1-chars search)
	private int mMinCharSearch = 0;	
	// Global timer used for benchmarking app
	private long mTimer = 0;
	
	// Listview of suggestions returned by SQLite query
	private ListView mListView = null;	
	// ListView of section titles (shortcuts)
	private ListView mSectionView = null;
	// Webview used to display "Fachinformation" and the "med interaction basket"
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
	// Searching for interactions?
	private boolean mSearchInteractions = false;
	// Drug interaction basket
	private Interactions mMedInteractionBasket = null;
	
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
		
	// This is the drawerlayout for the section titles in expert view
	private DrawerLayout mDrawerLayout = null;
	
	// This is the global toast object
	private CustomToast mToastObject = null;

	private BroadcastReceiver mBroadcastReceiver;
	
	// In-text-search hits counter
	private TextView mSearchHitsCntView = null;
	
	// Global flag for checking if a search is in progress
	private boolean mSearchInProgress = false;	
	// Global flag for checking of we are restoring a state
	private boolean mRestoringState = false;
	// Global flag to signal that update is in progress
	private boolean mUpdateInProgress = false;
	
	// Splash screen dialog
	private Dialog mSplashDialog = null; 
	
	/** 
	 * The download manager is a system service that handles long-running HTTP downloads. 
	 * Clients may request that a URI be downloaded to a particular destination file. 
	 * The download manager will conduct the download in the background, taking care of 
	 * HTTP interactions and retrying downloads after failures or across connectivity changes 
	 * and system reboots.
	 */
	private DownloadManager mDownloadManager = null;
	private long mDatabaseId = 0;		// SQLite DB (zipped)
	private long mReportId = 0;			// Report file (html)
	private long mInteractionsId = 0;	// Drug interactions file (zipped)
	private long mDownloadedFileCount = 0;
			
	/**
	 * Show soft keyboard
	 */
	private void showSoftKeyboard(int duration) {
		mSearch.requestFocus();		
		mSearch.postDelayed(new Runnable() {
            @Override
            public void run() {        	
            	// Display keyboard
            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            	imm.showSoftInput(mSearch, InputMethodManager.SHOW_IMPLICIT);
            }
		}, duration);
	}
	
	/**
	 * Hide soft keyboard
	 */
	private void hideSoftKeyboard(int duration) {
		mSearch.requestFocus();
		mSearch.postDelayed(new Runnable() {
            @Override
            public void run() {       	
            	// Remove keyboard
        		InputMethodManager imm = (InputMethodManager) getSystemService(Service.INPUT_METHOD_SERVICE);
        		if (imm!=null && mSearch.getWindowToken()!=null)
        			imm.hideSoftInputFromWindow(mSearch.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY); 
            }			
		}, duration);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return mMedis;
	}	
    
    /**
     * Shows the splash screen over the full Activity
     */
    protected void showSplashScreen(boolean showIt) {
    	if (showIt) {
    		mSplashDialog = new Dialog(this, android.R.style.Theme_Holo /*Translucent_NoTitleBar_Fullscreen*/);
    		mSplashDialog.setContentView(R.layout.splash_screen);
    		mSplashDialog.setCancelable(false);
    		mSplashDialog.show();
    		
    	   	// Enable flag (disable toast message)
        	mRestoringState = true;         		
    		
    		mSplashDialog.setOnDismissListener(new OnDismissListener() {
    			@Override
    			public void onDismiss(DialogInterface dialog) {
    				createMainLayout();
	    	    	// Re-enable toaster once the splash screen has been removed...
	    	    	mRestoringState = false;
	    	    	// 
	    	    	mSearch.requestFocus();	    	    	
	    	    	// Show keyboard
	    	    	showSoftKeyboard(100);	
    			}
    		}); 		

	    	// Set Runnable to remove splash screen just in case
	    	final Handler handler = new Handler();
	    	handler.postDelayed(new Runnable() {
	    		@Override
	    		public void run() {
	    	        if (mSplashDialog!=null) {
	    	            mSplashDialog.dismiss();
	    	            mSplashDialog = null;
	    	        }
	    		}
	    	}, 3000);	    	
    	}
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
    		int direction = -1;
    		if (mCurrentView==mShowView || mCurrentView==mReportView) {
    			direction = 1;
    		}
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
        	// Hide keyboard
    		if (mCurrentView==mShowView || mCurrentView==mReportView) {
    			hideSoftKeyboard(1000);
    		}
    	}
    }
       
    /**
     * Changes to "suggestView" - called from ExpertInfoView
     */
    public void setSuggestView() {
    	// Enable flag    	
    	mRestoringState = true;
    	// Set view
    	setCurrentView(mSuggestView, true);
    	// Remove search hit counter
    	mSearchHitsCntView.setVisibility(View.GONE);
    	// Old search query
    	mSearch.setText(mSearchQuery);
    	Editable s = mSearch.getText();    	
    	// Set cursor at the end
    	Selection.setSelection(s, s.length());
   		// Restore search results
    	showResults(mMedis);
    	// Restore hint
		mSearch.setHint(getString(R.string.search) + " " + mActionName);   
		// Show keyboard
		showSoftKeyboard(300);
		// Disable flag
		mRestoringState = false;
    }
    
    /**
     * Restore view state
     */
    public void resetView(boolean doSearch) {
		mRestoringState = true;
		// Set database
		mDatabaseUsed = "aips";	
		// Change view
		setCurrentView(mSuggestView, true);
		// Set tab
		getActionBar().setSelectedNavigationItem(0);
    	// Restore hint
		mActionName = getString(R.string.tab_name_1); // Präparat
		mSearch.setHint(getString(R.string.search) + " " + mActionName); 		
		// Reset search
		mSearch.setText("");
		if (doSearch==true)
			performSearch("");
		// Request menu update
		invalidateOptionsMenu();
		mRestoringState = false;
    }
    
	/**
	 * Implements listeners for action bar
	 * @author MaxL
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
				// Set hint
				mSearch.setHint(getString(R.string.search) + " " + mTabName);			
			}
			// Change content view
			if (mCurrentView==mSuggestView)
				setCurrentView(mSuggestView, true);
			else if (mCurrentView==mShowView || mCurrentView==mReportView)
				setSuggestView();
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mFragment = Fragment.instantiate(mActivity, mFragName);
			ft.add(android.R.id.content, mFragment);		
			mActionName = mTabName;
			if (mMedis!=null) {
				mTimer = System.currentTimeMillis();
				showResults(mMedis);
			}
			if (mSearch!=null) {
				// Set hint
				mSearch.setHint(getString(R.string.search) + " " + mTabName);
			}
			// Change content view
			if (mCurrentView==mSuggestView)
				setCurrentView(mSuggestView, true);
			else if (mCurrentView==mShowView || mCurrentView==mReportView)
				setSuggestView();
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {			
			ft.remove( mFragment );
			mFragment = null;
		}
	}		
		
	@TargetApi(16)
	void setLayoutTransition() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			LayoutTransition lt = new LayoutTransition();		
			lt.enableTransitionType(LayoutTransition.CHANGING);
			lt.setDuration(LayoutTransition.APPEARING, 100 /*500*/);
			lt.setDuration(LayoutTransition.DISAPPEARING, 100);
			mViewHolder.setLayoutTransition(lt);
		}
	}
	
	@TargetApi(16)
	void setFindListener(final WebView webView) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			webView.setFindListener(new FindListener() {
				@Override
				public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
					// Update hits counter
					if (isDoneCounting) {
		    			if (activeMatchOrdinal<numberOfMatches) {
		        			mSearchHitsCntView.setVisibility(View.VISIBLE);
			        		mSearchHitsCntView.setText((activeMatchOrdinal+1) + "/" + numberOfMatches);
		    			} else {			
		    				mSearchHitsCntView.setVisibility(View.GONE);
		    				webView.clearMatches();
		    			}
					}
				}
			});
		}
	}
		
	public void createMainLayout() {
		// 
		setContentView(R.layout.activity_main);
		
		// Initialize views
		mSuggestView = getLayoutInflater().inflate(R.layout.suggest_view, null);
		mShowView = getLayoutInflater().inflate(R.layout.show_view, null);
		mReportView = getLayoutInflater().inflate(R.layout.report_view, null);
		
		mCurrentView = mSuggestView;
		
		// Setup webviews
		setupReportView();
		setFindListener(mReportWebView);	
		setupGestureDetector(mReportWebView);	
		
		// Add views to viewholder
		mViewHolder = (ViewGroup) findViewById(R.id.main_layout);		
		mViewHolder.addView(mSuggestView);		
		mViewHolder.addView(mShowView);	
		mViewHolder.addView(mReportView);

		setLayoutTransition();			
		
		// Define and load webview
		ExpertInfoView mExpertInfoView = new ExpertInfoView(this, (WebView) findViewById(R.id.fach_info_view));
		mExpertInfoView.adjustZoom();
		
		mWebView = mExpertInfoView.getWebView();				
		setFindListener(mWebView);
		setupGestureDetector(mWebView);			
		
		// Set up observer to JS messages
		JSInterface jsinterface = mExpertInfoView.getJSInterface();
		jsinterface.addObserver(new Observer()  {
			@Override
			public void update(Observable o, Object arg) {
				String s = (String)arg;
				if (s.equals("notify_interaction")) {
					// Remove softkeyboard
					hideSoftKeyboard(100);
					// Take screenshot and start email activity after 500ms (wait for the keyboard to disappear)
					final Handler handler = new Handler();
				    handler.postDelayed(new Runnable() {
				        @Override
				        public void run() {					
							sendFeedbackScreenshot(MainActivity.this, 2);
				        }
				    }, 500);
				} else { 
					if (s.equals("delete_all")) {
						mMedInteractionBasket.clearBasket();
					} else {
						mMedInteractionBasket.deleteFromBasket(s);
					}
					//
					// TODO: please comment this "hack"
					//
					Handler mainHandler = new Handler(getMainLooper());
					mainHandler.post(new Runnable() {
					    @Override
					    public void run() {
							mMedInteractionBasket.updateInteractionsHtml();
							String html_str = mMedInteractionBasket.getInteractionsHtml();					    	
							mWebView.loadDataWithBaseURL("file:///android_res/drawable/", html_str, "text/html", "utf-8", null);
					    }
					});
				}
			}
		});

		// Initialize suggestion listview
		mListView = (ListView) findViewById(R.id.suggestView);
		mListView.setClickable(true);	
		
		// Set visibility of views
		mSuggestView.setVisibility(View.VISIBLE);			
		mShowView.setVisibility(View.GONE);			
		mReportView.setVisibility(View.GONE);	

		// Setup initial view
		setCurrentView(mSuggestView, false);			
	}
			
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mBroadcastReceiver);
	}
	
	@Override
	public void onResume() {
		super.onResume();
        registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}	
	
	/**
	 * Overrides onCreate method
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			AsyncLoadDBTask initDBTask = new AsyncLoadDBTask(this);						
			initDBTask.execute();		
		} catch (Exception e) {
			Log.e(TAG, "AsyncLoadDBTask exception caught!");
		}			
		
		// Load CSS from asset folder
		if (Utilities.isTablet(this))
			mCSS_str = Utilities.loadFromAssetsFolder(this, "amiko_stylesheet.css", "UTF-8"); 
		else
			mCSS_str = Utilities.loadFromAssetsFolder(this, "amiko_stylesheet_phone.css", "UTF-8");			
		
		// Flag for enabling the Action Bar on top
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		// Enable overlay mode for action bar (no good, search results disappear behind it...)
		// getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);	

		// Create action bar
		int mode = ActionBar.NAVIGATION_MODE_TABS;		
		if (savedInstanceState != null) {
			mode = savedInstanceState.getInt("mode", ActionBar.NAVIGATION_MODE_TABS);
		}			
		
		// Retrieve reference to the activity's action bar
		ActionBar ab = getActionBar();	
		// Disable activity title
		ab.setDisplayShowTitleEnabled(false);
		// Hide caret symbol ("<") upper left corner
		ab.setDisplayHomeAsUpEnabled(false);
		ab.setHomeButtonEnabled(false);
		// Sets color of action bar (including alpha-channel)
		ab.setBackgroundDrawable(new ColorDrawable(Color.argb(216,0,0,0)));

		// Sets tab navigators
		setTabNavigation(ab);		
		
		// Reset action name
		mActionName = getString(R.string.tab_name_1);
		
		// Load hashset containing registration numbers from persistent data store
		mFavoriteData = new DataStore(this.getFilesDir().toString());
		mFavoriteMedsSet = new HashSet<String>();
		mFavoriteMedsSet = mFavoriteData.load();
		
		// Init toast object
		mToastObject = new CustomToast(getApplicationContext());					
		
		// Initialize download manager
		mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
					long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
					if (downloadId==mDatabaseId || downloadId==mReportId || downloadId==mInteractionsId)
						mDownloadedFileCount++;
					// Before proceeding make sure all files have been downloaded before proceeding
					if (mDownloadedFileCount==3) {
						Query query = new Query();
						query.setFilterById(downloadId);
						Cursor c = mDownloadManager.query(query);
						if (c.moveToFirst()) {
							int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
							// Check if download was successful
							if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
								// Update database
								try {
									AsyncInitDBTask updateDBTask = new AsyncInitDBTask(MainActivity.this);						
									updateDBTask.execute();		
								} catch (Exception e) {
									Log.e(TAG, "AsyncInitDBTask-update: exception caught!");
								}			
								// Toast
								mToastObject.show("Databases downloaded successfully. Installing...", Toast.LENGTH_SHORT);
								mUpdateInProgress = false;
							}
						}
						c.close();
					}
				}
			}
		};
        registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}
		
	/**
	 * 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// super.onCreateOptionsMenu(menu);
		
		// Inflate the menu. Add items to the action bar if present.
		getMenuInflater().inflate(R.menu.actionbar, menu);

		// menu.findItem(R.id.menu_pref1).setChecked(false);

		mSearchItem = menu.findItem(R.id.menu_search);
		mSearchItem.expandActionView();
		mSearchItem.setVisible(true);				
		
		mSearch = (EditText) mSearchItem.getActionView().findViewById(R.id.search_box);		
		mSearch.setFocusable(true);
    	
		if (mSearch!=null) { 
			if (mSearchInteractions==false)
				mSearch.setHint(getString(R.string.search) + " " + getString(R.string.tab_name_1));
			else
				mSearch.setHint(getString(R.string.search) + " " + getString(R.string.interactions_search));
		}				
		
		mSearchHitsCntView = (TextView) mSearchItem.getActionView().findViewById(R.id.hits_counter);
		mSearchHitsCntView.setVisibility(View.GONE);
		
		mSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId==EditorInfo.IME_ACTION_SEARCH || actionId==EditorInfo.IME_ACTION_DONE) {
		        	if (mCurrentView==mSuggestView) {
		        		// Hide keyboard
		        		hideSoftKeyboard(500);
		        	} else if (mCurrentView==mShowView) {
		        		// Searches forward
		        		mWebView.findNext(true);
		        	} else if (mCurrentView==mReportView) {
		        		// Searches forward
		        		mReportWebView.findNext(true);
		        	}
		            return true;
		        }
		        return false;
		    }
		});
		
		mSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showSoftKeyboard(100);
				}
			}
		});
    	
		mSearch.setOnClickListener(new View.OnClickListener() {						
			@Override
			public void onClick(View v) {
				showSoftKeyboard(100);
			}
		});
		
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
				String text = mSearch.getText().toString();
				if (!mRestoringState) {
					if (text.length()>0)
						performSearch(text);
				} 
				mDelete.setVisibility( s.length()>0 ? View.VISIBLE : View.GONE );				
			}
		});

		mDelete = (Button) mSearchItem.getActionView().findViewById(R.id.delete);		
		mDelete.setVisibility( mSearch.getText().length()>0 ? View.VISIBLE : View.GONE );		
		
		mDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSearch.setText("");				
				if (mCurrentView==mShowView) {
					mSearchHitsCntView.setVisibility(View.GONE);
					mWebView.clearMatches();
				} else if (mCurrentView==mReportView) {
					mSearchHitsCntView.setVisibility(View.GONE);
					mReportWebView.clearMatches();				
				} else if (mCurrentView==mSuggestView) {
					showSoftKeyboard(300);
				}
			}
		});	
		
		return true;
	}	

	/**
	 * Gesture detector for expert-info webview
	 * @param webView
	 */
	private void setupGestureDetector(WebView webView) {
		GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {			
			@Override
			public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
				if (event1==null || event2==null)
					return false;
				if (event1.getPointerCount()>1 || event2.getPointerCount()>1)
					return false;
				else {
					try {
						// right to left swipe... return to mSuggestView						
						// float diffX = event1.getX()-event2.getX();
						// left to right swipe... return to mSuggestView
						float diffX = event2.getX()-event1.getX();
						if (diffX>120 && Math.abs(velocityX)>300) {
							setSuggestView();	
							return true;
						} 
					} catch (Exception e) {
						// Handle exceptions...
					}
					return false;
				}
			}
		};
		
		final GestureDetector detector = new GestureDetector(this, simpleOnGestureListener);
		
		webView.setOnTouchListener(new View.OnTouchListener() {			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (detector.onTouchEvent(event))
					return true;				
				hideSoftKeyboard(50);
				return false;
			}
		});			
	}
	
	private void setupReportView() {
		mReportWebView = (WebView) mReportView.findViewById(R.id.report_view);
		// Setup reportwebview
		mReportWebView.setPadding(5, 5, 5, 5);
		mReportWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);	
		mReportWebView.setScrollbarFadingEnabled(true);
		mReportWebView.setHorizontalScrollBarEnabled(false);
		mReportWebView.requestFocus(WebView.FOCUS_DOWN);		
		// Activate JavaScriptInterface
		mReportWebView.addJavascriptInterface(new JSInterface(this), "jsInterface");		
		// Enable javascript
		mReportWebView.getSettings().setJavaScriptEnabled(true);		
	}

	/**
	 * Asynchronous thread launched to initialize the SQLite database
	 * @author Max
	 */
	private class AsyncLoadDBTask extends AsyncTask<Void, Integer, Void> {
		
		public AsyncLoadDBTask(Context context) {
			// Do nothing
		}		
		
		@Override
		protected void onPreExecute() {
			// Show splashscreen while database is being initialized...
			if (!Constants.APP_NAME.equals(Constants.MEDDRUGS_NAME) 
					&& !Constants.APP_NAME.equals(Constants.MEDDRUGS_FR_NAME))
				showSplashScreen(true);
			else 
				showSplashScreen(false);
		}
				
		@Override
		protected Void doInBackground(Void... voids) {
	    	mMediDataSource = new DBAdapter(MainActivity.this);
	    	mMedInteractionBasket = new Interactions(MainActivity.this);	    	

	    	try {
	    		// Creates database
	    		mMediDataSource.create();
	    		// Opens SQLite database
	    		mMediDataSource.openSQLiteDB();	
	    	} catch( IOException e) {
	    		Log.d(TAG, "Unable to create SQLite database!");
	    		throw new Error("Unable to create SQLite database");
	    	}	 	
	   		// Open drug interactions csv file 
	    	mMedInteractionBasket.loadCsv();

			return null;
		}
							
		@Override
		protected void onPostExecute(Void result) {			
			// Do nothing
		}
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
		private int fileType = -1;
		
		public AsyncInitDBTask(Context context) {
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			if (Constants.DEBUG)
				Log.d(TAG, "onPreExecute(): progressDialog");	
	        // Initialize the dialog
			progressBar = new ProgressDialog(MainActivity.this);
	        progressBar.setMessage("Initializing SQLite database...");	       
	        // progressBar.setIndeterminate(true);
	        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        progressBar.setProgress(0);
	        progressBar.setMax(100);
	        progressBar.setCancelable(false);
	        progressBar.show();
		}
		
		@Override
		protected Void doInBackground(Void... voids) {
			if (Constants.DEBUG)
				Log.d(TAG, "doInBackground: open/overwrite database");
			
			// Case 1: mMediDataSource is not initialized 
			// Creates, opens or overwrites database (singleton)
			// TODO: there is only one database, so implement proper singleton pattern (getInstance())
			if (mMediDataSource==null) {
				mMediDataSource = new DBAdapter(this.context);
			} else {
				// Case 2: mMediDataSource is initialized
				// Database adapter already exists, reuse it!
				try {
					// First, move report file to appropriate folder
					mMediDataSource.copyReportFile();	
				}
				catch (IOException e) {
					Log.d(TAG, "Error copying report file!");
				}       	
			}
			
			try {
				// Add observer to totally unzipped bytes (for the progress bar)
				mMediDataSource.addObserver(new Observer() {
					@Override
					@SuppressWarnings({"unchecked"})
					public void update(Observable o, Object arg) {
						// Method will call onProgressUpdate(Progress...)
						fileType = ((List<Integer>)arg).get(1);
						publishProgress(((List<Integer>)arg).get(0));
					}
				});
				// Creates or overwrites database (if it already exists)
				mMediDataSource.create();
			} catch( IOException e) {
				Log.d(TAG, "Unable to create database!");
				throw new Error("Unable to create database");
			}	
			
			// Opens SQLite database
			mMediDataSource.openSQLiteDB();
	   		// Open drug interactions csv file 
	   		mMedInteractionBasket.loadCsv();
			
			return null;
		}
				
		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (progress!=null) {
				if (progress[0]<1) {
					if (fileType==1)
						progressBar.setMessage("Initializing SQLite database...");
					else if (fileType==2)
						progressBar.setMessage("Initializing drug interactions...");
				}
				int percentCompleted = progress[0];
				progressBar.setProgress(percentCompleted);
			}
		}
				
		@Override
		protected void onPostExecute(Void result) {			
			if (Constants.DEBUG) 
				Log.d(TAG, "mMediDataSource open!");			
			if (progressBar.isShowing())
				progressBar.dismiss();
			// Reset view
			resetView(true);
			// Friendly message
			mToastObject.show("Databases initialized successfully", Toast.LENGTH_LONG);
		}
	}
	
	/**
	 * Asynchronous thread launched to search in the SQLite database
	 * @author Max
	 *
	 */
	private class AsyncSearchTask extends AsyncTask<String, Void, Void> {

		private ProgressDialog progressBar;
		private List<Medication> medis = null;
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			mAsyncSearchTask.cancel(true);
			mSearchInProgress = false;
		}

		@Override
		protected void onPreExecute() {
			// Initialize progressbar
			progressBar = new ProgressDialog(MainActivity.this);       
	        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	        progressBar.setMessage("Daten werden geladen...");
	        progressBar.setIndeterminate(true);
	        if (mSearch!=null && mSearch.getText().length()<1)
	        	progressBar.show();
		}
		
		@Override
		protected Void doInBackground(String... search_key) {
			// Do the expensive work in the background here
			try {
				// Thread.sleep(1000L);
				if (!isCancelled() && !mSearchInProgress) {
					if (search_key[0].length()>=mMinCharSearch) {
						mSearchInProgress = true;
						medis = mMedis = getResults(search_key[0]);	
					}
				}
			} catch (Exception e) {
				if (progressBar.isShowing())
					progressBar.dismiss();			
			}
			return null;
		}	
		
		@Override
		protected void onPostExecute(Void r) {
			if (medis!=null) {		
				showResults(medis);
			}
			if (progressBar.isShowing())
				progressBar.dismiss();			
			mSearchInProgress = false;
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
	
	@TargetApi(16)
	void findAll(String key, WebView webView) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			webView.findAllAsync(key);
			try {
				Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);
				m.setAccessible(true);
				m.invoke(webView, true);
			} catch(Exception ignored) {
				// Exception is ignored
			}
		}
	}
	
	void performSearch(String search_key) {	
		if (!search_key.isEmpty()) {
			if (mCurrentView==mSuggestView) {
				long t0 = System.currentTimeMillis();
				// For this case, we don't need to discriminate between aips and favorites here
				// The distinction is made in the function getResults
				mAsyncSearchTask = new AsyncSearchTask();						
				mAsyncSearchTask.execute(search_key);
				mSearchQuery = search_key;
				if (Constants.DEBUG)
					Log.d(TAG, "Time for performing search: "+Long.toString(System.currentTimeMillis()-t0)+"ms");
			} else if (mCurrentView==mShowView) {
				if (mWebView!=null) {					
					// Native solution
					if (search_key.length()>2) {
						findAll(search_key, mWebView);
					} else {
						mSearchHitsCntView.setVisibility(View.GONE);
						mWebView.clearMatches();
					}
				}
			} else if (mCurrentView==mReportView) {
				if (mReportWebView!=null) {					
					// Native solution
					if (search_key.length()>2) {
						findAll(search_key, mReportWebView);
					} else {
						mSearchHitsCntView.setVisibility(View.GONE);
						mReportWebView.clearMatches();
					}
				}
			}
		} else {
			if (mCurrentView==mSuggestView) {
				long t0 = System.currentTimeMillis();
				if (mDatabaseUsed.equals("aips")) {
					// If "complete search" is already in progress, don't do anything!
					if (mSearchInProgress==false) {
						mAsyncSearchTask = new AsyncSearchTask();						
						mAsyncSearchTask.execute(search_key);	
					}
				} else if (mDatabaseUsed.equals("favorites")) {
					// Save time stamp
					mTimer = System.currentTimeMillis();
					// Clear the search container
					List<Medication> medis = new ArrayList<Medication>();
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
					if (medis!=null) {
						mMedis = medis;
						showResults(medis);
					}
				}
				if (Constants.DEBUG)
					Log.d(TAG, "Time performing search (empty search_key): " + Long.toString(System.currentTimeMillis()-t0)+"ms");
			}
		}
	}
	
	private List<Medication> getResults(String query) {
		List<Medication> medis = null;
		
		mTimer = System.currentTimeMillis();		
		
		if (mActionName.equals(getString(R.string.tab_name_1)))
	 		medis = mMediDataSource.searchTitle((query!=null ? query.toString() : "@@@@"));
	 	else if (mActionName.equals(getString(R.string.tab_name_2)))
	 		medis = mMediDataSource.searchAuth((query!=null ? query.toString() : "@@@@"));
	 	else if (mActionName.equals(getString(R.string.tab_name_3)))
	 		medis = mMediDataSource.searchATC((query!=null ? query.toString() : "@@@@")); 			
		else if (mActionName.equals(getString(R.string.tab_name_4)))	 		
	 		medis = mMediDataSource.searchRegnr((query!=null ? query.toString() : "@@@@"));			
		else if (mActionName.equals(getString(R.string.tab_name_5)))
	 		medis = mMediDataSource.searchApplication((query!=null ? query.toString() : "@@@@"));
		
		// Filter according to favorites
		if (mDatabaseUsed.equals("favorites")) {
			// This list contains all filtered medis
			List<Medication> favMedis = new ArrayList<Medication>();
			// Loop through all found medis
			for (Medication m : medis) {
				// If found medi is not in "favorites", remove it
				if (mFavoriteMedsSet.contains(m.getRegnrs()))
					favMedis.add(m); 	
			}
			// Filtered medis
			medis = favMedis;
			
			// Sort list of meds
			Collections.sort(medis, new Comparator<Medication>() {
				@Override
				public int compare(final Medication m1, final Medication m2) {
					return m1.getTitle().compareTo(m2.getTitle());
				}
			});
		}
		
		if (Constants.DEBUG)
			Log.d(TAG, "getResults() - " + medis.size() + " medis found in " + Long.toString(System.currentTimeMillis()-mTimer) + "ms");	   	
		
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
	   		/*
	   		if (!mRestoringState) {
	   			if (Constants.appLanguage().equals("de")) {
			   		mToastObject.show(medis.size() + " Suchresultate in " + (System.currentTimeMillis()-mTimer) + "ms", 
			   				Toast.LENGTH_SHORT);
	   			} else if (Constants.appLanguage().equals("fr")) {
	   				mToastObject.show(medis.size() + " résultats de la recherche en " + (System.currentTimeMillis()-mTimer) + "ms", 
			   				Toast.LENGTH_SHORT);
	   			}
	   		}
	   		*/
	   	}
	}	
	
    protected boolean isAlwaysExpanded() {
        return false;
    }    
       
    /**
     * Called through onOptionsItemSelected's invalidateMenuOptions function
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem aipsButton = menu.findItem(R.id.aips_button);
    	MenuItem favoritesButton = menu.findItem(R.id.favorites_button);
    	MenuItem interactionsButton = menu.findItem(R.id.interactions_button);
    	if (mSearchInteractions==false) {
       		interactionsButton.setIcon(R.drawable.interactions_icon_gy);    		
    		if (mDatabaseUsed.equals("aips")) {
    			aipsButton.setIcon(R.drawable.aips_icon_wh);
    			favoritesButton.setIcon(R.drawable.favorites_icon_gy);
    		} else if (mDatabaseUsed.equals("favorites")) {
    			aipsButton.setIcon(R.drawable.aips_icon_gy);
    			favoritesButton.setIcon(R.drawable.favorites_icon_wh);
    		}
    	} else {
    		aipsButton.setIcon(R.drawable.aips_icon_gy);
    		favoritesButton.setIcon(R.drawable.favorites_icon_gy);
    		interactionsButton.setIcon(R.drawable.interactions_icon_wh);
    	}
    	return super.onPrepareOptionsMenu(menu);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {		
		switch (item.getItemId()) {
		case (R.id.menu_search): {
			showSoftKeyboard(300);
			return true;
		}
		case (R.id.aips_button): {
			mToastObject.show(getString(R.string.aips_button), Toast.LENGTH_SHORT);
			if (mSearchInteractions==true || mDatabaseUsed.equals("favorites")) {
				// Switch to AIPS database				
				mDatabaseUsed = "aips";	
				mSearchInteractions = false;
				// Change view
				setCurrentView(mSuggestView, true);
			} else {
				// Switch to AIPS database				
				mDatabaseUsed = "aips";	
				mSearchInteractions = false;				
				// We are already in AIPS mode
				mSearch.setText("");
				performSearch("");
			}
			// Request menu update
			invalidateOptionsMenu();
			return true;
		}
		case (R.id.favorites_button): {
			mToastObject.show(getString(R.string.favorites_button), Toast.LENGTH_SHORT);
			// Switch to favorites database
			mDatabaseUsed = "favorites";
			mSearchInteractions = false;
			// Change view
			setCurrentView(mSuggestView, true);
			// Reset search
			mSearch.setText("");
			performSearch("");
			// Request menu update
			invalidateOptionsMenu();
			return true;
		}
		case (R.id.interactions_button): {
			mToastObject.show(getString(R.string.interactions_button), Toast.LENGTH_SHORT);
			// Keep used database
			mSearchInteractions = true;
			// Update interaction basket
			updateInteractionBasket();
			// Reset and change search hint
			if (mSearch!=null) {
				mSearch.setText("");
				mSearch.setHint(getString(R.string.search) + " " + getString(R.string.interactions_search));
			}			
			// Update webview		
			String html_str = mMedInteractionBasket.getInteractionsHtml();			
			mWebView.loadDataWithBaseURL("file:///android_res/drawable/", html_str, "text/html", "utf-8", null);
			// Change view
			setCurrentView(mShowView, true);				
			// Request menu update
			invalidateOptionsMenu();
			return true;
		}
		case (R.id.menu_pref2): {
			mToastObject.show(getString(R.string.menu_pref2), Toast.LENGTH_SHORT);	
			// Enable restoring state flag
			mRestoringState = true;
			// Reset and change search hint
			if (mSearch != null) {
				mSearch.setText("");
				mSearch.setHint(getString(R.string.search) + " " + getString(R.string.report_search));
			}
			// Load report from file
			String parse_report = loadReport(Constants.appReportFile());
			mReportWebView.loadDataWithBaseURL("file:///android_res/drawable/", parse_report, "text/html", "utf-8", null);
			// Display report
			setCurrentView(mReportView, true);	
			// Disable restoring state
			mRestoringState = false;
			return true;
		}
		case (R.id.menu_pref3): {
			mToastObject.show(getString(R.string.menu_pref3), Toast.LENGTH_SHORT);
			// Download new database (blocking call)
			if (!mUpdateInProgress)
				downloadUpdates();
			return true;
		}
		case (R.id.menu_share): {
			// Remove softkeyboard
			hideSoftKeyboard(10);
			// Take screenshot and start email activity after 500ms (wait for the keyboard to disappear)
			final Handler handler = new Handler();
		    handler.postDelayed(new Runnable() {
		        @Override
		        public void run() {					
					mToastObject.show(getString(R.string.menu_share), Toast.LENGTH_SHORT);
					sendFeedbackScreenshot(MainActivity.this, 1);
		        }
		    }, 500);			
			return true;
		}
		case (R.id.menu_rate): {
			Intent intent = new Intent(Intent.ACTION_VIEW);			
		    // Try Google play
		    intent.setData(Uri.parse("market://details?id=com.ywesee.amiko.de"));
		    if (myStartActivity(intent)==false) {
		        // Market (Google play) app seems not installed, let's try to open a webbrowser
		        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.ywesee.amiko.de"));
		        if (myStartActivity(intent)==false) {
		            // Well if this also fails, we have run out of options, inform the user.
		            mToastObject.show("Could not open Android market, please install the market app.", Toast.LENGTH_SHORT);
		        }
		    }
			return true;
		}
		case (R.id.menu_help): {
			mToastObject.show(getString(R.string.menu_help), Toast.LENGTH_SHORT);
			if (Constants.appOwner().equals("ywesee")) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ywesee.com/AmiKo/Index"));
				startActivity(browserIntent);
			} else if (Constants.appOwner().equals("meddrugs")) {
				/*
				 * MEDDRUGS
				 */
				if (Constants.appLanguage().equals("de")) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.just-medical.com/shop/index.cfm?fuseaction=openShopOverview&catID=4&language=1&lang=1"));
					startActivity(browserIntent);
				} else if (Constants.appLanguage().equals("fr")) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.just-medical.com/shop/index.cfm?fuseaction=openShopOverview&catID=4&language=2&lang=2"));
					startActivity(browserIntent);
				}
			}
			return true;
		}		
		default:
			break;
		}

		return true;
	}    

	/**
	 * 
	 */
	public void updateInteractionBasket() {
		// Add medi to drug interaction basket
		if (mMedIndex>0) {
			Medication m = mMediDataSource.searchId(mMedIndex);
			mMedInteractionBasket.addToBasket(m.getTitle(), m);
		}
		// Update it
		mMedInteractionBasket.updateInteractionsHtml();		
		// Get all section titles
		mMedInteractionBasket.getInteractionsTitles();				
   		// Add section title view
   		List<String> section_ids = mMedInteractionBasket.getInteractionTitleIds();
   		List<String> section_titles = mMedInteractionBasket.getInteractionsTitles();		    							
   		// Get reference to listview in DrawerLayout
   		// Implements swiping mechanism!
		mSectionView = (ListView) findViewById(R.id.section_title_view);
		// Make it clickable
		mSectionView.setClickable(true);	    			
		SectionTitlesAdapter sectionTitles = 
				new SectionTitlesAdapter(this, R.layout.section_item, section_ids, section_titles);
		mSectionView.setAdapter(sectionTitles);				
	}
	
	/**
	 * Takes screenshot of the whole screen
	 * @param activity
	 */
	public void sendFeedbackScreenshot(final Activity activity, int mode) {
		try {			
			// Get root windows
			final View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content).getRootView();
			rootView.setDrawingCacheEnabled(true);
			Bitmap bitmap = rootView.getDrawingCache();
			// The file be saved to the download folder
			File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/amiko_screenshot.png");
			FileOutputStream fOutStream = new FileOutputStream(outputFile);
			// Picture is then compressed
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOutStream);
			rootView.setDrawingCacheEnabled(false);
			fOutStream.close();
			// Start email activity
			if (mode==1)
				startEmailActivity(activity, Uri.fromFile(outputFile), "", "AmiKo for Android");
			else if (mode==2)
				startEmailActivity(activity, Uri.fromFile(outputFile), "zdavatz@ywesee.com", "Interaction notification");			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Starts email activity
	 * @param context
	 * @param attachment
	 */
	public void startEmailActivity(Context context, Uri attachment, String emailAddr, String subject) {
		Intent sendEmailIntent = new Intent(Intent.ACTION_SEND);
		sendEmailIntent.setType("message/rfc822");
		sendEmailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddr});
		sendEmailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		sendEmailIntent.putExtra(Intent.EXTRA_TEXT, "AmiKo for Android\r\n\nGet it now: https://play.google.com/store/apps/details?id=com.ywesee.amiko.de\r\n\nEnjoy!");
		sendEmailIntent.putExtra(Intent.EXTRA_STREAM, attachment);
		context.startActivity(Intent.createChooser(sendEmailIntent, "Send email"));
	}

	private boolean myStartActivity(Intent aIntent) {
	    try {
	        startActivity(aIntent);
	        return true;
	    } catch (ActivityNotFoundException e) {
	        return false;
	    }
	}
	
	/**
	 * Converts given picture to a bitmap
	 * @param picture
	 * @return
	 */
	private static Bitmap pictureDrawable2Bitmap(Picture picture){
		PictureDrawable pictureDrawable = new PictureDrawable(picture);
	    Bitmap bitmap = Bitmap.createBitmap(pictureDrawable.getIntrinsicWidth(),pictureDrawable.getIntrinsicHeight(), Config.ARGB_8888);
	    Canvas canvas = new Canvas(bitmap);
	    canvas.drawPicture(pictureDrawable.getPicture());
	    return bitmap;
	}
	
	/**
	 * Downloads and updates the SQLite database and the error report file
	 */
	public void downloadUpdates() {
		// Signal that update is in progress
		mUpdateInProgress = true;
		mDownloadedFileCount = 0;
		
		// First check network connection
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		
		// Fetch data...
		if (networkInfo!=null && networkInfo.isConnected()) {
			// File URIs
			Uri databaseUri = Uri.parse("http://pillbox.oddb.org/" + Constants.appZippedDatabase());
			Uri reportUri = Uri.parse("http://pillbox.oddb.org/" + Constants.appReportFile());
			Uri interactionsUri = Uri.parse("http://pillbox.oddb.org/" + Constants.appZippedInteractionsFile());
			
			// NOTE: the default download destination is a shared volume where the system might delete your file if 
			// it needs to reclaim space for system use
			DownloadManager.Request requestDatabase = new DownloadManager.Request(databaseUri);
			DownloadManager.Request requestReport = new DownloadManager.Request(reportUri);
			DownloadManager.Request requestInteractions = new DownloadManager.Request(interactionsUri);
			// Allow download only over WIFI and Mobile network
			requestDatabase.setAllowedNetworkTypes(Request.NETWORK_WIFI|Request.NETWORK_MOBILE);
			requestReport.setAllowedNetworkTypes(Request.NETWORK_WIFI|Request.NETWORK_MOBILE);
			requestInteractions.setAllowedNetworkTypes(Request.NETWORK_WIFI|Request.NETWORK_MOBILE);
			// Download visible and shows in notifications while in progress and after completion
			requestDatabase.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			requestReport.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			requestInteractions.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			// Set the title of this download, to be displayed in notifications (if enabled).
			requestDatabase.setTitle("AmiKo SQLite database update");
			requestReport.setTitle("AmiKo report update");
			requestInteractions.setTitle("AmiKo drug interactions update");
			// Set a description of this download, to be displayed in notifications (if enabled)
			requestDatabase.setDescription("Updating the AmiKo database.");
			requestReport.setDescription("Updating the AmiKo error report.");
			requestInteractions.setDescription("Updating the AmiKo drug interactions.");
			// Set local destination to standard directory (place where files downloaded by the user are placed)
			requestDatabase.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Constants.appZippedDatabase());
			requestReport.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Constants.appReportFile());
			requestInteractions.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Constants.appZippedInteractionsFile());	        
			// Check if file exist, if yes, delete it 
			deleteDownloadedFile(Constants.appZippedDatabase());
			deleteDownloadedFile(Constants.appReportFile());
			deleteDownloadedFile(Constants.appZippedInteractionsFile());
			
			// The downloadId is unique across the system. It is used to make future calls related to this download.
			mDatabaseId = mDownloadManager.enqueue(requestDatabase);
			mReportId = mDownloadManager.enqueue(requestReport);
			mInteractionsId = mDownloadManager.enqueue(requestInteractions);
		} else {
			// Display error report
		}
	}
		
	/**
	 * Checks if file exists and deletes
	 */
	private boolean deleteDownloadedFile(String fileName) {
		File filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		File file = new File(filePath, fileName);
		if (file.exists()) {
			boolean ret = file.delete();
			Log.d(TAG, "Downloaded file found and deleted. Return code = " + ret);
			return ret;
		} else {
			Log.d(TAG, "File " + filePath + "/" + fileName + " does not exists. No need to delete.");
		}
		return false;
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

    	String title_str = "k.A.";		 // tab_name_1 = Präparat
    	String auth_str = "k.A.";		 // tab_name_2 = Inhaber
    	String atc_code_str = "k.A.";	 // tab_name_3 = Wirkstoff / ATC Code 
    	String atc_title_str = "k.A.";
    	String atc_class_str = "k.A.";	 //			   = ATC Klasse	
    	String regnr_str = "k.A.";		 // tab_name_4 = Reg.Nr.
    	String application_str = "k.A."; // tab_name_5 = Therapie / Indications
    	String pack_info_str = "";		
		
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
		    	// Inflations and findViewById are expensive operations...
		    	LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        mView = vi.inflate(id, null);
		        // viewHolder is a static variable and is instantiated only here
		        viewHolder = new ViewHolder();
		        viewHolder.owner_logo = (ImageView) mView.findViewById(R.id.mlogo);
		        viewHolder.owner_logo.setImageResource(R.drawable.logo_desitin);
		 		viewHolder.text_title = (TextView) mView.findViewById(R.id.mtitle);
			    viewHolder.text_subtitle = (TextView) mView.findViewById(R.id.mauth);
			    // Set text sizes
			    if (!Utilities.isTablet(MainActivity.this)) {
				    viewHolder.text_title.setTextSize(14);
				    viewHolder.text_subtitle.setTextSize(12);
			    }
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
		    
		    if (Constants.appLanguage().equals("de")) {
		    	title_str = "k.A.";		 
		    	auth_str = "k.A.";		 
		    	regnr_str = "k.A.";		 
		    	atc_code_str = "k.A.";
		    	atc_title_str = "k.A.";
		    	atc_class_str = "k.A.";	 
		    	application_str = "k.A."; 
		    } else if (Constants.appLanguage().equals("fr")) {
            	title_str = "n.s.";			
            	auth_str = "n.s.";			
            	regnr_str = "n.s.";			
            	atc_code_str = "n.s.";
		    	atc_title_str = "n.s.";            	
            	atc_class_str = "n.s.";		
            	application_str = "n.s.";	 
        	}
	    	pack_info_str = "";
	    	
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
			    	if (spannable!=null) {
				    	while (m_red.find()) {   		
				    		spannable.setSpan(new ForegroundColorSpan(Color.rgb(205,0,0)), m_red.start(), m_red.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);	    		
				    	}			    	
				    	while (m_green.find()) {
				    		spannable.setSpan(new ForegroundColorSpan(Color.rgb(50,188,50)), m_green.start(), m_green.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				    	}
				    	viewHolder.text_subtitle.setText(spannable, BufferType.SPANNABLE);
			    	}
			    	
		        	if (med.getCustomerId()==1) {
			        	viewHolder.owner_logo.setVisibility(View.VISIBLE);
		        	} else {
		        		viewHolder.owner_logo.setVisibility(View.GONE);
		        	}
			    }		 		
		 	} else if (mActionName.equals(getString(R.string.tab_name_2))) {
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
		 	} else if (mActionName.equals(getString(R.string.tab_name_3))) {
		 		// Display "Wirkstoff" and "ATC Code" (atccode)		 		
			    if (med!=null) {   
			    	if (med.getTitle()!=null)
			    		title_str = med.getTitle();
				    List<String> m_atc = Arrays.asList(med.getAtcCode().split("\\s*;\\s*"));			    				        
			    	if (m_atc.size()>1) {
			    		if (m_atc.get(0)!=null)
			    			atc_code_str = m_atc.get(0);
			    		if (m_atc.get(1)!=null)
			    			atc_title_str = m_atc.get(1);
			    	}
			    	String[] m_class = med.getAtcClass().split(";");
			    	if (m_class.length==2) {			// *** Ver.<1.2
			    		atc_class_str = m_class[1];
				    	viewHolder.text_title.setText(title_str);			     
				    	viewHolder.text_subtitle.setText(atc_code_str + " - " + atc_title_str + "\n" + atc_class_str);			    	
			        	viewHolder.text_title.setTextColor(Color.argb(255,10,10,10));
			        	viewHolder.text_subtitle.setTextColor(Color.argb(255,128,128,128));	
			    	} else if (m_class.length==3) {		// *** Ver.>=1.2
						String[] atc_class_l4_and_l5 = m_class[2].split("#");
						int n = atc_class_l4_and_l5.length;
						if (n>0)
							atc_class_str = atc_class_l4_and_l5[n-1];
				    	viewHolder.text_title.setText(title_str);			     
				    	viewHolder.text_subtitle.setText(atc_code_str + " - " + atc_title_str + "\n" + atc_class_str + "\n" + m_class[1]);			    	
			        	viewHolder.text_title.setTextColor(Color.argb(255,10,10,10));
			        	viewHolder.text_subtitle.setTextColor(Color.argb(255,128,128,128));							
			    	}	        
		        	if (med.getCustomerId()==1) {
			        	viewHolder.owner_logo.setVisibility(View.VISIBLE);			        	
		        	} else {
		        		viewHolder.owner_logo.setVisibility(View.GONE);
		        	}
			    }		 		
		 	} else if (mActionName.equals(getString(R.string.tab_name_4))) {
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
			} else if (mActionName.equals(getString(R.string.tab_name_5))) {
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
		    		}
										
					// Adapt the zoom settings depending on the device's orientation
					int orientation = getResources().getConfiguration().orientation;
					if (orientation==Configuration.ORIENTATION_PORTRAIT) {
						mWebView.getSettings().setTextZoom(175);
					} else if (orientation==Configuration.ORIENTATION_LANDSCAPE) {
						mWebView.getSettings().setTextZoom(125);						
					}
		    											
					mMedIndex = med.getId();
					if (Constants.DEBUG)
						Log.d(TAG, "medi id = " + mMedIndex);					
					Medication m = mMediDataSource.searchId(mMedIndex);
					
					if (m!=null && mSearchInteractions==false) {
						// Reset and change search hint
						if (mSearch!=null) {
							mSearch.setText("");
							mSearch.setHint(getString(R.string.search) + " " + getString(R.string.full_text_search));
						}							
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
						
						mWebView.loadDataWithBaseURL("file:///android_res/drawable/", mHtmlString, "text/html", "utf-8", null);					
							
						// Add NavigationDrawer, get handle to DrawerLayout
						mDrawerLayout = (DrawerLayout) findViewById(R.id.show_view_container);
					    // Set a custom shadow that overlays the main content when the drawer opens
					    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);					 
					    // Close any open drawers
						if (mDrawerLayout != null)
							mDrawerLayout.closeDrawers();

			    		// Add section title view
			    		String[] id_items = m.getSectionIds().split(",");
			    		List<String> section_ids = Arrays.asList(id_items);		    		
			    		String[] title_items = m.getSectionTitles().split(";");
			    		List<String> section_titles = Arrays.asList(title_items);		    			
						
			    		// Get reference to listview in DrawerLayout
			    		// Implements swiping mechanism!
						mSectionView = (ListView) findViewById(R.id.section_title_view);
						// Make it clickable
		    			mSectionView.setClickable(true);
			    			
		    			SectionTitlesAdapter sectionTitles = 
		    					new SectionTitlesAdapter(mContext, R.layout.section_item, section_ids, section_titles);
		    			mSectionView.setAdapter(sectionTitles);	
					} else {						
						// Update interaction basket
						updateInteractionBasket();
						// Reset and change search hint
						if (mSearch!=null) {
							mSearch.setText("");
							mSearch.setHint(getString(R.string.search) + " " + getString(R.string.interactions_search));
						}			
						// Update webview
						String html_str = mMedInteractionBasket.getInteractionsHtml();			
						mWebView.loadDataWithBaseURL("file:///android_res/drawable/", html_str, "text/html", "utf-8", null);
						// Change view
						setCurrentView(mShowView, true);				
					}
		    	}
	    	});	
		 	
		    // Long click listener
		    mView.setOnLongClickListener(new OnLongClickListener() {
		    	@Override
		    	public boolean onLongClick(View v) {
		    		if (mActionName.equals(getString(R.string.tab_name_3))) {			    				
			    		Medication m = mMediDataSource.searchId(med.getId());
			    		String[] atc_items = m.getAtcCode().split(";");
			    		if (atc_items!=null) {
			    			mSearch.setText(atc_items[0]);
			    			Editable s = mSearch.getText();    	
			    			// Set cursor at the end
			    			Selection.setSelection(s, s.length());
			    		}
		    		}
		    		return true;
		    	}
		    });
		    
			return mView;
		}
	}
	
	private String loadReport(String file_name) {	
		// Old Javascript-based solution... superseeded -> maybe useful for older version of android!
		/*		
		String js_str = loadFromAssetsFolder("jshighlight.js", "UTF-8"); // loadJS("jshighlight.js");    
        file_content = "<html><head>"
        		+ "<script type=\"text/javascript\">" + js_str + "</script></head>"
        		+ "<body>" + file_content + "</body></html>";
        */
		// New solution without Javascript...
		String file_content = Utilities.loadFromApplicationFolder(MainActivity.this, file_name, "UTF-8");		
        file_content = "<html><body>" + file_content + "</body></html>";
        
        return file_content;
	}	
		
	private String createHtml( String style_str, String content_str ) {
		// Old Javascript-based solution... superseeded -> maybe useful for older version of android!
		/*		
		String js_str = loadFromAssetsFolder("jshighlight.js", "UTF-8"); // loadJS("jshighlight.js");		
		String html_str = "<html><head>"
				+ "<script type=\"text/javascript\">" + js_str + "</script>"				
				+ "<style type=\"text/css\">" + style_str + "</style>"
				+ "</head><body>" + content_str + "</body></html>";
		*/
		
		String html_str = "<html><head>"			
				+ "<style type=\"text/css\">" + style_str + "</style>"
				+ "</head><body>" + content_str + "</body></html>";
		
		return html_str;
	}	 		
	
	/**
	 * This class implements the adapter for the section titles 
	 * @author Max
	 *
	 */
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
		    if (!Utilities.isTablet(MainActivity.this)) {
		    	text_title.setTextSize(12);	// in "scaled pixel units" (sp)
		    } else {
		    	// Defined in "section_item.xml" as 14sp
		    }
		    String title = (String) title_items.get(position);
		    final String id = (String) id_items.get(position);
		    
		    if (title != null ) {
		    	// Use German abbreviation if possible
		    	if (Constants.appLanguage().equals("de")) {
		    		Locale german = Locale.GERMAN;
		    		for (String s : SectionTitle_DE) {
		    			if (title.toLowerCase(german).contains(s.toLowerCase(german))) {
		    				title = s;
		    				break;
		    			}
		    		}
		    	} else if (Constants.appLanguage().equals("fr")) {
		    		// Use French abbreviation if possible
		    		Locale french = Locale.FRENCH;
		    		for (String s : SectionTitle_FR) {
			    		if (title.toLowerCase(french).contains(s.toLowerCase(french))) {
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
		    		// 16.07.2014: It's weird, but it works...
		    		String url_str = "file:///android_res/drawable/#" + id;
		    		// Log.d(TAG, url_str);	
		    		mWebView.loadUrl(url_str);
		    		// Close section title view
					if (mDrawerLayout != null)
						mDrawerLayout.closeDrawers();
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

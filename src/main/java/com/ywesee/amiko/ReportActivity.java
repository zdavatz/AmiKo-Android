package com.ywesee.amiko;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Method;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ReportActivity extends AppCompatActivity {
    // Webview used to display the report (About-File)
    private WebView mWebView;
    private MenuItem mSearchItem = null;
    private EditText mSearch = null;
    private Button mDelete = null;
    // In-text-search hits counter
    private TextView mSearchHitsCntView = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_view);

        mWebView = findViewById(R.id.report_view);

        setFindListener(mWebView);
        setupGestureDetector(mWebView);

        // Setup reportwebview
        mWebView.setPadding(5, 5, 5, 5);
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(true);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.requestFocus(WebView.FOCUS_DOWN);
        // Activate JavaScriptInterface
        mWebView.addJavascriptInterface(new JSInterface(this), "jsInterface");
        // Enable javascript
        mWebView.getSettings().setJavaScriptEnabled(true);

        String parse_report = loadReport(Constants.appReportFile());
        mWebView.loadDataWithBaseURL("file:///android_res/drawable/", parse_report, "text/html", "utf-8", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the menu. Add items to the action bar if present.
        getMenuInflater().inflate(R.menu.report_actionbar, menu);

        mSearchItem = menu.findItem(R.id.menu_search);
        mSearchItem.expandActionView();
        mSearchItem.setVisible(true);
        mSearch = (EditText) mSearchItem.getActionView().findViewById(R.id.search_box);
        if (!Utilities.isTablet(this)) {
            float textSize = 16.0f; // in [sp] = scaled pixels
            mSearch.setTextSize(textSize);
        }
        mSearch.setFocusable(true);
        mSearch.setHint(getString(R.string.search) + " " + getString(R.string.report_search));

        mSearchHitsCntView = (TextView) mSearchItem.getActionView().findViewById(R.id.hits_counter);
        mSearchHitsCntView.setVisibility(View.GONE);

        mSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==EditorInfo.IME_ACTION_SEARCH || actionId==EditorInfo.IME_ACTION_DONE) {
                    mWebView.findNext(true);
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
                if (text.length()>0) {
                    performSearch(text);
                }
                mDelete.setVisibility( s.length()>0 ? View.VISIBLE : View.GONE );
            }
        });

        mDelete = (Button) mSearchItem.getActionView().findViewById(R.id.delete);
        mDelete.setVisibility( mSearch.getText().length()>0 ? View.VISIBLE : View.GONE );

        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearch.getText().clear();
                mSearchHitsCntView.setVisibility(View.GONE);
            }
        });

        return true;
    }

    private String loadReport(String file_name) {
        String file_content = Utilities.loadFromApplicationFolder(ReportActivity.this, file_name, "UTF-8");
        file_content = "<html><body>" + file_content + "</body></html>";

        return file_content;
    }

    // Copied from MainActivity, TODO: generialise
    void setFindListener(final WebView webView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.setFindListener(new WebView.FindListener() {
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
                            finish();
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

    void performSearch(String searchKey){
        // Native solution
        if (searchKey.length()>2) {
            findAll(searchKey, mWebView);
        } else {
            mSearchHitsCntView.setVisibility(View.GONE);
            mWebView.clearMatches();
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
}

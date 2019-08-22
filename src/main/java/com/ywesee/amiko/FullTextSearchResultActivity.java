/*
Copyright (c) 2019 Brian Chan

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
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FullTextSearchResultActivity extends AppCompatActivity {
    // When opening this activity, the caller should specify this shared receiver,
    // because the data is usually too big to pass via Intent.
    public static Receiver receiver;

    private WebView webView;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private SectionListAdapter recyclerAdapter;
    private FullTextSearch fullTextSearchManager;
    private GestureDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_text_search_result);
        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.webView = (WebView) findViewById(R.id.webview);
        this.recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        this.recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));

        fullTextSearchManager = new FullTextSearch();

        this.webView.addJavascriptInterface(new FTJSInterface(new FTJSInterface.Callback() {
            public void run(final String regnr, final String anchor) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent data = new Intent();
                        data.putExtra("regnr", regnr);
                        data.putExtra("anchor", anchor);
                        setResult(0, data);
                        finish();
                    }
                });
            }
        }), "jsInterface");
        WebSettings wsettings = this.webView.getSettings();
        wsettings.setLoadWithOverviewMode(true);
        wsettings.setUseWideViewPort(true);
        wsettings.setBuiltInZoomControls(true);
        wsettings.setDisplayZoomControls(false);
        wsettings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
        wsettings.setJavaScriptEnabled(true);
        wsettings.setLoadsImagesAutomatically(true);

        // specify an adapter (see also next example)
        recyclerAdapter = new SectionListAdapter(new ArrayList<String>());
        final Context context = this;
        recyclerAdapter.onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = recyclerView.getChildLayoutPosition(v);
                String title = fullTextSearchManager.listOfSectionIds.get(itemPosition);
                updateWebViewWithFilter(title);
                drawerLayout.closeDrawers();
            }
        };
        recyclerView.setAdapter(recyclerAdapter);
        setupGestureDetector();

        if (receiver != null) {
            FullTextDBAdapter.Entry entry = receiver.entry;
            List<Medication> listOfArticles = receiver.listOfArticles;
            HashMap<String, ArrayList<String>> dict = entry.regChaptersDict;
            fullTextSearchManager.mListOfArticles = new ArrayList<>(listOfArticles);
            fullTextSearchManager.mDict = dict;

            updateWebViewWithFilter("");
        }
    }

    private void updateWebViewWithFilter(String filter) {
        final String fullTextContentStr = fullTextSearchManager.table(null, null, filter);

        String cssStr = Utilities.isTablet(this)
            ? Utilities.loadFromAssetsFolder(this, "amiko_stylesheet.css", "UTF-8")
            : Utilities.loadFromAssetsFolder(this, "amiko_stylesheet_phone.css", "UTF-8");

        String htmlString = createHtml(cssStr, fullTextContentStr);
        this.webView.loadDataWithBaseURL("file:///android_res/drawable/", htmlString, "text/html", "utf-8", null);

        recyclerAdapter.mDataset = fullTextSearchManager.listOfSectionTitles;
        recyclerAdapter.notifyDataSetChanged();
    }

    private String createHtml( String style_str, String content_str ) {
        String html_str = "<html><head>";

        html_str += "<meta name=\"viewport\" content=\"width=device-width\">";
        content_str = "<div id=\"fulltext\">" + content_str + "</div>";

        html_str +=
                "<style type=\"text/css\">" + style_str + "</style>"
                + "</head><body>" + content_str + "</body></html>";

        return html_str;
    }

    private void setupGestureDetector() {
        GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                if (event1==null || event2==null) {
                    return false;
                }
                if (event1.getPointerCount()>1 || event2.getPointerCount()>1) {
                    return false;
                } else {
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

        detector = new GestureDetector(this, simpleOnGestureListener);

        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (detector.onTouchEvent(event))
                    return true;
                return false;
            }
        });
    }

    public static class Receiver {
        FullTextDBAdapter.Entry entry;
        ArrayList<Medication> listOfArticles;

        public Receiver(FullTextDBAdapter.Entry entry, ArrayList<Medication> listOfArticles) {
            this.entry = entry;
            this.listOfArticles = listOfArticles;
        }
    }
}

class SectionListAdapter extends RecyclerView.Adapter<SectionListAdapter.ViewHolder> {
    public List<String> mDataset;
    public View.OnClickListener onClickListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SectionListAdapter(List<String> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SectionListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // create a new view
        TextView v = new TextView(parent.getContext());
        v.setTextSize(18);
        v.setOnClickListener(onClickListener);
        v.setWidth(parent.getWidth());
        v.setPadding(50, 30, 0, 30);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String p = mDataset.get(position);
        holder.mTextView.setText(p);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mDataset == null) return 0;
        return mDataset.size();
    }
}

class FTJSInterface {
    Callback callback;

    FTJSInterface(Callback cb) {
        this.callback = cb;
    }

    public interface Callback {
        public void run(String regnr, String anchor);
    }

    @JavascriptInterface
    public void navigationToFachInfo(String regnr, String anchor) {
        if (this.callback == null) return;
        this.callback.run(regnr, anchor);
    }
}

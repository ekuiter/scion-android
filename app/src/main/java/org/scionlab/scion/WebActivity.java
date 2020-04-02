package org.scionlab.scion;

import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class WebActivity extends Fragment {
    static final String ASSET = WebActivity.class.getCanonicalName() + ".ASSET";

    public String ContentURL;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.activity_web, container, false);
        WebView browser = layout.findViewById(R.id.webView);
        browser.loadUrl(ContentURL);
        return layout;
    }
}

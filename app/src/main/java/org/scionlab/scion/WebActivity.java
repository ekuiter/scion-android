package org.scionlab.scion;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class WebActivity extends AppCompatActivity {
    static final String ASSET = WebActivity.class.getCanonicalName() + ".ASSET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        WebView browser = findViewById(R.id.webView);
        browser.loadUrl(getIntent().getStringExtra(ASSET));
    }
}

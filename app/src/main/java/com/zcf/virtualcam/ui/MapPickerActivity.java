package com.zcf.virtualcam.ui;

// Usage: map picker page that returns selected coordinates.

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zcf.virtualcam.R;

import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";

    public static Intent createIntent(@NonNull Context context, double latitude, double longitude) {
        Intent intent = new Intent(context, MapPickerActivity.class);
        intent.putExtra(EXTRA_LATITUDE, latitude);
        intent.putExtra(EXTRA_LONGITUDE, longitude);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        double latitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0.0d);
        double longitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0.0d);

        WebView webView = findViewById(R.id.webview_map);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new JsBridge(), "Android");

        String url = buildAssetUrl(latitude, longitude);
        webView.loadUrl(url);
    }

    private String buildAssetUrl(double latitude, double longitude) {
        String base = "file:///android_asset/map_picker.html";
        String query = String.format(Locale.US, "lat=%.7f&lng=%.7f", latitude, longitude);
        return base + "?" + Uri.encode(query, "&=");
    }

    private final class JsBridge {
        @JavascriptInterface
        public void onLocationSelected(double latitude, double longitude) {
            runOnUiThread(() -> {
                Intent data = new Intent();
                data.putExtra(EXTRA_LATITUDE, latitude);
                data.putExtra(EXTRA_LONGITUDE, longitude);
                setResult(RESULT_OK, data);
                finish();
            });
        }
    }
}

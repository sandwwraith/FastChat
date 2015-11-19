package com.sandwwraith.fastchat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.sandwwraith.fastchat.social.SocialManager.Types;
import com.sandwwraith.fastchat.social.SocialWrapper;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        WebView web = (WebView) findViewById(R.id.webv);
        web.getSettings().setJavaScriptEnabled(true);
        Intent i = getIntent();
        final Types type = Types.valueOf(i.getStringExtra(Types.Intent_Param));
        final SocialWrapper wrapper = type.getWrapper();
        web.loadUrl(wrapper.generateTokenRequest());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("auth_activity", "URL: " + url);
                String tok = wrapper.retrieveToken(url);
                if (tok != null) {
                    //TODO: Error handling
                    Intent res = new Intent();
                    res.putExtra(Types.Intent_Param, type.name());
                    res.putExtra(type.getTokenString(), tok);
                    res.putExtra(type.getIdString(), wrapper.retrieveId(url));
                    setResult(RESULT_OK, res);
                    finish();
                }
            }
        });
    }
}

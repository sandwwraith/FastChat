package com.sandwwraith.fastchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class ChatActivity extends AppCompatActivity {

    public static final String NAME_INTENT = "NAME_INTENT";
    public static final String GENDER_INTENT = "GENDER_INTENT";
    public static final String THEME_INTENT = "THEME_INTENT";

    MenuItem timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent x = getIntent();
        String name = (x == null ? null : x.getStringExtra(NAME_INTENT));
        String theme = (x == null ? null : "Theme " + x.getIntExtra(THEME_INTENT, 42));

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(name);
        getSupportActionBar().setSubtitle(theme);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        timer = menu.findItem(R.id.chat_timer);
        timer.setEnabled(false);
        timer.setTitle("05:00");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.chat_timer) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

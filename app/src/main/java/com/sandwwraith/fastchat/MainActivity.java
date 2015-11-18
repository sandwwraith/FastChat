package com.sandwwraith.fastchat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.sandwwraith.fastchat.social.SocialManager;
import com.sandwwraith.fastchat.social.SocialUser;

public class MainActivity extends AppCompatActivity implements MessengerService.MessageReceiver, SocialManager.SocialManagerCallback {

    private SocialManager manager = null;
    private Snackbar snack = null;

    //Connection to service
    //Refer to documentation, "Bound service"
    //or to lesson #5
    private MessengerService messenger = null;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MessengerService.MessengerBinder binder = (MessengerService.MessengerBinder) service;
            MainActivity.this.messenger = binder.getService();

            //Connecting immediately
            messenger.connect();
            messenger.startReceiving(MainActivity.this);
            if (snack != null) snack.dismiss();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private TextView messageView;

    private void connectService() {
        Intent intent = new Intent(this, MessengerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        snack = Snackbar.make(messageView, R.string.connecting, Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null);
        snack.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            manager.continueAuth(data);
        } else {
            notifyUser("OAuth failed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        messageView = (TextView) findViewById(R.id.greetings);

        //Checking connection
        if (!isOnline()) {
            notifyUser(R.string.network_NA);
        } else {
            connectService();
            manager = new SocialManager(this, this);
            manager.validateToken(SocialManager.Types.TYPE_VK);
        }

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the message
                //TODO: Move to other screen
            }
        });*/
    }

    @Override
    protected void onDestroy() {
        if (messenger != null) {
            unbindService(connection);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void processMessage(String msg) {
        messageView.append(msg);
    }

    @Override
    public void onValidationFail(SocialManager.Types type) {
        notifyUser("Auth required");
        AuthorizationClick clicker = new AuthorizationClick();
        findViewById(R.id.vk_image).setOnClickListener(clicker);
    }

    @Override
    public void onUserInfoUpdated(boolean success, SocialUser user) {
        if (success) {
            ((TextView) findViewById(R.id.vk_text)).setText(user.getFirstName() + " " + user.getLastName());
        } else {
            notifyUser("User info get failed");
        }
    }

    public class AuthorizationClick implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            int id = view.getId();

            if (id == R.id.fb_image) {
                notifyUser("Sorry, facebook currently unavailable");
            } else if (id == R.id.vk_image) {
                Log.d("Main_activity", "Starting auth");
                manager.startAuth(SocialManager.Types.TYPE_VK);
            }
        }
    }

    public void notifyUser(String message) {
        Snackbar.make(messageView, message, Snackbar.LENGTH_SHORT)
                .setAction("Action", null).show();
    }

    public void notifyUser(int resourceId) {
        String msg = getResources().getString(resourceId);
        notifyUser(msg);
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
}

package com.sandwwraith.fastchat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.sandwwraith.fastchat.clientUtils.MessageDeserializer;
import com.sandwwraith.fastchat.clientUtils.MessageSerializer;
import com.sandwwraith.fastchat.clientUtils.Pair;
import com.sandwwraith.fastchat.social.SocialManager;
import com.sandwwraith.fastchat.social.SocialUser;
import com.sandwwraith.fastchat.social.SocialWrapper;

public class MainActivity extends AppCompatActivity implements MessengerService.connectResultHandler, MessengerService.messageHandler, SocialManager.SocialManagerCallback {

    private final static String LOG_TAG = "main_activity";
    FloatingActionButton queueButton;
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
            messenger.connect(MainActivity.this);
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
            notifyUser("OAuth failed"); //TODO: Normal info w/ errors. Remember, user can press back from auth screen
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        messageView = (TextView) findViewById(R.id.greetings);
        queueButton = (FloatingActionButton) findViewById(R.id.fab);

        //Checking connection
        if (!isOnline()) {
            ((TextView) findViewById(R.id.vk_text)).setText(R.string.network_NA);
            ((TextView) findViewById(R.id.fb_text)).setText(R.string.network_NA);
        } else {
            manager = new SocialManager(this, this);
            manager.validateToken(SocialManager.Types.TYPE_VK);
        }
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
//            Intent intent = new Intent(this, ChatActivity.class);
//            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void processMessage(byte[] msg) {
        try {
            Pair<int[], String> p = MessageDeserializer.deserializePairFound(msg);

            if (snack != null) snack.dismiss();

            Log.d(LOG_TAG, "Pair found " + p.second + " theme " + p.first[0]);
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.NAME_INTENT, p.second);
            intent.putExtra(ChatActivity.THEME_INTENT, p.first[0]);
            intent.putExtra(ChatActivity.GENDER_INTENT, p.first[1]);
            startActivity(intent);

        } catch (MessageDeserializer.MessageDeserializerException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void onValidationFail(SocialManager.Types type) {
        AuthorizationClick clicker = new AuthorizationClick();
        if (type == SocialManager.Types.TYPE_VK) {
            findViewById(R.id.vk_image).setOnClickListener(clicker);
            ((TextView) findViewById(R.id.vk_text)).setText(R.string.not_authorized);
        } else
            findViewById(R.id.fb_image).setOnClickListener(clicker);
    }

    @Override
    public void onConnectResult(boolean success) {
        if (success) {
            queueButton.setVisibility(View.VISIBLE);
            queueButton.setOnClickListener(new EnqueueClick());
            messenger.setReceiver(MainActivity.this);
            if (snack != null) snack.dismiss();
        } else {
            notifyUser(R.string.service_NA);
        }
    }

    @Override
    public void onUserInfoFailed(SocialManager.Types type, SocialWrapper.ErrorStorage lastError) {
        //Обработка различных ошибок. Пока что обрабатываем только устраревший токен
        notifyUser(lastError.toString());
        this.onValidationFail(type);
    }

    @Override
    public void onUserInfoUpdated(SocialUser user) {
        connectService();
        if (user.getType() == SocialManager.Types.TYPE_VK) {

            ((TextView) findViewById(R.id.vk_text)).setText(user.toString());
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

    /**
     * This method is called when new intent has been delivered to this screen
     * That is, the voting has ended and the user returned to the main screen
     * You can refer to {@code VotingActivity.onBackPressed()}
     *
     * @param intent Intent specifies, if user wants to be enqueued immediately
     */
    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(LOG_TAG, "New intent delivered");
        queueButton.setClickable(true);
        messenger.setReceiver(this);
        boolean now = intent.getBooleanExtra(VotingActivity.ENQUEUE_NOW, false);
        if (now) queueButton.callOnClick();
    }

    public class AuthorizationClick implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            int id = view.getId();

            if (id == R.id.fb_image) {
                notifyUser("Sorry, facebook currently not implemented");
            } else if (id == R.id.vk_image) {
                Log.d(LOG_TAG, "Starting auth");
                manager.startAuth(SocialManager.Types.TYPE_VK);
            }
        }
    }

    public class EnqueueClick implements View.OnClickListener { //TODO: Make ability to cancel enqueue

        /**
         * Handles click from the view (actually, from floating button)
         * and queues user to server.
         * Then sets {@code clickable} to false to prevent double-enqueuing
         *
         * @param v View that has been clicked
         */
        @Override
        public void onClick(View v) {
            //Starting queue
            SocialUser user = SocialManager.getUser(SocialManager.Types.TYPE_VK);
            Log.d(LOG_TAG, "Queuing user: " + user.toString());
            messenger.send(MessageSerializer.queueUser(user));
            snack = Snackbar.make(messageView, R.string.search_pair, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Action", null);
            snack.show();
            v.setClickable(false); //Queuing only one time
        }
    }
}

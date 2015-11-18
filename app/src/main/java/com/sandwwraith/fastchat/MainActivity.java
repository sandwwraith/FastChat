package com.sandwwraith.fastchat;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements MessengerService.MessageReceiver {

    //Connection to service
    //Refer to documentation, "Bound service"
    //or to lesson #5
    private MessengerService messenger = null;
    private Snackbar snack = null;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MessengerService.MessengerBinder binder = (MessengerService.MessengerBinder) service;
            MainActivity.this.messenger = binder.getService();

            //Connecting immediately
            messenger.connect();
            messenger.startReceiving(MainActivity.this);
            snack.dismiss();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private EditText inputMessage;
    private TextView messageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        messageView = (TextView) findViewById(R.id.greetings);

        /*Intent intent = new Intent(this,MessengerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        snack = Snackbar.make(messageView, R.string.connecting, Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null);
        snack.show();*/

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the message
                //TODO: Implement
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (messenger!=null) {
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

    public void onAuthorizationClick(View view) {
        int id = view.getId();

        if (id == R.id.fb_image) {
            Snackbar.make(messageView, "Sorry, facebook currently unavailable", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        }
    }
}

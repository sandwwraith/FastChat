package com.sandwwraith.fastchat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sandwwraith.fastchat.clientUtils.Pair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity extends AppCompatActivity {

    public static final String NAME_INTENT = "NAME_INTENT";
    public static final String GENDER_INTENT = "GENDER_INTENT";
    public static final String THEME_INTENT = "THEME_INTENT";

    private MenuItem timer;
    private RecyclerAdapter adapter;

    private ArrayList<MessageHolder> messages = new ArrayList<>();

    private int seconds = 5 * 60;
    //Connection to service
    //Refer to documentation, "Bound service"
    //or to lesson #5
    private MessengerService messenger = null;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MessengerService.MessengerBinder binder = (MessengerService.MessengerBinder) service;
            ChatActivity.this.messenger = binder.getService();

            //messenger.setReceiver(...);
            Timer mTimer = new Timer();
            mTimer.schedule(new TimerTick(), 1000, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void connectService() {
        Intent intent = new Intent(this, MessengerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

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

        //-----TESTING----
        Random r = new Random();
        for (int i = 0; i < 42; i++) {
            messages.add(new MessageHolder(new Pair<>(new Date(), "Test" + r.nextFloat())
                    , r.nextBoolean() ? MessageHolder.M_SEND : MessageHolder.M_RECV
            ));
        }
        //-----TESTING----

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerAdapter();
        recyclerView.setAdapter(adapter);

        connectService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        timer = menu.findItem(R.id.chat_timer);
        timer.setEnabled(false);
        timer.setTitle("5:00");
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

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater li = LayoutInflater.from(ChatActivity.this);
            return new ViewHolder(li.inflate(R.layout.messagebox, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MessageHolder h = messages.get(position);

            holder.message.setText(h.getMessage());
            holder.timeStamp.setText(h.getFormattedDate());

            if (h.getType() == MessageHolder.M_SEND) holder.layout.setGravity(Gravity.RIGHT);
            else holder.layout.setGravity(Gravity.LEFT);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView message;
            final TextView timeStamp;
            final LinearLayout layout;

            public ViewHolder(View itemView) {
                super(itemView);

                message = (TextView) itemView.findViewById(R.id.textView);
                timeStamp = (TextView) itemView.findViewById(R.id.textView2);
                layout = (LinearLayout) itemView.findViewById(R.id.messagebox);
            }
        }
    }

    private class MessageHolder {
        public static final int M_SEND = 0x1;
        public static final int M_RECV = 0x2;

        private final int type; // Indicates if this message was "my" (sent to server) or from my opponent
        private final Date time;
        private final String msg;

        public int getType() {
            return type;
        }

        MessageHolder(Pair<Date, String> msg, int type) {
            this.type = type;
            this.time = msg.first;
            this.msg = msg.second;
        }

        public String getFormattedDate() {
            return time.toString(); //TODO: Format beautiful
        }

        public String getMessage() {
            return msg;
        }
    }

    private class TimerTick extends TimerTask {
        @Override
        public void run() {
            seconds--;
            final String s = Integer.toString(seconds / 60) + ":"
                    + new DecimalFormat("00").format(seconds % 60);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timer.setTitle(s);
                }
            });
        }
    }
}

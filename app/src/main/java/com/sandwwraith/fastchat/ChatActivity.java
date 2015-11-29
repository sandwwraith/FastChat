package com.sandwwraith.fastchat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sandwwraith.fastchat.clientUtils.MessageParser;
import com.sandwwraith.fastchat.clientUtils.MessageSerializer;
import com.sandwwraith.fastchat.clientUtils.Pair;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity extends AppCompatActivity implements MessageParser.MessageResult {

    public static final String NAME_INTENT = "NAME_INTENT";
    public static final String GENDER_INTENT = "GENDER_INTENT";
    public static final String THEME_INTENT = "THEME_INTENT";

    private MenuItem timer;
    private RecyclerAdapter adapter;
    private EditText editText;
    RecyclerView recyclerView;

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

            messenger.setReceiver(new MessageParser(ChatActivity.this));
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

        editText = (EditText) findViewById(R.id.msg_text);
        findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                messages.add(new MessageHolder(msg));
                messenger.send(MessageSerializer.serializeMessage(msg));
                adapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
            }
        });

        //-----TESTING----
        /*Random r = new Random();
        for (int i = 0; i < 3; i++) {
            messages.add(new MessageHolder(new Pair<>(new Date(), "Test" + r.nextFloat())
                    , r.nextBoolean() ? MessageHolder.M_SEND : MessageHolder.M_RECV
            ));
        }*/
        //-----TESTING----

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
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

    //------Message handling section
    @Override
    public void onPairFound(Pair<int[], String> companion) {
        // No action on this screen
    }

    @Override
    public void onTextMessageReceived(Pair<Date, String> message) {
        messages.add(new MessageHolder(message, MessageHolder.M_RECV));
        adapter.notifyDataSetChanged();
        recyclerView.smoothScrollToPosition(adapter.getItemCount());
    }

    @Override
    public void onTimeout() {
        //TODO: Timeout handle
    }

    @Override
    public void onVotingResults(Pair<String, String> voted) {
        //No action on this screen
    }

    @Override
    public void onLeave() {

    }

    @Override
    public void onServerError() {

    }

    @Override
    public void onMalformedSequence(@Nullable String errorDescription) {
        Toast.makeText(getApplicationContext(), errorDescription == null ? "Malformed sequence" : errorDescription, Toast.LENGTH_LONG)
                .show();
    }

    //------Message handling section end

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

        MessageHolder(String msg) {//Use this to create a sent message
            this.time = new Date();
            this.type = M_SEND;
            this.msg = msg;
        }

        public String getFormattedDate() {
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return df.format(time);
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

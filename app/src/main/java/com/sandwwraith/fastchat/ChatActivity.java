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
import android.util.Log;
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

import com.sandwwraith.fastchat.chatUtils.LeaveDialogFragment;
import com.sandwwraith.fastchat.chatUtils.MessageHolder;
import com.sandwwraith.fastchat.clientUtils.MessageParser;
import com.sandwwraith.fastchat.clientUtils.MessageSerializer;
import com.sandwwraith.fastchat.clientUtils.Pair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity extends AppCompatActivity implements MessageParser.MessageResult, LeaveDialogFragment.LeaveDialogFragmentListener {

    public static final String NAME_INTENT = "NAME_INTENT";
    public static final String GENDER_INTENT = "GENDER_INTENT";
    public static final String THEME_INTENT = "THEME_INTENT";

    public static final String SECONDS_STATE = "CHAT_SECONDS";

    public static final String LOG_TAG = "chat_activity";
    RecyclerView recyclerView;
    private MenuItem timerView;
    private RecyclerAdapter adapter;
    private EditText editText;
    private TimerTick timer_task;

    private ArrayList<MessageHolder> messages = new ArrayList<>(); //TODO: Save messages... or just prohibit rotation ???

    private int seconds = 5 * 60;
    private String op_name = "";

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
            timer_task = new TimerTick();
            mTimer.scheduleAtFixedRate(timer_task, 1000, 1000);
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
        op_name = (x == null ? null : x.getStringExtra(NAME_INTENT));
        String theme = (x == null ? null : "Theme " + x.getIntExtra(THEME_INTENT, 42));

        Log.d(LOG_TAG, x == null ? "Intent null" : "Intent not null");
        Log.d(LOG_TAG, savedInstanceState == null ? "SavedState null" : "SavedState not null");

        if (savedInstanceState != null) {
            seconds = savedInstanceState.getInt(SECONDS_STATE);
        }

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(op_name);
        getSupportActionBar().setSubtitle(theme);

        editText = (EditText) findViewById(R.id.msg_text);
        findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                if (msg.trim().equals("")) return;
                messages.add(new MessageHolder(msg));
                messenger.send(MessageSerializer.serializeMessage(msg));
                editText.setText("");
                adapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerAdapter();
        recyclerView.setAdapter(adapter);

        connectService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        timerView = menu.findItem(R.id.chat_timer);
//        timerView.setEnabled(false);
        timerView.setTitle(formatSeconds());
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

            //----DEBUGGING
            timer_task.cancel();
            this.timedOutEvent();
            //----Debugging

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        LeaveDialogFragment lf = new LeaveDialogFragment();
        lf.show(getFragmentManager(), "leave");
    }

    @Override
    public void onLeaveConfirm() {
        messenger.send(MessageSerializer.serializeLeave());
        returnToMain();
    }

    private void timedOutEvent() {
        messenger.send(MessageSerializer.serializeTimeout());
        this.onTimeout();
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
        Toast.makeText(this, R.string.timeout_msg, Toast.LENGTH_SHORT).show();

        timer_task.cancel();
        Intent intent = new Intent(this, VotingActivity.class);
        intent.putExtra(NAME_INTENT, op_name);
        startActivity(intent);
    }

    @Override
    public void onVotingResults(Pair<String, String> voted) {
        //No action on this screen
    }

    @Override
    public void onLeave() {
        //TODO: Handle opponent leaving
        Toast.makeText(this, "YOU PARTNER IS PIDOR", Toast.LENGTH_LONG).show();
        returnToMain();
    }

    public void returnToMain() {
        Intent in = new Intent(getApplicationContext(), MainActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(in);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SECONDS_STATE, seconds);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (timer_task != null) timer_task.cancel();
        if (messenger != null) unbindService(connection);
        super.onDestroy();
    }

    private String formatSeconds() {
        return Integer.toString(seconds / 60) + ":"
                + new DecimalFormat("00").format(seconds % 60);
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

            if (h.getType() == MessageHolder.M_SEND) {
                holder.message.setGravity(Gravity.RIGHT);
                holder.layout.setGravity(Gravity.RIGHT);
            } else {
                holder.message.setGravity(Gravity.LEFT);
                holder.layout.setGravity(Gravity.LEFT);
            }
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

    private class TimerTick extends TimerTask {
        @Override
        public void run() {
            seconds--;
            if (seconds == 0) {
                TimerTick.this.cancel();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ChatActivity.this.timedOutEvent();
                    }
                });
            } else {
                final String s = formatSeconds();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timerView.setTitle(s);
                    }
                });
            }
        }
    }
}

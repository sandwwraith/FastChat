package com.sandwwraith.fastchat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sandwwraith.fastchat.chatUtils.UserHolder;
import com.sandwwraith.fastchat.clientUtils.MessageParser;
import com.sandwwraith.fastchat.clientUtils.MessageSerializer;
import com.sandwwraith.fastchat.clientUtils.Pair;
import com.sandwwraith.fastchat.social.SocialManager;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class VotingActivity extends AppCompatActivity implements MessageParser.MessageResult, View.OnClickListener {

    public final static String LOG_TAG = "vote_activity";
    public static final String ENQUEUE_NOW = "ENQUEUE_NOW";
    public static final String SUCCESS_VOTE_KEY = "SUCCESS_VOTE_KEY";
    private TextView textName;
    private TextView timerTextView;

    private int my_vote = -1;
    private int op_vote = -1;
    private Pair<String, String> op_result = null;

    private VoteTimer voteTimer = null;

    private MessengerService messenger = null;
    private MessageParser parser;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MessengerService.MessengerBinder binder = (MessengerService.MessengerBinder) service;
            VotingActivity.this.messenger = binder.getService();

            parser = new MessageParser(VotingActivity.this);
            messenger.setReceiver(parser);

            findViewById(R.id.button_like).setOnClickListener(VotingActivity.this);
            findViewById(R.id.button_dislike).setOnClickListener(VotingActivity.this);
            timerTextView.setVisibility(View.VISIBLE);

            voteTimer = new VoteTimer();
            new Timer().scheduleAtFixedRate(voteTimer, 1000, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private int seconds = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String name = getIntent().getStringExtra(ChatActivity.NAME_INTENT);
        textName = ((TextView) findViewById(R.id.text_name));
        textName.setText(name);

        timerTextView = (TextView) (findViewById(R.id.votingTimerText));
        timerTextView.setText(formatSeconds());

        connectService();
//        findViewById(R.id.button_like).setOnClickListener(VotingActivity.this);
//        findViewById(R.id.button_dislike).setOnClickListener(VotingActivity.this);
    }

    private void connectService() {
        Intent intent = new Intent(this, MessengerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPairFound(Pair<int[], String> companion) {
        //No action
    }

    @Override
    public void onTextMessageReceived(Pair<Date, String> message) {
        //If you ignore it, maybe it will go away
    }

    @Override
    public void onTimeout() {
        //Seriously?
    }

    @Override
    public void onVotingResults(Pair<String, String> voted) {
        //HAHA, THAT'S WAT WE NEED
        if (op_vote != -1) return;
        op_result = voted;
        if (op_result.first != null) op_vote = 1;
        else op_vote = 0;

        if (my_vote != -1) this.finishVote();
    }

    @Override
    public void onLeave() {
        op_vote = 0;
        if (my_vote != -1) this.finishVote();
    }

    @Override
    public void onServerError() {

    }

    @Override
    public void onMalformedSequence(@Nullable String errorDescription) {
        Toast.makeText(getApplicationContext(), errorDescription == null ? "Malformed sequence" : errorDescription, Toast.LENGTH_LONG)
                .show();
    }

    /**
     * Click on like/dislike views
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        byte[] msg = null;
        switch (id) {
            case R.id.button_like:
                msg = MessageSerializer.serializeVoting(true, SocialManager.getUser(SocialManager.Types.TYPE_VK)); //TODO: Facebook
                my_vote = 1;
                //noinspection deprecation
                v.setBackgroundColor(getResources().getColor(R.color.vote_Like));
                break;
            case R.id.button_dislike:
                msg = MessageSerializer.serializeVoting(false, SocialManager.getUser(SocialManager.Types.TYPE_VK));
                my_vote = 0;
                //noinspection deprecation
                v.setBackgroundColor(getResources().getColor(R.color.vote_Dislike));
                break;
        }
        //TODO: After changing color buttons lost their rounded corners. Bring it back

        findViewById(R.id.button_like).setClickable(false);
        findViewById(R.id.button_dislike).setClickable(false);

        if (msg != null) messenger.send(msg);

        if (op_vote != -1) this.finishVote();
    }

    private void finishVote() {
        if (voteTimer != null) voteTimer.cancel();
        timerTextView.setVisibility(View.GONE);

        if (my_vote == 1 && op_vote == 1) {
            textName.setText(op_result.first);
            TextView url = ((TextView) findViewById(R.id.url_place));
            url.setText(op_result.second);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Set<String> success_votes = new HashSet<>();
            Set<String> old_votes = sharedPref.getStringSet(SUCCESS_VOTE_KEY, success_votes);
            // According to the docs, we cannot modify old_votes;
            success_votes.addAll(old_votes);
            SharedPreferences.Editor editor = sharedPref.edit();
            success_votes.add(new UserHolder(op_result.first, op_result.second).ConvertToJson());

            editor.putStringSet(SUCCESS_VOTE_KEY, success_votes).apply();
        } else {
            Snackbar.make(textName, R.string.vote_fail, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show(); //TODO: Proper notification
        }
        Button again = (Button) findViewById(R.id.try_again_button);
        again.setVisibility(View.VISIBLE);
        again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VotingActivity.this.returnToMain(true);
            }
        });
    }

    /**
     * Returns user to the main screen instead of inappropriate chat screen
     */
    @Override
    public void onBackPressed() {
        if (my_vote == -1) {
            my_vote = 0;
            messenger.send(MessageSerializer.serializeVoting(
                    false, SocialManager.getUser(SocialManager.Types.TYPE_VK)));
        }
        this.returnToMain(false);
    }

    /**
     * Method returns user to the main screen with proper back stack navigation
     *
     * @param enqueueNow Determines whether send request for enqueue user in chat
     *                   immediately or not.
     */
    public void returnToMain(boolean enqueueNow) {
        Intent in = new Intent(getApplicationContext(), MainActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        in.putExtra(ENQUEUE_NOW, enqueueNow);
        startActivity(in);
    }

    @Override
    protected void onDestroy() {
        if (messenger != null) {
            messenger.unbindReceiver(parser);
            unbindService(connection);
        }
        super.onDestroy();
    }

    private String formatSeconds() {
        return new DecimalFormat("00").format(seconds);
    }

    private class VoteTimer extends TimerTask {
        @Override
        public void run() {
            seconds--;
            if (seconds <= 0) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Simulating user behaviour
                        if (my_vote == -1)
                            findViewById(R.id.button_dislike).callOnClick();
                        if (op_vote == -1)
                            VotingActivity.this.onVotingResults(new Pair<String, String>(null, null));
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timerTextView.setText(formatSeconds());
                    }
                });
            }
        }
    }
}

package com.sandwwraith.fastchat;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sandwwraith.fastchat.chatUtils.MessageHolder;
import com.sandwwraith.fastchat.chatUtils.UserHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerAdapter adapter;
    private final ArrayList<UserHolder> success_votings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerAdapter();
        recyclerView.setAdapter(adapter);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> votes_default = new HashSet<>();
        Set<String> votes = sharedPref.getStringSet(VotingActivity.SUCCESS_VOTE_KEY, votes_default);
        for (String json : votes) {
            success_votings.add(new UserHolder(json));
        }

        adapter.notifyDataSetChanged();
        recyclerView.smoothScrollToPosition(adapter.getItemCount());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater li = LayoutInflater.from(HistoryActivity.this);
            return new ViewHolder(li.inflate(R.layout.userbox, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder vh, int position) {
            UserHolder h = success_votings.get(position);
            vh.username.setText(h.username);
            vh.link.setText(h.link);
        }

        @Override
        public int getItemCount() {
            return success_votings.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView username;
            final TextView link;
            final LinearLayout layout;

            public ViewHolder(View itemView) {
                super(itemView);

                username = (TextView) itemView.findViewById(R.id.tvUsername);
                link = (TextView) itemView.findViewById(R.id.tvLink);
                layout = (LinearLayout) itemView.findViewById(R.id.userbox);
            }
        }
    }
}

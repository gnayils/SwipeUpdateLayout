package org.gnayils.android.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.gnayils.android.widget.SwipeUpdateLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                TextView textView = new TextView(parent.getContext());
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
                CardView.LayoutParams textViewLayoutParam = new CardView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                textViewLayoutParam.gravity = Gravity.CENTER;
                textView.setLayoutParams(textViewLayoutParam);

                CardView cardView = new CardView(parent.getContext());
                cardView.addView(textView);
                cardView.setRadius(4);
                RecyclerView.LayoutParams cardViewLayoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200);
                cardViewLayoutParams.setMargins(8, 8, 8, 8);
                cardView.setLayoutParams(cardViewLayoutParams);
                RecyclerViewHolder holder = new RecyclerViewHolder(cardView, textView);
                return holder;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ((RecyclerViewHolder) holder).textView.setText(String.valueOf(position));
            }

            @Override
            public int getItemCount() {
                return 10;
            }
        });

        final SwipeUpdateLayout swipeUpdateLayout = (SwipeUpdateLayout) findViewById(R.id.swipe_update_layout);
        swipeUpdateLayout.setOnUpdateListener(new SwipeUpdateLayout.OnUpdateListener() {
            @Override
            public void onUpdate(int updatingPosition) {
                if(updatingPosition == SwipeUpdateLayout.AT_TOP) {
                    System.out.println("get latest data");
                } else if(updatingPosition == SwipeUpdateLayout.AT_BOTTOM) {
                    System.out.println("get older data");
                }
            }
        });
        swipeUpdateLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeUpdateLayout.updateTop();
            }
        }, 1000);

        swipeUpdateLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeUpdateLayout.stopUpdating();
            }
        }, 3000);

        swipeUpdateLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                recyclerView.smoothScrollToPosition(10);
            }
        }, 4000);

        swipeUpdateLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeUpdateLayout.updateBottom();
            }
        }, 5000);

        swipeUpdateLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeUpdateLayout.stopUpdating();
            }
        }, 8000);
    }


    static class RecyclerViewHolder extends RecyclerView.ViewHolder {

        public TextView textView;

        public RecyclerViewHolder(View itemView, TextView textView) {
            super(itemView);
            this.textView = textView;
        }
    }
}

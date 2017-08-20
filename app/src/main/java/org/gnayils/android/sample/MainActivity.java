/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gnayils.android.sample;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.gnayils.android.widget.SwipeUpdateLayout;

public class MainActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {

    private static final int PERCENTAGE_TO_SHOW_IMAGE = 20;

    private View fab;
    private boolean isImageHidden;
    private int currentVerticalOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fab = findViewById(R.id.fab);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        AppBarLayout appbar = (AppBarLayout) findViewById(R.id.appbar);
        appbar.addOnOffsetChangedListener(this);
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                TextView textView = new TextView(parent.getContext());
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setTypeface(Typeface.SANS_SERIF);
                textView.setTextColor(Color.GRAY);
                int padding = (int) (8 * getResources().getDisplayMetrics().density);
                textView.setPadding(padding, padding, padding, padding);
                CardView.LayoutParams textViewLayoutParam = new CardView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                textViewLayoutParam.gravity = Gravity.CENTER;
                textView.setLayoutParams(textViewLayoutParam);

                CardView cardView = new CardView(parent.getContext());
                cardView.addView(textView);
                cardView.setRadius(4 * getResources().getDisplayMetrics().density);
                RecyclerView.LayoutParams cardViewLayoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (200 * getResources().getDisplayMetrics().density));
                int margin = (int) (8 * getResources().getDisplayMetrics().density);
                cardViewLayoutParams.setMargins(margin, 0, margin, margin);
                cardView.setLayoutParams(cardViewLayoutParams);
                RecyclerViewHolder holder = new RecyclerViewHolder(cardView, textView);
                return holder;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ((MainActivity.RecyclerViewHolder) holder).textView.setText(getString(R.string.lorem));
            }

            @Override
            public int getItemCount() {
                return 10;
            }
        });
        final SwipeUpdateLayout swipeUpdateLayout = (SwipeUpdateLayout) findViewById(R.id.swipeupdatelayout);
        swipeUpdateLayout.setOnChildScrollCallback(new SwipeUpdateLayout.OnChildVerticalScrollCallback() {
            @Override
            public boolean canChildScrollUp(SwipeUpdateLayout parent, @Nullable View child) {
                return recyclerView.canScrollVertically(-1) || currentVerticalOffset < 0;
            }

            @Override
            public boolean canChildScrollDown(SwipeUpdateLayout parent, @Nullable View child) {
                return recyclerView.canScrollVertically(1);
            }
        });
        swipeUpdateLayout.setOnUpdateListener(new SwipeUpdateLayout.OnUpdateListener() {
            @Override
            public void onUpdate(int updatingPosition) {
                if(updatingPosition == SwipeUpdateLayout.AT_TOP) {
                    System.out.println("get latest data");
                } else if(updatingPosition == SwipeUpdateLayout.AT_BOTTOM) {
                    System.out.println("get older data");
                }
                swipeUpdateLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeUpdateLayout.stopUpdating();
                    }
                }, 1000);
            }
        });

    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

        currentVerticalOffset = verticalOffset;

        int currentScrollPercentage = (Math.abs(verticalOffset)) * 100 / appBarLayout.getTotalScrollRange();

        if (currentScrollPercentage >= PERCENTAGE_TO_SHOW_IMAGE) {
            if (!isImageHidden) {
                isImageHidden = true;
                ViewCompat.animate(fab).scaleY(0).scaleX(0).start();
            }
        }

        if (currentScrollPercentage < PERCENTAGE_TO_SHOW_IMAGE) {
            if (isImageHidden) {
                isImageHidden = false;
                ViewCompat.animate(fab).scaleY(1).scaleX(1).start();
            }
        }
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {

        public TextView textView;

        public RecyclerViewHolder(View itemView, TextView textView) {
            super(itemView);
            this.textView = textView;
        }
    }
}

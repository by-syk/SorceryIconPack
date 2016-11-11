package com.sorcerer.sorcery.iconpack.ui.activities;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Display;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sorcerer.sorcery.iconpack.BuildConfig;
import com.sorcerer.sorcery.iconpack.R;
import com.sorcerer.sorcery.iconpack.models.IconBean;
import com.sorcerer.sorcery.iconpack.ui.adapters.recyclerviewAdapter.SearchAdapter;
import com.sorcerer.sorcery.iconpack.ui.exposedSearch.FadeInTransition;
import com.sorcerer.sorcery.iconpack.ui.exposedSearch.FadeOutTransition;
import com.sorcerer.sorcery.iconpack.ui.exposedSearch.SearchBar;
import com.sorcerer.sorcery.iconpack.ui.exposedSearch.SearchTransitioner;
import com.sorcerer.sorcery.iconpack.ui.exposedSearch.SimpleTransitionListener;
import com.sorcerer.sorcery.iconpack.ui.exposedSearch.ViewFader;
import com.sorcerer.sorcery.iconpack.utils.KeyboardUtil;
import com.sorcerer.sorcery.iconpack.utils.ResourceUtil;
import com.sorcerer.sorcery.iconpack.utils.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class SearchActivity extends AppCompatActivity {

    @BindView(R.id.imageView_search_graphic)
    ImageView mSearchGraphic;

    @BindView(R.id.searchBar)
    SearchBar mSearchBar;

    @BindView(R.id.relativeLayout_content_search)
    RelativeLayout mContent;

    @BindView(R.id.recyclerView_search)
    RecyclerView mRecyclerView;

    @BindView(R.id.textView_search_hint)
    TextView mHintTextView;

    private SearchAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private int mSpanCount;

    private ViewFader mViewFader = new ViewFader();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);

        setSupportActionBar(mSearchBar);

        if (savedInstanceState == null && SearchTransitioner.supportTransitions()) {
            mViewFader.hideContentOf(mSearchBar);
            ViewTreeObserver viewTreeObserver = mSearchBar.getViewTreeObserver();
            viewTreeObserver
                    .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mSearchBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            animateShowSearch();
                        }

                        private void animateShowSearch() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                TransitionManager.beginDelayedTransition(mSearchBar,
                                        FadeInTransition.createTransition());
                                mContent.animate().alpha(1).setDuration(250).start();
                            }
                            mViewFader.showContent(mSearchBar);
                        }
                    });
        }

        mSearchBar.addTextWatcher(new SimpleTextWatcher() {
            private Handler mHandler = new Handler();

            private Runnable mRunnable;

            @Override
            public void afterTextChanged(final Editable editable) {
                super.afterTextChanged(editable);
                if (editable != null && editable.length() > 0) {
                    mSearchGraphic.setVisibility(GONE);
                } else {
                    mSearchGraphic.setVisibility(VISIBLE);
                }
                mAdapter.search(editable.toString().toLowerCase());
//                mHandler.removeCallbacks(mRunnable);
//                mRunnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        mAdapter.search(editable.toString());
//                    }
//                };
//                mHandler.postDelayed(mRunnable, 500);
            }
        });

        mSpanCount = calSpanCount(this);

        mAdapter = new SearchAdapter(this, mSpanCount);
        mAdapter.setHintTextView(mHintTextView);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new GridLayoutManager(this, mSpanCount, VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                KeyboardUtil.closeKeyboard(SearchActivity.this);
                return false;
            }
        });

        mSearchBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSearchBar.requestEditTextFocus();
            }
        }, 250);

        if (savedInstanceState != null) {
            String text = savedInstanceState.getString(EDIT_DATA_KEY);
            if (text != null && text.length() > 0) {
                mSearchBar.setText(savedInstanceState.getString(EDIT_DATA_KEY));
                mAdapter.search(text);
                mSearchGraphic.setVisibility(GONE);
            }
        }

        getIconBeanList();
    }

    @Override
    public void finish() {
        if (SearchTransitioner.supportTransitions()) {
            exitTransitionWithAction(new Runnable() {
                @Override
                public void run() {
                    SearchActivity.super.finish();
                    overridePendingTransition(0, 0);
                }
            });
        } else {
            super.finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final String EDIT_DATA_KEY = "edit-data-key";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EDIT_DATA_KEY, mSearchBar.getText());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private static int calSpanCount(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float s = activity.getResources().getDimension(R.dimen.icon_grid_item_size);
        return (int) (size.x / s);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void exitTransitionWithAction(final Runnable endingAction) {
        Transition transition = FadeOutTransition.withAction(new SimpleTransitionListener() {
            @Override
            public void onTransitionEnd(Transition transition) {
                endingAction.run();
            }
        });
        TransitionManager.beginDelayedTransition(mSearchBar, transition);
        mViewFader.hideContentOf(mSearchBar);
        TransitionManager.beginDelayedTransition(mContent, new Fade(Fade.OUT));
        mContent.animate().alpha(0).setDuration(250).start();
    }

    private void getIconBeanList() {
        Observable.create(new Observable.OnSubscribe<List<IconBean>>() {
            @Override
            public void call(Subscriber<? super List<IconBean>> subscriber) {
                List<IconBean> list = new ArrayList<>();
                for (String name : ResourceUtil.getStringArray(SearchActivity.this, "icon_pack")) {
                    if (name.startsWith("**")) {
                        continue;
                    }
                    IconBean iconBean = new IconBean(name);
                    int res = getResources().getIdentifier(name, "drawable", getPackageName());
                    if (res != 0) {
                        final int thumbRes =
                                getResources().getIdentifier(name, "drawable", getPackageName());
                        if (thumbRes != 0) {
                            iconBean.setRes(thumbRes);
                        }
                    }
                    list.add(iconBean);
                }
                subscriber.onNext(list);
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<IconBean>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onNext(List<IconBean> iconBeen) {
                        mAdapter.setData(iconBeen);
                        mAdapter.search(mSearchBar.getText());
                    }
                });
    }
}
package org.gnayils.android.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

public class SwipeUpdateLayout extends ViewGroup {

    private static final String LOG_TAG = SwipeUpdateLayout.class.getSimpleName();

    public static final int AT_NOWHERE = -1;
    public static final int AT_TOP = 0;
    public static final int AT_BOTTOM = 1;
    public static final int AT_BOTH = 2;

    private static final float MAX_PROGRESS_ANGLE = .8f;
    private static final int INVALID_POINTER = -1;
    private static final int CIRCLE_VIEW_BG_LIGHT = 0xFFFAFAFA;

    private static final int CIRCLE_VIEW_DIAMETER = 40;
    private int mCircleViewDiameter;

    private static final int TOTAL_DRAG_DISTANCE = 64;
    private int mTotalDragDistance;

    private int mCircleViewCenterVerticalOffset;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private DecelerateInterpolator mDecelerateInterpolator;

    private MaterialProgressDrawable mProgressDrawable;
    private CircleImageView mCircleView;
    private View mContentView;

    private int mTouchSlop;
    private float mAnimateFrom;
    private int mCircleViewIndex = -1;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    private float mInitialDownY;
    private float mInitialMotionY;
    private float mLastMotionY;

    private OnChildVerticalScrollCallback mChildVerticalScrollCallback;

    private OnUpdateListener mListener;
    private boolean mUpdating;
    private int mUpdatingPosition = AT_NOWHERE;
    private boolean mAllowTopUpdate = false;
    private boolean mAllowBottomUpdate = false;
    private boolean mFreezeContentWhileTopUpdate = false;
    private boolean mFreezeContentWhileBottomUpdate = false;

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            float targetTranslationY = 0;
            if (mUpdatingPosition == AT_TOP) {
                targetTranslationY = mAnimateFrom - (int) ((mAnimateFrom - mTotalDragDistance) * interpolatedTime);
            } else if (mUpdatingPosition == AT_BOTTOM) {
                targetTranslationY = mAnimateFrom + (int) ((-mTotalDragDistance - mAnimateFrom) * interpolatedTime);
            }
            moveChildViewVertically(targetTranslationY);
        }
    };

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            float targetTranslationY = mAnimateFrom + (int) ((0 - mAnimateFrom) * interpolatedTime);
            moveChildViewVertically(targetTranslationY);
        }
    };

    private Animation.AnimationListener mAnimateToPositionListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @SuppressLint("NewApi")
        @Override
        public void onAnimationEnd(Animation animation) {
            if (mUpdating) {
                if (!mProgressDrawable.isRunning()) {
                    mProgressDrawable.start();
                }
                if (mListener != null) {
                    mListener.onUpdate(mUpdatingPosition);
                }
            } else {
                mCircleView.clearAnimation();
                mProgressDrawable.stop();
                mUpdatingPosition = AT_NOWHERE;
            }
        }
    };


    public SwipeUpdateLayout(Context context) {
        this(context, null);
    }

    public SwipeUpdateLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleViewDiameter = (int) (CIRCLE_VIEW_DIAMETER * metrics.density);
        mTotalDragDistance = (int) (TOTAL_DRAG_DISTANCE * metrics.density);
        mCircleViewCenterVerticalOffset = mTotalDragDistance / 2 - mCircleViewDiameter / 2;

        mCircleView = new CircleImageView(getContext(), CIRCLE_VIEW_BG_LIGHT);
        mProgressDrawable = new MaterialProgressDrawable(getContext(), mCircleView);
        mProgressDrawable.setBackgroundColor(CIRCLE_VIEW_BG_LIGHT);
        mProgressDrawable.setAlpha(255);
        mCircleView.setImageDrawable(mProgressDrawable);
        addView(mCircleView);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.enabled});
        setEnabled(typedArray.getBoolean(0, true));
        typedArray.recycle();

        typedArray = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.colorPrimary});
        mProgressDrawable.setColorSchemeColors(typedArray.getColor(0, Color.BLACK));
        typedArray.recycle();

        typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeUpdateLayout);
        setFreezeContentAtUpdatePosition(typedArray.getInt(R.styleable.SwipeUpdateLayout_freezeContent, AT_NOWHERE));
        setAllowUpdatePosition(typedArray.getInt(R.styleable.SwipeUpdateLayout_updatePosition, AT_TOP));
        typedArray.recycle();

        setWillNotDraw(false);
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mContentView == null) {
            ensureContentViewExists();
        }
        if (mContentView == null) {
            return;
        }
        mContentView.measure(
                MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY)
        );
        mCircleView.measure(
                MeasureSpec.makeMeasureSpec(mCircleViewDiameter, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleViewDiameter, MeasureSpec.EXACTLY)
        );
        mCircleViewIndex = -1;
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mCircleView) {
                mCircleViewIndex = index;
                break;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mContentView == null) {
            ensureContentViewExists();
        }
        if (mContentView == null) {
            return;
        }
        final View child = mContentView;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        int circleWidth = mCircleView.getMeasuredWidth();
        int circleHeight = mCircleView.getMeasuredHeight();
        mCircleView.layout(width / 2 - circleWidth / 2, t - circleHeight - mCircleViewCenterVerticalOffset,
                width / 2 + circleWidth / 2, t - mCircleViewCenterVerticalOffset);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        ensureContentViewExists();
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mLastMotionY = mInitialDownY = mInitialMotionY = ev.getY(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id");
                }
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                if (!mIsBeingDragged) {
                    startDragging(y);
                }
                mLastMotionY = y;
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }
        return mIsBeingDragged || mUpdating;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        if ((android.os.Build.VERSION.SDK_INT < 21 && mContentView instanceof AbsListView)
                || (mContentView != null && !ViewCompat.isNestedScrollingEnabled(mContentView))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex = -1;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mLastMotionY = mInitialDownY = mInitialMotionY = ev.getY(pointerIndex);
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN:
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index");
                    return false;
                }
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id");
                    return false;
                }
                final float currentMotionY = ev.getY(pointerIndex);
                if (!mIsBeingDragged) {
                    startDragging(currentMotionY);
                } else {
                    dragging(ev, currentMotionY);
                }
                mLastMotionY = currentMotionY;
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }
                if (mIsBeingDragged) {
                    mIsBeingDragged = false;
                    finishDragging();
                }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }
        return true;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            return mCircleViewIndex;
        } else if (i >= mCircleViewIndex) {
            return i + 1;
        } else {
            return i;
        }
    }

    public void updateTop() {
        mUpdating = true;
        ensureCircleViewPosition(AT_TOP);
        animateOffsetToCorrectPosition(mCircleView.getTranslationY());

    }

    public void updateBottom() {
        mUpdating = true;
        ensureCircleViewPosition(AT_BOTTOM);
        animateOffsetToCorrectPosition(mCircleView.getTranslationY());

    }

    public void stopUpdating() {
        mUpdating = false;
        animateOffsetToStartPosition(mCircleView.getTranslationY());
    }

    private boolean canChildScrollUp() {
        if (!mAllowTopUpdate) {
            return true;
        }
        if (mChildVerticalScrollCallback != null) {
            return mChildVerticalScrollCallback.canChildScrollUp(this, mContentView);
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mContentView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mContentView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mContentView, -1) || mContentView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mContentView, -1);
        }
    }

    private boolean canChildScrollDown() {
        if (!mAllowBottomUpdate) {
            return true;
        }
        if (mChildVerticalScrollCallback != null) {
            return mChildVerticalScrollCallback.canChildScrollDown(this, mContentView);
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mContentView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mContentView;
                return absListView.getChildCount() > 0
                        && (absListView.getLastVisiblePosition() < absListView.getAdapter().getCount() - 1
                        || absListView.getChildAt(absListView.getChildCount() - 1).getBottom() > (absListView.getBottom() - absListView.getPaddingBottom()));
            } else {
                if (ViewCompat.canScrollVertically(mContentView, 1)) {
                    return true;
                } else if (mContentView instanceof ViewGroup && ((ViewGroup) mContentView).getChildCount() > 0) {
                    View childView = ((ViewGroup) mContentView).getChildAt(0);
                    return mContentView.getScrollY() + mContentView.getHeight() < childView.getHeight();
                } else {
                    return false;
                }
            }
        } else {
            return ViewCompat.canScrollVertically(mContentView, 1);
        }
    }

    private void ensureContentViewExists() {
        if (mContentView == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mCircleView)) {
                    mContentView = child;
                    break;
                }
            }
        }
    }

    private void ensureCircleViewPosition(int updatingPosition) {
        if (updatingPosition == AT_TOP) {
            mCircleView.layout(getWidth() / 2 - mCircleView.getWidth() / 2, getTop() - mCircleView.getHeight() - mCircleViewCenterVerticalOffset,
                    getWidth() / 2 + mCircleView.getWidth() / 2, getTop() - mCircleViewCenterVerticalOffset);
            mUpdatingPosition = AT_TOP;
        } else if (updatingPosition == AT_BOTTOM) {
            mCircleView.layout(getWidth() / 2 - mCircleView.getWidth() / 2, getBottom() + mCircleViewCenterVerticalOffset,
                    getWidth() / 2 + mCircleView.getWidth() / 2, getBottom() + mCircleView.getHeight() + mCircleViewCenterVerticalOffset);
            mUpdatingPosition = AT_BOTTOM;
        }
    }

    private void startDragging(float currentMotionY) {
        if (!mIsBeingDragged) {
            if (mUpdating) {
                if (currentMotionY - mInitialDownY > mTouchSlop) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsBeingDragged = true;
                } else if (currentMotionY - mInitialDownY < -mTouchSlop) {
                    mInitialMotionY = mInitialDownY - mTouchSlop;
                    mIsBeingDragged = true;
                }
            } else {
                if (currentMotionY - mInitialDownY > mTouchSlop && !canChildScrollUp()) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsBeingDragged = true;
                    ensureCircleViewPosition(AT_TOP);
                } else if (currentMotionY - mInitialDownY < -mTouchSlop && !canChildScrollDown()) {
                    mInitialMotionY = mInitialDownY - mTouchSlop;
                    mIsBeingDragged = true;
                    ensureCircleViewPosition(AT_BOTTOM);
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void dragging(MotionEvent ev, float currentMotionY) {
        float dragLength = currentMotionY - mInitialMotionY;
        float offsetY = currentMotionY - mLastMotionY;
        float dragPercent = Math.min(1f, Math.abs(dragLength) / (mTotalDragDistance * 4f));
        float adjustedOffsetY = offsetY * (1f - dragPercent);
        if (mUpdatingPosition == AT_TOP) {
            if (!canChildScrollUp() && mCircleView.getTranslationY() + adjustedOffsetY > 0) {
                moveChildViewVertically(mCircleView.getTranslationY() + adjustedOffsetY);
            } else if (mCircleView.getTranslationY() == 0) {
                mContentView.dispatchTouchEvent(ev);
            } else if (mCircleView.getTranslationY() + adjustedOffsetY < 0) {
                moveChildViewVertically(0);
                ev.setAction(MotionEvent.ACTION_DOWN);
                mContentView.dispatchTouchEvent(ev);
            }
        } else if (mUpdatingPosition == AT_BOTTOM) {
            if (!canChildScrollDown() && mCircleView.getTranslationY() + adjustedOffsetY < 0) {
                moveChildViewVertically(mCircleView.getTranslationY() + adjustedOffsetY);
            } else if (mCircleView.getTranslationY() == 0) {
                mContentView.dispatchTouchEvent(ev);
            } else if (mCircleView.getTranslationY() + adjustedOffsetY > 0) {
                moveChildViewVertically(0);
                ev.setAction(MotionEvent.ACTION_DOWN);
                mContentView.dispatchTouchEvent(ev);
            }
        }
        float scrollPercent = Math.min(1f, Math.abs(mCircleView.getTranslationY() / (mTotalDragDistance * 4f)));
        if (!mProgressDrawable.isRunning()) {
            mProgressDrawable.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, dragPercent));
            mProgressDrawable.setProgressRotation(scrollPercent);
        }
    }

    private void finishDragging() {
        if (mCircleView.getTranslationY() > mTotalDragDistance) {
            updateTop();
        } else if (mCircleView.getTranslationY() < -mTotalDragDistance) {
            updateBottom();
        } else {
            stopUpdating();
        }
    }

    private void animateOffsetToCorrectPosition(float from) {
        mAnimateFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(300);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        mAnimateToCorrectPosition.setAnimationListener(mAnimateToPositionListener);
        clearAnimation();
        startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(float from) {
        mAnimateFrom = from;
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(300);
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        mAnimateToStartPosition.setAnimationListener(mAnimateToPositionListener);
        clearAnimation();
        startAnimation(mAnimateToStartPosition);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void moveChildViewVertically(float targetTranslationY) {
        mCircleView.setTranslationY(targetTranslationY);
        if (mUpdatingPosition == AT_TOP && !mFreezeContentWhileTopUpdate) {
            mContentView.setTranslationY(targetTranslationY);
        } else if (mUpdatingPosition == AT_BOTTOM && !mFreezeContentWhileBottomUpdate) {
            mContentView.setTranslationY(targetTranslationY);
        }
    }

    public void setAllowUpdatePosition(int allowUpdatePosition) {
        if (allowUpdatePosition == AT_TOP) {
            mAllowTopUpdate = true;
            mAllowBottomUpdate = false;
        } else if (allowUpdatePosition == AT_BOTTOM) {
            mAllowTopUpdate = false;
            mAllowBottomUpdate = true;
        } else if (allowUpdatePosition == AT_BOTH) {
            mAllowTopUpdate = true;
            mAllowBottomUpdate = true;
        }
    }

    public void setFreezeContentAtUpdatePosition(int freezeContentAtUpdatePosition) {
        if (freezeContentAtUpdatePosition == AT_TOP) {
            mFreezeContentWhileTopUpdate = true;
            mFreezeContentWhileBottomUpdate = false;
        } else if (freezeContentAtUpdatePosition == AT_BOTTOM) {
            mFreezeContentWhileTopUpdate = false;
            mFreezeContentWhileBottomUpdate = true;
        } else if (freezeContentAtUpdatePosition == AT_BOTH) {
            mFreezeContentWhileTopUpdate = true;
            mFreezeContentWhileBottomUpdate = true;
        } else if (freezeContentAtUpdatePosition == AT_NOWHERE) {
            mFreezeContentWhileTopUpdate = false;
            mFreezeContentWhileBottomUpdate = false;
        }
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        mListener = listener;
    }

    public void setOnChildScrollCallback(@Nullable OnChildVerticalScrollCallback callback) {
        mChildVerticalScrollCallback = callback;
    }

    public interface OnUpdateListener {

        void onUpdate(int updatingPosition);
    }

    public interface OnChildVerticalScrollCallback {

        boolean canChildScrollUp(SwipeUpdateLayout parent, @Nullable View child);

        boolean canChildScrollDown(SwipeUpdateLayout parent, @Nullable View child);
    }
}

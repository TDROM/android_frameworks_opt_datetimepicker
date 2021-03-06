/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.datetimepicker.R;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.MonthAdapter.CalendarDay;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * A calendar-like view displaying a specified month and the appropriate selectable day numbers
 * within the specified month.
 */
public abstract class MonthView extends View {
    private static final String TAG = "MonthView";
    private static final boolean DEBUG = false;

    /**
     * These params can be passed into the view to control how it appears.
     * {@link #VIEW_PARAMS_WEEK} is the only required field, though the default
     * values are unlikely to fit most layouts correctly.
     */
    /**
     * This sets the height of this week in pixels
     */
    public static final String VIEW_PARAMS_HEIGHT = "height";
    /**
     * This specifies the position (or weeks since the epoch) of this week,
     * calculated using {@link Utils#getWeeksSinceEpochFromJulianDay}
     */
    public static final String VIEW_PARAMS_MONTH = "month";
    /**
     * This specifies the position (or weeks since the epoch) of this week,
     * calculated using {@link Utils#getWeeksSinceEpochFromJulianDay}
     */
    public static final String VIEW_PARAMS_YEAR = "year";
    /**
     * This sets one of the days in this view as selected {@link Time#SUNDAY}
     * through {@link Time#SATURDAY}.
     */
    public static final String VIEW_PARAMS_SELECTED_DAY = "selected_day";
    /**
     * Which day the week should start on. {@link Time#SUNDAY} through
     * {@link Time#SATURDAY}.
     */
    public static final String VIEW_PARAMS_WEEK_START = "week_start";
    /**
     * How many days to display at a time. Days will be displayed starting with
     * {@link #mWeekStart}.
     */
    public static final String VIEW_PARAMS_NUM_DAYS = "num_days";
    /**
     * Which month is currently in focus, as defined by {@link Time#month}
     * [0-11].
     */
    public static final String VIEW_PARAMS_FOCUS_MONTH = "focus_month";
    /**
     * If this month should display week numbers. false if 0, true otherwise.
     */
    public static final String VIEW_PARAMS_SHOW_WK_NUM = "show_wk_num";


    /**
     * Configuration flags to customize the month view's look
     * override parameters through  {@link #customizeViewParameters}
     */
    public static final String CONFIG_EDGE_PADDING = "edge_padding";
    public static final String CONFIG_HEADER_SIZE = "header_size";
    public static final String CONFIG_MONTH_LABEL_TEXT_SIZE = "month_label_text_size";
    public static final String CONFIG_MONTH_DAY_TEXT_SIZE = "month_day_text_size";
    public static final String CONFIG_MONTH_HEADER_LABEL_FLAGS = "month_header_label_flags";
    public static final String CONFIG_MONTH_ROW_HEIGHT = "month_row_height";
    public static final String CONFIG_FILL_PARENT_CONTAINER = "fill_parent_container";
    public static final String CONFIG_HEADER_TITLE_COLOR = "header_title_color";
    public static final String CONFIG_DAY_SELECTED_CIRCLE_COLOR = "day_selected_circle_color";
    public static final String CONFIG_DAY_SELECTED_CIRCLE_ALPHA = "day_selected_circle_alpha";
    public static final String CONFIG_CURRENT_DAY_COLOR = "current_day_color";
    public static final String CONFIG_HEADER_TITLE_OFFSET = "header_title_offset";

    protected static int DEFAULT_HEIGHT = 32;
    protected static int MIN_HEIGHT = 10;
    protected static final int DEFAULT_SELECTED_DAY = -1;
    protected static final int DEFAULT_WEEK_START = Calendar.SUNDAY;
    protected static final int DEFAULT_NUM_DAYS = 7;
    protected static final int DEFAULT_SHOW_WK_NUM = 0;
    protected static final int DEFAULT_FOCUS_MONTH = -1;
    protected static final int DEFAULT_NUM_ROWS = 6;
    protected static final int MAX_NUM_ROWS = 6;

    // used for scaling to the device density
    protected static float mScale = 0;

    private int mSelectedCircleAlpha = 60;
    protected int mDaySeparatorWidth = 1;
    protected int mMiniDayNumberTextSize;
    protected int mMonthLabelTextSize;
    protected int mMonthDayLabelTextSize;
    protected int mMonthHeaderSize;
    protected int mDaySelectedCircleSize;

    protected DatePickerController mController;

    // affects the padding on the sides of this view
    protected int mEdgePadding = 0;

    private String mDayOfWeekTypeface;
    private String mMonthTitleTypeface;

    protected Paint mMonthNumPaint;
    protected Paint mMonthTitlePaint;
    protected Paint mMonthTitleBGPaint;
    protected Paint mSelectedCirclePaint;
    protected Paint mMonthDayLabelPaint;
    protected Paint mLinePaint;

    private final Formatter mFormatter;
    private final StringBuilder mStringBuilder;

    // The Julian day of the first day displayed by this item
    protected int mFirstJulianDay = -1;
    // The month of the first day in this week
    protected int mFirstMonth = -1;
    // The month of the last day in this week
    protected int mLastMonth = -1;

    protected int mMonth;

    protected int mYear;
    // Quick reference to the width of this view, matches parent
    protected int mWidth;
    // The height this view should draw at in pixels, set by height param
    protected int mRowHeight = DEFAULT_HEIGHT;
    // If this view contains the today
    protected boolean mHasToday = false;
    // Which day is selected [0-6] or -1 if no day is selected
    protected int mSelectedDay = -1;
    // Which day is today [0-6] or -1 if no day is today
    protected int mToday = DEFAULT_SELECTED_DAY;
    // Which day of the week to start on [0-6]
    protected int mWeekStart = DEFAULT_WEEK_START;
    // How many days to display
    protected int mNumDays = DEFAULT_NUM_DAYS;
    // The number of days + a spot for week number if it is displayed
    protected int mNumCells = mNumDays;
    // The left edge of the selected day
    protected int mSelectedLeft = -1;
    // The right edge of the selected day
    protected int mSelectedRight = -1;

    private final Calendar mCalendar;
    protected final Calendar mDayLabelCalendar;
    private final MonthViewTouchHelper mTouchHelper;

    protected int mNumRows = DEFAULT_NUM_ROWS;

    // Optional listener for handling day click actions
    protected OnDayClickListener mOnDayClickListener;

    // Whether to prevent setting the accessibility delegate
    private boolean mLockAccessibilityDelegate;

    protected int mDayTextColor;
    protected int mTodayNumberColor;
    protected int mDisabledDayTextColor;
    protected int mMonthTitleColor;
    protected int mMonthTitleBGColor;

    // default format for the header label
    protected int mHeaderDateFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
            | DateUtils.FORMAT_NO_MONTH_DAY;
    protected boolean mShouldFillParent;
    protected int mHeaderTitleColor;
    protected int mSelectedCircleColor;
    protected int mHeaderTitleOffset;

    public MonthView(Context context) {
        this(context, null);
    }

    public MonthView(Context context, AttributeSet attr) {
        super(context, attr);
        Resources res = context.getResources();

        mDayLabelCalendar = Calendar.getInstance();
        mCalendar = Calendar.getInstance();

        mDayOfWeekTypeface = res.getString(R.string.day_of_week_label_typeface);
        mMonthTitleTypeface = res.getString(R.string.sans_serif);

        mDayTextColor = res.getColor(R.color.date_picker_text_normal);
        mHeaderTitleColor = mDayTextColor;
        mTodayNumberColor = res.getColor(R.color.blue);
        mSelectedCircleColor = mTodayNumberColor;
        mDisabledDayTextColor = res.getColor(R.color.date_picker_text_disabled);
        mMonthTitleColor = res.getColor(android.R.color.white);
        mMonthTitleBGColor = res.getColor(R.color.circle_background);

        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        mMiniDayNumberTextSize = res.getDimensionPixelSize(R.dimen.day_number_size);
        mMonthLabelTextSize = res.getDimensionPixelSize(R.dimen.month_label_size);
        mMonthDayLabelTextSize = res.getDimensionPixelSize(R.dimen.month_day_label_text_size);
        mMonthHeaderSize = res.getDimensionPixelOffset(R.dimen.month_list_item_header_height);
        mDaySelectedCircleSize = res
                .getDimensionPixelSize(R.dimen.day_number_select_circle_radius);

        mRowHeight = (res.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height)
                - getMonthHeaderSize()) / MAX_NUM_ROWS;

        // Set up accessibility components.
        mTouchHelper = getMonthViewTouchHelper();
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        mLockAccessibilityDelegate = true;

        // Sets up any standard paints that will be used
        initView();
    }

    public void setDatePickerController(DatePickerController controller) {
        mController = controller;
    }

    protected MonthViewTouchHelper getMonthViewTouchHelper() {
        return new MonthViewTouchHelper(this);
    }

    @Override
    public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
        // Workaround for a JB MR1 issue where accessibility delegates on
        // top-level ListView items are overwritten.
        if (!mLockAccessibilityDelegate) {
            super.setAccessibilityDelegate(delegate);
        }
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        mOnDayClickListener = listener;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // First right-of-refusal goes the touch exploration helper.
        if (mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                final int day = getDayFromLocation(event.getX(), event.getY());
                if (day >= 0) {
                    onDayClick(day);
                }
                break;
        }
        return true;
    }

    /**
     * Sets up the text and style properties for painting. Override this if you
     * want to use a different paint.
     */
    protected void initView() {
        mMonthTitlePaint = new Paint();
        mMonthTitlePaint.setFakeBoldText(true);
        mMonthTitlePaint.setAntiAlias(true);
        mMonthTitlePaint.setTextSize(mMonthLabelTextSize);
        mMonthTitlePaint.setTypeface(Typeface.create(mMonthTitleTypeface, Typeface.BOLD));
        mMonthTitlePaint.setColor(mHeaderTitleColor);
        mMonthTitlePaint.setTextAlign(Align.CENTER);
        mMonthTitlePaint.setStyle(Style.FILL);

        mMonthTitleBGPaint = new Paint();
        mMonthTitleBGPaint.setFakeBoldText(true);
        mMonthTitleBGPaint.setAntiAlias(true);
        mMonthTitleBGPaint.setColor(mMonthTitleBGColor);
        mMonthTitleBGPaint.setTextAlign(Align.CENTER);
        mMonthTitleBGPaint.setStyle(Style.FILL);

        mSelectedCirclePaint = new Paint();
        mSelectedCirclePaint.setFakeBoldText(true);
        mSelectedCirclePaint.setAntiAlias(true);
        mSelectedCirclePaint.setColor(mSelectedCircleColor);
        mSelectedCirclePaint.setTextAlign(Align.CENTER);
        mSelectedCirclePaint.setStyle(Style.FILL);
        mSelectedCirclePaint.setAlpha(mSelectedCircleAlpha);

        mMonthDayLabelPaint = new Paint();
        mMonthDayLabelPaint.setAntiAlias(true);
        mMonthDayLabelPaint.setTextSize(mMonthDayLabelTextSize);
        mMonthDayLabelPaint.setColor(mDayTextColor);
        mMonthDayLabelPaint.setTypeface(Typeface.create(mDayOfWeekTypeface, Typeface.NORMAL));
        mMonthDayLabelPaint.setStyle(Style.FILL);
        mMonthDayLabelPaint.setTextAlign(Align.CENTER);
        mMonthDayLabelPaint.setFakeBoldText(true);

        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStrokeWidth(0);
        mLinePaint.setColor(Color.BLACK);

        mMonthNumPaint = new Paint();
        mMonthNumPaint.setAntiAlias(true);
        mMonthNumPaint.setTextSize(mMiniDayNumberTextSize);
        mMonthNumPaint.setStyle(Style.FILL);
        mMonthNumPaint.setTextAlign(Align.CENTER);
        mMonthNumPaint.setFakeBoldText(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawMonthTitle(canvas);
        drawMonthDayLabels(canvas);
        drawMonthNums(canvas);
    }

    private int mDayOfWeekStart = 0;

    /**
     * Override the default drawing parameters of the view
     *
     * @params is a dictionary of attributes (CONFIG_*) with their corresponding values in
     * pixels
     */
    public void customizeViewParameters(HashMap<String, Integer> params) {
        if (params == null) return;
        for (String key : params.keySet()) {

            int paramValue = params.get(key);
            if (key.equals(CONFIG_MONTH_LABEL_TEXT_SIZE)) {
                mMonthLabelTextSize = paramValue;

            } else if (key.equals(CONFIG_HEADER_SIZE)) {
                mMonthHeaderSize = paramValue;

            } else if (key.equals(CONFIG_MONTH_DAY_TEXT_SIZE)) {
                mMonthDayLabelTextSize = paramValue;
                mMiniDayNumberTextSize = paramValue;

            } else if (key.equals(CONFIG_MONTH_HEADER_LABEL_FLAGS)) {
                mHeaderDateFlags = paramValue;

            } else if (key.equals(CONFIG_MONTH_ROW_HEIGHT)) {
                mRowHeight = paramValue;

            } else if (key.equals(CONFIG_EDGE_PADDING)) {
                mEdgePadding = paramValue;

            } else if (key.equals(CONFIG_FILL_PARENT_CONTAINER)) {
                mShouldFillParent = (paramValue == 1);

            } else if (key.equals(CONFIG_HEADER_TITLE_COLOR)) {
                mHeaderTitleColor = paramValue;

            } else if (key.equals(CONFIG_CURRENT_DAY_COLOR)) {
                mTodayNumberColor = paramValue;

            } else if (key.equals(CONFIG_DAY_SELECTED_CIRCLE_ALPHA)) {
                mSelectedCircleAlpha = paramValue;

            } else if (key.equals(CONFIG_DAY_SELECTED_CIRCLE_COLOR)) {
                mSelectedCircleColor = paramValue;

            } else if (key.equals(CONFIG_HEADER_TITLE_OFFSET)) {
                mHeaderTitleOffset = paramValue;
            }
        }

        // reinitialize paints as some of their properties might have been overridden
        initView();
    }

    /**
     * Sets all the parameters for displaying this week. The only required
     * parameter is the week number. Other parameters have a default value and
     * will only update if a new value is included, except for focus month,
     * which will always default to no focus month if no value is passed in. See
     * {@link #VIEW_PARAMS_HEIGHT} for more info on parameters.
     *
     * @param params A map of the new parameters, see
     *            {@link #VIEW_PARAMS_HEIGHT}
     */
    public void setMonthParams(HashMap<String, Integer> params) {
        if (!params.containsKey(VIEW_PARAMS_MONTH) && !params.containsKey(VIEW_PARAMS_YEAR)) {
            throw new InvalidParameterException("You must specify month and year for this view");
        }
        setTag(params);
        // We keep the current value for any params not present
        if (params.containsKey(VIEW_PARAMS_HEIGHT)) {
            mRowHeight = params.get(VIEW_PARAMS_HEIGHT);
            if (mRowHeight < MIN_HEIGHT) {
                mRowHeight = MIN_HEIGHT;
            }
        }
        if (params.containsKey(VIEW_PARAMS_SELECTED_DAY)) {
            mSelectedDay = params.get(VIEW_PARAMS_SELECTED_DAY);
        }

        // Allocate space for caching the day numbers and focus values
        mMonth = params.get(VIEW_PARAMS_MONTH);
        mYear = params.get(VIEW_PARAMS_YEAR);

        // Figure out what day today is
        final Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        mHasToday = false;
        mToday = -1;

        mCalendar.set(Calendar.MONTH, mMonth);
        mCalendar.set(Calendar.YEAR, mYear);
        mCalendar.set(Calendar.DAY_OF_MONTH, 1);
        mDayOfWeekStart = mCalendar.get(Calendar.DAY_OF_WEEK);

        if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
            mWeekStart = params.get(VIEW_PARAMS_WEEK_START);
        } else {
            mWeekStart = mCalendar.getFirstDayOfWeek();
        }

        mNumCells = Utils.getDaysInMonth(mMonth, mYear);
        for (int i = 0; i < mNumCells; i++) {
            final int day = i + 1;
            if (sameDay(day, today)) {
                mHasToday = true;
                mToday = day;
            }
        }
        mNumRows = calculateNumRows();

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot();
    }

    public void setSelectedDay(int day) {
        mSelectedDay = day;
    }

    public void reuse() {
        mNumRows = DEFAULT_NUM_ROWS;
        requestLayout();
    }

    private int calculateNumRows() {
        int offset = findDayOffset();
        int dividend = (offset + mNumCells) / mNumDays;
        int remainder = (offset + mNumCells) % mNumDays;
        return (dividend + (remainder > 0 ? 1 : 0));
    }

    private boolean sameDay(int day, Time today) {
        return mYear == today.year &&
                mMonth == today.month &&
                day == today.monthDay;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mShouldFillParent) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                    MeasureSpec.getSize(heightMeasureSpec));
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mRowHeight * mNumRows
                    + getMonthHeaderSize());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot();
    }

    public int getMonth() {
        return mMonth;
    }

    public int getYear() {
        return mYear;
    }

    /**
     * A wrapper to the MonthHeaderSize to allow override it in children
     */
    protected int getMonthHeaderSize() {
        return mMonthHeaderSize;
    }

    private String getMonthAndYearString() {
        mStringBuilder.setLength(0);
        long millis = mCalendar.getTimeInMillis();
        return DateUtils.formatDateRange(getContext(), mFormatter, millis, millis, mHeaderDateFlags,
                Time.getCurrentTimezone()).toString();
    }

    protected void drawMonthTitle(Canvas canvas) {
        // the title is centered within the view
        int x = mWidth / 2;
        int y = (getMonthHeaderSize() - mMonthDayLabelTextSize) / 2 + (mMonthLabelTextSize / 3) +
                mHeaderTitleOffset;

        if (DEBUG) {
            canvas.drawLine(0, 0, canvas.getWidth(), 0, mLinePaint);
            canvas.drawLine(x, 0, x, mMonthHeaderSize, mLinePaint);
            canvas.drawLine(0, mMonthHeaderSize, canvas.getWidth(), mMonthHeaderSize, mLinePaint);
        }
        canvas.drawText(getMonthAndYearString(), x, y, mMonthTitlePaint);
    }

    protected void drawMonthDayLabels(Canvas canvas) {
        int y = getMonthHeaderSize() - (mMonthDayLabelTextSize / 2);
        int dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2);

        boolean isRtl = Utils.isRtl();          // current locale's written script direction
        for (int i = 0; i < mNumDays; i++) {
            int calendarDay = (i + mWeekStart) % mNumDays;
            int x = (2 * i + 1) * dayWidthHalf + mEdgePadding;
            mDayLabelCalendar.set(Calendar.DAY_OF_WEEK, calendarDay);
            String dayDisplayName = mDayLabelCalendar.getDisplayName(Calendar.DAY_OF_WEEK,
                    Calendar.SHORT, Locale.getDefault()).toUpperCase(Locale.getDefault());

            String dayLabel = generateDayLabel(dayDisplayName, isRtl);
            canvas.drawText(dayLabel, x, y, mMonthDayLabelPaint);
        }
    }

    /**
     * Draws the week and month day numbers for this week. Override this method
     * if you need different placement.
     *
     * @param canvas The canvas to draw on
     */
    protected void drawMonthNums(Canvas canvas) {
        int y = (((mRowHeight + mMiniDayNumberTextSize) / 2) - mDaySeparatorWidth)
                + getMonthHeaderSize();
        final float dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2.0f);
        int j = findDayOffset();
        for (int dayNumber = 1; dayNumber <= mNumCells; dayNumber++) {
            final int x = (int)((2 * j + 1) * dayWidthHalf + mEdgePadding);

            int yRelativeToDay = (mRowHeight + mMiniDayNumberTextSize) / 2 - mDaySeparatorWidth;

            final int startX = (int)(x - dayWidthHalf);
            final int stopX = (int)(x + dayWidthHalf);
            final int startY = (int)(y - yRelativeToDay);
            final int stopY = (int)(startY + mRowHeight);

            drawMonthDay(canvas, mYear, mMonth, dayNumber, x, y, startX, stopX, startY, stopY);

            j++;
            if (j == mNumDays) {
                j = 0;
                y += mRowHeight;
            }
        }
    }

    /**
     * This method should draw the month day.  Implemented by sub-classes to allow customization.
     *
     * @param canvas  The canvas to draw on
     * @param year  The year of this month day
     * @param month  The month of this month day
     * @param day  The day number of this month day
     * @param x  The default x position to draw the day number
     * @param y  The default y position to draw the day number
     * @param startX  The left boundary of the day number rect
     * @param stopX  The right boundary of the day number rect
     * @param startY  The top boundary of the day number rect
     * @param stopY  The bottom boundary of the day number rect
     */
    public abstract void drawMonthDay(Canvas canvas, int year, int month, int day,
            int x, int y, int startX, int stopX, int startY, int stopY);

    protected int findDayOffset() {
        return (mDayOfWeekStart < mWeekStart ? (mDayOfWeekStart + mNumDays) : mDayOfWeekStart)
                - mWeekStart;
    }


    /**
     * Calculates the day that the given x position is in, accounting for week
     * number. Returns the day or -1 if the position wasn't in a day.
     *
     * @param x The x position of the touch event
     * @return The day number, or -1 if the position wasn't in a day
     */
    public int getDayFromLocation(float x, float y) {
        final int day = getInternalDayFromLocation(x, y);
        if (day < 1 || day > mNumCells) {
            return -1;
        }
        return day;
    }

    /**
     * Calculates the day that the given x position is in, accounting for week
     * number.
     *
     * @param x The x position of the touch event
     * @return The day number
     */
    protected int getInternalDayFromLocation(float x, float y) {
        int dayStart = mEdgePadding;
        if (x < dayStart || x > mWidth - mEdgePadding) {
            return -1;
        }
        // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
        int row = (int) (y - getMonthHeaderSize()) / mRowHeight;
        int column = (int) ((x - dayStart) * mNumDays / (mWidth - dayStart - mEdgePadding));

        int day = column - findDayOffset() + 1;
        day += row * mNumDays;
        return day;
    }

    /**
     * Returns the text center of the nearest Day relative to an input set of coordinates
     *
     * The returned location is the center of day text. This is useful when you are trying to map a
     * click to the nearest day or when implementing snap functionality.
     *
     * @param clickX input x-coordinate
     * @param clickY input y-coordinate
     * @return a Pair of coordinates (x,y) that are the closest to the input, or null if the input
     * isn't valid or inside the header region
     */
    public Pair<Float, Float> mapToNearestDayCoordinates(float clickX, float clickY) {
        // ensure the click maps to a valid 'clickable' region of the view
        if (getDayFromLocation(clickX, clickY) == -1) return null;

        // map the click to a row and column
        int row = (int) (clickY - getMonthHeaderSize()) / mRowHeight;
        int column = (int) ((clickX - mEdgePadding) * mNumDays / (mWidth - 2 * mEdgePadding));

        // calculate the day text center drawn at a particular row and column
        final float dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2.0f);
        float retX = (2 * column + 1) * dayWidthHalf + mEdgePadding;
        int startY = (((mRowHeight + mMiniDayNumberTextSize) / 2) - mDaySeparatorWidth)
                + getMonthHeaderSize();
        float retY = startY + row * mRowHeight;

        return new Pair<Float, Float>(retX, retY);
    }

    /**
     * Called when the user clicks on a day. Handles callbacks to the
     * {@link OnDayClickListener} if one is set.
     * <p/>
     * If the day is out of the range set by minDate and/or maxDate, this is a no-op.
     *
     * @param day The day that was clicked
     */
    private void onDayClick(int day) {
        // If the min / max date are set, only process the click if it's a valid selection.
        if (isOutOfRange(mYear, mMonth, day)) {
            return;
        }


        if (mOnDayClickListener != null) {
            mOnDayClickListener.onDayClick(this, new CalendarDay(mYear, mMonth, day));
        }

        // This is a no-op if accessibility is turned off.
        mTouchHelper.sendEventForVirtualView(day, AccessibilityEvent.TYPE_VIEW_CLICKED);
    }

    /**
     * @return true if the specified year/month/day are within the range set by minDate and maxDate.
     * If one or either have not been set, they are considered as Integer.MIN_VALUE and
     * Integer.MAX_VALUE.
     */
    protected boolean isOutOfRange(int year, int month, int day) {
        if (isBeforeMin(year, month, day)) {
            return true;
        } else if (isAfterMax(year, month, day)) {
            return true;
        }

        return false;
    }

    private boolean isBeforeMin(int year, int month, int day) {
        if (mController == null) {
            return false;
        }
        Calendar minDate = mController.getMinDate();
        if (minDate == null) {
            return false;
        }

        if (year < minDate.get(Calendar.YEAR)) {
            return true;
        } else if (year > minDate.get(Calendar.YEAR)) {
            return false;
        }

        if (month < minDate.get(Calendar.MONTH)) {
            return true;
        } else if (month > minDate.get(Calendar.MONTH)) {
            return false;
        }

        if (day < minDate.get(Calendar.DAY_OF_MONTH)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAfterMax(int year, int month, int day) {
        if (mController == null) {
            return false;
        }
        Calendar maxDate = mController.getMaxDate();
        if (maxDate == null) {
            return false;
        }

        if (year > maxDate.get(Calendar.YEAR)) {
            return true;
        } else if (year < maxDate.get(Calendar.YEAR)) {
            return false;
        }

        if (month > maxDate.get(Calendar.MONTH)) {
            return true;
        } else if (month < maxDate.get(Calendar.MONTH)) {
            return false;
        }

        if (day > maxDate.get(Calendar.DAY_OF_MONTH)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return The date that has accessibility focus, or {@code null} if no date
     *         has focus
     */
    public CalendarDay getAccessibilityFocus() {
        final int day = mTouchHelper.getFocusedVirtualView();
        if (day >= 0) {
            return new CalendarDay(mYear, mMonth, day);
        }
        return null;
    }

    /**
     * Clears accessibility focus within the view. No-op if the view does not
     * contain accessibility focus.
     */
    public void clearAccessibilityFocus() {
        mTouchHelper.clearFocusedVirtualView();
    }

    /**
     * Attempts to restore accessibility focus to the specified date.
     *
     * @param day The date which should receive focus
     * @return {@code false} if the date is not valid for this month view, or
     *         {@code true} if the date received focus
     */
    public boolean restoreAccessibilityFocus(CalendarDay day) {
        if ((day.year != mYear) || (day.month != mMonth) || (day.day > mNumCells)) {
            return false;
        }
        mTouchHelper.setFocusedVirtualView(day.day);
        return true;
    }

    /**
     * Provides a virtual view hierarchy for interfacing with an accessibility
     * service.
     */
    protected class MonthViewTouchHelper extends ExploreByTouchHelper {
        private static final String DATE_FORMAT = "dd MMMM yyyy";

        private final Rect mTempRect = new Rect();
        private final Calendar mTempCalendar = Calendar.getInstance();

        public MonthViewTouchHelper(View host) {
            super(host);
        }

        public void setFocusedVirtualView(int virtualViewId) {
            getAccessibilityNodeProvider(MonthView.this).performAction(
                    virtualViewId, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null);
        }

        public void clearFocusedVirtualView() {
            final int focusedVirtualView = getFocusedVirtualView();
            if (focusedVirtualView != ExploreByTouchHelper.INVALID_ID) {
                getAccessibilityNodeProvider(MonthView.this).performAction(
                        focusedVirtualView,
                        AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
                        null);
            }
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            final int day = getDayFromLocation(x, y);
            if (day >= 0) {
                return day;
            }
            return ExploreByTouchHelper.INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int day = 1; day <= mNumCells; day++) {
                virtualViewIds.add(day);
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            event.setContentDescription(getItemDescription(virtualViewId));
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId,
                AccessibilityNodeInfoCompat node) {
            getItemBounds(virtualViewId, mTempRect);

            node.setContentDescription(getItemDescription(virtualViewId));
            node.setBoundsInParent(mTempRect);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);

            if (virtualViewId == mSelectedDay) {
                node.setSelected(true);
            }

        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action,
                Bundle arguments) {
            switch (action) {
                case AccessibilityNodeInfo.ACTION_CLICK:
                    onDayClick(virtualViewId);
                    return true;
            }

            return false;
        }

        /**
         * Calculates the bounding rectangle of a given time object.
         *
         * @param day The day to calculate bounds for
         * @param rect The rectangle in which to store the bounds
         */
        protected void getItemBounds(int day, Rect rect) {
            final int offsetX = mEdgePadding;
            final int offsetY = getMonthHeaderSize();
            final int cellHeight = mRowHeight;
            final int cellWidth = ((mWidth - (2 * mEdgePadding)) / mNumDays);
            final int index = ((day - 1) + findDayOffset());
            final int row = (index / mNumDays);
            final int column = (index % mNumDays);
            final int x = (offsetX + (column * cellWidth));
            final int y = (offsetY + (row * cellHeight));

            rect.set(x, y, (x + cellWidth), (y + cellHeight));
        }

        /**
         * Generates a description for a given time object. Since this
         * description will be spoken, the components are ordered by descending
         * specificity as DAY MONTH YEAR.
         *
         * @param day The day to generate a description for
         * @return A description of the time object
         */
        protected CharSequence getItemDescription(int day) {
            mTempCalendar.set(mYear, mMonth, day);
            final CharSequence date = DateFormat.format(DATE_FORMAT,
                    mTempCalendar.getTimeInMillis());

            if (day == mSelectedDay) {
                return getContext().getString(R.string.item_is_selected, date);
            }

            return date;
        }
    }

    /**
     * Handles callbacks when the user clicks on a time object.
     */
    public interface OnDayClickListener {
        public void onDayClick(MonthView view, CalendarDay day);
    }

    /**
     * Formats a given Day's display name into a single character label
     *
     * @param input display name of the day (Saturday, Monday .. etc)
     * @param isRtl true if the input's written script is right-to-left
     * @return
     */
    private String generateDayLabel(String input, boolean isRtl) {
        Locale locale = Locale.getDefault();
        // 'special-case'-ing Chinese languages as their Day labels are written right-to-left
        if (isRtl || locale.equals(Locale.SIMPLIFIED_CHINESE) ||
                locale.equals(Locale.TRADITIONAL_CHINESE) ||
                locale.equals(new Locale("zh", "HK"))) {
            return input.substring(input.length()-1);
        } else {
            return input.substring(0,1);
        }
    }
}

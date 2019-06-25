package com.lishang.checkin;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Observable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;

import com.lishang.library.R;


/**
 * 签到进度
 */
public class CheckInProgress extends View {
    private final static String TAG = "CheckInProgress";

    private int textDateSize = 12; //日期文字大小sp
    private int textDateColor = 0xff8e8e8e;
    private int radius = 9;// 半径 dp
    private int circleColor = 0xffFF4441;
    private int lineHeight = 1;//背景线的高度
    private int lineColor = 0xfffF4241;
    private int textScoreSize = 12;
    private int textScoreColor = 0xff8e8e8e;
    private Bitmap checkIn;
    private int circleMargin = 10; //圆与日期间隔
    private Paint.Style circleStyle = Paint.Style.FILL;
    private int circleStrokeWidth = 1;
    private int circleStrokeColor = 0xffffffff;
    private boolean checkInProgressShow = false; //是否显示签到进度
    private int checkInProgressColor = lineColor;
    private int checkInColor = checkInProgressColor;
    private int checkInHookColor = lineColor;
    private int checkInHookSize = circleStrokeWidth;
    private boolean checkInLeakShow = false; //是否显示漏签


    private Paint datePaint; //日期画笔
    private Paint scorePaint;//分数画笔
    private Paint linePaint;//线画笔

    private Align align = Align.TOP; //对齐方式
    private int verticalHeight; //从日期到积分垂直高度;


    private SparseArray<Point> datePointPool = new SparseArray<>(); //记录日期位置
    private SparseArray<Point> scorePointPool = new SparseArray<>(); //记录分数位置
    private SparseArray<Point> circlePointPool = new SparseArray<>(); //记录圆位置
    private Adapter adapter;

    private Point downPoint = new Point();
    private OnClickCheckInListener listener;
    private CheckInProgressDataObserver mObserver;

    enum Align {
        TOP, CENTER, BOTTOM
    }


    public CheckInProgress(Context context) {
        this(context, null);
    }

    public CheckInProgress(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckInProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CheckInProgress);
        int align = array.getInt(R.styleable.CheckInProgress_align, 1);
        if (align == 3) {
            this.align = Align.BOTTOM;
        } else if (align == 2) {
            this.align = Align.CENTER;
        } else {
            this.align = Align.TOP;
        }
        textDateSize = array.getDimensionPixelOffset(R.styleable.CheckInProgress_text_date_size, textDateSize);
        textDateColor = array.getColor(R.styleable.CheckInProgress_text_date_color, textDateColor);
        radius = array.getDimensionPixelOffset(R.styleable.CheckInProgress_radius, radius);
        circleColor = array.getColor(R.styleable.CheckInProgress_circle_color, circleColor);
        lineHeight = array.getDimensionPixelOffset(R.styleable.CheckInProgress_line_height, lineHeight);
        lineColor = array.getColor(R.styleable.CheckInProgress_line_color, lineColor);
        textScoreSize = array.getDimensionPixelOffset(R.styleable.CheckInProgress_text_score_size, textScoreSize);
        textScoreColor = array.getColor(R.styleable.CheckInProgress_text_score_color, textScoreColor);
        int resId = array.getResourceId(R.styleable.CheckInProgress_check_in_bitmap, -1);
        if (resId != -1) {
            checkIn = BitmapFactory.decodeResource(getResources(), resId);
        }
        circleMargin = array.getDimensionPixelOffset(R.styleable.CheckInProgress_circle_margin, circleMargin);
        circleStrokeWidth = array.getDimensionPixelOffset(R.styleable.CheckInProgress_circle_stroke_width, circleStrokeWidth);
        circleStrokeColor = array.getColor(R.styleable.CheckInProgress_circle_stroke_color, circleStrokeColor);
        int style = array.getInt(R.styleable.CheckInProgress_circle_style, 0);
        switch (style) {
            case 1:
                circleStyle = Paint.Style.STROKE;
                break;
            default:
                circleStyle = Paint.Style.FILL;
                break;
        }

        checkInProgressShow = array.getBoolean(R.styleable.CheckInProgress_check_in_progress_show, checkInProgressShow);
        checkInProgressColor = array.getColor(R.styleable.CheckInProgress_check_in_progress_color, checkInProgressColor);
        checkInColor = array.getColor(R.styleable.CheckInProgress_check_in_color, checkInColor);
        checkInHookColor = array.getColor(R.styleable.CheckInProgress_check_in_hook_color, checkInHookColor);
        checkInHookSize = array.getDimensionPixelOffset(R.styleable.CheckInProgress_check_in_hook_size, checkInHookSize);
        checkInLeakShow = array.getBoolean(R.styleable.CheckInProgress_check_in_leak_show, checkInLeakShow);

        array.recycle();
        init();
    }

    private void init() {
        mObserver = new CheckInProgressDataObserver();

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStrokeWidth((lineHeight));
        linePaint.setColor(lineColor);

        scorePaint = new Paint();
        scorePaint.setAntiAlias(true);

        datePaint = new Paint();
        datePaint.setAntiAlias(true);
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTextSize((textDateSize));

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //计算元素位置
        onCalculation();

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        if (heightMode == MeasureSpec.AT_MOST && verticalHeight != 0) {
            heightSize = verticalHeight;
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    /**
     * 先计算好画布上每个元素的位置
     */
    private void onCalculation() {
        if (adapter == null) return;
        calculationDate();
        calculationScore();

        //元素垂直高度
        int total = datePointPool.get(0).y - getPaddingTop(); //日期的高度
        total += (circleMargin); // + 间距
        total += (radius) * 2; //+积分圆的直径
        verticalHeight = total;

    }

    /**
     * 日期元素位置
     */
    private void calculationDate() {
        int left = getPaddingLeft();
        int right = getPaddingRight();
        int top = getPaddingTop();

        int width = getMeasuredWidth() - left - right;


        int margin = width / (adapter.size());

        //日期位置
        int cy = 0;
        for (int i = 0; i < adapter.size(); i++) {

            String str = adapter.getDateText(i);
            Rect rect = new Rect();
            datePaint.getTextBounds(str, 0, str.length(), rect);
            int y = top + rect.height();
            if (cy < y) {
                cy = y;
            }
        }

        for (int i = 0; i < adapter.size(); i++) {
            int cx = left + margin / 2 + i * margin;
            Point point = new Point(cx, cy);
            datePointPool.put(i, point);
        }
    }

    /**
     * 积分元素位置
     */
    private void calculationScore() {

        int radiusPx = (radius);
        int left = datePointPool.get(0).x;
        int right = datePointPool.get(datePointPool.size() - 1).x;
        int top = datePointPool.get(0).y;

        int width = right - left;
        int cy = top + radiusPx + (circleMargin);

        int margin = width / (adapter.size() - 1);
        for (int i = 0; i < adapter.size(); i++) {

            int cx = left + i * margin;
            Point p = new Point(cx, cy);
            circlePointPool.put(i, p);

            scorePaint.setTextSize((textScoreSize));
            scorePaint.setStyle(Paint.Style.FILL);
            scorePaint.setColor(textScoreColor);
            scorePaint.setTextAlign(Paint.Align.CENTER);
            String str = "+" + adapter.getScoreText(i);
            if (adapter.isLeakCheckIn(i) && checkInLeakShow) {
                str = "补";
            }
            Rect rect = new Rect();
            scorePaint.getTextBounds(str, 0, str.length(), rect);

            Paint.FontMetricsInt fontMetrics = scorePaint.getFontMetricsInt();

            Point point = new Point(p.x, p.y + (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent);
            scorePointPool.put(i, point);
        }

    }

    private int calculationAlign() {
        int margin = 0;
        if (align == Align.CENTER) {
            margin = (getMeasuredHeight() - verticalHeight) / 2 - getPaddingTop();
        } else if (align == Align.BOTTOM) {
            margin = (getMeasuredHeight() - verticalHeight) - getPaddingTop();
        }
        return margin;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawDate(canvas);
        drawBgLine(canvas);
        drawScore(canvas);


    }

    /**
     * 画日期
     *
     * @param canvas
     */
    private void drawDate(Canvas canvas) {

        int margin = calculationAlign();
        if (datePointPool.size() != 0) {

            for (int i = 0; i < adapter.size(); i++) {

                String str = adapter.getDateText(i);

                datePaint.setColor(textDateColor);

                Point point = datePointPool.get(i);

                canvas.drawText(str, point.x, point.y + margin, datePaint);

            }

        }
    }

    private void drawBgLine(Canvas canvas) {
        int margin = calculationAlign();

        int startX = datePointPool.get(0).x;
        int startY = datePointPool.get(0).y + (radius) + (circleMargin) + margin;
        int stopX = datePointPool.get(datePointPool.size() - 1).x;
        int stopY = startY;
        canvas.drawLine(startX, startY, stopX, stopY, linePaint);


    }

    private void drawScore(Canvas canvas) {
        int radiusPx = (radius);
        int margin = calculationAlign();

        for (int i = 0; i < adapter.size(); i++) {
            Point p = circlePointPool.get(i);


            if (adapter.isCheckIn(i)) {

                if (checkInProgressShow && i + 1 < adapter.size()) {
                    //进度
                    scorePaint.setStyle(Paint.Style.FILL);
                    scorePaint.setColor(checkInProgressColor);
                    scorePaint.setStrokeWidth((lineHeight));

                    Point p1 = circlePointPool.get(i + 1);
                    canvas.drawLine(p.x, p.y + margin, p1.x, p1.y + margin, scorePaint);
                }

                if (checkIn != null) {
                    float scale = radiusPx * 2.0f / checkIn.getWidth();
                    Matrix matrix = new Matrix();
                    matrix.postScale(scale, scale);
                    canvas.save();
                    canvas.translate(p.x - radiusPx, p.y + margin - radiusPx);
                    canvas.drawBitmap(checkIn, matrix, scorePaint);
                    canvas.restore();
                } else {
                    scorePaint.setColor(checkInColor);
                    scorePaint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(p.x, p.y + margin, radiusPx, scorePaint);

                    //画勾
                    scorePaint.setStyle(Paint.Style.FILL);
                    scorePaint.setColor(checkInHookColor);
                    scorePaint.setStrokeWidth((checkInHookSize));
                    int startX = p.x - radiusPx / 4 * 3;
                    int startY = p.y + margin;
                    int stopX = p.x - radiusPx / 4;
                    int stopY = p.y + margin + radiusPx / 2;
                    canvas.drawLine(startX, startY, stopX, stopY, scorePaint);

                    startX = stopX;
                    startY = stopY;
                    stopX = p.x + radiusPx / 4 * 3;
                    stopY = p.y + margin - radiusPx / 2;
                    canvas.drawLine(startX, startY, stopX, stopY, scorePaint);

                    canvas.drawCircle(startX, startY, checkInHookSize / 2.0f, scorePaint);
                }


            } else {
                scorePaint.setStyle(Paint.Style.FILL);
                scorePaint.setColor(circleColor);
                canvas.drawCircle(p.x, p.y + margin, radiusPx, scorePaint);

                if (circleStyle == Paint.Style.STROKE) {
                    scorePaint.setColor(circleStrokeColor);
                    scorePaint.setStrokeWidth((circleStrokeWidth));
                    scorePaint.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(p.x, p.y + margin, radiusPx, scorePaint);
                }

                scorePaint.setTextSize((textScoreSize));
                scorePaint.setStyle(Paint.Style.FILL);
                scorePaint.setColor(textScoreColor);
                scorePaint.setTextAlign(Paint.Align.CENTER);
                String str = "+" + adapter.getScoreText(i);
                if (adapter.isLeakCheckIn(i) && checkInLeakShow) {
                    str = "补";
                }

                Point point = scorePointPool.get(i);

                canvas.drawText(str, point.x, point.y + margin, scorePaint);
            }

        }
    }

    public void setAdapter(Adapter adapter) {
        if (this.adapter != null) {
            adapter.unregisterAdapterDataObserver(mObserver);
        }
        if (adapter != null) {
            this.adapter = adapter;
            adapter.registerAdapterDataObserver(mObserver);
            requestLayout();
        }
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setOnClickCheckInListener(OnClickCheckInListener listener) {
        this.listener = listener;
    }

    public int getTextDateSize() {
        return textDateSize;
    }

    /**
     * 设置日期字体大小
     *
     * @param textDateSize 单位sp
     */
    public void setTextDateSize(int textDateSize) {
        this.textDateSize = sp2px(textDateSize);
    }

    public int getTextDateColor() {
        return textDateColor;
    }

    /**
     * 日期字体颜色
     *
     * @param textDateColor
     */
    public void setTextDateColor(int textDateColor) {
        this.textDateColor = textDateColor;
    }

    public int getRadius() {
        return radius;
    }

    /**
     * 签到圆的半径
     *
     * @param radius dp
     */
    public void setRadius(int radius) {
        this.radius = dp2px(radius);
    }

    public int getCircleColor() {
        return circleColor;
    }

    /**
     * 签到圆的背景色
     *
     * @param circleColor
     */
    public void setCircleColor(int circleColor) {
        this.circleColor = circleColor;
    }

    public int getLineHeight() {
        return lineHeight;
    }

    /**
     * 连接线高度 单位dp
     *
     * @param lineHeight
     */
    public void setLineHeight(int lineHeight) {
        this.lineHeight = dp2px(lineHeight);
    }

    public int getLineColor() {
        return lineColor;
    }

    /**
     * 连接线颜色
     *
     * @param lineColor
     */
    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public int getTextScoreSize() {
        return textScoreSize;
    }

    /**
     * 设置签到积分字体大小
     *
     * @param textScoreSize
     */
    public void setTextScoreSize(int textScoreSize) {
        this.textScoreSize = sp2px(textScoreSize);
    }

    public int getTextScoreColor() {
        return textScoreColor;
    }

    /**
     * 设置签到积分颜色
     *
     * @param textScoreColor
     */
    public void setTextScoreColor(int textScoreColor) {
        this.textScoreColor = textScoreColor;
    }

    public Bitmap getCheckIn() {
        return checkIn;
    }

    /**
     * 设置签到图片
     *
     * @param checkIn
     */
    public void setCheckIn(Bitmap checkIn) {
        this.checkIn = checkIn;
    }

    public int getCircleMargin() {
        return circleMargin;
    }

    /**
     * 设置签到圆与日期的间距
     *
     * @param circleMargin
     */
    public void setCircleMargin(int circleMargin) {
        this.circleMargin = dp2px(circleMargin);
    }

    public Paint.Style getCircleStyle() {
        return circleStyle;
    }

    /**
     * 未签到圆的样式
     *
     * @param circleStyle
     */
    public void setCircleStyle(Paint.Style circleStyle) {
        this.circleStyle = circleStyle;
    }

    public int getCircleStrokeWidth() {
        return circleStrokeWidth;
    }

    /**
     * 未签到圆描边大小
     *
     * @param circleStrokeWidth
     */
    public void setCircleStrokeWidth(int circleStrokeWidth) {
        this.circleStrokeWidth = dp2px(circleStrokeWidth);
    }

    public int getCircleStrokeColor() {
        return circleStrokeColor;
    }

    /**
     * 未签到圆描边颜色
     *
     * @param circleStrokeColor
     */
    public void setCircleStrokeColor(int circleStrokeColor) {
        this.circleStrokeColor = circleStrokeColor;
    }

    public boolean isCheckInProgressShow() {
        return checkInProgressShow;
    }

    /**
     * 是否显示签到进度
     *
     * @param checkInProgressShow
     */
    public void setCheckInProgressShow(boolean checkInProgressShow) {
        this.checkInProgressShow = checkInProgressShow;
    }

    public int getCheckInProgressColor() {
        return checkInProgressColor;
    }

    /**
     * 签到进度的颜色
     *
     * @param checkInProgressColor
     */
    public void setCheckInProgressColor(int checkInProgressColor) {
        this.checkInProgressColor = checkInProgressColor;
    }

    public int getCheckInColor() {
        return checkInColor;
    }

    /**
     * 未设置敲到图片时，签到背景的颜色
     *
     * @param checkInColor
     */
    public void setCheckInColor(int checkInColor) {
        this.checkInColor = checkInColor;
    }

    public int getCheckInHookColor() {
        return checkInHookColor;
    }

    /**
     * 未设置敲到图片时，签到中线勾的颜色
     *
     * @param checkInHookColor
     */
    public void setCheckInHookColor(int checkInHookColor) {
        this.checkInHookColor = checkInHookColor;
    }

    public int getCheckInHookSize() {
        return checkInHookSize;
    }

    /**
     * 未设置敲到图片时，签到中线勾的大小
     *
     * @param checkInHookSize
     */
    public void setCheckInHookSize(int checkInHookSize) {
        this.checkInHookSize = dp2px(checkInHookSize);
    }

    public boolean isCheckInLeakShow() {
        return checkInLeakShow;
    }

    /**
     * 是否显示补签
     *
     * @param checkInLeakShow
     */
    public void setCheckInLeakShow(boolean checkInLeakShow) {
        this.checkInLeakShow = checkInLeakShow;
    }


    public Align getAlign() {
        return align;
    }

    /**
     * 位置
     *
     * @param align
     */
    public void setAlign(Align align) {
        this.align = align;
    }

    /**
     * dp转像素
     *
     * @param size
     * @return
     */
    private int dp2px(int size) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, getResources().getDisplayMetrics());
    }

    /**
     * sp转像素
     *
     * @param size
     * @return
     */
    private int sp2px(int size) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getResources().getDisplayMetrics());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (listener == null) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downPoint.x = (int) event.getX();
            downPoint.y = (int) event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            int margin = calculationAlign();
            for (int i = 0; i < circlePointPool.size(); i++) {
                Point p = circlePointPool.get(i);
                Rect rect = new Rect(p.x - radius, p.y + margin - radius, p.x + radius, p.y + margin + radius);
                if (rect.contains(downPoint.x, downPoint.y) && rect.contains(x, y)) {
                    listener.OnClick(i);
                    break;
                }
            }
        }

        return true;
    }

    static class AdapterDataObservable extends Observable<AdapterDataObserver> {

        public void notifyChanged() {
            Log.d(TAG, "notifyChanged observable changed");
            for (int i = this.mObservers.size() - 1; i >= 0; --i) {
                (this.mObservers.get(i)).onChanged();
            }
        }
    }


    private abstract class AdapterDataObserver {
        public void onChanged() {
        }
    }

    private class CheckInProgressDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            Log.d(TAG, "CheckInProgressDataObserver changed");
            CheckInProgress that = CheckInProgress.this;

            that.requestLayout();

            that.postInvalidate();
        }
    }

    public static abstract class Adapter {
        private final AdapterDataObservable mObservable = new AdapterDataObservable();

        public abstract String getDateText(int position);

        public abstract String getScoreText(int position);

        public abstract boolean isCheckIn(int position);

        public abstract int size();

        public boolean isLeakCheckIn(int position) {
            return false;
        }

        private void registerAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            Log.d(TAG, "register adapter observer");
            this.mObservable.registerObserver(observer);
        }

        private void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            Log.d(TAG, "unregister adapter observer");
            this.mObservable.unregisterObserver(observer);
        }

        public void notifyDataSetChanged() {
            Log.d(TAG, "notifyDataSetChanged observable changed");
            this.mObservable.notifyChanged();
        }

    }

}

package com.lishang.checkin.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;

import com.lishang.checkin.R;


/**
 * 签到进度
 */
public class CheckInProgress extends View {
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
    private int circleStorkeWidth = 1;
    private Paint datePaint; //日期画笔
    private Paint scorePaint;//分数画笔
    private Paint linePaint;//线画笔

    private Align align = Align.TOP; //对齐方式
    private int verticalHeight; //从日期到积分垂直高度;


    private SparseArray<Point> datePointPool = new SparseArray<>(); //记录日期位置
    private SparseArray<Point> scorePointPool = new SparseArray<>(); //记录分数位置
    private SparseArray<Point> circlePointPool = new SparseArray<>(); //记录圆位置
    private CheckInAdapter adapter;

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
        circleStorkeWidth = array.getDimensionPixelOffset(R.styleable.CheckInProgress_circle_stroke_width, circleStorkeWidth);


        array.recycle();
        init();
    }

    private void init() {

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

            Rect rect = new Rect();
            scorePaint.getTextBounds(str, 0, str.length(), rect);
            Point point = new Point(p.x, p.y + rect.height() / 2);
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
                if (checkIn != null) {
                    float scale = radiusPx * 2.0f / checkIn.getWidth();
                    Matrix matrix = new Matrix();
                    matrix.postScale(scale, scale);
                    canvas.save();
                    canvas.translate(p.x - radiusPx, p.y + margin - radiusPx);
                    canvas.drawBitmap(checkIn, matrix, scorePaint);
                    canvas.restore();
                } else {
                    scorePaint.setColor(circleColor);
                    scorePaint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(p.x, p.y + margin, radiusPx, scorePaint);

                    //画勾
                    scorePaint.setStyle(Paint.Style.FILL);
                    scorePaint.setColor(Color.WHITE);
                    scorePaint.setStrokeWidth((circleStorkeWidth));
                    int startX = p.x - radiusPx / 4 * 3;
                    int startY = p.y;
                    int stopX = p.x - radiusPx / 4;
                    int stopY = p.y + radiusPx / 2;
                    canvas.drawLine(startX, startY, stopX, stopY, scorePaint);

                    startX = stopX;
                    startY = stopY;
                    stopX = p.x + radiusPx / 4 * 3;
                    stopY = p.y - radiusPx / 2;
                    canvas.drawLine(startX, startY, stopX, stopY, scorePaint);

                }


            } else {
                scorePaint.setStyle(Paint.Style.FILL);
                scorePaint.setColor(Color.WHITE);
                canvas.drawCircle(p.x, p.y + margin, radiusPx, scorePaint);

                scorePaint.setColor(circleColor);
                scorePaint.setStrokeWidth((circleStorkeWidth));
                scorePaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(p.x, p.y + margin, radiusPx, scorePaint);

                scorePaint.setTextSize((textScoreSize));
                scorePaint.setStyle(Paint.Style.FILL);
                scorePaint.setColor(textScoreColor);
                scorePaint.setTextAlign(Paint.Align.CENTER);
                String str = "+" + adapter.getScoreText(i);

                Point point = scorePointPool.get(i);

                canvas.drawText(str, point.x, point.y + margin, scorePaint);
            }

        }
    }

    public void setAdapter(CheckInAdapter adapter) {
        if (adapter != null) {
            this.adapter = adapter;
            requestLayout();
        }
    }



    public abstract static class CheckInAdapter {
        public abstract String getDateText(int position);

        public abstract String getScoreText(int position);

        public abstract boolean isCheckIn(int position);

        public abstract int size();
    }

}

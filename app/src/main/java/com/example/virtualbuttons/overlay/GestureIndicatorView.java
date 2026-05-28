package com.example.virtualbuttons.overlay;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class GestureIndicatorView extends View {
    private final Paint linePaint;
    private final Paint arrowPaint;
    private final Paint glowPaint;
    private final Path arrowPath;
    private ValueAnimator animator;
    private float animationPhase;
    private float indicatorAlpha;
    private int edge;
    private boolean isShowing;
    private final Handler handler;
    private final Runnable hideRunnable;

    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;
    public static final int EDGE_TOP = 4;
    public static final int EDGE_BOTTOM = 8;

    public GestureIndicatorView(Context context) {
        super(context);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setAlpha(100);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setAlpha(160);
        arrowPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        arrowPath = new Path();
        handler = new Handler(Looper.getMainLooper());
        hideRunnable = () -> hide();
    }

    public void show(int edge) {
        this.edge = edge;
        isShowing = true;
        indicatorAlpha = 1f;

        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
        animator.setDuration(1200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            animationPhase = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();

        setAlpha(1f);
        setVisibility(VISIBLE);
        handler.removeCallbacks(hideRunnable);
        handler.postDelayed(hideRunnable, 2000);
    }

    public void hide() {
        if (!isShowing) return;
        isShowing = false;
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        animate().alpha(0f).setDuration(200).withEndAction(() -> {
            setVisibility(GONE);
            indicatorAlpha = 0f;
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isShowing && indicatorAlpha < 0.01f) return;

        float w = getWidth();
        float h = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float cx = w / 2f;
        float cy = h / 2f;

        float pulse = (float) (Math.sin(animationPhase) * 0.3f + 0.7f);
        float alpha = indicatorAlpha * 180 * pulse;

        linePaint.setAlpha((int) alpha);
        arrowPaint.setAlpha((int) (alpha * 1.5f));

        float lineLen = 120 * density;
        float arrowSize = 12 * density;

        if ((edge & EDGE_LEFT) != 0) {
            float x = 8 * density;
            glowPaint.setShader(null);
            glowPaint.setColor(Color.rgb(103, 80, 164));
            glowPaint.setAlpha((int) (alpha * 0.15f));
            canvas.drawCircle(x, cy, 40 * density * pulse, glowPaint);

            canvas.drawLine(x, cy - lineLen / 2f, x, cy + lineLen / 2f, linePaint);
            arrowPath.reset();
            arrowPath.moveTo(x, cy - arrowSize * 2);
            arrowPath.lineTo(x - arrowSize, cy);
            arrowPath.lineTo(x, cy + arrowSize * 2);
            canvas.drawPath(arrowPath, arrowPaint);
        }

        if ((edge & EDGE_RIGHT) != 0) {
            float x = w - 8 * density;
            glowPaint.setShader(null);
            glowPaint.setColor(Color.rgb(103, 80, 164));
            glowPaint.setAlpha((int) (alpha * 0.15f));
            canvas.drawCircle(x, cy, 40 * density * pulse, glowPaint);

            canvas.drawLine(x, cy - lineLen / 2f, x, cy + lineLen / 2f, linePaint);
            arrowPath.reset();
            arrowPath.moveTo(x, cy - arrowSize * 2);
            arrowPath.lineTo(x + arrowSize, cy);
            arrowPath.lineTo(x, cy + arrowSize * 2);
            canvas.drawPath(arrowPath, arrowPaint);
        }

        if ((edge & EDGE_TOP) != 0) {
            float y = 8 * density;
            glowPaint.setColor(Color.rgb(103, 80, 164));
            glowPaint.setAlpha((int) (alpha * 0.15f));
            canvas.drawCircle(cx, y, 40 * density * pulse, glowPaint);

            canvas.drawLine(cx - lineLen / 2f, y, cx + lineLen / 2f, y, linePaint);
            arrowPath.reset();
            arrowPath.moveTo(cx - arrowSize * 2, y);
            arrowPath.lineTo(cx, y - arrowSize);
            arrowPath.lineTo(cx + arrowSize * 2, y);
            canvas.drawPath(arrowPath, arrowPaint);
        }

        if ((edge & EDGE_BOTTOM) != 0) {
            float y = h - 8 * density;
            glowPaint.setColor(Color.rgb(103, 80, 164));
            glowPaint.setAlpha((int) (alpha * 0.15f));
            canvas.drawCircle(cx, y, 40 * density * pulse, glowPaint);

            canvas.drawLine(cx - lineLen / 2f, y, cx + lineLen / 2f, y, linePaint);
            arrowPath.reset();
            arrowPath.moveTo(cx - arrowSize * 2, y);
            arrowPath.lineTo(cx, y + arrowSize);
            arrowPath.lineTo(cx + arrowSize * 2, y);
            canvas.drawPath(arrowPath, arrowPaint);
        }
    }
}

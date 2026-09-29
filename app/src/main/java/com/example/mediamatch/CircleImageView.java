package com.example.mediamatch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class CircleImageView extends android.widget.ImageView {
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CircleImageView(Context context) {
        super(context);
        init();
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.CENTER_CROP);
        bgPaint.setColor(Color.parseColor("#DCEBFF"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);
        borderPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                super.onDraw(canvas);
                return;
            }

            float radius = Math.min(width, height) / 2f;
            float cx = width / 2f;
            float cy = height / 2f;

            canvas.drawCircle(cx, cy, radius - 2f, bgPaint);

            Drawable drawable = getDrawable();
            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                if (bitmap != null && !bitmap.isRecycled() && bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
                    BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    imagePaint.setShader(shader);

                    float scale = Math.max((float) width / bitmap.getWidth(), (float) height / bitmap.getHeight());
                    if (!Float.isNaN(scale) && !Float.isInfinite(scale) && scale > 0) {
                        android.graphics.Matrix matrix = new android.graphics.Matrix();
                        matrix.setScale(scale, scale);
                        float dx = (width - bitmap.getWidth() * scale) / 2f;
                        float dy = (height - bitmap.getHeight() * scale) / 2f;
                        matrix.postTranslate(dx, dy);
                        shader.setLocalMatrix(matrix);

                        canvas.drawCircle(cx, cy, radius - 4f, imagePaint);
                    }
                    imagePaint.setShader(null);
                }
            } else if (drawable != null) {
                super.onDraw(canvas);
                return;
            }

            canvas.drawCircle(cx, cy, radius - 3f, borderPaint);
        } catch (Exception e) {
            try {
                super.onDraw(canvas);
            } catch (Exception ignored) {}
        }
    }
}

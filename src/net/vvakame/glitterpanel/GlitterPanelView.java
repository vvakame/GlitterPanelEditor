package net.vvakame.glitterpanel;

import static net.vvakame.glitterpanel.GlitterPanelFragment.*;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class GlitterPanelView extends View implements ITag {

	Paint mFramePaint;
	Paint mFillPaint;

	boolean[] mDisplayPattern = new boolean[PIXELS];

	int mOldCx = -1;
	int mOldCy = -1;

	public GlitterPanelView(Context context) {
		super(context);
		init();
	}

	public GlitterPanelView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public GlitterPanelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	void init() {
		setClickable(true);

		mFramePaint = new Paint();
		mFramePaint.setAntiAlias(false);
		mFramePaint.setColor(Color.WHITE);
		mFramePaint.setStyle(Style.STROKE);
		mFramePaint.setStrokeWidth(1);

		mFillPaint = new Paint();
		mFillPaint.setAntiAlias(false);
		mFillPaint.setColor(Color.RED);
		mFillPaint.setStyle(Style.FILL);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		final int size = Math.min(getWidth(), getHeight());

		final int w = size / PANEL_WIDTH * PANEL_WIDTH;
		final int h = size / PANEL_HEIGHT * PANEL_HEIGHT;
		final int cw = w / PANEL_WIDTH;
		final int ch = h / PANEL_HEIGHT;
		canvas.drawRect(0, 0, w - 1, h - 1, mFramePaint);
		for (int x = 0; x < PANEL_WIDTH; x++) {
			for (int y = 0; y < PANEL_HEIGHT; y++) {
				float left = cw * x;
				float top = ch * y;
				float right = cw * (x + 1);
				float bottom = ch * (y + 1);
				RectF rect = new RectF(left, top, right, bottom);
				if (mDisplayPattern[x + y * PANEL_HEIGHT]) {
					canvas.drawRect(rect, mFillPaint);
				}
				canvas.drawRect(rect, mFramePaint);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int size = Math.min(getWidth(), getHeight());

		final int w = size / PANEL_WIDTH * PANEL_WIDTH;
		final int h = size / PANEL_HEIGHT * PANEL_HEIGHT;
		final int cw = w / PANEL_WIDTH;
		final int ch = h / PANEL_HEIGHT;

		float x = event.getX();
		float y = event.getY();

		int cx = (int) (x / cw);
		int cy = (int) (y / ch);

		if (0 <= cx && cx < PANEL_WIDTH && 0 <= cy && cy < PANEL_HEIGHT) {

			handleTouchEvent(cx, cy);

			int left = cw * cx;
			int top = ch * cy;
			int right = cw * (cx + 1);
			int bottom = ch * (cy + 1);
			Rect rect = new Rect(left, top, right, bottom);
			invalidate(rect);

			if (event.getAction() == MotionEvent.ACTION_UP) {
				mOldCx = -1;
				mOldCy = -1;
			}

			return true;
		}

		return super.onTouchEvent(event);
	}

	public boolean[] getDisplayPattern() {
		return mDisplayPattern;
	}

	public void clearDisplayPattern() {
		for (int i = 0; i < mDisplayPattern.length; i++) {
			mDisplayPattern[i] = false;
		}
		invalidate();
	}

	void handleTouchEvent(int cx, int cy) {
		if (mOldCx == cx && mOldCy == cy) {
			// 前回と同じ所がタッチされていたら無視する
			return;
		}
		int idx = cx + PANEL_WIDTH * cy;
		mDisplayPattern[idx] = !mDisplayPattern[idx];

		mOldCx = cx;
		mOldCy = cy;
	}
}

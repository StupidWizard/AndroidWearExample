package come.example.wearexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.TimeZone;

/**
 * This stupid code is created by thantieuhodo on 10/8/15.
 */
public class WearService extends CanvasWatchFaceService{

    @Override
    public Engine onCreateEngine() {
        return new WearEngine();
    }


    private class WearEngine extends CanvasWatchFaceService.Engine {
        private static final long INTERACTIVE_UPDATE_RATE_MS = 30;
        private static final int MSG_UPDATE_TIME = 0;

        /* handler to update the time once a second in interactive mode */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }


        /* receiver to update the time zone */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };



        /* device features */
        boolean mLowBitAmbient;

        private boolean mRegisteredTimeZoneReceiver;

        private Time mTime;

        private Paint mPaint;

        private float textWidth;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            configureSystemUI();

            loadResource();

            initCountTimeObject();
        }



        //================================================================================
        // Init Service
        //================================================================================

        private void configureSystemUI() {
            setWatchFaceStyle(new WatchFaceStyle.Builder(WearService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
        }

        private void loadResource() {
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(Color.BLACK);
            mPaint.setAntiAlias(true);
            mPaint.setFilterBitmap(true);
            mPaint.setDither(true);
            mPaint.setTextSize(54);

            textWidth = mPaint.measureText("00:00:00");
        }

        private void initCountTimeObject() {
            mTime = new Time();
        }



        //================================================================================
        // Event
        //================================================================================

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (mLowBitAmbient) {
                mPaint.setAntiAlias(!inAmbientMode);
            }
            updateTimer();
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            }
            else {
                unregisterReceiver();
            }

            updateTimer();
        }



        //================================================================================
        // Controller
        //================================================================================

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WearService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WearService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }



        //================================================================================
        // Draw
        //================================================================================

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            mTime.setToNow();

            mPaint.setColor(Color.WHITE);
            canvas.drawRect(bounds, mPaint);

            mPaint.setColor(Color.BLACK);
            canvas.drawText(String.format("%02d:%02d:%02d", mTime.hour, mTime.minute, mTime.second),
                    bounds.centerX() - textWidth/2, bounds.centerY() + (mPaint.getTextSize())/3, mPaint);

            canvas.drawRect(bounds.left, bounds.centerY() - 1, bounds.right, bounds.centerY() * 1, mPaint);
        }

    }
}

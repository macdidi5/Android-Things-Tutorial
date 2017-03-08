package net.macdidi5.at.thingscommander;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.contrib.driver.ssd1306.BitmapHelper;
import com.google.android.things.contrib.driver.ssd1306.Ssd1306;

import java.io.IOException;

import static net.macdidi5.at.thingscommander.R.drawable.ic_eye_center;
import static net.macdidi5.at.thingscommander.R.drawable.ic_eye_left;
import static net.macdidi5.at.thingscommander.R.drawable.ic_eye_right;

public class Screen {

    private static final String TAG = Screen.class.getSimpleName();

    private Ssd1306 screen;
    private Handler screenHandler = new Handler();

    private Bitmap[] bitmaps = new Bitmap[4];

    private int actionCount = 0;

    public Screen(Context context, String i2cBus) throws IOException {
        screen = new Ssd1306(i2cBus);

        Resources res = context.getResources();
        bitmaps[0] = BitmapFactory.decodeResource(res, ic_eye_center);
        bitmaps[1] = BitmapFactory.decodeResource(res, ic_eye_right);
        bitmaps[2] = BitmapFactory.decodeResource(res, ic_eye_center);
        bitmaps[3] = BitmapFactory.decodeResource(res, ic_eye_left);
    }

    public void startWatching() {
        screenHandler.post(screenRunnable);
    }

    public void stopWatching() {
        screenHandler.removeCallbacks(screenRunnable);
    }

    private Runnable screenRunnable = new Runnable() {

        int index = 0;

        @Override
        public void run() {
            try {
                if (actionCount == 0) {
                    screen.clearPixels();
                    BitmapHelper.setBmpData(screen, 0, 0, bitmaps[index], false);
                    screen.show();
                    index++;

                    if (index >= bitmaps.length) {
                        index = 0;
                    }
                }

                screenHandler.postDelayed(this, 1000);
            }
            catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

    public enum Position {
        LEFT, CENTER, RIGHT;
    }

    private void drawBitmap(Bitmap bitmap, Position position) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        int screenWidth = screen.getLcdWidth();
        int screenHeight = screen.getLcdHeight();
        int offestWidth = screenWidth - bitmapWidth;
        int offestHeight = screenHeight - bitmapHeight;

        int x = 0, y = offestHeight / 2;

        switch (position) {
            case LEFT:
                x = 0;
                break;
            case CENTER:
                x = offestWidth / 2;
                break;
            case RIGHT:
                x = offestWidth;
                break;
        }

        screen.clearPixels();
        BitmapHelper.setBmpData(screen, x, y, bitmap, false);

        try {
            screen.show();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void showAction(Bitmap bitmap) {
        showAction(bitmap, 1500);
    }

    public void showAction(final Bitmap bitmap, final long delay) {
        actionCount++;

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                drawBitmap(bitmap, Position.CENTER);

                try {
                    Thread.sleep(delay);
                }
                catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                }

                actionCount--;
            }
        });
    }

}

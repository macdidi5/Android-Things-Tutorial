package net.macdidi5.at.helloled;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // 控制LED閃爍的Handler物件
    private Handler ledHandler = new Handler();
    // 連接LED的GPIO物件
    private Gpio ledGpio;
    // 連接LED的GPIO pin
    private final String PIN_NAME = "BCM23";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");

        // 建立設備管理員服物物件
        PeripheralManagerService service = new PeripheralManagerService();

        try {
            // 建立GPIO物件
            ledGpio = service.openGpio(PIN_NAME);
            // 設定GPIO為輸出模式，預設為低電壓
            ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            // 啟動控制LED閃爍的Handler物件
            ledHandler.post(ledRunnable);
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy");

        // 移除控制LED閃爍的Handler物件
        ledHandler.removeCallbacks(ledRunnable);

        try {
            // 關閉GPIO物件
            ledGpio.close();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        finally {
            ledGpio = null;
        }
    }

    // 控制LED閃爍的Handler物件
    private Runnable ledRunnable = new Runnable() {
        @Override
        public void run() {
            if (ledGpio == null) {
                return;
            }

            try {
                // 設定GPIO狀態
                ledGpio.setValue(!ledGpio.getValue());
                Log.i(TAG, "LED: " + (ledGpio.getValue() ? "ON" : "OFF"));
                // 1秒以後重複執行
                ledHandler.postDelayed(ledRunnable, 1000);
            }
            catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

}

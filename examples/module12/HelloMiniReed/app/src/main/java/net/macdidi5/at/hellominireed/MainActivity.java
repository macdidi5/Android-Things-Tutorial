package net.macdidi5.at.hellominireed;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // 連接開關的GPIO物件
    private Gpio miniReedGpio;
    // 連接迷你磁簧開關模組的GPIO pin
    private final String PIN_NAME = "BCM23";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        // 建立設備管理員服物物件
        PeripheralManagerService service = new PeripheralManagerService();

        try {
            // 建立GPIO物件
            miniReedGpio = service.openGpio(PIN_NAME);
            // 設定GPIO為輸入模式
            miniReedGpio.setDirection(Gpio.DIRECTION_IN);
            // 設定GPIO的監聽狀態
            //    Gpio.EDGE_NONE:    不作用
            //    Gpio.EDGE_FALLING: 由高電壓變化為低電壓
            //    Gpio.EDGE_RISING:  由低電壓變化為高電壓
            //    Gpio.EDGE_BOTH:    高、低電壓變化
            miniReedGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            // 設定GPIO的作用模式
            //    Gpio.ACTIVE_HIGH: 偵測到高電壓時傳回true,低電壓傳回false
            //    Gpio.ACTIVE_LOW:  偵測到高電壓時傳回false,低電壓傳回true
            miniReedGpio.setActiveType(Gpio.ACTIVE_HIGH);

            // 註冊GPIO監聽物件
            miniReedGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    try {
                        // 顯示GPIO狀態
                        Log.i(TAG, "Mini Reed: " + gpio.getValue());
                    }
                    catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }

                    return true;
                }
            });
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        if (miniReedGpio != null) {
            try {
                // 關閉GPIO物件
                miniReedGpio.close();
            }
            catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            finally {
                miniReedGpio = null;
            }
        }
    }

}

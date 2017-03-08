package net.macdidi5.at.thingscommander;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorManager.DynamicSensorCallback;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.pwmservo.Servo;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.R.attr.key;
import static net.macdidi5.at.thingscommander.R.drawable.ic_add_a_photo_white;
import static net.macdidi5.at.thingscommander.R.drawable.ic_lightbulb_outline_white;
import static net.macdidi5.at.thingscommander.R.drawable.ic_lock_open_white;
import static net.macdidi5.at.thingscommander.R.drawable.ic_lock_white;
import static net.macdidi5.at.thingscommander.R.drawable.ic_settings_white;
import static net.macdidi5.at.thingscommander.R.drawable.ic_warning_white;

/**
 * macdidi.uuu@gmail.com
 * https://thingscommander-f3f74.firebaseio.com/
 *
 * Lego Beach Hut
 *      Top left: BCM17, Top right: BCM23
 *      Bottom left: BCM27, Bottom right: BCM22
 *      MQ2: BCM25
 *      Left door: BCM24
 *      Right door: PWM0/BCM18
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // GPIO 管理物件
    private GpioContainer gpioContainer;
    // Listener GPIO 名稱管理物件
    private Set<String> listeners;

    // FirebaseDatabase 物件
    private FirebaseDatabase firebaseDatabase;

    // Firebase 節點名稱：控制、監聽、照片、伺服馬達、溫度與設備
    private final String CHILD_CONTROL_NAME = "control";
    private final String CHILD_LISTENER_NAME = "listener";
    private final String CHILD_IMAGE_NAME = "image";
    private final String CHILD_SERVO_NAME = "pwm";
    private final String CHILD_MONITOR_NAME = "monitor";
    private final String CHILD_DEVICE_NAME = "device";

    // Firebase 節點物件：控制、監聽、照片、伺服馬達、溫度與設備
    private DatabaseReference childControl, childListener, childImage,
                              childServo, childMonitor, childDevice;

    // Firebase 節點資料監聽物件
    private ValueEventListener valueEventListener;

    // 設備管理物件與更新設備執行緒物件
    private List<Device> devices = new ArrayList<>();
    private Handler deviceHandler = new Handler();

    // MCP3008 ADC 物件
    private MCP3008 mcp3008;

    // 伺服馬達物件
    private Servo servo;

    // 相機物件與相機執行緒物件
    private Camera camera;
    private Handler cameraHandler;
    private HandlerThread cameraThread;

    // OLED 畫面物件
    private Screen screen;

    // BMP280 感應模組物件與感應器管理員物件
    private Bmx280SensorDriver bmp280SensorDriver;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        // 建立設備管理員服物物件
        PeripheralManagerService service = new PeripheralManagerService();

        try {
            // 建立設備管理員服物物件
            mcp3008 = new MCP3008(service, "BCM5", "BCM6", "BCM19", "BCM26");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        listeners = new HashSet<>();

        // 建立 GPIO 管理物件
        gpioContainer = new GpioContainer(service);

        for (PiGPIO piGPIO : PiGPIO.values()) {
            try {
                gpioContainer.getPin(piGPIO);
            }
            catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        initialize();
    }

    private void initialize() {
        Log.d(TAG, "initialize() start...");

        firebaseDatabase = FirebaseDatabase.getInstance();

        childControl = firebaseDatabase.getReference(CHILD_CONTROL_NAME);
        childListener = firebaseDatabase.getReference(CHILD_LISTENER_NAME);
        childImage = firebaseDatabase.getReference(CHILD_IMAGE_NAME);
        childServo = firebaseDatabase.getReference(CHILD_SERVO_NAME);
        childMonitor = firebaseDatabase.getReference(CHILD_MONITOR_NAME);
        childDevice = firebaseDatabase.getReference(CHILD_DEVICE_NAME);

        configScreen();
        screen.showAction(setting, 3000);
        configControl();
        configListener();
        configDevice();
        configServo();
        configCamera();
        configBMP280();

        Log.d(TAG, "initialize() done...");
    }

    // 執行控制設定
    private void configControl() {
        Log.d(TAG, "configControl() start...");

        // 建立 Firebase 資料改變監聽物件
        valueEventListener = new ValueEventListener() {

            // 資料改變
            @Override
            public void onDataChange(DataSnapshot ds) {
                PiGPIO pin = PiGPIO.getPiGPIO(ds.getKey());

                if (pin == null) {
                    return;
                }

                Log.d(TAG, pin.name() + ":" + ds.getValue());

                try {
                    // 取得 GPIO 物件
                    Gpio workPin = gpioContainer.getPin(pin);
                    // 讀取雲端資料
                    boolean status = (Boolean) ds.getValue();
                    // 設定 GPIO
                    workPin.setValue(status);
                }
                catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError de) {
            }
        };

        for (PiGPIO piGPIO : PiGPIO.values()) {
            childControl.child(piGPIO.name()).addValueEventListener(valueEventListener);
        }

        Log.d(TAG, "configControl() done...");
    }

    private void configListener() {
        Log.d(TAG, "configListener() start...");

        // 建立與註冊 listener Firebase 監聽節點事件
        childListener.addChildEventListener(new ChildEventListenerAdapter() {
            // 新增節點
            @Override
            public void onChildAdded(DataSnapshot ds, String previousChildKey) {
                PiGPIO pin = PiGPIO.getPiGPIO(ds.getKey());
                String pinName = pin.name();

                if (listeners.contains(pinName)) {
                    return;
                }

                // 讀取 Firebase 資料
                String notifyStatus = (String) ds.getValue();
                String[] nsa = notifyStatus.split(",");

                // 移除控制節點的監聽物件
                childControl.child(pinName)
                        .removeEventListener(valueEventListener);

                // 新增監聽的 GPIO
                listeners.add(pinName);

                try {
                    addListener(pin, nsa[0], nsa[1]);
                }
                catch (IOException e) {
                    Log.e(TAG, e.toString());
                }

                Log.d(TAG, "Add listener: " + pinName + "," + notifyStatus);
            }
        });

        Log.d(TAG, "configListener() done...");
    }

    private void configDevice() {
        Log.d(TAG, "configDevice() start...");

        childDevice.addChildEventListener(new ChildEventListenerAdapter() {
            // 新增節點
            @Override
            public void onChildAdded(DataSnapshot ds, String previousChildKey) {
                Device device = ds.getValue(Device.class);
                devices.add(device);
                Log.d(TAG, device.toString());
            }
        });

        deviceHandler.post(deviceRunnable);

        Log.d(TAG, "configDevice() done...");
    }

    // 加入監聽
    //   String pinName         GPIO 名稱
    //   final String isHigh    高電壓的說明
    //   String isLow           低電壓的說明
    private void addListener(final PiGPIO pin,
                             final String isHigh,
                             final String isLow) throws IOException {
        Gpio workPin = gpioContainer.getPin(pin);
        workPin.setDirection(Gpio.DIRECTION_IN);
        workPin.setEdgeTriggerType(Gpio.EDGE_BOTH);

        workPin.registerGpioCallback(new GpioCallback() {
            @Override
            public boolean onGpioEdge(Gpio gpio) {
                try {
                    boolean uploadPicture =
                            (isHigh.equals("true") && gpio.getValue()) ||
                            (isLow.equals("true") && (!gpio.getValue()));

                    if (uploadPicture) {
                        screen.showAction(addPhoto);

                        camera.setPinName(pin.name());
                        camera.setStatus(gpio.getValue());
                        camera.takePicture();
                    }
                    else {
                        childControl.child(pin.name()).setValue(gpio.getValue());
                    }

                    Log.d(TAG, "Listener: " + pin.name() + "/" + gpio.getValue());
                }
                catch (IOException e) {
                    Log.e(TAG, e.toString());
                }

                return true;
            }
        });

    }

    private final String PWM_NAME = "PWM0";

    private void configServo() {
        Log.d(TAG, "configServo() start...");

        try {
            // 建立伺服馬達物件
            servo = new Servo(PWM_NAME);
            // 設定伺服馬達的範圍，0到180度
            servo.setAngleRange(0f, 180f);
            // 啟動伺服馬達
            servo.setEnabled(true);

            // 建立與註冊 Firebase 伺服馬達節點事件
            childServo.child(PWM_NAME).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean status = (Boolean) dataSnapshot.getValue();
                    Log.d(TAG, key + ":" + status);
                    int angle = status ? 180 : 10;

                    screen.showAction(status ? lockOpen : lockClose);

                    // 控制伺服馬達
                    try {
                        servo.setAngle(angle);
                        Log.d(TAG, "Servo setAngle(" + angle + ")");
                    }
                    catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, databaseError.toString());
                }
            });
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        Log.d(TAG, "configServo() done...");
    }

    private void configCamera() {
        Log.d(TAG, "configCamera() start...");

        camera = Camera.getInstance();
        cameraThread = new HandlerThread("CameraThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        camera.initializeCamera(this, cameraHandler, imageAvailableListener);

        Log.d(TAG, "configCamera() done...");
    }

    private Bitmap addPhoto, lightBulb, lockOpen, lockClose, warning, setting;

    private void configScreen() {
        Log.d(TAG, "configScreen() start...");

        addPhoto = BitmapFactory.decodeResource(getResources(), ic_add_a_photo_white);
        lightBulb = BitmapFactory.decodeResource(getResources(), ic_lightbulb_outline_white);
        lockOpen = BitmapFactory.decodeResource(getResources(), ic_lock_open_white);
        lockClose = BitmapFactory.decodeResource(getResources(), ic_lock_white);
        warning = BitmapFactory.decodeResource(getResources(), ic_warning_white);
        setting = BitmapFactory.decodeResource(getResources(), ic_settings_white);

        try {
            screen = new Screen(this, "I2C1");
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        screen.startWatching();

        Log.d(TAG, "configScreen() done...");
    }

    private void configBMP280() {
        Log.d(TAG, "configBMP280() start...");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerDynamicSensorCallback(sensorCallback);
//        sensorManager.registerDynamicSensorCallback(sensorCallback);

        try {
            bmp280SensorDriver = new Bmx280SensorDriver("I2C1");
            bmp280SensorDriver.registerTemperatureSensor();
//            bmp280SensorDriver.registerPressureSensor();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        Log.d(TAG, "configBMP280() done...");
    }

    private int currentTemperature = 0, currentPressure = 0;

    private DynamicSensorCallback sensorCallback = new DynamicSensorCallback() {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            int type = sensor.getType();

            switch (type) {
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    sensorManager.registerListener(temperatureListener,
                            sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    Log.i(TAG, "Temperature sensor connected");
                    break;
//                case Sensor.TYPE_PRESSURE:
//                    sensorManager.registerListener(pressureListener,
//                            sensor, SensorManager.SENSOR_DELAY_NORMAL);
//                    Log.i(TAG, "Pressure sensor connected");
//                    break;
            }
        }
    };

    private SensorEventListener temperatureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            int temp = (int) event.values[0];

            if (temp != currentTemperature) {
                currentTemperature = temp;
                childMonitor.child("m001").setValue(temp);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };

//    private SensorEventListener pressureListener = new SensorEventListener() {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            int pres = (int) event.values[0];
//
//            if (pres != currentPressure) {
//                currentPressure = pres;
//                int altitude = (int) (pres * 100 / 3389.39F);
//                childMonitor.child("m002").setValue(altitude);
//            }
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//            // do nothing
//        }
//    };

    // MCP3008，10 bit ADC
    public static final double RATE = 100D / 1023;

    private Runnable deviceRunnable = new Runnable() {
        @Override
        public void run() {
            // 如果有需要讀取的設備資料
            if (!devices.isEmpty()) {
                for (Device device : devices) {
                    try {
                        // 讀取指定通道的類比值
                        int adcValue = mcp3008.read(device.getChannel());
                        // 換算為0-100範圍的值
                        int value = (int) (adcValue * RATE);

                        // 如果是反相的資料值
                        if (!device.isIsIncrememt()) {
                            value = 100 - value;
                        }

                        // 判斷是否需要儲存設備讀取的資料
                        boolean isUpload = Math.abs(
                                value - device.getValue()) > device.getAccuracy();
                        device.setValue(value);

                        // 如果需要儲存設備讀取的資料到 Firebase
                        if (isUpload) {
                            childDevice.child(device.getId()).setValue(device);
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }

            deviceHandler.postDelayed(deviceRunnable, 1000);
        }
    };

    private ImageReader.OnImageAvailableListener imageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG, "onImageAvailable()");

                    Image image = reader.acquireLatestImage();
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    savePicture(imageBytes);
                }
            };

    private void savePicture(final byte[] imageBytes) {
        if (imageBytes != null) {
            Log.d(TAG, "savePicture()");

            String imageStr = Base64.encodeToString(
                    imageBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            childImage.setValue(imageStr, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError de, DatabaseReference dr) {
                    if (de != null) {
                        Log.e(TAG, de.toString());
                    }
                    else {
                        Log.d(TAG, "Picture upload successfully.");
                        childControl.child(camera.getPinName()).setValue(camera.getStatus());
                        Log.d(TAG, "Listener: " + camera.getPinName() + "/" + camera.getStatus());
                    }
                }
            });
        }
    }

}
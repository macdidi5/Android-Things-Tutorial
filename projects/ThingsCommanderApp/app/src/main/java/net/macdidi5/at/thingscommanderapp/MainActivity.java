package net.macdidi5.at.thingscommanderapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.macdidi5.at.dynamicgrid.BaseDynamicGridAdapter;
import net.macdidi5.at.dynamicgrid.DynamicGridView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public final static String CHILD_CONTROL_NAME = "control";
    public final static String CHILD_LISTENER_NAME = "listener";
    public final static String CHILD_IMAGE_NAME = "image";
    public final static String CHILD_SERVO_NAME = "pwm";
    public final static String CHILD_MONITOR_NAME = "monitor";
    public final static String CHILD_DEVICE_NAME = "device";

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference childControl, childListener, childDevice,
            childServo, childImage, childMonitor;

    private ValueEventListener fbListener;

    // 啟動連線與項目代碼
    private static final int REQUEST_ITEM = 0;

    // 啟動連線與項目ACTION名稱
    public static final String ADD_ITEM_ACTION =
            "net.macdidi5.picomfire.ADD_ITEM";
    public static final String DELETE_ITEM_ACTION =
            "net.macdidi5.picomfire.DELETE_ITEM";

    // 主畫面容器
    private RelativeLayout main;

    // 分頁物件
    private CommandPagerAdapter commandPagerAdapter;
    private PiViewPager mypager;
    private static DynamicGridView[] dynamicGridViews =
            new DynamicGridView[3];

    // 分頁使用的 Fragmemt 物件
    private Fragment[] fragments;

    // 控制與監聽 Adapter
    private static CommandAdapter controllerCommandAdapter,
            listenerCommandAdapter;
    // 溫濕度 Adapter
    private static MonitorAdapter monitorAdapter;
    // 感應設備 Adapter
    private static DeviceAdapter deviceAdapter;

    // 控制,監聽與溫度項目
    private List<CommanderItem> controllerCommanderItems,
            listenerCommanderItems, monitorCommanderItems;
    // 感應設備項目
    private List<Device> deviceItems;

    // 是否正在處理工作
    private boolean processMenu = false;

    // 連線狀態背景圖示
    private ImageView connect_imageview;
    // 選單
    private MenuItem door_menu, connect_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 讀取儲存的控制與監聽項目
        controllerCommanderItems = TurtleUtil.getControllers(this);
        listenerCommanderItems = TurtleUtil.getListeners(this);

        // 溫度與海拔高度項目
        monitorCommanderItems = new ArrayList<>();
        monitorCommanderItems.add(new CommanderItem("m001", "Temperature", "1"));
        monitorCommanderItems.add(new CommanderItem("m002", "Altitude", "1"));

        // 建立感應設備項目
        deviceItems = new ArrayList<>();

        // 建立控制,監聽,溫度海拔高度與感應設備的 Adapter 物件
        controllerCommandAdapter = new CommandAdapter(this, controllerCommanderItems);
        listenerCommandAdapter = new CommandAdapter(this, listenerCommanderItems);
        monitorAdapter = new MonitorAdapter(this, monitorCommanderItems);
        deviceAdapter = new DeviceAdapter(this, deviceItems);

        // 建立分頁使用的 Fragment 物件
        fragments = new Fragment[] {
                CommandFragment.newInstance(0),
                CommandFragment.newInstance(1),
                CommandFragment.newInstance(2),
                DeviceFragment.newInstance(3)
        };

        // 建立分頁 Adapter 物件
        commandPagerAdapter = new CommandPagerAdapter(
                getSupportFragmentManager(), fragments);
        mypager = (PiViewPager) findViewById(R.id.mypager);
        mypager.setAdapter(commandPagerAdapter);
        mypager.setOffscreenPageLimit(3);

        processViews();

        // 建立與啟動監聽服務元件
//        new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                super.handleMessage(msg);
//                startService(new Intent(MainActivity.this, ListenService.class));
//            }
//        }.sendEmptyMessage(0);

        // 建立 Firebase 資料監聽物件
        fbListener = new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot ds) {
                String key = ds.getKey();
                boolean value = (Boolean) ds.getValue();
                setItemStatus(key, value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.toString());
            }

        };

        final Handler handlerDevice = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                for (Device device : deviceItems) {
                    deviceAdapter.update(device);
                }

                sendEmptyMessageDelayed(0, 2000);
            }
        };

        mypager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (firebaseDatabase == null) {
                    return;
                }

                if (position == 3) {
                    handlerDevice.sendEmptyMessageDelayed(0, 1000);
                }
                else {
                    handlerDevice.removeMessages(0);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // 新增控制與監聽示範項目
        addDemo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // 新增或刪除項目
            if (requestCode == REQUEST_ITEM) {
                processItem(data);
            }
        }

        processMenu = false;
    }

    // 新增或刪除項目
    private void processItem(Intent data) {
        // 讀取項目位置與種類
        int itemPosition = data.getIntExtra("itemPosition", -1);
        int commandType = data.getIntExtra("commandType", -1);
        String commandTypeValue = commandType == 0 ?
                TurtleUtil.CONTROLLER_COMMANDER :
                TurtleUtil.LISTENER_COMMANDER;

        // 新增項目
        if (itemPosition == -1) {
            // 讀取項目資訊
            String gpioName = data.getStringExtra("gpioName");
            String desc = data.getStringExtra("desc");

            // 建立項目物件
            CommanderItem item = new CommanderItem(
                    gpioName, desc, commandTypeValue);

            // 控制項目
            if (commandType == 0) {
                // 新增與儲存控制項目
                controllerCommandAdapter.add(item);
                TurtleUtil.saveCommanders(this,
                        controllerCommandAdapter.getCommanderItems());
            }
            // 監聽項目
            else {
                // 讀取與設定監聽項目資運訊
                item.setHighDesc(data.getStringExtra("highDesc"));
                item.setLowDesc(data.getStringExtra("lowDesc"));
                item.setHighNotify(data.getBooleanExtra("highNotify", false));
                item.setLowNotify(data.getBooleanExtra("lowNotify", false));

                // 新增與儲存監聽項目
                listenerCommandAdapter.add(item);
                TurtleUtil.saveCommanders(this,
                        listenerCommandAdapter.getCommanderItems());

                // 新增 Firebase 監聽項目資料
                if (childListener != null) {
                    childListener.child(gpioName).setValue(
                            item.isHighNotify() + "," + item.isLowNotify());
                }
            }

            // 新增 Firebase 資料異動監聽
            addChangeStatusListener(gpioName);
        }
        // 刪除項目
        else {
            // 控制項目
            if (commandType == 0) {
                controllerCommandAdapter.remove(itemPosition);
            }
            // 監聽項目
            else {
                listenerCommandAdapter.remove(itemPosition);
            }

            // 刪除已儲存的項目資料
            TurtleUtil.deleteCommander(this, itemPosition, commandTypeValue);
        }

        // 通知 Adapter 資料已更新
        if (commandType == 0) {
            controllerCommandAdapter.notifyDataSetChanged();
        }
        else {
            listenerCommandAdapter.notifyDataSetChanged();
        }
    }

    private void processFirebase() {
        // 建立 Firebase 物件
        firebaseDatabase = FirebaseDatabase.getInstance();

        // 控制, 監聽, 門, 溫度海拔高度, 感應設備
        childControl = firebaseDatabase.getReference(CHILD_CONTROL_NAME);
        childListener = firebaseDatabase.getReference(CHILD_LISTENER_NAME);
        childServo = firebaseDatabase.getReference(CHILD_SERVO_NAME);
        childMonitor = firebaseDatabase.getReference(CHILD_MONITOR_NAME);
        childDevice = firebaseDatabase.getReference(CHILD_DEVICE_NAME);

        // Firebase 溫度海拔高度節點監聽
        childMonitor.addChildEventListener(new ChildEventListenerAdapter() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String key = dataSnapshot.getKey();
                Long value = (Long) dataSnapshot.getValue();
                int position = monitorAdapter.getPosition(key);
                monitorAdapter.setValue(position, value.toString());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String key = dataSnapshot.getKey();
                Long value = (Long) dataSnapshot.getValue();
                int position = monitorAdapter.getPosition(key);
                monitorAdapter.setValue(position, value.toString());
            }
        });

        // Firebase 感應設備節點監聽
        childDevice.addChildEventListener(new ChildEventListenerAdapter() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Device device = dataSnapshot.getValue(Device.class);
                deviceAdapter.add(device);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Device device = dataSnapshot.getValue(Device.class);
                deviceAdapter.update(device);
            }
        });

        // 建立控制項目的 Firebase 資料監聽
        List<CommanderItem> cs = TurtleUtil.getControllers(this);

        for (CommanderItem ci : cs) {
            addChangeStatusListener(ci.getGpioName());
        }

        // 建立監聽項目的 Firebase 資料監聽
        List<CommanderItem> ls = TurtleUtil.getListeners(this);

        for (CommanderItem ci : ls) {
            addChangeStatusListener(ci.getGpioName());
        }

        // 設定背景為已經連線
        main.setBackgroundResource(R.drawable.background_enable);
        connect_imageview.setImageResource(R.drawable.bg_connected);

        // 建立門的 Firebase 資料監聽
        childServo.child("PWM0").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean value = (Boolean) dataSnapshot.getValue();
                doorOpen = value;
                door_menu.setIcon(doorOpen ? R.drawable.ic_lock_open_white_48dp :
                        R.drawable.ic_lock_white_48dp);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // 顯示連線訊息
        Toast.makeText(this, R.string.message_connected, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        door_menu = menu.findItem(R.id.door_menu);
        connect_menu = menu.findItem(R.id.connect_menu);
        return true;
    }

    private void processViews() {
        main = (RelativeLayout)findViewById(R.id.main);
        connect_imageview = (ImageView)findViewById(R.id.connect_imageview);
    }

    // 設定項目狀態
    private void setItemStatus(final String gpioName, final boolean status) {
        final CommanderItem item = getCommanderItem(gpioName);

        if (item == null) {
            Log.e(TAG, "Item does not exist");
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                item.setStatus(status);

                if (item.getCommandType().equals(
                        TurtleUtil.CONTROLLER_COMMANDER)) {
                    controllerCommandAdapter.notifyDataSetChanged();
                }
                else {
                    listenerCommandAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    // 溫度海拔高度用的 adapter 類別
    private class MonitorAdapter extends BaseDynamicGridAdapter {
        private Context context;
        private List<CommanderItem> commanderItems;

        public MonitorAdapter(Context context,
                              List<CommanderItem> commanderItems) {
            super(context, commanderItems, 2);
            this.context = context;
            this.commanderItems = commanderItems;
        }

        // 新增項目
        public void add(CommanderItem commanderItem) {
            super.add(commanderItem);
        }

        // 刪除項目
        public void remove(int position) {
            super.remove(commanderItems.get(position));
        }

        // 傳回所有項目
        public List<CommanderItem> getCommanderItems() {
            return commanderItems;
        }

        // 設定數值
        public void setValue(int position, String value) {
            CommanderItem item = commanderItems.get(position);
            item.setCommandType(value);
            notifyDataSetChanged();
        }

        // 傳回位置
        public int getPosition(String target) {
            int result = -1;

            for (int i = 0; i < commanderItems.size(); i++) {
                if (commanderItems.get(i).getGpioName().equals(target)) {
                    result = i;
                }
            }

            return result;
        }

        // 傳回項目數量
        @Override
        public int getCount() {
            return commanderItems.size();
        }

        // 傳回參數指定位置的項目
        @Override
        public Object getItem(int position) {
            return commanderItems.get(position);
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(
                        R.layout.monitor_item, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }
            else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.build(position);

            return convertView;
        }

        private class ViewHolder {

            private TextView item_desc;
            private eu.hansolo.enzo.Gauge item_gauge;

            private ViewHolder(View view) {
                item_desc = (TextView) view.findViewById(R.id.item_desc);
                item_gauge = (eu.hansolo.enzo.Gauge)
                        view.findViewById(R.id.item_gauge);
            }

            void build(int position) {
                CommanderItem item = commanderItems.get(position);
                item_desc.setText(item.getDesc());
                item_gauge.setValue(Integer.parseInt(item.getCommandType()));
            }
        }
    }

    // 控制與監聽用的 adapter 類別
    private class CommandAdapter extends BaseDynamicGridAdapter {

        private Context context;
        private List<CommanderItem> commanderItems;

        public CommandAdapter(Context context,
                              List<CommanderItem> commanderItems) {
            super(context, commanderItems, 2);
            this.context = context;
            this.commanderItems = commanderItems;
        }

        // 新增項目
        public void add(CommanderItem commanderItem) {
            super.add(commanderItem);
        }

        // 切換高低電壓狀態
        public void toggle(int position) {
            CommanderItem item = commanderItems.get(position);
            item.setStatus(!item.isStatus());

            // 儲存狀態到 Firebase
            String gpioName = item.getGpioName();
            childControl.child(gpioName).setValue(item.isStatus());
        }

        // 刪除項目
        public void remove(int position) {
            super.remove(commanderItems.get(position));
        }

        // 傳回所有項目
        public List<CommanderItem> getCommanderItems() {
            return commanderItems;
        }

        // 傳回項目數量
        @Override
        public int getCount() {
            return commanderItems.size();
        }

        // 傳回參數指定的項目
        @Override
        public Object getItem(int position) {
            return commanderItems.get(position);
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(
                        R.layout.commander_item, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }
            else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.build(position);

            return convertView;
        }

        private class ViewHolder {

            private TextView item_desc;
            private Switch item_switch;

            private ViewHolder(View view) {
                item_desc = (TextView)view.findViewById(R.id.item_desc);
                item_switch = (Switch)view.findViewById(R.id.item_switch);
            }

            void build(int position) {
                CommanderItem item = commanderItems.get(position);
                item_desc.setText(item.getDesc());
                item_switch.setChecked(item.isStatus());

                if (item.getCommandType().equals(TurtleUtil.LISTENER_COMMANDER)) {
                    String statusText = item_switch.isChecked() ?
                            item.getHighDesc() : item.getLowDesc();
                    item_desc.setText(statusText + "\n" + item.getDesc());
                    item_switch.setClickable(false);
                }
            }
        }
    }

    // 傳回參數指定名稱的項目物件
    private CommanderItem getCommanderItem(String gpioName) {
        // 取得控制項目
        CommanderItem result =
                getCommanderItem(controllerCommanderItems, gpioName);

        if (result == null) {
            // 取得監聽項目
            result = getCommanderItem(listenerCommanderItems, gpioName);
        }

        return result;
    }

    // 傳回參數指定名稱的項目物件
    public static CommanderItem getCommanderItem(List<CommanderItem> items,
                                                 String gpioName) {
        CommanderItem result = null;

        for (CommanderItem item : items) {
            if (item.getGpioName().equals(gpioName)) {
                result = item;
                break;
            }
        }

        return result;
    }

    // 分頁使用的 Adapter 類別
    public class CommandPagerAdapter extends FragmentPagerAdapter {

        private Fragment[] fragments;

        public CommandPagerAdapter(FragmentManager fragmentManager,
                                   Fragment[] fragments) {
            super(fragmentManager);
            this.fragments = fragments;
        }

        // 傳回參數指定的分頁物件
        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        // 傳回分頁數量
        @Override
        public int getCount() {
            return fragments.length;
        }

        // 傳回分頁標題
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Controller";
                case 1:
                    return "Listener";
                case 2:
                    return "Monitor";
                case 3:
                    return "Device";
            }

            return null;
        }

    }

    // 控制, 監聽與溫度海拔高度分頁使用的 Fragment 類別
    public static class CommandFragment extends Fragment {

        private static final String KEY_POSITION = "position";

        public static CommandFragment newInstance(int position) {
            CommandFragment result = new CommandFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_POSITION, position);
            result.setArguments(args);

            return result;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int position = getArguments().getInt(KEY_POSITION);
            View rootView = inflater.inflate(
                    R.layout.fragment_commander, container, false);
            DynamicGridView item_gridview = (DynamicGridView)
                    rootView.findViewById(R.id.item_gridview);
            processControllers(item_gridview, position, getActivity());

            // 控制
            if (position == 0) {
                item_gridview.setAdapter(controllerCommandAdapter);
                dynamicGridViews[0] = item_gridview;
            }
            // 監聽
            else if (position == 1) {
                item_gridview.setAdapter(listenerCommandAdapter);
                dynamicGridViews[1] = item_gridview;
            }
            // 溫度海拔高度
            else if (position == 2) {
                item_gridview.setAdapter(monitorAdapter);
                dynamicGridViews[2] = item_gridview;
            }

            return rootView;
        }
    }

    // 感應設備分頁使用的 Fragment 類別
    public static class DeviceFragment extends Fragment {

        private static final String KEY_POSITION = "position";

        public static DeviceFragment newInstance(int position) {
            DeviceFragment result = new DeviceFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_POSITION, position);
            result.setArguments(args);

            return result;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // 讀取畫面配置檔案
            View rootView = inflater.inflate(
                    R.layout.fragment_device, container, false);
            // 讀取列表元件
            RecyclerView item_list = (RecyclerView)
                    rootView.findViewById(R.id.item_list);
            // 設定項目樣式控制物件
            item_list.addItemDecoration(new SpacesItemDecoration(12));
            // 不使用動畫設定
            item_list.setItemAnimator(null);
            item_list.setHasFixedSize(true);
            // 取得與設定畫面配置元計
            RecyclerView.LayoutManager rLayoutManager =
                    new LinearLayoutManager(rootView.getContext());
            item_list.setLayoutManager(rLayoutManager);
            // 設定列表元件使用的 Adapter 物件
            item_list.setAdapter(deviceAdapter);
            return rootView;
        }
    }

    // 設定使用者操作功能
    private static void processControllers(final DynamicGridView gridview,
                                           final int commandType,
                                           final Context context) {
        // 使用者選擇項目, 控制
        if (commandType == 0) {
            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // 切換狀態
                    controllerCommandAdapter.toggle(position);
                    controllerCommandAdapter.notifyDataSetChanged();
                }
            });
        }

        // 使用者長按項目
        gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                CommanderItem item;

                // 控制
                if (commandType == 0) {
                    item = (CommanderItem) controllerCommandAdapter.getItem(position);
                }
                // 監聽
                else {
                    item = (CommanderItem) listenerCommandAdapter.getItem(position);
                }

                // 建立與啟動刪除項目元件
                Intent intent = new Intent(DELETE_ITEM_ACTION);

                intent.putExtra("commandType", commandType);
                intent.putExtra("gpioName", item.getGpioName());
                intent.putExtra("desc", item.getDesc());
                intent.putExtra("itemPosition", position);

                ((Activity)context).startActivityForResult(intent, REQUEST_ITEM);

                return true;
            }
        });

    }

    // 新增控制與監聽示範項目
    private void addDemo() {
        if (! TurtleUtil.isFirstTime(this)) {
            return;
        }

        TurtleUtil.setFirstTime(this);

        addItem(0, "BCM17", "2F Left", 0, "", "", "", false, false);
        addItem(0, "BCM23", "2F Right", 0, "", "", "", false, false);
        addItem(0, "BCM27", "1F Left", 0, "", "", "", false, false);
        addItem(0, "BCM22", "1F Right", 0, "", "", "", false, false);
        addItem(1, "BCM24", "Door", 0, "", "Opened", "Closed", true, false);
        addItem(1, "BCM25", "Gas", 0, "", "Dangerous", "Normal", true, false);
    }

    private void addItem(int commandType, String gpioName, String desc,
                         int addressValue, String mcpType,
                         String highDesc, String lowDesc,
                         boolean highNotify, boolean lowNotify) {
        String commandTypeValue = commandType == 0 ?
                TurtleUtil.CONTROLLER_COMMANDER :
                TurtleUtil.LISTENER_COMMANDER;

        CommanderItem item = null;

        // Raspberry Pi GPIO
        item = new CommanderItem(
                gpioName, desc, commandTypeValue);

        // GPIO Controller
        if (commandType == 0) {
            controllerCommandAdapter.add(item);
            TurtleUtil.saveCommanders(this,
                    controllerCommandAdapter.getCommanderItems());
        }
        // GPIO Listener
        else {
            item.setHighDesc(highDesc);
            item.setLowDesc(lowDesc);
            item.setHighNotify(highNotify);
            item.setLowNotify(lowNotify);

            listenerCommandAdapter.add(item);
            TurtleUtil.saveCommanders(this,
                    listenerCommandAdapter.getCommanderItems());

            if (childListener != null) {
                childListener.child(gpioName).setValue(
                        item.isHighNotify() + "," + item.isLowNotify());
            }
        }

        addChangeStatusListener(gpioName);
    }

    // 新增 Firebase 資料異動監聽
    private void addChangeStatusListener(String name) {
        if (childControl != null) {
            childControl.child(name).addValueEventListener(fbListener);
        }
    }

    // 啟動或關閉移動項目模式
    public void clickEditMode(MenuItem item) {
        final int index = mypager.getCurrentItem();

        if (dynamicGridViews[index].isEditMode()) {
            mypager.setPagingEnabled(true);

            item.setIcon(R.drawable.ic_apps_white_48dp);
            dynamicGridViews[index].stopEditMode();

            if (index == 0) {
                TurtleUtil.saveCommanders(this,
                        controllerCommandAdapter.getCommanderItems());
            }
            else if (index == 1) {
                TurtleUtil.saveCommanders(this,
                        listenerCommandAdapter.getCommanderItems());
            }
        }
        else {
            item.setIcon(R.drawable.ic_apps_black_48dp);
            mypager.setPagingEnabled(false);
            dynamicGridViews[index].startEditMode();
        }
    }

    // 門是否開啟
    private static boolean doorOpen = false;

    // 門
    public void clickDoor(MenuItem item) {
        if (childServo != null) {
            doorOpen = !doorOpen;
            item.setIcon(doorOpen ? R.drawable.ic_lock_open_white_48dp :
                    R.drawable.ic_lock_white_48dp);
            childServo.child("PWM0").setValue(doorOpen);
        }
    }

    // 連線到 Firebase
    public void clickConnect(MenuItem item) {
        if (processMenu) {
            return;
        }

        processMenu = true;

        if (firebaseDatabase == null) {
            processFirebase();
            processServiceConnect();
            connect_menu.setEnabled(false);
        }

        processMenu = false;
    }

    private void processServiceConnect() {
        ServiceConnection serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName,
                                           IBinder iBinder) {
                ListenService.ListenServiceBinder binder =
                        (ListenService.ListenServiceBinder) iBinder;
                ListenService listenService = binder.getListenService();

                if (listenService != null) {
                    listenService.processListen();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        Intent bindIntent = new Intent(this, ListenService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    // 新增項目
    public void clickAdd(MenuItem item) {
        int commandType = mypager.getCurrentItem();

        if (processMenu || commandType > 1) {
            return;
        }

        processMenu = true;

        // 建立與啟動新增項目元件
        Intent intent = new Intent(ADD_ITEM_ACTION);
        intent.putExtra("menuItemId", item.getItemId());
        intent.putExtra("commandType", commandType);

        startActivityForResult(intent, REQUEST_ITEM);
    }

}

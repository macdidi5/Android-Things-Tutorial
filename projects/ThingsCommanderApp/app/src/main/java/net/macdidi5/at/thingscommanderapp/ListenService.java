package net.macdidi5.at.thingscommanderapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ListenService extends Service {

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean isConnect = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (TurtleUtil.checkNetwork(this)) {
            processListen();
        }
        else {
            Log.d(TAG, "onStartCommand: Connection required.");
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ListenServiceBinder();
    }

    public class ListenServiceBinder extends Binder {
        public ListenService getListenService() {
            return ListenService.this;
        }
    }

    public void processListen() {
        Log.d(TAG, "processListen() start...");

        isConnect = true;

        final FirebaseDatabase firebaseDatabase;
        final DatabaseReference childControl;

        firebaseDatabase = FirebaseDatabase.getInstance();
        childControl = firebaseDatabase.getReference(MainActivity.CHILD_CONTROL_NAME);

        ValueEventListener listener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String gpioName = dataSnapshot.getKey();
                final CommanderItem item = MainActivity.getCommanderItem(
                        TurtleUtil.getListeners(ListenService.this), gpioName);

                if (item != null) {
                    item.setStatus((Boolean) dataSnapshot.getValue());

                    if ((item.isStatus() && item.isHighNotify()) ||
                            (!item.isStatus() && item.isLowNotify())) {

                        DatabaseReference imageRef =
                                firebaseDatabase.getReference(MainActivity.CHILD_IMAGE_NAME);

                        imageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                String imageStr = (String) snapshot.getValue();

                                if (imageStr == null) {
                                    processNotify(null, item, gpioName);
                                    return;
                                }

                                byte[] imageByte = Base64.decode(
                                        imageStr, Base64.NO_WRAP | Base64.URL_SAFE);
                                Bitmap imageBitmap = BitmapFactory.decodeByteArray(
                                        imageByte, 0, imageByte.length);
                                processNotify(imageBitmap, item, gpioName);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, databaseError.toString());
                            }
                        });
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.toString());
            }
        };


        PiGPIO[] allGPIO = PiGPIO.values();
        int length = allGPIO.length;
        DatabaseReference gpios[] = new DatabaseReference[length];

        for (int i = 0; i < length; i++) {
            gpios[i] = childControl.child(allGPIO[i].name());
            gpios[i].addValueEventListener(listener);
        }

        Log.d(TAG, "processListen() done...");
    }

    private void processNotify(Bitmap bigPicture, CommanderItem item, String gpioName) {
        if (bigPicture == null) {
            bigPicture = BitmapFactory.decodeResource(
                    getResources(), R.drawable.notify_big_picture);
        }

        String nm = item.isStatus() ?
                item.getHighDesc() :
                item.getLowDesc();
        nm = item.getDesc() + ":" + nm;

        Notification.Builder builder = new Notification.Builder(ListenService.this);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(ListenService.this, 0, new Intent(), 0);

        builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setTicker(ListenService.this.getString(R.string.app_name))
                .setContentTitle(ListenService.this.getString(R.string.app_name))
                .setContentText(nm)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        Notification.BigPictureStyle bigPictureStyle =
                new Notification.BigPictureStyle();
        bigPictureStyle.bigPicture(bigPicture)
                .setSummaryText(nm);
        builder.setStyle(bigPictureStyle);

        NotificationManager manager =(NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        manager.notify(gpioName, 0, notification);
    }

}

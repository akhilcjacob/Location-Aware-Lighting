package com.lights.locationawarelights;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.KeyEvent;
import android.view.View;

import com.github.lzyzsd.circleprogress.ArcProgress;


public class MainActivity extends WearableActivity {
    public static int BRIGHTNESS = 50;

    private ArcProgress arc;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothHeadset bluetoothHeadset;

    private BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = (BluetoothHeadset) proxy;
            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = null;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arc = findViewById(R.id.arc);
        setAmbientEnabled();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);


        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }




    }

    @Override /* KeyEvent.Callback */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        System.out.println("something was pressed");
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                System.out.println("gesture up");
                // Do something that advances a user View to the next item in an ordered list.
                return brightness_up();
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                System.out.println("gdown");
                // Do something that advances a user View to the previous item in an ordered list.
                return brightness_down();
        }
        // If you did not handle it, let it be handled by the next possible element as deemed by the Activity.
        return super.onKeyDown(keyCode, event);
    }


    private int bound(int num) {
        if (num > 100)
            return 100;
        if (num < 0)
            return 0;
        return num;
    }

    private boolean brightness_up() {
        boolean handled = false;
        MainActivity.BRIGHTNESS = bound(MainActivity.BRIGHTNESS + 10);
        arc.setProgress(BRIGHTNESS);
        return true;
    }

    private boolean brightness_down() {
        boolean handled = false;
        MainActivity.BRIGHTNESS = bound(MainActivity.BRIGHTNESS - 10);
        arc.setProgress(BRIGHTNESS);
        return true;
    }

    public void brightness_up(View view) {
        brightness_up();
    }
    public void brightness_down(View view) {
        brightness_down();
    }
    protected void onDestroy() {
        super.onDestroy();
    }

}



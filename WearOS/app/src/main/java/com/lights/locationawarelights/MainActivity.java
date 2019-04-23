package com.lights.locationawarelights;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.wearable.activity.WearableActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.lzyzsd.circleprogress.ArcProgress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends WearableActivity{
    public static int BRIGHTNESS = 50;

    private ArcProgress arc;
    private HashMap<String, Integer> scan_results = new HashMap<>();
    private HashMap<String, Integer> update_result = new HashMap<>();
    private HashMap<String, String> human_read = new HashMap<>();
    private TextView t;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Launch the bluetooth service
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothLeScanner mLEScanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder().build();
        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();

        // Probably should be using this
        //ScanFilter.Builder builder = new ScanFilter.Builder();

        // Launch the view pager that allows for horizontal page scrolling
        ViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new CustomPagerAdapter(this));

        //Scan Results: for the specific mac ids the object that the rssi values
        scan_results.put("B8:27:EB:69:8F:AD", 0);
        scan_results.put("B8:27:EB:BF:07:54", 0);
        scan_results.put("B8:27:EB:41:3F:64", 0);

        //Update Result: Stores the update number of the rssi value
        update_result.put("B8:27:EB:69:8F:AD", 0);
        update_result.put("B8:27:EB:BF:07:54", 0);
        update_result.put("B8:27:EB:41:3F:64", 0);

        // Converts the mac ids to a human readable format
        human_read.put("B8:27:EB:69:8F:AD", "RP1");
        human_read.put("B8:27:EB:BF:07:54", "RP2");
        human_read.put("B8:27:EB:41:3F:64", "RP3");


        // Testing in the union: some random mac id
//        scan_results.put("10:A5:6B:41:D9:55", 0);
//        update_result.put("10:A5:6B:41:D9:55", 0);
//        human_read.put("10:A5:6B:41:D9:55", "Rando");



        // Defines the arc that ring that shows brightness
        arc = findViewById(R.id.arc);

        System.out.println("Running the setup");
        for(int x =0; x<1;x++)
        setupNetwork();
        System.out.println("Done the setup");


        // The callback that runs for each discovered mac address
        ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                // The MAC id of the newest scan device
                String mac_id = result.getDevice().toString();
                System.out.println(mac_id);

                // Check if this is a mac ID we want to store information on
                if (human_read.containsKey(mac_id)) {
                    scan_results.put("B8:27:EB:69:8F:AD", 1);
                    scan_results.put("B8:27:EB:BF:07:54", 1);
                    scan_results.put("B8:27:EB:41:3F:64", 1);

                    // Update the results of the rssi
                    scan_results.put(mac_id, result.getRssi());
                    // Update the counter of updates for this pi
                    update_result.put(mac_id, update_result.get(mac_id) + 1);

                    // Convert into readable text
                    StringBuilder output_blue = new StringBuilder();

                    //Build the string that goes on the watch face
                    Iterator it = scan_results.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        output_blue.append(human_read.get(pair.getKey())).append(" : ").append(pair.getValue()).append(",  U: ").append(update_result.get(pair.getKey()))
                                .append("\n");
                        it.remove(); // avoids a ConcurrentModificationException
                    }
                    t = findViewById(R.id.bluetooth_rsi);

                    if (t != null)
                        t.setText(output_blue.toString());
                }
            }
        };
        // Start the bluetooth scanner
        mLEScanner.startScan(filters, settings, mScanCallback);

    }
    public Map<String, String> getHeaders()
    {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        return headers;
    }
    private void setupNetwork(){
        RequestQueue queue = Volley.newRequestQueue(this);
//        String url ="http://www.google.com";
        String url ="https://ws-api.iextrading.com/1.0/stock/aapl/delayed-quote";

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.PUT, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        System.out.println("Response is: ");
                        System.out.println(response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("That didn't work!");
                System.out.println(error.toString());
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);



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
            // Do something that advances a user View to the previous item in an ordered
            // list.
            return brightness_down();
        }
        // If you did not handle it, let it be handled by the next possible element as
        // deemed by the Activity.
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

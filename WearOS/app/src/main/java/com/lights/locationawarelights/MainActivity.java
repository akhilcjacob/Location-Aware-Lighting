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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.lzyzsd.circleprogress.ArcProgress;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends WearableActivity {
    public static int BRIGHTNESS = 50;


    private ArcProgress arc;
    public static HashMap<String, Integer> scan_results = new LinkedHashMap<>();
    //    static {
//        scan_results = new LinkedHashMap<>(); // Diamond operator requires Java 1.7+
//        scan_results.put("B8:27:EB:69:8F:AD", 0);
//        scan_results.put("B8:27:EB:BF:07:54", 0);
//        scan_results.put("B8:27:EB:41:3F:64", 0);
//    }
    public static HashMap<String, Integer> update_result = new HashMap<>();
    //    static {
//
//        update_result.put("B8:27:EB:69:8F:AD", 0);
//        update_result.put("B8:27:EB:BF:07:54", 0);
//        update_result.put("B8:27:EB:41:3F:64", 0);
//    }
    public static HashMap<String, String> human_read = new HashMap<>();
    //    static{
//        human_read.put("B8:27:EB:69:8F:AD", "RP1");
//        human_read.put("B8:27:EB:BF:07:54", "RP2");
//        human_read.put("B8:27:EB:41:3F:64", "RP3");
//    }
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

//        Scan Results: for the specific mac ids the object that the rssi values
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
        for (int x = 0; x < 1; x++)
            setupNetwork();
        System.out.println("Done the setup");

        System.out.println("First time: " + MainActivity.scan_results);
        // The callback that runs for each discovered mac address
        ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                // The MAC id of the newest scan device
                String mac_id = result.getDevice().toString();

//                MainActivity.scan_results.put("B8:27:EB:69:8F:AD",   MainActivity.scan_results.get("B8:27:EB:69:8F:AD"));
//                MainActivity.scan_results.put("B8:27:EB:BF:07:54",   MainActivity.scan_results.get("B8:27:EB:BF:07:54"));
//                MainActivity.scan_results.put("B8:27:EB:41:3F:64",   MainActivity.scan_results.get("B8:27:EB:41:3F:64"));
//                System.out.println(mac_id+ MainActivity.scan_results.toString());
                // Check if this is a mac ID we want to store information on
                if (human_read.containsKey(mac_id)) {
                    setupNetwork();

                    // Update the results of the rssi
                    MainActivity.scan_results.put(mac_id, result.getRssi());
//                    System.out.println("Thisis the new hash");
//                    System.out.println(mac_id+ MainActivity.scan_results.toString());

                    // Update the counter of updates for this pi
                    MainActivity.update_result.put(mac_id, MainActivity.update_result.get(mac_id) + 1);

                    // Convert into readable text
                    StringBuilder output_blue = new StringBuilder();

                    //Build the string that goes on the watch face
                    Iterator it = MainActivity.scan_results.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        output_blue.append(MainActivity.human_read.get(pair.getKey())).append(" : ").append(pair.getValue()).append(",  U: ").append(update_result.get(pair.getKey()))
                                .append("\n");
//                        it.remove(); // avoids a ConcurrentModificationException
                    }
                    t = findViewById(R.id.bluetooth_rsi);

//                    System.out.println(mac_id+scan_results.toString());
                    if (t != null) {
                        t.setText(output_blue.toString());
                    }
                }
            }
        };
        // Start the bluetooth scanner
        mLEScanner.startScan(filters, settings, mScanCallback);

    }
//
//    public Map<String, String> getHeaders() {
//        Map<String, String> headers = new HashMap<String, String>();
//        headers.put("Content-Type", "application/json");
//        return headers;
//    }

    private void setupNetwork() {
        RequestQueue queue = Volley.newRequestQueue(this);
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("rp1", scan_results.get("B8:27:EB:69:8F:AD"));
            jsonObject.put("rp2", scan_results.get("B8:27:EB:BF:07:54"));
            jsonObject.put("rp3", scan_results.get("B8:27:EB:41:3F:64"));
            jsonObject.put("rp1_update", update_result.get("B8:27:EB:69:8F:AD"));
            jsonObject.put("rp2_update", update_result.get("B8:27:EB:BF:07:54"));
            jsonObject.put("rp3_update", update_result.get("B8:27:EB:41:3F:64"));
            jsonObject.put("brightness", BRIGHTNESS);
//            jsonObject.put("rp1", scan_results.get("B8:27:EB:69:8F:AD"));
//            jsonObject.put("rp2", scan_results.get("B8:27:EB:BF:07:54"));
//            jsonObject.put("rp3", scan_results.get("B8:27:EB:41:3F:64"));
        } catch (JSONException e) {
            System.out.println("json object failed to build");
            // handle exception
        }

//        System.out.println("This is the json object");
//        System.out.println(jsonObject.toString());

        String url = "rp1:5000/";
        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PUT, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d("Response", response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.toString());
                    }
                }
        ) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }

            @Override
            public byte[] getBody() {

                try {
                    Log.i("json", jsonObject.toString());
                    return jsonObject.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
//// Add the request to the RequestQueue.
        queue.add(putRequest);


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
        arc = findViewById(R.id.arc);
        setupNetwork();
        if (arc != null)
            arc.setProgress(BRIGHTNESS);
        return true;
    }

    private boolean brightness_down() {
        boolean handled = false;
        MainActivity.BRIGHTNESS = bound(MainActivity.BRIGHTNESS - 10);
        arc = findViewById(R.id.arc);
        setupNetwork();
        if (arc != null)
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

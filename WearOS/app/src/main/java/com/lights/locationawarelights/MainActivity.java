package com.lights.locationawarelights;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.lzyzsd.circleprogress.ArcProgress;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

//import james.wearcolorpicker.WearColorPickerActivity;

public class MainActivity extends WearableActivity {

    public static int BRIGHTNESS = 50;
    public static String COLOR = "#FFFFFF";


    private ArcProgress arc;
    public static HashMap<String, Integer> scan_results = new LinkedHashMap<>();
    public static HashMap<String, Integer> update_result = new HashMap<>();
    public static HashMap<String, String> human_read = new HashMap<>();

    private TextView t;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Launch the bluetooth service

        // Probably should be using this
        //ScanFilter.Builder builder = new ScanFilter.Builder();

        // Launch the view pager that allows for horizontal page scrolling
        ViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new CustomPagerAdapter(this));

        // Scan Results: for the specific mac ids the object that the rssi values
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
        // scan_results.put("10:A5:6B:41:D9:55", 0);
        // update_result.put("10:A5:6B:41:D9:55", 0);
        // human_read.put("10:A5:6B:41:D9:55", "Rando");


        // Defines the arc that ring that shows brightness
        arc = findViewById(R.id.arc);
        
        new Runnable(){
            @Override
            public void run() {
                run_blue();
            }
        }.run();

    }


    private void run_blue(){
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothLeScanner mLEScanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY).build();
        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        String[] filterlist = {
                "B8:27:EB:69:8F:AD",
                "B8:27:EB:BF:07:54",
                "B8:27:EB:41:3F:64",
        };
        for (int i=0; i< filterlist.length ; i++) {
            filters.add(new ScanFilter.Builder().setDeviceAddress(filterlist[i]).build());
            Log.v("Filter: "," "+ filters.get(i).getDeviceAddress());
        }


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
                    setupNetwork();

                    // Update the results of the rssi
                    MainActivity.scan_results.put(mac_id, result.getRssi());

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
                    }
                    t = findViewById(R.id.bluetooth_rsi);

                    if (t != null) {
                        t.setText(output_blue.toString());
                    }
                }
            }
        };

        // Start the bluetooth scanner
        mLEScanner.startScan(filters, settings, mScanCallback);

    }

    private void setupNetwork() {
        RequestQueue queue = Volley.newRequestQueue(this);
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("pi1", scan_results.get("B8:27:EB:69:8F:AD"));
            jsonObject.put("pi2", scan_results.get("B8:27:EB:BF:07:54"));
            jsonObject.put("pi3", scan_results.get("B8:27:EB:41:3F:64"));
            jsonObject.put("pi1_update", update_result.get("B8:27:EB:69:8F:AD"));
            jsonObject.put("pi2_update", update_result.get("B8:27:EB:BF:07:54"));
            jsonObject.put("pi3_update", update_result.get("B8:27:EB:41:3F:64"));
            jsonObject.put("brightness", BRIGHTNESS);
            jsonObject.put("color", COLOR);
        } catch (JSONException e) {
            System.out.println("json object failed to build");
        }


        String url = "http://rp1:5000/";
        InetAddress address = null;

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        TextView netBox = findViewById(R.id.netStat);
                        if(netBox != null)
                            netBox.setText("Connected!");
                        // response
                        Log.d("Response", response.toString());
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        String message = null;
                        TextView netBox = findViewById(R.id.netStat);
                        System.out.println(volleyError.toString());
                            if(netBox == null)return;
                            if (volleyError instanceof NetworkError) {
                                message = "Can't connect to Internet...";
                            } else if (volleyError instanceof ServerError) {
                                message = "The server could not be found. ";
                            } else if (volleyError instanceof AuthFailureError) {
                                message = "Can't connect to Internet...";
                            } else if (volleyError instanceof ParseError) {
                                message = "Parsing error!";
                            } else if (volleyError instanceof NoConnectionError) {
                                message = "Can't connect to Internet...";
                            } else if (volleyError instanceof TimeoutError) {
                                message = "Connection Timeout!";
                            }
                            netBox.setText(message);

                    }
                }
        ) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("content-type", "application/json");
                headers.put("accept", "application/json");
                return headers;
            }

            @Override
            public byte[] getBody() {
                Log.i("json", jsonObject.toString());
                return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
            }
        };
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

    private String to_hex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 247) {
            if (resultCode == RESULT_OK && data != null && data.hasExtra(WearColorPickerActivity.EXTRA_COLOR)) {
                COLOR = to_hex(data.getIntExtra(WearColorPickerActivity.EXTRA_COLOR, Color.BLACK));
                //do something with the color value
            } else {
                if (data != null) {
                    COLOR = to_hex(data.getIntExtra(WearColorPickerActivity.EXTRA_COLOR, Color.BLACK));
                    System.out.println(to_hex(data.getIntExtra(WearColorPickerActivity.EXTRA_COLOR, Color.BLACK)));
                    //the color has not been changed - the color picker activity has been closed without pressing the 'done' button
                }
            }
        }
        View screen = findViewById(R.id.colorpickingscreen);
        if (screen != null)screen.setBackgroundColor(Color.parseColor(COLOR));
        setupNetwork();

    }

    public void pick_acolor(View view) {
        Intent intent = new Intent(this, WearColorPickerActivity.class);
        intent.putExtra(WearColorPickerActivity.EXTRA_COLOR, COLOR);
        startActivityForResult(intent, 247);
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

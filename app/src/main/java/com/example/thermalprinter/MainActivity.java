package com.example.thermalprinter;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static BluetoothDevice device;
    private AlertDialog.Builder alertDlgBuilder;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter mBluetoothAdapter = null;
    public static BluetoothPrinter BLUETOOTH_PRINTER = null;

    private static Button mBtnConnetBluetoothDevice = null;
    private static Button mBtnPrint = null;
    private static TextView tvPosStatus = null;
    static boolean isPrinterConnected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        alertDlgBuilder = new AlertDialog.Builder(MainActivity.this);

        // Get device's Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not available in your device
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;

        }
        //Initialize
        Init();
    }

    private void Init() {
        tvPosStatus = (TextView) findViewById(R.id.tvPosStatus);
        mBtnConnetBluetoothDevice = (Button) findViewById(R.id.btn_connect_pos_device);
        mBtnConnetBluetoothDevice.setOnClickListener(mBtnConnetBluetoothDeviceOnClickListener);
        mBtnPrint = (Button) findViewById(R.id.btn_print);
        mBtnPrint.setOnClickListener(mBtnPrintOnClickListener);

    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that to be enabled.
        // initializeBluetoothDevice() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (BLUETOOTH_PRINTER == null) {
                tvPosStatus.setTextColor(this.getResources().getColor(R.color.gray));
                tvPosStatus.setText(R.string.no_pos);
                initializeBluetoothDevice();
            } else {
                if (BLUETOOTH_PRINTER.IsNoConnection()) {
                    tvPosStatus.setTextColor(this.getResources().getColor(R.color.gray));
                    tvPosStatus.setText(R.string.no_pos);
                } else {
                    tvPosStatus.setTextColor(this.getResources().getColor(R.color.green));
                    tvPosStatus.setText(R.string.title_connected_to);

                    tvPosStatus.append(device.getName());

                }
            }

        }
    }

    private void initializeBluetoothDevice() {

        // Initialize BluetoothPrinter class to perform bluetooth connections
        BLUETOOTH_PRINTER = BluetoothPrinter.getInstance();//
        BLUETOOTH_PRINTER.setHandler(new BluetoothHandler(MainActivity.this));
    }

    /**
     * The Handler that gets information back from Bluetooth Devices
     */
    class BluetoothHandler extends Handler {
        private final WeakReference<MainActivity> myWeakReference;

        //Creating weak reference of BluetoothPrinterActivity class to avoid any leak
        BluetoothHandler(MainActivity weakReference) {
            myWeakReference = new WeakReference<MainActivity>(weakReference);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = myWeakReference.get();
            if (mainActivity != null) {
                super.handleMessage(msg);
                Bundle bn = msg.getData();
                int flag = bn.getInt("flag");
                int state = bn.getInt("state");
                switch (flag) {
                    case 1:

                        switch (state) {
                            case 12:
                                tvPosStatus.setTextColor(getApplicationContext().getResources().getColor(R.color.green));
                                tvPosStatus.setText(R.string.title_connected_to);
                                tvPosStatus.append(device.getName());
                                isPrinterConnected = true;
                                Toast.makeText(getApplicationContext(), "Connection successful.", Toast.LENGTH_SHORT).show();

                                break;
                            case 4:
                                tvPosStatus.setTextColor(getApplicationContext().getResources().getColor(R.color.gray));
                                tvPosStatus.setText(R.string.title_connecting);
                                break;

                            case 11:
                                tvPosStatus.setTextColor(getApplicationContext().getResources().getColor(R.color.red));
                                tvPosStatus.setText(R.string.no_pos);
                                break;
                        }
                        break;
                    case 4:

                        tvPosStatus.setTextColor(getApplicationContext().getResources().getColor(R.color.gray));
                        tvPosStatus.setText(R.string.title_connecting);
                        break;
                    case 2:
                        tvPosStatus.setTextColor(getApplicationContext().getResources().getColor(R.color.red));
                        Toast.makeText(getApplicationContext(), "Connection failed.", Toast.LENGTH_SHORT).show();

                        break;
                    default:
                        tvPosStatus.setTextColor(getApplicationContext().getResources().getColor(R.color.gray));
                        tvPosStatus.setText(R.string.no_pos);
                        break;

                }
            }
        }

        ;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When POSDeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(POSDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    BLUETOOTH_PRINTER.start();
                    BLUETOOTH_PRINTER.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    initializeBluetoothDevice();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_on_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }


    View.OnClickListener mBtnQuitOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Stop the Bluetooth chat services
            if (!PrintPosReceipt.printPOSReceipt(getApplicationContext())) {
                Toast.makeText(MainActivity.this, "No printer is connected!!", Toast.LENGTH_LONG).show();
            }

        }

        ;
    };

    View.OnClickListener mBtnPrintOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            PrintPosReceipt.printPOSReceipt(MainActivity.this);
        }
    };

    View.OnClickListener mBtnConnetBluetoothDeviceOnClickListener = new View.OnClickListener() {
        Intent serverIntent = null;

        public void onClick(View arg0) {

            //If bluetooth is disabled then ask user to enable it again
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {//If the connection is lost with last connected bluetooth printer
                if (BLUETOOTH_PRINTER.IsNoConnection()) {
                    serverIntent = new Intent(MainActivity.this, POSDeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else { //If an existing connection is still alive then ask user to kill it and re-connect again
                    alertDlgBuilder.setTitle(getResources().getString(R.string.title));
                    alertDlgBuilder.setMessage(getResources().getString(R.string.message));
                    alertDlgBuilder.setNegativeButton(getResources().getString(R.string.btn_negative), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }
                    );
                    alertDlgBuilder.setPositiveButton(getResources().getString(R.string.btn_positive), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    BLUETOOTH_PRINTER.stop();
                                    serverIntent = new Intent(MainActivity.this, POSDeviceListActivity.class);
                                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                                }
                            }
                    );
                    alertDlgBuilder.show();

                }
            }

        }

        ;

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BLUETOOTH_PRINTER.IsNoConnection())
            BLUETOOTH_PRINTER.stop();
    }
}
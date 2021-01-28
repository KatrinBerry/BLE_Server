package com.iagodkin.ble_server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MenuItem menuItem;
    private BluetoothAdapter btAdapter;
    private BluetoothManager btManager;
    private BluetoothLeAdvertiser btLeAdvertiser;
    private BluetoothGattServer btGattServer;

    public final int ENABLE_REQUEST = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        if (!checkBluetoothSupport(btAdapter)) {
            finish();
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!btAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently off... turning on");
            btAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth turned on... starting services");
            startAdvertising();
            startServer();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.server_menu, menu);
        menuItem = menu.findItem(R.id.id_bt_btn);
        setBtIcon();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.id_bt_btn){
            if (!btAdapter.isEnabled()){
                enableBt();
            } else {
                btAdapter.disable();
                menuItem.setIcon(R.drawable.ic_bt_on);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ENABLE_REQUEST){
            if (resultCode == RESULT_OK){
                setBtIcon();
            }
        }
    }

    private void setBtIcon(){
        if(btAdapter.isEnabled()){
            menuItem.setIcon(R.drawable.ic_bt_off);
        } else {
            menuItem.setIcon(R.drawable.ic_bt_on);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
//        registerReceiver()
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (btAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }

        unregisterReceiver(mBluetoothReceiver);
    }

    private void init(){
        btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
    }

    private void enableBt(){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent,ENABLE_REQUEST);
    }

    private boolean checkBluetoothSupport (BluetoothAdapter bluetoothAdapter){
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth isn't supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE isn't supported");
            return false;
        }

        return true;
    }

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
            }
        }
    };

    private void startAdvertising() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btLeAdvertiser = btAdapter.getBluetoothLeAdvertiser();
            if (btLeAdvertiser == null) {
                Log.w(TAG, "Advertiser wasn't create");
                return;
            }

            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .build();

            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(true)
                    .addServiceUuid(ParcelUuid.fromString("7C300001-A3B4-G282-F058-000000000023"))
                    .build();

            btLeAdvertiser.startAdvertising(settings, data, btAdvertiserCallback);
        }
    }

    private void stopAdvertising() {
        if (btLeAdvertiser == null) return;

        btLeAdvertiser.stopAdvertising(btAdvertiseCallback);
    }

    private void startServer() {
        btGattServer = btManager.openGattServer(this, gattServerCallback);
        if(btGattServer == null) {
            Log.w(TAG, "GATT server can't create");
            return;
        }

        btGattServer.addService()
    }


}
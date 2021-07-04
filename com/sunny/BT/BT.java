package com.sunny.BT;
import android.app.Activity;
import android.os.Build;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.ActivityResultListener;
@DesignerComponent(androidMinSdk = 17,
    version = 1,
    versionName = "1.2", 
    description = "An extension to work with BlueTooth<br>Developed by Sunny Gupta", 
    category = ComponentCategory.EXTENSION, 
    nonVisible = true, 
    iconName = "https://res.cloudinary.com/andromedaviewflyvipul/image/upload/c_scale,h_20,w_20/v1571472765/ktvu4bapylsvnykoyhdm.png", 
    helpUrl = "https://community.appinventor.mit.edu/t/bt-an-extension-to-work-with-bluetooth/15379")
@SimpleObject(external = true)
@UsesPermissions(permissionNames="android.permission.BLUETOOTH,android.permission.BLUETOOTH_ADMIN,android.permission.ACCESS_FINE_LOCATION,android.permission.ACCESS_COARSE_LOCATION")
public class BT extends AndroidNonvisibleComponent implements OnDestroyListener,ActivityResultListener{
	public Activity activity;
    public int duration = 120;
    public int PERMISSION_CODE;
    public List<String> newlist = new ArrayList<>();
    public List<String> pairedlist = new ArrayList<>();
    public BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            OnStateChange(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR));
        }
    };
    public BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    OnDiscoveryStarted();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = bluetoothDevice.getAddress();
                        if (IsPairedDevice(address)) {
                            pairedlist.add(address);
                        }else{
                            newlist.add(address);
                        }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    OnDiscoveryFinished(pairedlist,newlist);
                    newlist.clear();
                    pairedlist.clear();
                    break;
            }
        }
    };
    BroadcastReceiver pairRequest = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    OnPairingStateChanged(device.getAddress(), true);
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    OnPairingStateChanged(device.getAddress(), false);
                }
            }
        }
    };
    @Override
    public void resultReturned(int requestCode, int resultCode, Intent data){
        if (requestCode == PERMISSION_CODE) {
            AfterSetDuration(resultCode == duration,duration);
        }
    }

    public BT(ComponentContainer container){
    	super(container.$form());
    	activity = (Activity) container.$context();
        PERMISSION_CODE = form.registerForActivityResult(this);
    	activity.registerReceiver(broadcastReceiver,new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        activity.registerReceiver(discoveryReceiver,intentFilter);
        IntentFilter pair = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        activity.registerReceiver(pairRequest,pair);
    }
   	@SimpleFunction(description="Enables/Disables bluetooth.")
    public void Toggle(boolean enable){
        try{
        if (enable){
            mBluetoothAdapter.enable();
            return;
        }
        mBluetoothAdapter.disable();
    }catch(Exception e){
        parseException(e);
    }
    }
    @SimpleFunction(description="Returns true if given address points to a paired device.")
    public boolean IsPairedDevice(String address){
        return GetPairedDevices().contains(address);
    }
    @SimpleEvent(description="Event indicating that discovery has been started.")
    public void OnDiscoveryStarted(){
    	EventDispatcher.dispatchEvent(this,"OnDiscoveryStarted");
    }
    @SimpleFunction(description="Returns bt device name from address.")
    public String GetName(String address){
        try{
            return mBluetoothAdapter.getRemoteDevice(address).getName();
        }catch(Exception e){
            parseException(e);
            return "";
        }
    }
    @SimpleEvent(description="Event indicating that discovery has been finished and returns paired and new devices.")
    public void OnDiscoveryFinished(List<String> pairedDevices,List<String> newDevices){
    	EventDispatcher.dispatchEvent(this,"OnDiscoveryFinished",pairedDevices,newDevices);
    }
    @SimpleFunction(description="Returns a list of paired devices.")
    public List<String> GetPairedDevices(){
        List<String> paired = new ArrayList<String>();
        for (BluetoothDevice bd:mBluetoothAdapter.getBondedDevices()){
            paired.add(bd.getAddress());
        }
        return paired;
    }
    @SimpleFunction(description="Returns true if bluetooth is enabled.")
    public boolean IsEnabled(){
        return mBluetoothAdapter.isEnabled();
    }
    @SimpleFunction(description="Renames the device.")
    public boolean Rename(String newName){
        try{
            boolean en = false;
                while (!en){
                    en = IsEnabled();
                    Toggle(true);
                }
            return mBluetoothAdapter.setName(newName);
        }catch(Exception e){
            parseException(e);
            return false;
        }
        
    }
    @SimpleFunction(description="Tries to start discovery for nearby devices.")
    public void StartDiscovery(){
        mBluetoothAdapter.startDiscovery();
    }
    @SimpleFunction(description="Tries to stop discovery for nearby devices.")
    public void CancelDiscovery(){
        mBluetoothAdapter.cancelDiscovery();
    }
    @SimpleFunction(description="Returns device's bluetooth name.")
    public String Name(){
        return mBluetoothAdapter.getName();
    }
    @SimpleFunction(description="Returns device's bluetooth address.")
    public String Address(){
        return mBluetoothAdapter.getAddress();
    }
    @SimpleEvent(description="Event indicating that device's bluetooth state has changed.")
    public void OnStateChange(int state){
    	EventDispatcher.dispatchEvent(this,"OnStateChange",state);
    }
    @SimpleEvent(description="Event indicating that a device's state has changed.Either it has paired or unpaired with this device.")
    public void OnPairingStateChanged(String address,boolean isPaired){
    	EventDispatcher.dispatchEvent(this,"OnPairingStateChanged",address,isPaired);
    }
    @SimpleFunction(description="Tries to pair with given device.")
    public void Pair(String address){
        try{
            mBluetoothAdapter.getRemoteDevice(address).createBond();
        }catch (Exception e){
            parseException(e);
        }
    }
    @SimpleFunction(description="Tries to unpair with given device.")
    public void UnPair(String address){
        try{
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        }catch (Exception e){
            parseException(e);
        }
    }
    @SimpleFunction(description="Returns bluetooth scan mode.")
    public int GetScanMode(){
        return mBluetoothAdapter.getScanMode();
    }
    @SimpleFunction(description="Returns true if given address is valid.")
    public boolean IsValidAddress(String address){
        return BluetoothAdapter.checkBluetoothAddress(address);
    }
    @SimpleFunction(description="Returns true if device is searching for nearby bluetooth devices.")
    public boolean IsDiscovering(){
        return mBluetoothAdapter.isDiscovering();
    }
    @SimpleEvent(description="Even indicating that a error has been occurred and returns error message.")
    public void OnError(String errorMessage){
        EventDispatcher.dispatchEvent(this,"OnError",errorMessage);
    }
    public void parseException(Exception e){
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) {
            OnError(e.toString());
        }else{
            OnError(msg);
        }
    }
    @SimpleFunction(description="Sets visibility of device in seconds.")
    public void SetVisibility(int seconds){
        duration = seconds;
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
            activity.startActivityForResult(discoverableIntent,PERMISSION_CODE);
        /*if (Build.VERSION.SDK_INT < 24) {
            Method method;
                try {
                    method = mBluetoothAdapter.getClass().getMethod("setScanMode", int.class, int.class);
                    method.invoke(mBluetoothAdapter,BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,seconds);
                 }
                catch (Exception e){
                    parseException(e);
                }
        }else{
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
            activity.startActivity(discoverableIntent);
        }*/
    }
    @SimpleEvent(description="Event indicating whether duration was set successfuly or not.")
    public void AfterSetDuration(boolean success,int seconds){
        EventDispatcher.dispatchEvent(this,"AfterSetDuration",success,seconds);
    }
    @Override
    public void onDestroy(){
        activity.unregisterReceiver(broadcastReceiver);
        activity.unregisterReceiver(discoveryReceiver);
        activity.unregisterReceiver(pairRequest);
    }
}

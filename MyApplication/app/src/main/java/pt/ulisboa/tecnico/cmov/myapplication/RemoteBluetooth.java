package pt.ulisboa.tecnico.cmov.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import pt.ulisboa.tecnico.cmov.myapplication.biometric.BiometricCallback;
import pt.ulisboa.tecnico.cmov.myapplication.biometric.BiometricManager;

import java.util.Timer;
import java.util.TimerTask;

public class RemoteBluetooth extends Activity  implements BiometricCallback {

    // Layout view
    private TextView mTitle;

    private Timer timer;

    // Intent request codesSharedPreference
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothCommandService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for Bluetooth Command Service
    private BluetoothCommandService mCommandService = null;

    BiometricManager mBiometricManager;

    /**
     * Called when the activity is first created.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 55);
        }

        // Set up the window layout

        //setContentView(R.layout.main);
        setContentView(R.layout.comimagem);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupCommand() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        // otherwise set up the command service
        else {
            if (mCommandService == null)
                setupCommand();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mCommandService != null) {
            if (mCommandService.getState() == BluetoothCommandService.STATE_NONE) {
                mCommandService.start();
            }
        }

        if (timer != null) {
            timer.cancel();
            Log.i("Main", "cancel timer");
            timer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        timer = new Timer();
        Log.i("Main", "Invoking logout timer");
        LogOutTimerTask logoutTimeTask = new LogOutTimerTask();
        timer.schedule(logoutTimeTask, 10000); //auto logout in 1 minute
    }

    private class LogOutTimerTask extends TimerTask {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void run() {
            finish();
        }
    }

    private void setupCommand() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mCommandService = new BluetoothCommandService(this, mHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCommandService != null)
            mCommandService.stop();
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private void setupDevice() {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);


        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothCommandService.STATE_CONNECTED:
                            break;
                        case BluetoothCommandService.STATE_CONNECTING:
                            break;
                        case BluetoothCommandService.STATE_LISTEN:
                        case BluetoothCommandService.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mCommandService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupCommand();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 55:

                startHelp();
                autheticateFingerPrint();

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void autheticateFingerPrint() {
        /*
         *
         * */
        mBiometricManager = new BiometricManager.BiometricBuilder(RemoteBluetooth.this)
                .setTitle("DriveKeeper")
                .setSubtitle(" ")
                .setDescription("Validate your Finger")
                .setNegativeButtonText(" ")
                .build();

        //start authentication
        mBiometricManager.authenticate(RemoteBluetooth.this);
    }

    public void startHelp(){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(RemoteBluetooth.this);
        builder1.setMessage("Use the physical device buttons!\n\nVolume Up for Encoding\nVolume Down for Decoding\n");
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "Ok, I Understand",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;

            case R.id.setup:
                // Ensure this device is discoverable by others
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    AssymetricUtils assymetricUtils = new AssymetricUtils(this);
                }
                return true;
            case R.id.help:
                // Ensure this device is discoverable by others
                startHelp();
                return true;
        }
        return false;
    }

    int flag1 = 0;
    int flag2 = 0;
    int keyCode = -1;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if(flag1==0){
                this.keyCode = keyCode;
                autheticateFingerPrint();

                return true;
            }else{
                Toast.makeText(this, "Files already ciphered", Toast.LENGTH_SHORT).show();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if(flag2==0){
                this.keyCode = keyCode;
                autheticateFingerPrint();

                return true;
            }else{
                Toast.makeText(this, "Files already deciphered", Toast.LENGTH_SHORT).show();
                return true;
            }

        }

        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onSdkVersionNotSupported() {
        /*
         *  Will be called if the device sdk version does not support Biometric authentication
         */
        Toast.makeText(getApplicationContext(), "device sdk version does not support Biometric authentication", Toast.LENGTH_SHORT);
    }

    @Override
    public void onBiometricAuthenticationNotSupported() {
        /*
         *  Will be called if the device does not contain any fingerprint sensors
         */
        Toast.makeText(getApplicationContext(), "device does not contain any fingerprint sensors", Toast.LENGTH_SHORT);
    }

    @Override
    public void onBiometricAuthenticationNotAvailable() {
        /*
         *  The device does not have any biometrics registered in the device.
         */
        Toast.makeText(getApplicationContext(), "device does not have any biometrics registered in the device.", Toast.LENGTH_SHORT);
    }

    @Override
    public void onBiometricAuthenticationPermissionNotGranted() {
        /*
         *  android.permission.USE_BIOMETRIC permission is not granted to the app
         */
        Toast.makeText(getApplicationContext(), "permission is not granted to the app", Toast.LENGTH_SHORT);
    }

    @Override
    public void onBiometricAuthenticationInternalError(String error) {
        /*
         *  This method is called if one of the fields such as the title, subtitle,
         * description or the negative button text is empty
         */
        Toast.makeText(getApplicationContext(), "InternalError", Toast.LENGTH_SHORT);
    }

    @Override
    public void onAuthenticationFailed() {
        /*
         * When the fingerprint doesn’t match with any of the fingerprints registered on the device,
         * then this callback will be triggered.
         */
        Toast.makeText(getApplicationContext(), "the fingerprint doesn’t match with any of the fingerprints registered on the device", Toast.LENGTH_SHORT);
    }

    @Override
    public void onAuthenticationCancelled() {
        /*
         * The authentication is cancelled by the user.
         */
        Toast.makeText(getApplicationContext(), "The authentication is cancelled by the user.", Toast.LENGTH_SHORT);
    }

    @Override
    public void onAuthenticationSuccessful() {
        /*
         * When the fingerprint is has been successfully matched with one of the fingerprints
         * registered on the device, then this callback will be triggered.
         */
        Toast.makeText(getApplicationContext(), "the fingerprint is has been successfully matched with one of the fingerprints", Toast.LENGTH_SHORT);

        if (keyCode != -1 && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mCommandService.write(BluetoothCommandService.VOL_UP);
            flag1++;
            flag2=0;
        }else if (keyCode != -1 ){
            mCommandService.write(BluetoothCommandService.VOL_DOWN);
            flag2++;
            flag1=0;
        }
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        /*
         * This method is called when a non-fatal error has occurred during the authentication
         * process. The callback will be provided with an help code to identify the cause of the
         * error, along with a help message.
         */
        Toast.makeText(getApplicationContext(), "Help", Toast.LENGTH_SHORT);
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        /*
         * When an unrecoverable error has been encountered and the authentication process has
         * completed without success, then this callback will be triggered. The callback is provided
         * with an error code to identify the cause of the error, along with the error message.
         */
        Toast.makeText(getApplicationContext(), "Error" + errorCode, Toast.LENGTH_SHORT);
    }

}
package cordova.plugin.nfc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import static android.nfc.NfcAdapter.EXTRA_ADAPTER_STATE;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.util.Arrays;

/**
 * This class echoes a string called from JavaScript.
 */
public class NfcPlugin extends CordovaPlugin {

    public static final String TAG = "nfcHcePlugin";

    private  CallbackContext _callbackContext;
    private CordovaInterface _cordova;
    public Context applicationContext;
    public static NfcManager nfcManager;
    public static NfcPlugin instance = null;


    /*************
     * Constants *
     *************/

    public static final int NFC_STATE_VALUE_UNKNOWN = 0;
    public static final int NFC_STATE_VALUE_OFF = 1;
    public static final int NFC_STATE_VALUE_TURNING_ON = 2;
    public static final int NFC_STATE_VALUE_ON = 3;
    public static final int NFC_STATE_VALUE_TURNING_OFF = 4;

    public static final String NFC_STATE_UNKNOWN = "unknown";
    public static final String NFC_STATE_OFF = "powered_off";
    public static final String NFC_STATE_TURNING_ON = "powering_on";
    public static final String NFC_STATE_ON = "powered_on";
    public static final String NFC_STATE_TURNING_OFF = "powering_off";

    protected String currentNFCState = NFC_STATE_UNKNOWN;

    // AID
    private static String HCE_AID;
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};

    // Recommend NfcAdapter flags for reading from other Android devices. Indicates that this
    // activity is interested in NFC-A devices (including other Android devices), and that the
    // system should not check for the presence of NDEF-formatted data (e.g. Android Beam).
    public static int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.v(TAG, "initialize: NfcPlugin");

        instance = this;
        _cordova = cordova;
        applicationContext = cordova.getActivity().getApplicationContext();

        try {
            applicationContext.registerReceiver(NFCStateChangedReceiver, new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED));
            nfcManager = (NfcManager) applicationContext.getSystemService(Context.NFC_SERVICE);
        }catch(Exception e){
            Log.e(TAG, "Unable to register NFC state change receiver: " + e.getMessage());
        }

        try {
            currentNFCState = isNFCAvailable() ? NFC_STATE_ON : NFC_STATE_OFF;
        }catch(Exception e){
            Log.w(TAG, "Unable to get initial NFC state: " + e.getMessage());
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        
        _callbackContext = callbackContext;

        if (action.equals("switchToNFCSettings")){
            switchToNFCSettings();
            callbackContext.success();
            return true;
        } else if(action.equals("isNFCPresent")) {
            callbackContext.success(isNFCPresent() ? 1 : 0);
            return true;
        } else if(action.equals("isNFCEnabled")) {
            callbackContext.success(isNFCEnabled() ? 1 : 0);
            return true;
        } else if(action.equals("isNFCAvailable")) {
            callbackContext.success(isNFCAvailable() ? 1 : 0);
            return true;
        } else if (action.equals("readHce")) {
            HCE_AID = args.getString(0);
            readHce(callbackContext);
            return true;
        } else if (action.equals("disableReaderMode")) {
            disableReaderMode(callbackContext);
            return true;
        }
        return false;
    }

    /************
     * Internals
     ***********/

    public void switchToNFCSettings() {
        Log.d(TAG, "Switch to NFC Settings");
        Intent settingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            settingsIntent = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
        }
        _cordova.getActivity().startActivity(settingsIntent);
    }

    public boolean isNFCPresent() {
        boolean result = false;
        try {
            NfcAdapter adapter = nfcManager.getDefaultAdapter();
            result = adapter != null;
        }catch(Exception e){
            Log.e(TAG, e.getMessage());
        }
        return result;
    }

    public boolean isNFCEnabled() {
        boolean result = false;
        try {
            NfcAdapter adapter = nfcManager.getDefaultAdapter();
            result = adapter != null && adapter.isEnabled();
        }catch(Exception e){
            Log.e(TAG, e.getMessage());
        }
        return result;
    }

    public boolean isNFCAvailable() {
        boolean result = isNFCPresent() && isNFCEnabled();
        return result;
    }

    public void notifyNFCStateChange(int stateValue){
        String newState = getNFCState(stateValue);
        try {
            if(newState != currentNFCState){
                Log.d(TAG, "NFC state changed to: " + newState);
                executePluginJavascript("onNFCStateChange(\"" + newState +"\");");
                currentNFCState = newState;
            }
        }catch(Exception e){
            Log.e(TAG, "Error retrieving current NFC state on state change: "+e.toString());
        }
    }

    public String getNFCState(int stateValue){

        String state;
        switch(stateValue){
            case NFC_STATE_VALUE_OFF:
                state = NFC_STATE_OFF;
                break;
            case NFC_STATE_VALUE_TURNING_ON:
                state = NFC_STATE_TURNING_ON;
                break;
            case NFC_STATE_VALUE_ON:
                state = NFC_STATE_ON;
                break;
            case NFC_STATE_VALUE_TURNING_OFF:
                state = NFC_STATE_TURNING_OFF;
                break;
            default:
                state = NFC_STATE_UNKNOWN;
        }
        return state;
    }

    public void executePluginJavascript(final String jsString){
        _cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:cordova.plugins.NfcPlugin." + jsString);
            }
        });
    }

    private void readHce(CallbackContext callbackContext) {
        Log.i(TAG, "readHce");
        Activity activity = _cordova.getActivity();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.enableReaderMode(activity, readerCallback, READER_FLAGS, null);
        }
    }

    private void disableReaderMode(CallbackContext callbackContext) {
        Log.i(TAG, "Disabling reader mode");
        Activity activity = _cordova.getActivity();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.disableReaderMode(activity);
        }
        callbackContext.success();
    }

    private NfcAdapter.ReaderCallback readerCallback = new NfcAdapter.ReaderCallback() {
        @Override
        public void onTagDiscovered(Tag tag) {
            Log.i(TAG, "New tag discovered");
            // Android's Host-based Card Emulation (HCE) feature implements the ISO-DEP (ISO 14443-4)
            // protocol.
            //
            // In order to communicate with a device using HCE, the discovered tag should be processed
            // using the IsoDep class.
            IsoDep isoDep = IsoDep.get(tag);
            if (isoDep != null) {
                try {
                    // Connect to the remote NFC device
                    isoDep.connect();
                    // Build SELECT AID command for our loyalty card service.
                    // This command tells the remote device which service we wish to communicate with.
                    Log.i(TAG, "Requesting remote AID: " + HCE_AID);
                    byte[] command = BuildSelectApdu(HCE_AID);
                    // Send command to remote device
                    Log.i(TAG, "Sending: " + ByteArrayToHexString(command));
                    byte[] result = isoDep.transceive(command);
                    // If AID is successfully selected, 0x9000 is returned as the status word (last 2
                    // bytes of the result) by convention. Everything before the status word is
                    // optional payload, which is used here to hold the account number.
                    int resultLength = result.length;
                    byte[] statusWord = {result[resultLength-2], result[resultLength-1]};
                    byte[] payload = Arrays.copyOf(result, resultLength-2);
                    if (Arrays.equals(SELECT_OK_SW, statusWord)) {
                        String stringPayload = new String(payload, "UTF-8");
                        Log.i(TAG, "Received: " + stringPayload);
                        _callbackContext.success(stringPayload);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error communicating with hce: " + e.toString());
                }
            }
        }
    };

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }

    /**
     * Utility class to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Utility class to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /************
     * Overrides
     ***********/

    protected final BroadcastReceiver NFCStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        try {
            final String action = intent.getAction();
            if(instance != null && action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)){

                Log.v(TAG, "onReceiveNFCStateChange");
                final int stateValue = intent.getIntExtra(EXTRA_ADAPTER_STATE, -1);
                instance.notifyNFCStateChange(stateValue);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error receiving NFC state change: "+e.toString());
        }
        }
    };
}

package com.zebra.scannercontrol.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.zebra.scannercontrol.SDKHandler;
import com.zebra.scannercontrol.app.R;
import com.zebra.scannercontrol.app.application.Application;
import com.zebra.scannercontrol.app.dialogs.ScannerDialog;
import com.zebra.scannercontrol.app.helpers.AvailableScanner;
import com.zebra.scannercontrol.app.helpers.Barcode;
import com.zebra.scannercontrol.app.helpers.Constants;
import com.zebra.scannercontrol.app.helpers.ScannerAppEngine;

import java.util.ArrayList;
import java.util.List;

import static com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_CONN_TYPES.DCSSDK_CONNTYPE_BT_NORMAL;
import static com.zebra.scannercontrol.app.helpers.Constants.DEBUG_TYPE.TYPE_DEBUG;
import static com.zebra.scannercontrol.app.helpers.Constants.logAsMessage;

public class BaseActivity extends AppCompatActivity implements ScannerAppEngine, IDcsSdkApiDelegate {
    protected static String TAG="BaseActivity";
    private static ArrayList<IScannerAppEngineDevConnectionsDelegate> mDevConnDelegates = new ArrayList<IScannerAppEngineDevConnectionsDelegate>();
    private static ArrayList<IScannerAppEngineDevEventsDelegate> mDevEventsDelegates = new ArrayList<IScannerAppEngineDevEventsDelegate>();
    private static ArrayList<DCSScannerInfo> mScannerInfoList;
    Button scanner_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScannerInfoList = Application.mScannerInfoList;
        TAG = getClass().getSimpleName();
        if (Application.sdkHandler == null) {
            Application.sdkHandler = new SDKHandler(this, false);
        }

        Application.sdkHandler.dcssdkSetDelegate(this);
        initializeDcsSdkWithAppSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.sdkHandler.dcssdkSetDelegate(this);
        IntentFilter filter = new IntentFilter(Constants.ACTION_SCANNER_CONNECTED);
        filter.addAction(Constants.ACTION_SCANNER_DISCONNECTED);
        filter.addAction(Constants.ACTION_SCANNER_AVAILABLE);
        filter.addAction(Constants.ACTION_SCANNER_CONN_FAILED);
        filter.setPriority(2);
        TAG = getClass().getSimpleName();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void initializeDcsSdkWithAppSettings() {
        // Restore preferences
        Log.d("func_flow",TAG+"5");
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);

        Application.MOT_SETTING_EVENT_ACTIVE = settings.getBoolean(Constants.PREF_EVENT_ACTIVE, true);
        Application.MOT_SETTING_EVENT_AVAILABLE = settings.getBoolean(Constants.PREF_EVENT_AVAILABLE, true);
        Application.MOT_SETTING_EVENT_BARCODE = settings.getBoolean(Constants.PREF_EVENT_BARCODE, true);
        int notifications_mask = 0;
        if (Application.MOT_SETTING_EVENT_AVAILABLE)
        {
            notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
        }
        if (Application.MOT_SETTING_EVENT_ACTIVE)
        {
            notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
        }
        if (Application.MOT_SETTING_EVENT_BARCODE)
        {
            notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
        }
        Application.sdkHandler.dcssdkSubsribeForEvents(notifications_mask);

    }


    /* ###################################################################### */
    /* ########## Utility functions ######################################### */
    /* ###################################################################### */
    @Override
    public void showMessageBox(String message) {
        //TODO - Handle the callback from SDK Handler
    }

    @Override
    public int showBackgroundNotification(String text) {
        //TODO - Handle the callback from SDK Handler
        return 0;
    }

    @Override
    public int dismissBackgroundNotifications() {
        //TODO - Handle the callback from SDK Handler
        return 0;
    }

    @Override
    public boolean isInBackgroundMode(final Context context) {
        return false;
    }

    @Override
    public void addDevListDelegate(IScannerAppEngineDevListDelegate delegate) {
        Log.d("func_flow",TAG+"6");
        if (Application.mDevListDelegates == null)
            Application.mDevListDelegates = new ArrayList<IScannerAppEngineDevListDelegate>();
        Application.mDevListDelegates.add(delegate);
    }

    @Override
    public void addDevConnectionsDelegate(IScannerAppEngineDevConnectionsDelegate delegate) {
        Log.d("func_flow",TAG+"7");
        if (mDevConnDelegates == null)
            mDevConnDelegates = new ArrayList<IScannerAppEngineDevConnectionsDelegate>();
        mDevConnDelegates.add(delegate);
    }

    @Override
    public void addDevEventsDelegate(IScannerAppEngineDevEventsDelegate delegate) {
        Log.d("func_flow",TAG+"8");
        if (mDevEventsDelegates == null)
            mDevEventsDelegates = new ArrayList<IScannerAppEngineDevEventsDelegate>();
        mDevEventsDelegates.add(delegate);
    }

    @Override
    public void removeDevListDelegate(IScannerAppEngineDevListDelegate delegate) {
        Log.d("func_flow",TAG+"9");
        if (Application.mDevListDelegates != null)
            Application.mDevListDelegates.remove(delegate);
    }

    @Override
    public void removeDevConnectiosDelegate(IScannerAppEngineDevConnectionsDelegate delegate) {
        Log.d("func_flow",TAG+"10");
        if (mDevConnDelegates != null)
            mDevConnDelegates.remove(delegate);
    }

    @Override
    public void removeDevEventsDelegate(ScannerAppEngine.IScannerAppEngineDevEventsDelegate delegate) {
        Log.d("func_flow",TAG+"11");
        if (mDevEventsDelegates != null)
            mDevEventsDelegates.remove(delegate);
    }

    @Override
    public List<DCSScannerInfo> getActualScannersList() {
        Log.d("func_flow",TAG+"12");
        return mScannerInfoList;
    }


    @Override
    public DCSScannerInfo getScannerByID(int scannerId) {
        Log.d("func_flow",TAG+"14");
        if (mScannerInfoList != null) {
            for (DCSScannerInfo scannerInfo : mScannerInfoList) {
                if (scannerInfo != null && scannerInfo.getScannerID() == scannerId)
                    return scannerInfo;
            }
        }
        return null;
    }

    @Override
    public void raiseDeviceNotificationsIfNeeded() {

    }

    /* ###################################################################### */
    /* ########## Interface for DCS SDK ##################################### */
    /* ###################################################################### */
    @Override
    public void updateScannersList() {
        Log.d("func_flow",TAG+"15");
        if (Application.sdkHandler != null) {
            mScannerInfoList.clear();
            ArrayList<DCSScannerInfo> scannerTreeList = new ArrayList<DCSScannerInfo>();
            Application.sdkHandler.dcssdkGetAvailableScannersList(scannerTreeList);
            Application.sdkHandler.dcssdkGetActiveScannersList(scannerTreeList);
            createFlatScannerList(scannerTreeList);
        }
    }

    private void createFlatScannerList(ArrayList<DCSScannerInfo> scannerTreeList) {
        Log.d("func_flow",TAG+"16");
        for (DCSScannerInfo s : scannerTreeList) {
            addToScannerList(s);
        }
    }

    private void addToScannerList(DCSScannerInfo s) {
        Log.d("func_flow",TAG+"17");
        mScannerInfoList.add(s);
        if (s.getAuxiliaryScanners() != null) {
            for (DCSScannerInfo aux :
                    s.getAuxiliaryScanners().values()) {
                addToScannerList(aux);
            }
        }
    }


    @Override
    public DCSSDKDefs.DCSSDK_RESULT connect(int scannerId) {
        Log.d("func_flow",TAG+"17");
        if (Application.sdkHandler != null) {
            if (ScannersActivity.curAvailableScanner != null) {
                Application.sdkHandler.dcssdkTerminateCommunicationSession(ScannersActivity.curAvailableScanner.getScannerId());
            }
            return Application.sdkHandler.dcssdkEstablishCommunicationSession(scannerId);
        } else {
            return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
        }
    }

    @Override
    public void disconnect(int scannerId) {
        Log.d("func_flow",TAG+"18");
        if (Application.sdkHandler != null) {
            DCSSDKDefs.DCSSDK_RESULT ret = Application.sdkHandler.dcssdkTerminateCommunicationSession(scannerId);
            ScannersActivity.curAvailableScanner = null;
            Application.intentionallyDisconnected =true;
            updateScannersList();
        }
    }

    @Override
    public DCSSDKDefs.DCSSDK_RESULT setAutoReconnectOption(int scannerId, boolean enable) {
        Log.d("func_flow",TAG+"19");
        DCSSDKDefs.DCSSDK_RESULT ret;
        if (Application.sdkHandler != null) {
            ret = Application.sdkHandler.dcssdkEnableAutomaticSessionReestablishment(enable, scannerId);
            return ret;
        }
        return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
    }

    @Override
    public void enableScannersDetection(boolean enable) {
        Log.d("func_flow",TAG+"20");
        if (Application.sdkHandler != null) {
            Application.sdkHandler.dcssdkEnableAvailableScannersDetection(enable);
        }
    }

    @Override
    public void enableBluetoothScannerDiscovery(boolean enable) {
        Log.d("func_flow",TAG+"21");
        if (Application.sdkHandler != null) {
            Application.sdkHandler.dcssdkEnableBluetoothScannersDiscovery(enable);
        }
    }

    @Override
    public void configureNotificationAvailable(boolean enable) {
        Log.d("func_flow",TAG+"22");
        if (Application.sdkHandler != null) {
            if (enable) {
                Application.sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
            } else {
                Application.sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
            }
        }
    }

    @Override
    public void configureNotificationActive(boolean enable) {
        Log.d("func_flow",TAG+"23");
        if (Application.sdkHandler != null) {
            if (enable) {
                Application.sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
            } else {
                Application.sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
            }
        }
    }

    @Override
    public void configureNotificationBarcode(boolean enable)
    {
        Log.d("func_flow",TAG+"24");
        if (Application.sdkHandler != null) {
            if (enable) {
                Application.sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
            } else {
                Application.sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
            }
        }
    }

    @Override
    public void configureNotificationImage(boolean enable)
    {
        Log.d("func_flow",TAG+"25");
    }

    @Override
    public void configureNotificationVideo(boolean enable)
    {

    }

    @Override
    public void configureOperationalMode(DCSSDKDefs.DCSSDK_MODE mode)
    {

    }

    @Override
    public boolean executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, StringBuilder outXML, int scannerID) {
        Log.d("func_flow",TAG+"28");
        if (Application.sdkHandler != null) {
            if (outXML == null) {
                outXML = new StringBuilder();
            }
            DCSSDKDefs.DCSSDK_RESULT result = Application.sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode, inXML, outXML, scannerID);
            if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS)
                return true;
            else if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE)
                return false;
        }
        return false;
    }

    @Override
    public void dcssdkEventScannerAppeared(DCSScannerInfo availableScanner) {
        Log.d("func_flow",TAG+"30");
        dataHandler.obtainMessage(Constants.SCANNER_APPEARED, availableScanner).sendToTarget();
    }

    @Override
    public void dcssdkEventScannerDisappeared(int scannerID) {
        Log.d("func_flow",TAG+"31");
        dataHandler.obtainMessage(Constants.SCANNER_DISAPPEARED, scannerID).sendToTarget();
       int scId = Application.currentScannerId;
    }

    @Override
    public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo activeScanner) {
        Log.d("func_flow",TAG+"32");
        dataHandler.obtainMessage(Constants.SESSION_ESTABLISHED, activeScanner).sendToTarget();
    }

    @Override
    public void dcssdkEventCommunicationSessionTerminated(int scannerID) {
        Log.d("func_flow",TAG+"33");
        dataHandler.obtainMessage(Constants.SESSION_TERMINATED, scannerID).sendToTarget();

    }

    @Override
    public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
        Log.d("func_flow",TAG+"34");
        Barcode barcode = new Barcode(barcodeData, barcodeType, fromScannerID);
        dataHandler.obtainMessage(Constants.BARCODE_RECEIVED, barcode).sendToTarget();
    }

    @Override
    public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {
        Log.d("func_flow",TAG+"35");
        dataHandler.obtainMessage(Constants.FW_UPDATE_EVENT, firmwareUpdateEvent).sendToTarget();
    }

    @Override
    public void dcssdkEventAuxScannerAppeared(DCSScannerInfo newTopology, DCSScannerInfo auxScanner) {
        Log.d("func_flow",TAG+"36");
        dataHandler.obtainMessage(Constants.AUX_SCANNER_CONNECTED, auxScanner).sendToTarget();
    }


    @Override
    public void dcssdkEventImage(byte[] imageData, int fromScannerID) {
        Log.d("func_flow",TAG+"37");
        dataHandler.obtainMessage(Constants.IMAGE_RECEIVED, imageData).sendToTarget();
    }

    @Override
    public void dcssdkEventVideo(byte[] videoFrame, int fromScannerID) {
        Log.d("func_flow",TAG+"38");
        dataHandler.obtainMessage(Constants.VIDEO_RECEIVED, videoFrame).sendToTarget();
    }

    @Override
    public void dcssdkEventBinaryData(byte[] binaryData, int fromScannerID) {
        // todo: implement this
        Log.d("func_flow",TAG+"39");
        logAsMessage(TYPE_DEBUG, TAG, "BinaryData Event received no.of bytes : " + binaryData.length + " for Scanner ID : " + fromScannerID);
    }

    public ArrayList<Barcode> getBarcodeData(int scannerId) {
        Log.d("func_flow",TAG+"40");
        ArrayList<Barcode> barcodes = new ArrayList<Barcode>();
        for (Barcode barcode : Application.barcodeData) {
            if (barcode.getFromScannerID() == scannerId) {
                barcodes.add(barcode);
            }
        }
        return barcodes;
    }


    protected Handler dataHandler = new Handler() {
        boolean notificaton_processed = false;
        boolean result = false;
        boolean found = false;


        @Override
        public void handleMessage(Message msg) {
            Log.d("func_flow",TAG+"43");
            switch (msg.what) {

                case Constants.BARCODE_RECEIVED:
                    Log.d("handler_func","barcode_received");
                    logAsMessage(TYPE_DEBUG, TAG, "Barcode Received");
                    Barcode barcode = (Barcode) msg.obj;
                    Application.barcodeData.add(barcode);
                    for (IScannerAppEngineDevEventsDelegate delegate : mDevEventsDelegates) {
                        if (delegate != null) {
                            logAsMessage(TYPE_DEBUG, TAG, "Show Barcode Received");
                            delegate.scannerBarcodeEvent(barcode.getBarcodeData(), barcode.getBarcodeType(), barcode.getFromScannerID());
                        }
                    }
                    if (Application.MOT_SETTING_NOTIFICATION_BARCODE && !notificaton_processed) {
                        String scannerName = "";
                        if (mScannerInfoList != null) {
                            for (DCSScannerInfo ex_info : mScannerInfoList) {
                                if (ex_info.getScannerID() == barcode.getFromScannerID()) {
                                    scannerName = ex_info.getScannerName();
                                    break;
                                }
                            }
                        }
                        if (isInBackgroundMode(getApplicationContext())) {
                            Intent intent = new Intent();
                            intent.setAction(Constants.ACTION_SCANNER_BARCODE_RECEIVED);
                            intent.putExtra(Constants.NOTIFICATIONS_TEXT, "Barcode received from " + scannerName);
                            intent.putExtra(Constants.NOTIFICATIONS_TYPE, Constants.BARCODE_RECEIVED);
                            sendOrderedBroadcast(intent, null);
                        } else {

                            Toast.makeText(getApplicationContext(), "Barcode received from " + scannerName, Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (!Application.isFirmwareUpdateInProgress) {
                        if (Application.currentScannerId != Application.SCANNER_ID_NONE) {
                            Log.d("func_flow","getting in bBase Activity");
                       /*      intent.setComponent(new ComponentName(getPackageName(), ActiveScannerActivity.class.getName()));
                            intent.putExtra(Constants.SCANNER_NAME, Application.currentScannerName);
                            intent.putExtra(Constants.SCANNER_ADDRESS, Application.currentScannerAddress);
                            intent.putExtra(Constants.SCANNER_ID, Application.currentScannerId);
                            intent.putExtra(Constants.AUTO_RECONNECTION, Application.currentAutoReconnectionState);
                            intent.putExtra(Constants.SHOW_BARCODE_VIEW, true);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplicationContext().startActivity(intent);*/
                        }
                    }

                    break;
                case Constants.SESSION_ESTABLISHED:
                    Log.d("handler_func","session established");
                    DCSScannerInfo activeScanner = (DCSScannerInfo) msg.obj;
                    notificaton_processed = false;
                    result = false;
                    ScannersActivity.curAvailableScanner = new AvailableScanner(activeScanner);
                    ScannersActivity.curAvailableScanner.setConnected(true);
                    setAutoReconnectOption(activeScanner.getScannerID(), true);
                    /* notify connections delegates */
                    if (mDevConnDelegates != null) {
                        for (IScannerAppEngineDevConnectionsDelegate delegate : mDevConnDelegates) {
                            if (delegate != null) {
                                result = delegate.scannerHasConnected(activeScanner.getScannerID());
                                if (result) {
                                /*
                                 DevConnections delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                                    notificaton_processed = false;
                                }
                            }
                        }
                    }

                    /* update dev list */
                    found = false;
                    if (mScannerInfoList != null) {
                        for (DCSScannerInfo ex_info : mScannerInfoList) {
                            if (ex_info.getScannerID() == activeScanner.getScannerID()) {
                                mScannerInfoList.remove(ex_info);
                                Application.barcodeData.clear();
                                found = true;
                                break;
                            }
                        }
                    }


                    if (mScannerInfoList != null)
                        mScannerInfoList.add(activeScanner);

                    /* notify dev list delegates */
                    if (Application.mDevListDelegates != null) {
                        for (IScannerAppEngineDevListDelegate delegate : Application.mDevListDelegates) {
                            if (delegate != null) {
                                result = delegate.scannersListHasBeenUpdated();
                                if (result) {
                                    /*
                                     DeList delegates should NOT display any UI alerts,
                                     so from UI notification side the event is not processed
                                     */
                                    notificaton_processed = false;
                                }
                            }
                        }
                    }

                    //TODO - Showing notifications in foreground and background mode

                    if (Application.MOT_SETTING_NOTIFICATION_ACTIVE && !notificaton_processed) {
                        StringBuilder notification_Msg = new StringBuilder();
                        if (!found) {
                            notification_Msg.append(activeScanner.getScannerName()).append(" has appeared and connected");

                        } else {
                            notification_Msg.append(activeScanner.getScannerName()).append(" has connected");

                        }
                        if (isInBackgroundMode(getApplicationContext())) {
                            Intent intent = new Intent();
                            intent.setAction(Constants.ACTION_SCANNER_CONNECTED);
                            intent.putExtra(Constants.NOTIFICATIONS_TEXT, notification_Msg.toString());
                            intent.putExtra(Constants.NOTIFICATIONS_TYPE, Constants.SESSION_ESTABLISHED);
                            sendOrderedBroadcast(intent, null);
                        } else {
                            Toast.makeText(getApplicationContext(), notification_Msg.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case Constants.SESSION_TERMINATED:
                    Log.d("handler_func","Session terminated");

                    int scannerID = (Integer) msg.obj;
                    String scannerName = "";
                    notificaton_processed = false;
                    result = false;

                    /* notify connections delegates */
                    for (IScannerAppEngineDevConnectionsDelegate delegate : mDevConnDelegates) {
                        if (delegate != null) {
                            result = delegate.scannerHasDisconnected(scannerID);
                            if (result) {
                            /*
                             DevConnections delegates should NOT display any UI alerts,
                             so from UI notification side the event is not processed
                             */
                                notificaton_processed = false;
                            }
                        }
                    }

                    DCSScannerInfo scannerInfo = getScannerByID(scannerID);
                    if (scannerInfo != null) {
                        scannerName = scannerInfo.getScannerName();
                        ScannersActivity.curAvailableScanner = null;
                    }
                    updateScannersList();

                    /* notify dev list delegates */
                    for (IScannerAppEngineDevListDelegate delegate : Application.mDevListDelegates) {
                        if (delegate != null) {
                            result = delegate.scannersListHasBeenUpdated();
                            if (result) {
                                /*
                                 DeList delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                                notificaton_processed = false;
                            }
                        }
                    }
                    SharedPreferences virtualTetherSharedPreferences = getSharedPreferences(Constants.PREFS_NAME, 0);
                    boolean virtualTetherEnabled =  virtualTetherSharedPreferences.getBoolean(Constants.PREF_VIRTUAL_TETHER_SCANNER_SETTINGS, false);
                    if (Application.MOT_SETTING_NOTIFICATION_ACTIVE && !notificaton_processed && !virtualTetherEnabled) {
                        if (isInBackgroundMode(getApplicationContext())) {
                            Intent intent = new Intent();
                            intent.setAction(Constants.ACTION_SCANNER_DISCONNECTED);
                            intent.putExtra(Constants.NOTIFICATIONS_TEXT, scannerName + " has disconnected");
                            intent.putExtra(Constants.NOTIFICATIONS_TYPE, Constants.SESSION_TERMINATED);
                            sendOrderedBroadcast(intent, null);
                        } else {
                            Toast.makeText(getApplicationContext(), scannerName + " has disconnected", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case Constants.SCANNER_APPEARED:
                    Log.d("handler_func","Scanner appeared");

                    result = false;
                    DCSScannerInfo availableScanner = (DCSScannerInfo) msg.obj;


                    for (DCSScannerInfo ex_info : mScannerInfoList) {
                        if (ex_info.getScannerID() == availableScanner.getScannerID()) {
                            mScannerInfoList.remove(ex_info);
                            break;
                        }
                    }

                    mScannerInfoList.add(availableScanner);

                    /* notify dev list delegates*/
                    for (IScannerAppEngineDevListDelegate delegate : Application.mDevListDelegates) {
                        if (delegate != null) {
                            result = delegate.scannersListHasBeenUpdated();
                            if (result) {
                            /*
                             DeList delegates should NOT display any UI alerts,
                             so from UI notification side the event is not processed
*/

                                notificaton_processed = false;
                            }
                        }
                    }

                    //TODO - Showing notifications in foreground and background mode
                    if (Application.MOT_SETTING_NOTIFICATION_AVAILABLE && !notificaton_processed && !found) {
                        if (isInBackgroundMode(getApplicationContext())) {
                            Intent intent = new Intent();
                            intent.setAction(Constants.ACTION_SCANNER_CONNECTED);
                            intent.putExtra(Constants.NOTIFICATIONS_TEXT, availableScanner.getScannerName() + " has appeared");
                            intent.putExtra(Constants.NOTIFICATIONS_TYPE, Constants.SCANNER_APPEARED);
                            sendOrderedBroadcast(intent, null);
                        } else {
                            Toast.makeText(getApplicationContext(), availableScanner.getScannerName() + " has appeared", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;

            }
        }
    };

}

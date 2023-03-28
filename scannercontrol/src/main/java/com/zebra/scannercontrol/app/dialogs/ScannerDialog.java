package com.zebra.scannercontrol.app.dialogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.zebra.scannercontrol.SDKHandler;
import com.zebra.scannercontrol.app.R;
import com.zebra.scannercontrol.app.activities.ScannersActivity;
import com.zebra.scannercontrol.app.application.Application;
import com.zebra.scannercontrol.app.helpers.AvailableScanner;
import com.zebra.scannercontrol.app.helpers.Constants;
import com.zebra.scannercontrol.app.helpers.ScannerAppEngine;
import java.util.ArrayList;

public class ScannerDialog extends BaseScannerDialog  implements  ScannerAppEngine.IScannerAppEngineDevListDelegate,ScannerAppEngine.IScannerAppEngineDevEventsDelegate, ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate {

    public static final String TAG = "--Truelab-- " + ScannerDialog.class.getSimpleName();
    Activity activity;
    private Button btnSave, btnCancel;
    TextView txtNetStatus,txtIpv4Addr,txtIpv6Addr,txtwlan0,txteth1,txteth2,txteth0;
    ProgressBar progressBar;
    private int EVENT = 2;
    private ProgressDialog progressDialog;
    private static ArrayList<ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate> mDevConnDelegates = new ArrayList<ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate>();
    private static ArrayList<ScannerAppEngine.IScannerAppEngineDevEventsDelegate> mDevEventsDelegates = new ArrayList<ScannerAppEngine.IScannerAppEngineDevEventsDelegate>();
    private static ArrayList<DCSScannerInfo> mScannerInfoList;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 10;
    public static  AvailableScanner curAvailableScanner=null;
    public int scannerId;
    static MyAsyncTask cmdExecTask=null;
    static NewAsyncTask cmdNewTask=null;

    boolean network_available=false;
    public ScannerDialog(Activity a) {
        super(a);
        this.activity = a;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.network_status_dialog);
        View v = getWindow().getDecorView();
        v.setBackgroundResource(android.R.color.transparent);
        getWindow().setLayout((getWidth(activity) / 100) * 80, WindowManager.LayoutParams.WRAP_CONTENT);
        mScannerInfoList = Application.mScannerInfoList;
        if (Application.sdkHandler == null) {
            Application.sdkHandler = new SDKHandler(activity, false);
        }
        addDevListDelegate(this);
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_REQUEST_CODE);
        }
        else{
            Log.d("second","getting in second initialize");
            initialize();
        }
        addDevConnectionsDelegate(this);

        if(curAvailableScanner!=null && curAvailableScanner.isConnected())
        {
            addDevEventsDelegate(this);
            UpdateScannerListView(EVENT);
        }

    }



    private final Handler UpdateScannersHandler = new Handler() {

        public void handleMessage(Message msg) {
            updateScannersList();
            boolean enableLastScannerConnection = false;
            for(DCSScannerInfo device:getActualScannersList()){
                Log.d("handler_lst","for loop detecting device "+getActualScannersList().size());
                if(device.isActive())
                {
                    Log.d("handler_lst","getting in handler isactive");
                    AvailableScanner availableScanner = new AvailableScanner(device.getScannerID(),device.getScannerName(), device.getScannerHWSerialNumber(),true,device.isAutoCommunicationSessionReestablishment(),device.getConnectionType());
                    Application.currentConnectedScanner = device;
                    Application.lastConnectedScanner = Application.currentConnectedScanner;
                    availableScanner.setIsConnectable(true);
                    Log.d("Scanr","getting in device active");
                    ConnectToScanner(availableScanner);
                }
                else
                {
                    Log.d("handler_lst","getting in handler else");
                    AvailableScanner availableScanner = new AvailableScanner(device.getScannerID(),device.getScannerName(), device.getScannerHWSerialNumber(),false,device.isAutoCommunicationSessionReestablishment(),device.getConnectionType());
                    availableScanner.setIsConnectable(true);
                    ConnectToScanner(availableScanner);
                }
            }

        }
    };

    private void initialize() {
        initializeDcsSdk();
    }
    private void initializeDcsSdk(){
        Application.sdkHandler.dcssdkEnableAvailableScannersDetection(true);
        Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_SNAPI);
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeDevListDelegate(this);
        removeDevEventsDelegate(this);
        removeDevConnectiosDelegate(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        addDevListDelegate(this);
    }

    private void UpdateScannerListView(int what) {
        Message msg =  new Message();
        msg.what= what;
        Log.d("list","getting in list view");
        UpdateScannersHandler.sendMessage(msg);
    }

    private void ConnectToScanner(AvailableScanner availableScanner) {
        if (availableScanner != null)
        {
            Log.d("scan_f","getting in availble scanner");
            if (!availableScanner.isConnected()) {

                if ((curAvailableScanner!=null) &&(!availableScanner.getScannerAddress().equals(curAvailableScanner.getScannerAddress())))                 {
                    if (curAvailableScanner.isConnected())
                        disconnect(curAvailableScanner.getScannerId());
                }
                cmdExecTask=new MyAsyncTask(availableScanner);
                cmdExecTask.execute();
                Application.currentScannerName = availableScanner.getScannerName();
                Application.currentScannerAddress = availableScanner.getScannerAddress();
                Application.currentAutoReconnectionState = availableScanner.isAutoReconnection();
                Application.currentScannerId = availableScanner.getScannerId();
                scannerId=availableScanner.getScannerId();
                pullTrigger();
            } else {
                curAvailableScanner = availableScanner;
                if (curAvailableScanner.isConnected()) {
                    Log.d("func_flow","getting inside current available");
                    Application.currentScannerName = availableScanner.getScannerName();
                    Application.currentScannerAddress = availableScanner.getScannerAddress();
                    Application.currentAutoReconnectionState = availableScanner.isAutoReconnection();
                    Application.currentScannerId = availableScanner.getScannerId();
                    scannerId=availableScanner.getScannerId();
                    pullTrigger();
                }
            }
        }
    }

    public void releaseTrigger() {
        String in_xml = "<inArgs><scannerID>" + scannerId + "</scannerID></inArgs>";
        cmdNewTask = new NewAsyncTask(scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_RELEASE_TRIGGER, null);
        cmdNewTask.execute(new String[]{in_xml});
    }

    public void pullTrigger() {
        Log.d("func_flow","Gettign in pull trigger");
        String in_xml = "<inArgs><scannerID>" + scannerId + "</scannerID></inArgs>";
        cmdNewTask = new NewAsyncTask(scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER, null);
        cmdNewTask.execute(new String[]{in_xml});
    }



    public static int getWidth(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }


    @Override
    public boolean scannersListHasBeenUpdated() {
        Log.d("handler_lst","scanner LST Update");
        UpdateScannerListView(EVENT);
        return true;
    }

    @Override
    public boolean scannerHasConnected(int scannerID) {
        addDevEventsDelegate(this);
        addDevConnectionsDelegate(this);
        return false;
    }

    @Override
    public boolean scannerHasDisconnected(int scannerID) {
        removeDevEventsDelegate(this);
        removeDevConnectiosDelegate(this);
        Log.d("disconn","scanner has been disconnected");
        return false;
    }

    @Override
    public void scannerBarcodeEvent(byte[] barcodeData, int barcodeType, int scannerID) {
        String barcode=new String(barcodeData);
        Toast.makeText(getContext(),barcode,Toast.LENGTH_SHORT).show();
        Log.d("func_flow",barcode+" ***********************************");
    }



    private class NewAsyncTask extends AsyncTask<String, Integer, Boolean> {
        int scannerId;
        StringBuilder outXML;
        DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode;
        private ProgressDialog progressDialog;
        String scannerFeature = "";

        public NewAsyncTask(int scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode, StringBuilder outXML) {
            this.scannerId = scannerId;
            this.opcode = opcode;
            this.outXML = outXML;
        }

        public NewAsyncTask(int scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode, StringBuilder outXML, String scannerFeature) {
            this.scannerId = scannerId;
            this.opcode = opcode;
            this.outXML = outXML;
            this.scannerFeature = scannerFeature;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(activity);
        }


        @Override
        protected Boolean doInBackground(String... strings) {
            return executeCommand(opcode, strings[0], outXML, scannerId);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            if (!b &&(null != scannerFeature && scannerFeature.isEmpty())) {
                Toast.makeText(activity, "Cannot perform the action" + " " + scannerFeature, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class MyAsyncTask extends AsyncTask<Void,AvailableScanner,Boolean> {
        private AvailableScanner  scanner;
        public MyAsyncTask(AvailableScanner scn){
            this.scanner=scn;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(activity);
            progressDialog.setCancelable(false);
            progressDialog.show();
            progressDialog.setOnCancelListener(new ProgressDialog.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {

                    if (cmdExecTask != null) {
                        cmdExecTask.cancel(true);
                    }
                }
            });

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            DCSSDKDefs.DCSSDK_RESULT result =connect(scanner.getScannerId());
            if(result== DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS){
                curAvailableScanner = scanner;
                curAvailableScanner.setConnected(true);
                return true;
            }
            else {
                curAvailableScanner=null;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if(!b){
                Toast.makeText(getContext(),"Unable to communicate with scanner",Toast.LENGTH_SHORT).show();
                scannersListHasBeenUpdated();
            }

        }
    }


}


package com.zebra.scannercontrol.app.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.app.R;
import com.zebra.scannercontrol.app.helpers.ScannerAppEngine;
import com.zebra.scannercontrol.app.application.Application;
import com.zebra.scannercontrol.app.helpers.AvailableScanner;

import java.util.ArrayList;
import java.util.Collections;


public class ScannersActivity extends BaseActivity implements  ScannerAppEngine.IScannerAppEngineDevListDelegate,ScannerAppEngine.IScannerAppEngineDevEventsDelegate, ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate
{

    private ProgressDialog progressDialog;
    public static  AvailableScanner curAvailableScanner=null;
    public int scannerId;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 10;
    static MyAsyncTask cmdExecTask=null;
    static NewAsyncTask cmdNewTask=null;
    private int EVENT = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanners);
        Log.d("func_flow","got in ScannersActivity");

        addDevListDelegate(this);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_REQUEST_CODE);
        }
        else{
            initialize();
        }
        addDevConnectionsDelegate(this);
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
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        String ns = Context.NOTIFICATION_SERVICE;
        removeDevListDelegate(this);
        removeDevEventsDelegate(this);
        removeDevConnectiosDelegate(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeDevListDelegate(this);
        Application.sdkHandler.dcssdkStopScanningDevices();
        if (isInBackgroundMode(getApplicationContext())) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String ns = Context.NOTIFICATION_SERVICE;
        addDevListDelegate(this);
    }

    private void UpdateScannerListView(int what) {
        Message msg =  new Message();
        msg.what= what;
        UpdateScannersHandler.sendMessage(msg);
    }

    @Override
    public void onBackPressed() {
        if(Application.isAnyScannerConnected){
            Log.d("func_flow","getting in back pressed");
        }else {
                super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void ConnectToScanner(AvailableScanner availableScanner) {

        if (availableScanner != null)
        {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public boolean scannersListHasBeenUpdated() {
        Log.d("handler_lst","scanner LST Update");
        UpdateScannerListView(EVENT);
        return true;
    }

    @Override
    public boolean scannerHasConnected(int scannerID)
    {
        addDevEventsDelegate(this);
        addDevConnectionsDelegate(this);
        return false;
    }

    @Override
    public boolean scannerHasDisconnected(int scannerID) {

        removeDevEventsDelegate(this);
        removeDevConnectiosDelegate(this);

        return false;
    }

    @Override
    public void scannerBarcodeEvent(byte[] barcodeData, int barcodeType, int scannerID) {

        String barcode=new String(barcodeData);
        Toast.makeText(getApplicationContext(),barcode,Toast.LENGTH_SHORT).show();
        Log.d("func_flow",barcode+" ***********************************");
    }



    public void pullTrigger() {
        Log.d("func_flow","Gettign in pull trigger");
        String in_xml = "<inArgs><scannerID>" + scannerId + "</scannerID></inArgs>";
        cmdNewTask = new NewAsyncTask(scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER, null);
        cmdNewTask.execute(new String[]{in_xml});
    }

    public void releaseTrigger() {

        String in_xml = "<inArgs><scannerID>" + scannerId + "</scannerID></inArgs>";
        cmdNewTask = new NewAsyncTask(scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_RELEASE_TRIGGER, null);
        cmdNewTask.execute(new String[]{in_xml});
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
            progressDialog = new ProgressDialog(ScannersActivity.this);

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
                Toast.makeText(ScannersActivity.this, "Cannot perform the action" + " " + scannerFeature, Toast.LENGTH_SHORT).show();
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
            progressDialog = new ProgressDialog(ScannersActivity.this);
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
                Toast.makeText(getApplicationContext(),"Unable to communicate with scanner",Toast.LENGTH_SHORT).show();
                scannersListHasBeenUpdated();
            }

        }
    }
}

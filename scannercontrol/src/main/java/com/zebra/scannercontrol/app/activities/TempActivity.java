package com.zebra.scannercontrol.app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.zebra.scannercontrol.app.R;
import com.zebra.scannercontrol.app.dialogs.ScannerDialog;

public class TempActivity extends AppCompatActivity {

    Button scanner_btn;
    ScannerDialog scannerDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp);

        scanner_btn=findViewById(R.id.scanner_btn);

        scanner_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scannerDialog=new ScannerDialog(TempActivity.this);
                scannerDialog.show();
            }
        });
    }
}
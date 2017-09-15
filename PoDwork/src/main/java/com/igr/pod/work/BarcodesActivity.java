package com.igr.pod.work;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
/*
import com.google.zxing.client.android;
*/
/*
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
*/

public class BarcodesActivity extends AppCompatActivity {
    // Constant
    // UI
    private ListViewEx _lveBarcodeList;
    private Button _btnScan;
    private Button _btnOk;
    private Button _btnCancel;
    private TextView _tvTitle;
    // Variables
    private String mTitle;
    private MainActivity.ArrayRecs mScanList = new MainActivity.ArrayRecs();
    boolean mIsScanNext = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcodes);

        initUI();
        initVariables();
        initListeners();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
/*
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if ( result==null || result.getContents()==null )
            return;
        String sCode = result.getContents();
*/
        switch (requestCode) {
            case MainActivity.CODE_SCAN:
                if (resultCode != RESULT_OK || intent==null )
                    break;
                String sCode = intent.getStringExtra(MainActivity.SCAN_CODE);
                int id = R.string.idsContine;
                if (!mIsScanNext) {
                    MainActivity.ListRec lListRec = new MainActivity.ListRec(new File(sCode), false);
                    _lveBarcodeList.AddItem(lListRec.mFile, lListRec.mIsSigned);
                    mScanList.add(lListRec);
                } else {
                    int position = _lveBarcodeList.getPositionItem(new File(sCode));
                    if (position >= 0 && position < mScanList.size()) {
                        MainActivity.ListRec lListRec = mScanList.get(position);
                        lListRec.mIsSigned = !lListRec.mIsSigned;
                        if ( !lListRec.mIsSigned )
                            id = R.string.idsUncheckUnload;
                        _lveBarcodeList.AddItem(lListRec.mFile, lListRec.mIsSigned);
                    } else
                        id = R.string.idsErrorUnload;
                }
                continueDialog(id);
                break;
        }
    }
    // Buttons click
    public void onScan(View view) {
        mIsScanNext = false;
        startScan();
    }
    public void onScanNext(View view) {
        mIsScanNext = true;
        startScan();
    }
    public void onOk(View view) {
        Intent intent = getIntent();
        intent.putStringArrayListExtra(MainActivity.SCAN_DATA, mScanList.toStringArrayList());
        setResult(RESULT_OK, intent);
        finish();
    }
    public void onCancel(View view) {
        setResult(RESULT_CANCELED, getIntent());
        finish();
    }
    // Initialize
    private void initUI() {
        _lveBarcodeList = (ListViewEx) findViewById(R.id.idBarcodeList);
        _btnScan = (Button) findViewById(R.id.idBtnScan);
        _btnOk = (Button) findViewById(R.id.idOk);
        _btnCancel = (Button) findViewById(R.id.idCancel);
        _tvTitle = (TextView) findViewById(R.id.idBarcodeTitle);
    }
    private void initVariables() {
        Intent intent = getIntent();
        mScanList.fromStringArrayList( intent.getStringArrayListExtra(MainActivity.SCAN_DATA) );
        for (int i = 0; i < mScanList.size(); i++) {
            MainActivity.ListRec lListRec = mScanList.get(i);
            _lveBarcodeList.AddItem(lListRec.mFile, lListRec.mIsSigned);
        }
        mTitle = intent.getStringExtra(MainActivity.PDF_FILENAME);
        if ( _tvTitle!=null && mTitle!=null )
            _tvTitle.setText(mTitle);
    }
    public void initListeners()
    {
        _lveBarcodeList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if ( position<0 || position>=mScanList.size() )
                    return;
                _lveBarcodeList.setItemChecked(position, !_lveBarcodeList.isItemChecked(position));
            }
        });
        _lveBarcodeList.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if ( position<0 || position>=mScanList.size() )
                    return false;
                if ( _lveBarcodeList.isItemChecked(position) )
                    return false;
                removeItemDialog(position, view);
                return true;
            }
        });
    }
    // Private functions
    private void startScan() {
/*
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
    //        intent.putExtra("SCAN_MODE", "QR_CODE,EAN_13,EAN_8,RSS_14,UPC_A,UPC_E,CODE_39,CODE_93,CODE_128,ITF,CODABAR,DATA_MATRIX");
        intent.putExtra("SCAN_FORMAT", "EAN_13,EAN_8,CODE_39,CODE_93,CODE_128");
        startActivityForResult(Intent.createChooser(intent, "Choose barcode scanner"), CODE_SCAN);
*/
/*
        IntentIntegrator mIntegrator = new IntentIntegrator(this);
        final String[] myArr = {"EAN_8", "EAN_13", "CODE_39", "CODE_93", "CODE_128"};
        Collection<String> MY_TYPES = Collections.unmodifiableList(Arrays.asList(myArr));
        mIntegrator.setDesiredBarcodeFormats(MY_TYPES);//IntentIntegrator.ONE_D_CODE_TYPES
        int promptId = (mIsScanNext) ? R.string.idsScanNext : R.string.idsScan;
        mIntegrator.setPrompt(getResources().getString(promptId));//mIntegrator.setPrompt("Scan a barcode");
        mIntegrator.setCameraId(0);
        mIntegrator.setOrientationLocked(true);
        //        mIntegrator.setBarcodeImageEnabled(true);
        mIntegrator.initiateScan();
*/
        Intent intent = new Intent(this, ScanZxingActivity.class);
        startActivityForResult(intent, MainActivity.CODE_SCAN);
    }
    private void continueDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(id))
                .setPositiveButton(getResources().getString(R.string.idsYes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startScan();
                    }
            })
                .setNegativeButton(getResources().getString(R.string.idsNo), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
            });
        builder.create().show();
    }
    private void removeItemDialog(final int position, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.idsRemoveScan))
                .setPositiveButton(getResources().getString(R.string.idsYes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        removeItem(position);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.idsNo), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
//        builder.create().show();
        MainActivity.showDialogAfterItem(builder, view);
    }
    private void removeItem(int position) {
        if ( position<0 ) {
            _lveBarcodeList.RemItem(null);
            mScanList.clear();
        } else {
            MainActivity.ListRec lListRec = mScanList.get(position);
            if (lListRec.mIsSigned)
                return;
            _lveBarcodeList.RemItem(lListRec.mFile);
            mScanList.remove(position);
        }
    }

}

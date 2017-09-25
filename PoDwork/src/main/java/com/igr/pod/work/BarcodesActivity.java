package com.igr.pod.work;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private TextView _tvTitle;
    private ListViewEx _lveBarcodeList;
    private Button _btnScan;
    private Button _btnScanNext;
    private Button _btnOk;
    private Button _btnCancel;
    // Variables
    private String mTitle;
    private ListViewEx.ArrayRec mScanList = new ListViewEx.ArrayRec();
    boolean mIsScanNext = false;
    boolean mIsManual = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcodes);

        initUI();
        initVariables();
        initListeners();
    }
    // Buttons click
    public void onScan(View view) {
        mIsScanNext = false;
        mIsManual = false;
        startScan();
    }
    public void onScanNext(View view) {
        mIsScanNext = true;
        mIsManual = false;
        startScan();
    }
    public void onOk(View view) {
        Intent intent = getIntent();
        if ( mScanList != null )
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
        _tvTitle = (TextView) findViewById(R.id.idBarcodeTitle);
        _lveBarcodeList = (ListViewEx) findViewById(R.id.idBarcodeList);
        _btnScan = (Button) findViewById(R.id.idBtnScan);
        _btnScanNext = (Button) findViewById(R.id.idBtnScanNext);
        _btnOk = (Button) findViewById(R.id.idOk);
        _btnCancel = (Button) findViewById(R.id.idCancel);
    }
    private void initVariables() {
        Intent intent = getIntent();
        mTitle = intent.getStringExtra(MainActivity.PDF_FILENAME);
        if ( _tvTitle!=null && mTitle!=null )
            _tvTitle.setText(mTitle);
        mScanList.fromStringArrayList( intent.getStringArrayListExtra(MainActivity.SCAN_DATA) );
        for (int i = 0; i < mScanList.size(); i++)
            _lveBarcodeList.AddItem(mScanList.get(i));
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
                dialogRemove(position, view);
                return true;
            }
        });
        _btnScan.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v){
                mIsScanNext = false;
                mIsManual = true;
                startScanManual();
                return false;
            }
        });
        _btnScanNext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v){
                mIsScanNext = true;
                mIsManual = true;
                startScanManual();
                return false;
            }
        });

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
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 300);

                int id = changeItem( intent.getStringExtra(MainActivity.SCAN_CODE) );
                dialogContinue(id);
                break;
        }
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
/* BARCODE */
        Intent intent = new Intent(this, ScanZxingActivity.class);
        startActivityForResult(intent, MainActivity.CODE_SCAN);
/**/
    }

    private void startScanManual() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
        builder.setTitle(R.string.idsEnterBarcode);
        LinearLayout _llDialogMail = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_mail, null);
        builder.setView(_llDialogMail);
        final EditText _etEditEmail = (EditText)_llDialogMail.findViewById(R.id.idEditEmail);
        builder.setPositiveButton(R.string.idsOK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int id = changeItem( _etEditEmail.getText().toString() );
                dialogContinue(id);
            }
        })
        .setNegativeButton(R.string.idsCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ;
            }
        });
        builder.create().show();
    }

    private void dialogContinue(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(id))
                .setPositiveButton(getResources().getString(R.string.idsYes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if ( !mIsManual )
                            startScan();
                        else
                            startScanManual();
                    }
            })
                .setNegativeButton(getResources().getString(R.string.idsNo), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ;
                    }
            });
        builder.create().show();
    }
    private void dialogRemove(final int position, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.idsRemoveScan))
                .setPositiveButton(getResources().getString(R.string.idsYes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        removeItem(position);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.idsNo), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ;
                    }
                });
        MainActivity.showDialogAfterItem(builder, view);
    }
    private int changeItem(String sCode)
    {
        int id = R.string.idsContine;
        int position = _lveBarcodeList.getPositionFromString(sCode);
        if (!mIsScanNext) {
            if (position >= 0 && position < mScanList.size()) {
                id = R.string.idsErrorLoad;
            } else {
                ListViewEx.Rec lListRec = new ListViewEx.Rec(new File(sCode), false);
                _lveBarcodeList.AddItem(lListRec);
                mScanList.add(lListRec);
            }
        } else {
            if (position >= 0 && position < mScanList.size()) {
                ListViewEx.Rec lListRec = mScanList.get(position);
                lListRec.mIsSigned = !lListRec.mIsSigned;
                if (!lListRec.mIsSigned)
                    id = R.string.idsUncheckUnload;
                _lveBarcodeList.AddItem(lListRec);
            } else
                id = R.string.idsErrorUnload;
        }
        return id;
    }
    private void removeItem(int position) {
        if ( position<0 ) {
            _lveBarcodeList.RemItem(position);
            mScanList.clear();
        } else {
            ListViewEx.Rec lListRec = mScanList.get(position);
            if (lListRec.mIsSigned)
                return;
            _lveBarcodeList.RemItem(position);
            mScanList.remove(position);
        }
    }
}

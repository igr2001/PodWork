package com.igr.pod.work;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.igr.pod.work.R;

import java.io.File;


public class PdfViewActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener
{
    // Controls
    private PDFView _rlPdfView;
    private DrawViewEx _dvSignatureView;
    private EditText _etUserData;
    private Button _bOk;
    private Button _bCancel;
    private Button _bClear;

    private String mFileName = null;
    private int mPageCurrent = 0;
    private String mUserData = new String();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_view);

        LoadSetting();
        InitControls();
        OpenPdf();
    }
    @Override
    public void onPageChanged(int page, int pageCount) {
        mPageCurrent = page;
        updateUi();
    }
    @Override
    public void loadComplete(int nbPages) {
        updateUi();
    }
    // Private functions
    private void LoadSetting() {
        Intent intent = getIntent();
        mFileName = intent.getStringExtra(MainActivity.PDF_FILENAME);
        mUserData = intent.getStringExtra(MainActivity.PDF_USERDATA);
    }
    private void InitControls()
    {
        _rlPdfView = (PDFView) findViewById(R.id.idPdfView);
        _dvSignatureView = (DrawViewEx) findViewById(R.id.idSignature);
        _etUserData = (EditText) findViewById(R.id.idEditUserData);
        _bOk = (Button)findViewById(R.id.idPdfOK);
        _bCancel = (Button)findViewById(R.id.idPdfCancel);
        _bClear = (Button)findViewById(R.id.idClear);
        if ( MainActivity.mSignViewOnly ) {
//            _etUserData.setInputType(InputType.TYPE_NULL);
            _etUserData.setEnabled(false);
            _bOk.setEnabled(false);
            _bClear.setEnabled(false);
        }
        _bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClosePdf(RESULT_OK);
            }
        });
        _bCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClosePdf(RESULT_CANCELED);
            }
        });
        _bClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _dvSignatureView.Clear();
            }
        });
    }
    private boolean OpenPdf() {
        File lFile = new File(mFileName);
        _rlPdfView.fromFile(lFile)
            .onLoad(this)
            .onPageChange(this)
            .defaultPage(mPageCurrent)
            .enableAnnotationRendering(true)
            .scrollHandle(new DefaultScrollHandle(this))
            .load();
        String sSignName =  MainActivity.GetSignName(mFileName);
        _dvSignatureView.LoadImage(sSignName);
        _etUserData.setText(mUserData);
        return true;
    }
    private void ClosePdf(int resultCode) {
        if ( _dvSignatureView.isEmpty() )
            resultCode = RESULT_FIRST_USER;
        Intent intent = getIntent();
        setResult(resultCode, intent);
        if ( resultCode==RESULT_OK) {
            String sSignName = MainActivity.GetSignName(mFileName);
            _dvSignatureView.SaveImage(sSignName);
            mUserData = _etUserData.getText().toString();
            intent.putExtra(MainActivity.PDF_USERDATA, mUserData);
        }
        finish();
    }
    private void updateUi() {
    }
}

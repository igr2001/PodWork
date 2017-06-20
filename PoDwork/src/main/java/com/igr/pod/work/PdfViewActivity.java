package com.igr.pod.work;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

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

    private String mFileName = null;
    private int mPageCurrent = 0;
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
        mFileName = getIntent().getStringExtra(MainActivity.PDF_FILENAME);
    }
    private void InitControls()
    {
        _rlPdfView = (PDFView) findViewById(R.id.idPdfView);
        _dvSignatureView = (DrawViewEx) findViewById(R.id.idSignature);
        findViewById(R.id.idPdfOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClosePdf(RESULT_OK);
            }
        });
        findViewById(R.id.idPdfCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClosePdf(RESULT_CANCELED);
            }
        });
        findViewById(R.id.idClear).setOnClickListener(new View.OnClickListener() {
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
        return true;
    }
    private void ClosePdf(int resultCode) {
        if ( _dvSignatureView.isEmpty() )
            resultCode = RESULT_FIRST_USER;
        setResult(resultCode, getIntent());

        String sSignName = MainActivity.GetSignName(mFileName);
        if ( resultCode==RESULT_OK)
            _dvSignatureView.SaveImage(sSignName);
        finish();
    }
    private void updateUi() {
    }
/*
    private void ShowPage(int index) {
        _rlPdfView.jumpTo(index);
        updateUi();
    }
*/
}

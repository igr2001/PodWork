package com.igr.pod.work;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Constants
    final static int CODE_LOGIN = 123;
    final static int CODE_SETTING = 124;
    final static int CODE_PDFVIEW = 125;
    final static int CODE_SCAN = 126;

    static final int REQUEST_PERMISSIONS = 2001;

    final static int MSG_ERROR = 100;
    final static int MSG_DOWNLOAD = 1;
    final static int MSG_UPLOAD = 2;
    final static int MSG_GETLIST = 3;

    final static int TS_LOGIN = 1;
    final static int TS_SETTING = 2;
    final static int TS_ALL = 3;

    final static String PREF_NAME = "pref_setting";
    final static String HOST_NAME = "host_name";
    final static String SRV_NAME = "srv_name";
    final static String DB_NAME = "dc_name";
    final static String USER_NAME = "user_name";
    final static String PASSWORD = "password";
    final static String COMPANY = "company";
    final static String CHECK_USER = "check_user";
    final static String CHECK_COMPANY = "check_company";
    final static String CHECK_ADDUSER = "check_adduser";
    final static String CHECK_SCAN = "check_scan";
    final static String PDF_FILENAME = "pdf_name";
    final static String PDF_USERDATA = "user_data";
    final static String LOGIN_ERROR = "login_error";
    final static String SCAN_DATA = "scan_data";
    final static String SCAN_CODE = "scan_code";

    // Controls
    private Toolbar _tbMain = null;
    private ListViewEx _lveLocalList = null;
    private ListViewEx _lveFilterList = null;
    private Button _btnGetlist = null;
    private Button _btnDownload = null;
    private Button _btnUlpoad = null;
    private Button _btnMailto = null;
    private Button _btnClear = null;
    private MenuItem _miLogin = null;
    private MenuItem _miLogout = null;
    private EditText _etFilter = null;
    private EditText _etEditEmail = null;
    // Local members
    private PfdWork mPfdWork = null;
    private Handler mHandler = null;
    private File mDataPath;
    private FilenameFilter mFileFilter = null;
    private FilenameFilter mSignFilter = null;
    private FilenameFilter mTextFilter = null;
    private int mSelectPos = -1;
    private String mFilter;
    // Settings members
    private String mHostName;
    private String mServerName;
    private String mDataBase;
    private String mUserName;
    private String mPassword;
    private String mCompany;
    private boolean mCheckUser;
    private boolean mCheckCompany;
    private boolean mCheckAddUser;
    private boolean mCheckScan;
    // statics
    static private List<ListRecEx> mLocalDataList = new ArrayList<ListRecEx>();
    static private ArrayRecs mServerDataList = new ArrayRecs();
    static private ArrayRecs mFilterDataList = new ArrayRecs();
    static private String mLoginName = null;
    static public String GetSignName(String sFileName) { return sFileName.replace(".pdf", ".png"); }
    static public String GetTextName(String sFileName) { return sFileName.replace(".pdf", ".txt"); }
    static public boolean IsFirst() { return (mLoginName == null); }
    static public boolean mSignViewOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestAllPermissions();

        _tbMain = (Toolbar) findViewById(R.id.idToolbar);
        setSupportActionBar(_tbMain);

        mPfdWork = new PfdWork(this);
        mDataPath =  getDir("documents", MODE_PRIVATE);

        LoadSetting(TS_ALL);
        initControls();
        initListener();
        initHandler();
//      Login();
        LoadLists();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SaveSetting(TS_ALL);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        _miLogin = menu.findItem(R.id.idLogin);
        _miLogout = menu.findItem(R.id.idLogout);
        OutTitle();
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.idLogin:
                Intent intentLogin = new Intent(MainActivity.this, LoginDlgActivity.class);
                startActivityForResult(intentLogin, CODE_LOGIN);
                return true;
            case R.id.idLogout:
                SetLogout();
                return true;
            case R.id.idSetting:
                Intent intentSetting = new Intent(MainActivity.this, SettingActivity.class);
                startActivityForResult(intentSetting, CODE_SETTING);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
/*
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(result != null) {
            if(result.getContents() != null) {
                String sCode = result.getContents() + " " + result.getFormatName();
                ((EditText)findViewById(R.id.idEditScan)).setText(sCode);
            }
            return;
        }
*/
        switch (requestCode) {
            case CODE_LOGIN:
                if (resultCode != RESULT_OK) {
                    String sError = intent.getStringExtra(LOGIN_ERROR);
                    if (sError!=null && !sError.isEmpty() )
                        errorDialog(sError);
                    break;
                }
                SetLogin();
                break;
            case CODE_SETTING:
                if (resultCode != RESULT_OK)
                    break;
                LoadSetting(TS_SETTING);
                break;
            case CODE_PDFVIEW:
                if ( !mSignViewOnly )
                {
                    if (resultCode != RESULT_OK) {
                        _lveLocalList.setItemChecked(mSelectPos, false);
                    } else {
                        if ( intent != null ) {
                            ListRecEx lLocalData = mLocalDataList.get(mSelectPos);
                            lLocalData.mUserData = intent.getStringExtra(MainActivity.PDF_USERDATA);
                            lLocalData.mIsSigned = true;
                            writeUserData(lLocalData.mFile, lLocalData.mUserData, lLocalData.mScanList);
                        }
                    }
                } else {
                    mSignViewOnly = false;
                    _lveLocalList.setItemChecked(mSelectPos, true);
                }
                mSelectPos = -1;
                break;
            case CODE_SCAN:
                if (resultCode != RESULT_OK)
                    break;
                if ( mSelectPos<0 || mSelectPos>=mLocalDataList.size() )
                    break;
                ListRecEx lLocalData = mLocalDataList.get(mSelectPos);
                if ( lLocalData.mScanList==null)
                    lLocalData.mScanList = new ArrayRecs();
                lLocalData.mScanList.fromStringArrayList(intent.getStringArrayListExtra(MainActivity.SCAN_DATA));
                writeUserData(lLocalData.mFile, lLocalData.mUserData, lLocalData.mScanList);
                mSelectPos = -1;
                break;
        }
    }
    // Buttons click
    public void onGetlist(View view) {
        String sUrl = mHostName + "/getlist.php";
        List<String> lListUrl = mPfdWork.getList(sUrl, mHandler);

        ClearServerList(-1);
        ClearFilterList(-1);
        for (int i = 0; i < lListUrl.size(); i++) {
            File lFile = new File(lListUrl.get(i));
            ListRecEx lServerData = new ListRecEx(lFile);
            mServerDataList.add(lServerData);
//            _lveFilterList.AddItem(lFile, false);
            AddFilterList(lServerData);
        }
    }
    public void onLoad(View view) {
        String sFileNameTmp = String.format("temp%d.pdf", _lveLocalList.getCount());
        for (int i = mFilterDataList.size()-1; i>=0 ; i--) {
            ListRec lFilterData = mFilterDataList.get(i);
            if ( !lFilterData.mIsSigned )
                continue;

            String sUrl = String.format("%s/%s", mHostName, lFilterData.mFile);
            File lFile = mPfdWork.downloadFile(sUrl, mDataPath.getAbsolutePath(), sFileNameTmp, mHandler);
            if (lFile != null) {
                _lveFilterList.RemItem(lFilterData.mFile);
                mFilterDataList.remove(i);
                RemoveFromServerList(lFilterData);

                String sSignName = GetSignName(lFile.getAbsolutePath());
                boolean mIsSigned = new File(sSignName).exists();
                if ( _lveLocalList.AddItem(lFile, mIsSigned) ) {
                    ListRecEx lLocalData = new ListRecEx(lFile);
                    mLocalDataList.add(lLocalData);
                }
            }
        }
    }
    public void UploadData(String sMailTo)
    {
        String sUrl = mHostName + "/upload.php";
        Integer nResponceCode;
        for (int i = mLocalDataList.size()-1; i>=0; i--) {
            if (!_lveLocalList.isItemChecked(i))
                continue;
            File lFile = mLocalDataList.get(i).mFile;
            String sFileName = lFile.getAbsolutePath();
            String sUsedData = mLocalDataList.get(i).mUserData;
            nResponceCode = mPfdWork.uploadFile(sUrl, sFileName, null, null, mHandler);
            if (nResponceCode == HttpURLConnection.HTTP_OK) {
                mLocalDataList.remove(i);
                _lveLocalList.RemItem(lFile);
                lFile.delete();
            } else
                continue;
            String sSignName = GetSignName(sFileName);
            nResponceCode = mPfdWork.uploadFile(sUrl, sSignName, sUsedData, sMailTo,  mHandler);
            if (nResponceCode == HttpURLConnection.HTTP_OK) {
                new File(sSignName).delete();
            } else
                continue;

            String sTextName = GetTextName(sFileName);
            nResponceCode = mPfdWork.uploadFile(sUrl, sTextName, null, null, mHandler);
            if (nResponceCode == HttpURLConnection.HTTP_OK) {
                new File(sTextName).delete();
            } else
                continue;
//            deleteUserData(lFile);
        }
    }
    public void onUpload(View view) {
        UploadData(null);
    }
    public void onMailto(View view) {
        selectEmailDialog();
    }
    public void onClear(View view) {
        ClearServerList(-1);
        ClearFilterList(-1);
// for debug
/*
        if ( IsFirst() ) {
            Intent intent = new Intent(MainActivity.this, BarcodesActivity.class);
            startActivityForResult(intent, CODE_SCAN);
        }
*/
    }

    // Initialize
    private void initControls() {
        _btnGetlist = (Button) findViewById(R.id.idGetlist);
        _btnDownload = (Button) findViewById(R.id.idDownload);
        _btnUlpoad = (Button) findViewById(R.id.idUpload);
        _btnMailto = (Button) findViewById(R.id.idMailto);
        _btnClear = (Button) findViewById(R.id.idClear);
        _lveLocalList = (ListViewEx) findViewById(R.id.idLocalList);
        _lveFilterList = (ListViewEx) findViewById(R.id.idServerList);
        _etFilter = (EditText) findViewById(R.id.idEditFilter);
    }
    private void initListener() {
        _lveLocalList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListRecEx lLocalData = mLocalDataList.get(position);
                mSignViewOnly = lLocalData.mIsSigned;
                Intent intent = new Intent(MainActivity.this, PdfViewActivity.class);
                String sFileName = lLocalData.mFile.getAbsolutePath();
                String sUserData = lLocalData.mUserData;
                intent.putExtra(MainActivity.PDF_FILENAME, sFileName);
                intent.putExtra(MainActivity.PDF_USERDATA, sUserData);
                startActivityForResult(intent, CODE_PDFVIEW);
                mSelectPos = position;
            }
        });
        _lveLocalList.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if ( position<0 || position>=mLocalDataList.size() )
                    return false;
                mSelectPos = position;
                if ( mCheckScan ) {
                    if (_lveLocalList.isItemChecked(mSelectPos)) {
                        Intent intent = new Intent(MainActivity.this, BarcodesActivity.class);
                        ListRecEx lLocalData = mLocalDataList.get(mSelectPos);
                        intent.putExtra(MainActivity.PDF_FILENAME, lLocalData.mFile.getName());
                        if (lLocalData.mScanList != null)
                            intent.putStringArrayListExtra(MainActivity.SCAN_DATA, lLocalData.mScanList.toStringArrayList());
                        startActivityForResult(intent, CODE_SCAN);
                    } else {
                        final View finalView = view;
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
//                            .setTitle(mLocalDataList.get(mSelectPos).mFile.getName())
                                .setItems(R.array.local_array, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0: // remove
                                                if (_lveLocalList.isItemChecked(mSelectPos))
                                                    return;
                                                removeItemDialog(mSelectPos, finalView);
                                                break;
                                            case 1: // scan
                                                Intent intent = new Intent(MainActivity.this, BarcodesActivity.class);
                                                ListRecEx lLocalData = mLocalDataList.get(mSelectPos);
                                                intent.putExtra(MainActivity.PDF_FILENAME, lLocalData.mFile.getName());
                                                if (lLocalData.mScanList != null)
                                                    intent.putStringArrayListExtra(MainActivity.SCAN_DATA, lLocalData.mScanList.toStringArrayList());
                                                startActivityForResult(intent, CODE_SCAN);
                                                break;
                                        }
                                    }
                                });
//                    builder.create().show();
                        showDialogAfterItem(builder, view);
                    }
                } else {
                    if (_lveLocalList.isItemChecked(position))
                        return false;
                    removeItemDialog(position, view);
                }
                return true;
            }
        });
/*
        _lveLocalList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
*/
        _lveFilterList.setOnItemClickListener(new ListView.OnItemClickListener() {
             @Override
             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                 if (position>=0 && position<mServerDataList.size()) {
                     ListRec lServerData = mFilterDataList.get(position);
                     lServerData.mIsSigned = _lveFilterList.isItemChecked(position);
                 }
             }
        });

        _etFilter.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mFilter = s.toString();
                FillFilterList();
            }
        });

        mFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String fileName) {return fileName.matches(".*\\.pdf");
            }
        };
        mSignFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String fileName) { return fileName.matches(".*\\.png"); }
        };
        mTextFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String fileName) { return fileName.matches(".*\\.txt"); }
        };

//        _etFilter.clearFocus();
//        _etFilter.setFocusableInTouchMode(false);
//        _btnClear.requestFocus();
    }
    // Private functions
    private void SetLogin() {
        LoadSetting(TS_LOGIN);
        mLoginName = mUserName;
        OutTitle();
    }
    private void SetLogout() {
        mLoginName = null;
        ClearServerList(-1);
        ClearFilterList(-1);
        OutTitle();
    }
    private void OutTitle()
    {
        boolean bIsFirst = IsFirst();
        String sTitle = getString(R.string.app_name)+ ((bIsFirst)?" : " + getString(R.string.idsDisonnect):" : user - " + mLoginName);
        _tbMain.setTitle(sTitle);
        _btnGetlist.setEnabled(!bIsFirst);
        _btnDownload.setEnabled(!bIsFirst);
        _btnUlpoad.setEnabled(!bIsFirst);
        _btnMailto.setEnabled(!bIsFirst);
        if ( _miLogin!=null )
            _miLogin.setEnabled(bIsFirst);
        if ( _miLogout != null )
            _miLogout.setEnabled(!bIsFirst);
    }
    private String readUserData(File lFilePdf, ArrayRecs lScanList)
    {
        File lFileTxt = new File(GetTextName(lFilePdf.getAbsolutePath()));
        if ( !lFileTxt.exists() )
            return null;
        String sUserData = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(lFileTxt));
            String sLine = "";
            int nCountLine = 0;
            while ( (sLine=br.readLine()) != null)
            {
                if ( nCountLine++ == 0 ) {
                    sUserData = new String(sLine);
                } else {
                    ListRec lListRec = new ListRec();
                    lListRec.fromString(sLine);
                    lScanList.add(lListRec);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sUserData;
    }
    private void writeUserData(File lFilePdf, String sUserData, ArrayRecs lScanList)
    {
        File lFileTxt = new File(GetTextName(lFilePdf.getAbsolutePath()));
        try {
//            FileWriter fw = new FileWriter(lFileTxt);
//            fw.write(sUserData);
            PrintWriter fw = new PrintWriter(new FileWriter(lFileTxt));
            if ( sUserData!=null  )
                fw.println(sUserData);
            else
                fw.println();
            if ( lScanList!= null )
                for (ListRec lListRec : lScanList)
                    fw.println( lListRec.toString() );
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
/*
    private void deleteUserData(File lFilePdf)
    {
        File lFileTxt = new File(GetTextName(lFilePdf.getAbsolutePath()));
        if ( lFileTxt.exists() )
            lFileTxt.delete();
    }
*/
    private void LoadLists() {
        if ( IsFirst() ) {
            removeItem(-1);
            File[] lFileList = mDataPath.listFiles(mFileFilter);
            if (lFileList != null) {
                File lFile, lFileTxt;
                boolean mIsSigned;
                for (int i = 0; i < lFileList.length; i++) {
                    lFile = lFileList[i];
                    mIsSigned = new File(GetSignName(lFile.getAbsolutePath())).exists();
                    _lveLocalList.AddItem(lFile, mIsSigned);

                    ArrayRecs lScanList = new ArrayRecs();
                    String sUserData = readUserData(lFile, lScanList);
                    ListRecEx lLocalData = new ListRecEx(lFile, mIsSigned, sUserData, lScanList);
                    mLocalDataList.add(lLocalData);
                }
            }
            ClearServerList(-1);
            ClearFilterList(-1);
        } else {
            for (int i = 0; i < mLocalDataList.size(); i++) {
                ListRecEx lLocalData = mLocalDataList.get(i);
                _lveLocalList.AddItem(lLocalData.mFile, lLocalData.mIsSigned);
            }

            for (int i = 0; i < mFilterDataList.size(); i++) {
                ListRec lFilterData = mFilterDataList.get(i);
                _lveFilterList.AddItem(lFilterData.mFile, lFilterData.mIsSigned);
            }
        }
    }
    private void FillFilterList()
    {
        ClearFilterList(-1);
        for (int i = 0; i < mServerDataList.size(); i++) {
            ListRec lServerData = mServerDataList.get(i);
//            _lveFilterList.AddItem(lFile, false);
            AddFilterList(lServerData);
        }
    }
    private void AddFilterList(ListRec lServerData)
    {
        if ( mFilter!=null )
        {
            String sFileName = lServerData.mFile.getName().toLowerCase();
            sFileName = sFileName.replace(".pdf", "");
            if ( !sFileName.contains(mFilter.toLowerCase()) )
                return;
        }
        mFilterDataList.add(lServerData);
        _lveFilterList.AddItem(lServerData.mFile, lServerData.mIsSigned);
    }
    private void RemoveFromServerList(ListRec lFilterData)
    {
        lFilterData.mIsSigned = true;
        int position = mServerDataList.indexOf(lFilterData);
        if ( position<0 )
            return;
        mServerDataList.remove(position);
    }
    private void ClearServerList(int position) {
        if ( position<0 ) {
//            _lveFilterList.RemItem(null);
            mServerDataList.clear();
        } else {
            ListRec lServerData = mServerDataList.get(position);
            mServerDataList.remove(position);
//          _lveFilterList.RemItem(lServerData.mFile);
        }
    }
    private void ClearFilterList(int position) {
        if ( position<0 ) {
            _lveFilterList.RemItem(null);
            mFilterDataList.clear();
        } else {
            ListRec lServerData = mFilterDataList.get(position);
            mFilterDataList.remove(position);
            _lveFilterList.RemItem(lServerData.mFile);
        }
    }
    private void removeItem(int position) {
        if ( position<0 ) {
            _lveLocalList.RemItem(null);
            mLocalDataList.clear();
        } else {
            ListRecEx lLocalData = mLocalDataList.get(position);
            if (lLocalData.mIsSigned)
                return;
            _lveLocalList.RemItem(lLocalData.mFile);
            lLocalData.mFile.delete();
            mLocalDataList.remove(position);
        }
    }
    private void removeItemDialog(int position, View view) {
//        mSelectPos = position;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.idsRemoveItem))
            .setPositiveButton(getResources().getString(R.string.idsYes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    removeItem(mSelectPos);
                    mSelectPos = -1;
                }
            })
            .setNegativeButton(getResources().getString(R.string.idsNo), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mSelectPos = -1;
                }
            });
//        builder.create().show();
        showDialogAfterItem(builder, view);
    }
    private void errorDialog(String sError) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(sError)
                .setPositiveButton(getResources().getString(R.string.idsOK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.create().show();
    }
    private void selectEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
        builder.setTitle(getResources().getString(R.string.idsEnterEmail));
        LinearLayout _llDialogMail = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_mail, null);
        builder.setView(_llDialogMail);
        _etEditEmail = (EditText)_llDialogMail.findViewById(R.id.idEditEmail);
//        builder.setCancelable(false);
        builder.setPositiveButton(getResources().getString(R.string.idsOK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String sMailTo = _etEditEmail.getText().toString();
                UploadData(sMailTo);
            }
        })
        .setNegativeButton(getResources().getString(R.string.idsCancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                UploadData(null);
            }
        });
//        AlertDialog alert = builder.create();        alert.show();
        builder.create().show();
    }
    // Setting functions
    private void LoadSetting(int ts_type) {
        SharedPreferences sPref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if ( (ts_type&TS_LOGIN) > 0 )  {
            mUserName = sPref.getString(USER_NAME, "sa");
            mPassword = sPref.getString(PASSWORD, "igr");
            mCompany = sPref.getString(COMPANY, "FVJ");
        }
        if ( (ts_type&TS_SETTING) > 0 ) {
            mHostName = sPref.getString(HOST_NAME, "http://10.0.2.2");
            mServerName = sPref.getString(SRV_NAME, ".\\SQLEXPRESS");
            mDataBase = sPref.getString(DB_NAME, "lox");
            mCheckUser = sPref.getBoolean(CHECK_USER, false);
            mCheckCompany = sPref.getBoolean(CHECK_COMPANY, false);
            mCheckAddUser = sPref.getBoolean(CHECK_ADDUSER, false);
            mCheckScan = sPref.getBoolean(CHECK_SCAN, false);
        }
        mPfdWork.LoadSetting(ts_type);
    }
    private void SaveSetting(int ts_type) {
        SharedPreferences sPref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Editor ed = sPref.edit();
        if ( (ts_type&TS_LOGIN) > 0 )  {
            ed.putString(USER_NAME, mUserName  );
            ed.putString(PASSWORD,  mPassword  );
            ed.putString(COMPANY,  mCompany  );
        }
        if ( (ts_type&TS_SETTING) > 0 ) {
            ed.putString(HOST_NAME, mHostName);
            ed.putString(SRV_NAME, mServerName);
            ed.putString(DB_NAME, mDataBase);
            ed.putBoolean(CHECK_USER, mCheckUser);
            ed.putBoolean(CHECK_COMPANY, mCheckCompany);
            ed.putBoolean(CHECK_ADDUSER, mCheckAddUser);
            ed.putBoolean(CHECK_SCAN, mCheckScan);
        }
        ed.commit();
    }
    // Handler functions
    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ERROR:
                    if (msg.obj == null)
                        break;
                    Exception lException = (Exception) msg.obj;
                    errorDialog(lException.toString());
//                    Toast.makeText(MainActivity.this, lException.toString(), Toast.LENGTH_LONG).show();
                    break;
                case MSG_DOWNLOAD:
                    if (msg.obj == null)
                        break;
//                        File lFile = (File) msg.obj;
                    break;
                case MSG_UPLOAD:
                    if (msg.obj == null)
                        break;
//                        Integer nResponceCode = (Integer) msg.obj;
                    break;
                case MSG_GETLIST:
                    if (msg.obj == null)
                        break;
//                        mListUrl = (List<String>) msg.obj;
                    break;
            }
            }
        };
    }
    static void showDialogAfterItem(AlertDialog.Builder builder, View view)
    {
        AlertDialog dialog = builder.create();
//      dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if ( view!=null) {
            WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
            wmlp.gravity = Gravity.LEFT| Gravity.TOP;
            wmlp.x = view.getLeft();
            wmlp.y = view.getBottom() + view.getHeight()*3/2;
        }
        dialog.show();
    }

    // request permissions
    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, REQUEST_PERMISSIONS);
    }

    // ListRec class
    public static class ListRec {
        // Constants
        public final static String mSep = " ";
        // Fields
        public File mFile = null;
        public boolean mIsSigned = false;

        public ListRec() {
        }
        public ListRec(File lFile) {
            this.mFile = lFile;
        }
        public ListRec(File lFile, boolean lIsSigned) {
            this.mFile = lFile;
            mIsSigned = lIsSigned;
        }
        public void fromString(String lFmt) {
            String [] aRec = lFmt.split(mSep);
            this.mFile = ( aRec.length>0 ) ? new File(aRec[0]) : null;
            this.mIsSigned = ( aRec.length>1 && Integer.parseInt(aRec[1])>0 );
        }
        @Override
        public String toString()
        {
            return String.format("%s%s%d", mFile.getName(), mSep, (mIsSigned)?1:0);
/*
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%s%s%d", mFile.getName(), mSep, (mIsSigned)?1:0) );
            return builder.toString();
*/
        }
    }
    public static class ArrayRecs extends ArrayList<ListRec> {
        public ArrayList<String> toStringArrayList() {
            int n = this.size();
            ArrayList<String> aStringList = new ArrayList<String>(n);
            for (int i = 0; i < n; i++) {
                aStringList.add( this.get(i).toString() );
            }
            return aStringList;
        }
        public void fromStringArrayList(ArrayList<String> aStringList) {
            this.clear();
            if ( aStringList==null )
                return;
            int n = aStringList.size();
            for (int i = 0; i < n; i++) {
                ListRec lListRec = new ListRec();
                lListRec.fromString(aStringList.get(i));
                this.add(lListRec);
            }
        }
    }
    // ListRecEx class
    public static class ListRecEx extends ListRec {
        public String mUserData = null;
        public ArrayRecs mScanList = null;

        public ListRecEx() {
        }
        public ListRecEx(File lFile) {
            super(lFile);
        }
        public ListRecEx(File lFile, boolean lIsSigned) {
            super(lFile, lIsSigned);
        }
        public ListRecEx(File lFile, boolean lIsSigned, String lUserData) {
            super(lFile, lIsSigned);
            mUserData = lUserData;
        }
        public ListRecEx(File lFile, boolean lIsSigned, String lUserData, ArrayRecs lScanList) {
            super(lFile, lIsSigned);
            mUserData = lUserData; mScanList = lScanList;
        }
    }
}
/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.idFab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
        });
*/

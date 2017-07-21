package com.igr.pod.work;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Constants
    final static int CODE_LOGIN = 123;
    final static int CODE_SETTING = 124;
    final static int CODE_PDFVIEW = 125;

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
    final static String PDF_FILENAME = "pdf_name";
    final static String PDF_USERDATA = "user_data";
    final static String LOGIN_ERROR = "login_error";

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
    // statics
    static private List<DataList> mLocalDataList = new ArrayList<DataList>();
    static private List<DataList> mServerDataList = new ArrayList<DataList>();
    static private List<DataList> mFilterDataList = new ArrayList<DataList>();
    static private String mLoginName = null;
    static public String GetSignName(String sFileName) { return sFileName.replace(".pdf", ".png"); }
    static public String GetTextName(String sFileName) { return sFileName.replace(".pdf", ".txt"); }
    static public boolean IsFirst() { return (mLoginName == null); }
    static public boolean mSignViewOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _tbMain = (Toolbar) findViewById(R.id.idToolbar);
        setSupportActionBar(_tbMain);

        mPfdWork = new PfdWork(this);
        mDataPath =  getDir("documents", MODE_PRIVATE);

        LoadSetting(TS_ALL);
        InitControls();
        InitHandler();
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
                            String sUserData = intent.getStringExtra(MainActivity.PDF_USERDATA);
                            DataList lLocalData = mLocalDataList.get(mSelectPos);
                            lLocalData.mIsSigned = true;
                            lLocalData.mUserData = sUserData;
                            writeUserData(lLocalData.mFile, lLocalData.mUserData);
                        }
                    }
                } else {
                    mSignViewOnly = false;
                    _lveLocalList.setItemChecked(mSelectPos, true);
                }
                mSelectPos = -1;
                break;
        }
    }
    // Buttons function
    public void onGetlist(View view) {
        String sUrl = mHostName + "/getlist.php";
        List<String> lListUrl = mPfdWork.getList(sUrl, mHandler);

        ClearServerList(-1);
        ClearFilterList(-1);
        for (int i = 0; i < lListUrl.size(); i++) {
            File lFile = new File(lListUrl.get(i));
            DataList lServerData = new DataList(lFile);
            mServerDataList.add(lServerData);
//            _lveFilterList.AddItem(lFile, false);
            AddFilterList(lServerData);
        }
    }
    public void onLoad(View view) {
        String sFileNameTmp = String.format("temp%d.pdf", _lveLocalList.getCount());
        for (int i = mFilterDataList.size()-1; i>=0 ; i--) {
            DataList lFilterData = mFilterDataList.get(i);
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
                    DataList lLocalData = new DataList(lFile);
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

            deleteUserData(lFile);
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
    }

    // Private functions
    private void InitControls() {
        _btnGetlist  = (Button) findViewById(R.id.idGetlist);
        _btnDownload = (Button) findViewById(R.id.idDownload);
        _btnUlpoad = (Button) findViewById(R.id.idUpload);
        _btnMailto = (Button) findViewById(R.id.idMailto);
        _btnClear = (Button) findViewById(R.id.idClear);
        _lveLocalList = (ListViewEx) findViewById(R.id.idLocalList);
        _lveFilterList = (ListViewEx) findViewById(R.id.idServerList);
        _etFilter = (EditText) findViewById(R.id.idEditFilter);

        _lveLocalList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DataList lLocalData = mLocalDataList.get(position);
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
                if ( _lveLocalList.isItemChecked(position) )
                    return false;
                removeItemDialog(position);
                return true;
            }
        });
        _lveFilterList.setOnItemClickListener(new ListView.OnItemClickListener() {
             @Override
             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                 if (position>=0 && position<mServerDataList.size()) {
                     DataList lServerData = mFilterDataList.get(position);
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

//        _etFilter.clearFocus();
//        _etFilter.setFocusableInTouchMode(false);
//        _btnClear.requestFocus();
    }

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

    private String readUserData(File lFilePdf)
    {
        File lFileTxt = new File(GetTextName(lFilePdf.getAbsolutePath()));
        if ( !lFileTxt.exists() )
            return null;
        String sUserData = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(lFileTxt));
            sUserData = br.readLine();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sUserData;
    }
    private void writeUserData(File lFilePdf, String sUserData)
    {
        if ( sUserData==null || sUserData.isEmpty() )
            return;
        File lFileTxt = new File(GetTextName(lFilePdf.getAbsolutePath()));
        try {
            FileWriter fw = new FileWriter(lFileTxt);
            fw.write(sUserData);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void deleteUserData(File lFilePdf)
    {
        File lFileTxt = new File(GetTextName(lFilePdf.getAbsolutePath()));
        if ( lFileTxt.exists() )
            lFileTxt.delete();
    }

    private void LoadLists() {
        if ( IsFirst() ) {
            ClearLocalList(-1);
            File[] lFileList = mDataPath.listFiles(mFileFilter);
            if (lFileList != null) {
                File lFile, lFileTxt;
                boolean mIsSigned;
                String sUserData;
                for (int i = 0; i < lFileList.length; i++) {
                    lFile = lFileList[i];
                    mIsSigned = new File(GetSignName(lFile.getAbsolutePath())).exists();
                    _lveLocalList.AddItem(lFile, mIsSigned);

                    sUserData = readUserData(lFile);
                    DataList lLocalData = new DataList(lFile, mIsSigned, sUserData);
                    mLocalDataList.add(lLocalData);
                }
            }
            ClearServerList(-1);
            ClearFilterList(-1);
        } else {
            DataList lLocalData;
            for (int i = 0; i < mLocalDataList.size(); i++) {
                lLocalData = mLocalDataList.get(i);
                _lveLocalList.AddItem(lLocalData.mFile, lLocalData.mIsSigned);
            }
            for (int i = 0; i < mFilterDataList.size(); i++) {
                lLocalData = mFilterDataList.get(i);
                _lveFilterList.AddItem(lLocalData.mFile, lLocalData.mIsSigned);
            }
        }
    }

    private void FillFilterList()
    {
        ClearFilterList(-1);
        for (int i = 0; i < mServerDataList.size(); i++) {
            DataList lServerData = mServerDataList.get(i);
//            _lveFilterList.AddItem(lFile, false);
            AddFilterList(lServerData);
        }
    }
    private void AddFilterList(DataList lServerData)
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
    private void RemoveFromServerList(DataList lFilterData)
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
            DataList lServerData = mServerDataList.get(position);
            mServerDataList.remove(position);
//          _lveFilterList.RemItem(lServerData.mFile);
        }
    }
    private void ClearFilterList(int position) {
        if ( position<0 ) {
            _lveFilterList.RemItem(null);
            mFilterDataList.clear();
        } else {
            DataList lServerData = mFilterDataList.get(position);
            mFilterDataList.remove(position);
            _lveFilterList.RemItem(lServerData.mFile);
        }
    }
    private void ClearLocalList(int position) {
        if ( position<0 ) {
            _lveLocalList.RemItem(null);
            mLocalDataList.clear();
        } else {
            DataList lLocalData = mLocalDataList.get(position);
            if (lLocalData.mIsSigned)
                return;
            _lveLocalList.RemItem(lLocalData.mFile);
            lLocalData.mFile.delete();
            mLocalDataList.remove(position);
        }
    }

    private void removeItemDialog(int position) {
        mSelectPos = position;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.idsRemoveItem))
            .setPositiveButton(getResources().getString(R.string.idsYes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    ClearLocalList(mSelectPos);
                    mSelectPos = -1;
                }
            })
            .setNegativeButton(getResources().getString(R.string.idsNo), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mSelectPos = -1;
                }
            });
        builder.create().show();
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
        }
        ed.commit();
    }
    // Handler functions
    private void InitHandler() {
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

    // DataList class
    public class DataList {
        public File mFile;
        public boolean mIsSigned;
        public String mUserData;
        public DataList(File lFile, boolean lIsSigned, String lUserData) {
            mFile = lFile; mIsSigned = lIsSigned; mUserData = lUserData;
        }
        public DataList(File lFile, boolean lIsSigned) {
            mFile = lFile; mIsSigned = lIsSigned; mUserData = null;
        }
        public DataList(File lFile) {
            mFile = lFile; mIsSigned = false; mUserData = null;
        }
        public DataList() {
            mFile = null; mIsSigned = false;; mUserData = null;
        }
    }
}
/*
    public void sendEmail(File lFile) {
        if ( lFile == null )
            return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"igr2001@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
        intent.putExtra(Intent.EXTRA_TEXT, "body of email");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(lFile));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(intent, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }
*/
/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.idFab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
        });
*/

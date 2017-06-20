package com.igr.pod.work;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

//import android.app.Activity;

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
    final static String CHECK_EDITSIGN = "check_editsign";
    final static String PDF_FILENAME = "pdf_name";
// Controls
    private Toolbar _tbMain = null;
    private ListViewEx _lveLocalList = null;
    private ListViewEx _lveServerList = null;
    private Button _btnGetlist = null;
    private Button _btnDownload = null;
    private Button _btnUlpoad = null;
    private MenuItem _miLogin = null;
    private MenuItem _miLogout = null;
// Local members
    public String mHostName;
    private String mServerName;
    private String mDataBase;
    private String mUserName;
    private String mPassword;
    private String mCompany;
    private boolean mCheckUser;
    private boolean mCheckCompany;
    private boolean mCheckEditSign;

    private PfdWork mPfdWork = null;
    private Handler mHandler = null;
    private File mDataPath;
    private FilenameFilter mFileFilter = null;
    private FilenameFilter mSignFilter = null;
    private int mSelectPos = -1;
// statics
    static private List<DataList> mLocalDataList = new ArrayList<DataList>();
    static private List<DataList> mServerDataList = new ArrayList<DataList>();
    static private String mLoginName = null;
    static public String GetSignName(String sFileName) { return sFileName.replace(".pdf", ".png"); }
    static public boolean IsFirst() { return (mLoginName == null); }
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
        FillLists();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SaveSetting();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CODE_LOGIN:
                if (resultCode != RESULT_OK)
                    break;
                SetLogin();
                break;
            case CODE_SETTING:
                if (resultCode != RESULT_OK)
                    break;
                LoadSetting(TS_SETTING);
                break;
            case CODE_PDFVIEW:
                if ( !mCheckEditSign )
                {
                    if (resultCode != RESULT_OK)
                        _lveLocalList.setItemChecked(mSelectPos, false);
                    break;
                }

                String sFileName = ((File) _lveLocalList.getItemAtPosition(mSelectPos)).getAbsolutePath();
                File fileSign = new File(GetSignName(sFileName));
                boolean bIsSigned = fileSign.exists();
                if ( resultCode == RESULT_FIRST_USER )
                {
                    fileSign.delete();
                    bIsSigned = false;
                }
                _lveLocalList.setItemChecked(mSelectPos, bIsSigned);
                mLocalDataList.get(mSelectPos).mIsSigned = bIsSigned;
                mSelectPos = -1;
                break;
        }
    }
    // Buttons function
    public void onGetlist(View view) {
        String sUrl = mHostName + "/getlist.php";
        List<String> lListUrl = mPfdWork.getList(sUrl, mHandler);

        ClearServerList();
        for (int i = 0; i < lListUrl.size(); i++) {
            File lFile = new File(lListUrl.get(i));
            _lveServerList.AddItem(lFile, false);
            DataList lServerDataList = new DataList(lFile, lListUrl.get(i));
            mServerDataList.add(lServerDataList);
        }
    }
    public void onLoad(View view) {
        String sFileNameTmp = String.format("temp%d.pdf", _lveLocalList.getCount());
        for (int i = mServerDataList.size()-1; i>=0 ; i--) {
            DataList lServerDataList = mServerDataList.get(i);
            if ( !lServerDataList.mIsSigned )
                continue;

            String sUrl = String.format("%s/%s", mHostName, lServerDataList.mFile);
            File lFile = mPfdWork.downloadFile(sUrl, mDataPath.getAbsolutePath(), sFileNameTmp, mHandler);
            if (lFile != null) {
                String sSignName = GetSignName(lFile.getAbsolutePath());
                _lveServerList.RemItem(lServerDataList.mFile);
                mServerDataList.remove(i);

                boolean mIsSigned = new File(sSignName).exists();
                if ( _lveLocalList.AddItem(lFile, mIsSigned) ) {
                    DataList lLocalDataList = new DataList(lFile, sUrl);
                    mLocalDataList.add(lLocalDataList);
                }
            }
        }
    }
    public void onUpload(View view) {
        String sUrl = mHostName + "/upload.php";
        Integer nResponceCode;
        for (int i = mLocalDataList.size()-1; i>=0; i--) {
            if (!_lveLocalList.isItemChecked(i))
                continue;

            File lFile = mLocalDataList.get(i).mFile;
            String sFileName = lFile.getAbsolutePath();
            nResponceCode = mPfdWork.uploadFile(sUrl, sFileName, mHandler);
            if (nResponceCode == HttpURLConnection.HTTP_OK) {
                mLocalDataList.remove(i);
                _lveLocalList.RemItem(lFile);
                lFile.delete();
            } else
                continue;

            String sSignName = GetSignName(sFileName);
            nResponceCode = mPfdWork.uploadFile(sUrl, sSignName, mHandler);
            if (nResponceCode == HttpURLConnection.HTTP_OK) {
                new File(sSignName).delete();
            } else
                continue;
        }
    }
    public void onClear(View view) {
        ClearServerList();
    }
    // Private functions
    private void InitControls() {
        _btnGetlist  = (Button) findViewById(R.id.idGetlist);
        _btnDownload = (Button) findViewById(R.id.idDownload);
        _btnUlpoad = (Button) findViewById(R.id.idUpload);
        _lveLocalList = (ListViewEx) findViewById(R.id.idLocalList);
        _lveServerList = (ListViewEx) findViewById(R.id.idServerList);

        _lveLocalList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            boolean mIsChecked = _lveLocalList.isItemChecked(position);
            if (mCheckEditSign || mIsChecked) {
                Intent intent = new Intent(MainActivity.this, PdfViewActivity.class);
                String sFileName = ((File) _lveLocalList.getItemAtPosition(position)).getAbsolutePath();
                intent.putExtra(MainActivity.PDF_FILENAME, sFileName);
                startActivityForResult(intent, CODE_PDFVIEW);
                mSelectPos = position;
                return;
            }
            if (!mCheckEditSign && !mIsChecked) {
                _lveLocalList.setItemChecked(position, true);
            }
            }
        });
        _lveLocalList.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if ( !_lveLocalList.isItemChecked(position) )
                    return false;
//                removeItemDialog(position);
                return true;
            }
        });
        _lveServerList.setOnItemClickListener(new ListView.OnItemClickListener() {
             @Override
             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                 if (position>=0 && position<mServerDataList.size()) {
                     DataList lDataList = mServerDataList.get(position);
                     boolean lIsSigned  = _lveServerList.isItemChecked(position);
                     lDataList.mIsSigned = lIsSigned;
                 }
             }
        });
        mFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String fileName) { return fileName.matches(".*\\.pdf"); }
        };
        mSignFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String fileName) { return fileName.matches(".*\\.png"); }
        };
    }

    private void InitHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ERROR:
                        if (msg.obj == null)
                            break;
                        Exception lException = (Exception) msg.obj;
                        Toast.makeText(MainActivity.this, lException.toString(), Toast.LENGTH_LONG).show();
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

    private void SetLogin() {
        LoadSetting(TS_LOGIN);
        mLoginName = mUserName;
        OutTitle();
    }
    private void SetLogout() {
        mLoginName = null;
        ClearServerList();
        OutTitle();
    }
    private void OutTitle()
    {
        boolean bIsFirst = IsFirst();
        String sTitle = getString(R.string.app_name)+ ((bIsFirst)?": " + getString(R.string.idsLogout):": user - " + mLoginName);
        _tbMain.setTitle(sTitle);
        _btnGetlist.setEnabled(!bIsFirst);
        _btnDownload.setEnabled(!bIsFirst);
        _btnUlpoad.setEnabled(!bIsFirst);
        if ( _miLogin!=null )
            _miLogin.setEnabled(bIsFirst);
        if ( _miLogout != null )
            _miLogout.setEnabled(!bIsFirst);
    }

    private void FillLists() {
        if ( IsFirst() ) {
            mLocalDataList.clear();
            File[] lFileList = mDataPath.listFiles(mFileFilter);
            if (lFileList != null) {
                File lFile;
                boolean mIsSigned;
                for (int i = 0; i < lFileList.length; i++) {
                    lFile = lFileList[i];
                    mIsSigned = new File(GetSignName(lFile.getAbsolutePath())).exists();
                    _lveLocalList.AddItem(lFile, mIsSigned);
                    DataList lDataList = new DataList(lFile);
                    mLocalDataList.add(lDataList);
                }
            }

            ClearServerList();
        } else {
            DataList lDataList;
            for (int i = 0; i < mLocalDataList.size(); i++) {
                lDataList = mLocalDataList.get(i);
                _lveLocalList.AddItem(lDataList.mFile, lDataList.mIsSigned);
            }

            for (int i = 0; i < mServerDataList.size(); i++) {
                lDataList = mServerDataList.get(i);
                _lveServerList.AddItem(lDataList.mFile, lDataList.mIsSigned);
            }
        }
    }

    private void ClearServerList() {
        mServerDataList.clear();
        _lveServerList.RemItem(null);
    }
    private void ClearLocalList() {
        mLocalDataList.clear();
        _lveLocalList.RemItem(null);
    }
    private void LoadSetting(int type) {
        SharedPreferences sPref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if ( (type&TS_LOGIN) > 0 )  {
            mUserName = sPref.getString(USER_NAME, "sa");
            mPassword = sPref.getString(PASSWORD, "igr");
            mCompany = sPref.getString(COMPANY, "FVJ");
        }
        if ( (type&TS_SETTING) > 0 ) {
            mHostName = sPref.getString(HOST_NAME, "http://10.0.2.2");
            mServerName = sPref.getString(SRV_NAME, ".\\SQLEXPRESS");
            mDataBase = sPref.getString(DB_NAME, "lox");
            mCheckUser = sPref.getBoolean(CHECK_USER, false);
            mCheckCompany = sPref.getBoolean(CHECK_COMPANY, false);
            mCheckEditSign = sPref.getBoolean(CHECK_EDITSIGN, false);
        }
        mPfdWork.LoadSetting(type);
    }

    private void SaveSetting() {
        SharedPreferences sPref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Editor ed = sPref.edit();
        ed.putString(HOST_NAME, mHostName  );
        ed.putString(SRV_NAME,  mServerName);
        ed.putString(DB_NAME,   mDataBase  );
        ed.putString(USER_NAME, mUserName  );
        ed.putString(PASSWORD,  mPassword  );
        ed.putString(COMPANY,  mCompany  );
        ed.putBoolean(CHECK_USER, mCheckUser);
        ed.putBoolean(CHECK_COMPANY, mCheckCompany);
        ed.putBoolean(CHECK_EDITSIGN, mCheckEditSign);
        ed.commit();
    }

    private void removeItemDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.idsRemoveItem))
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.idsYes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        Notes.deleteById(mDatabaseHelper, mNotes.get(position).getId());
//                        initNotesList();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.idsNo), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    // DataList class
    public class DataList {
        public File mFile;
        public String mUrl;
        public boolean mIsSigned;
        public DataList(File lFile, String lUrl, boolean lIsSigned) {
            mFile = lFile; mUrl = lUrl; mIsSigned = lIsSigned;
        }
        public DataList(File lFile, String lUrl) {
            mUrl = lUrl; mFile = lFile; mIsSigned = false;
        }
        public DataList(File lFile) {
            mFile = lFile; mUrl = null;mIsSigned = false;
        }
        public DataList() {
            mFile = null; mUrl = null; mIsSigned = false;
        }
    }
}

/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.idFab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                File[] lFileList = new File(mDataPath).listFiles(mFileFilter);
                File[] lFileList = mDataPath.listFiles(mFileFilter);
                for (int i = 0; i < lFileList.length; i++)
                    lFileList[i].delete();
                _lveLocalList.RemItem(null);
            }
        });
*/
/*
        mLocalDataList.clear();
        File[] lFileList = mDataPath.listFiles(mFileFilter);
        if ( lFileList != null ) {
            for (int i = 0; i < lFileList.length; i++)
                lFileList[i].delete();
        }
        lFileList = mDataPath.listFiles(mSignFilter);
        if ( lFileList != null ) {
            for (int i = 0; i < lFileList.length; i++)
                lFileList[i].delete();
        }
        _lveLocalList.RemItem(null);
    }
*/

package com.igr.pod.work;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.igr.pod.work.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LoginDlgActivity extends AppCompatActivity {
    private String mHostName;
    private String mServerName;
    private String mDataBase;
    private boolean mCheckCompany;

    private String mUserName;
    private String mPassword;
    private String mCompany;

    private EditText _etUserName;
    private EditText _etPassword;
    private EditText _etCompany;
    private CheckBox _cbShowPassword;

    private Context mContext = null;
    private String mError = new String();
    private String mResult = new String();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_dlg);

        LoadSetting();

        _etUserName = (EditText)findViewById(R.id.idEditUserName);
        _etPassword = (EditText)findViewById(R.id.idEditPassword);
        _etCompany = (EditText)findViewById(R.id.idEditCompany);

        _etUserName.setText(mUserName);
        _etPassword.setText(mPassword);
        if ( mCheckCompany )
            _etCompany.setText(mCompany);
        else {
            ((TextView)findViewById(R.id.idNameCompany)).setVisibility(View.INVISIBLE);
            _etCompany.setVisibility(View.INVISIBLE);
        }

        findViewById(R.id.idLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUserName = _etUserName.getText().toString();
                mPassword  = _etPassword.getText().toString();
                mCompany  = (mCheckCompany) ? _etCompany.getText().toString() : "";
                login(mHostName + "/login.php");
            }
        });
        findViewById(R.id.idBtnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveSetting(RESULT_CANCELED);
                finish();
            }
        });
        ((CheckBox)findViewById(R.id.idShowPassword)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int type = InputType.TYPE_CLASS_TEXT;
                if (!isChecked)
                    type |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
                _etPassword.setInputType(type);
            }
        });
    }

    private void LoadSetting()
    {
        SharedPreferences sPref = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        mHostName = sPref.getString(MainActivity.HOST_NAME, "http://10.0.2.2");
        mServerName = sPref.getString(MainActivity.SRV_NAME, ".\\SQLEXPRESS");
        mDataBase = sPref.getString(MainActivity.DB_NAME, "lox");
        mCheckCompany = sPref.getBoolean(MainActivity.CHECK_COMPANY, false);

        mUserName = sPref.getString(MainActivity.USER_NAME, "sa");
        mPassword = sPref.getString(MainActivity.PASSWORD, "igr");
        mCompany = ( mCheckCompany ) ? sPref.getString(MainActivity.COMPANY, "FVJ") : "" ;
    }

    private void SaveSetting(int resultCode) {
        if ( resultCode == RESULT_OK ) {
            SharedPreferences sPref = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor ed = sPref.edit();
            ed.putString(MainActivity.USER_NAME, mUserName);
            ed.putString(MainActivity.PASSWORD, mPassword);
            if ( mCheckCompany )
                ed.putString(MainActivity.COMPANY, mCompany);
            ed.commit();
        } else {
            Toast.makeText(this, "Login error:\r\n" + mError, Toast.LENGTH_LONG).show();
        }
        Intent intentMain = new Intent();
        setResult(resultCode, intentMain);
    }

    public void login(String sUrl) {
        mContext = this;
        mError = "";
        mResult = "";
        AsyncTask<String, Void, String> mProgressTask = new AsyncTask<String, Void, String>() {
            private Dialog loadingDialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(mContext, "Please wait", "Logging...");
            }
            @Override
            protected String doInBackground(String... params) {
                String sUrl = params[0];
                String sAdd = String.format("servername=%s&database=%s&username=%s&password=%s", mServerName, mDataBase, mUserName, mPassword);
                if (mCheckCompany)
                    sAdd += String.format("&company=%s", mCompany);
                sUrl += String.format("?%s", sAdd);
                try {
                    URL lURL = new URL(sUrl);
                    HttpURLConnection lUrlConnection = (HttpURLConnection) lURL.openConnection();
                    lUrlConnection.setRequestMethod("GET"); // lUrlConnection.setRequestMethod("POST");
                    lUrlConnection.setDoInput(true);
                    lUrlConnection.setDoOutput(true);
                    lUrlConnection.setConnectTimeout(5000);
                    lUrlConnection.connect();
                    int responseCode = lUrlConnection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        InputStream lInputStream = lUrlConnection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(lInputStream, "UTF-8"), 8);
                        String line;// = lUrlConnection.getResponseMessage();
                        while ((line = reader.readLine()) != null)
                            mResult += line;
                        lInputStream.close();
//                        if ( !mResult.contentEquals(getResources().getString(R.string.idsPhpSuccess)) )
                        if ( !mResult.endsWith(getResources().getString(R.string.idsPhpSuccess)) )
                        {
                            mError = mResult;
                            mResult = "";
                        }
                        return mResult;
                    } else
                    {
                        mError = lUrlConnection.getResponseMessage();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    mError = e.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                    mError = e.toString();
                }
                return null;
            }
            @Override
            protected void onPostExecute(String lResult){
                loadingDialog.dismiss();
                SaveSetting( (mError.isEmpty()) ? RESULT_OK :RESULT_CANCELED);
                finish();
            }
        };
        mProgressTask.execute(sUrl);
    }
}

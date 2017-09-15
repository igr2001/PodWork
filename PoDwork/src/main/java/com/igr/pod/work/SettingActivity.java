package com.igr.pod.work;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.igr.pod.work.R;

public class SettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        LoadSetting();
        findViewById(R.id.idBtnOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveSetting();
                setResult(RESULT_OK, getIntent());
                finish();
            }
        });
        findViewById(R.id.idBtnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED, getIntent());
                finish();
            }
        });
    }
    public void LoadSetting() {
        SharedPreferences sPref = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        ((EditText) findViewById(R.id.idEditHost)).setText( sPref.getString(MainActivity.HOST_NAME,"http://10.0.2.2") );
        ((EditText) findViewById(R.id.idEditServerName)).setText( sPref.getString(MainActivity.SRV_NAME, ".\\SQLEXPRESS") );
        ((EditText) findViewById(R.id.idEditDataBase)).setText(sPref.getString(MainActivity.DB_NAME,  "lox") );
        ((CheckBox) findViewById(R.id.idCheckUser)).setChecked(sPref.getBoolean(MainActivity.CHECK_USER, false) );
        ((CheckBox) findViewById(R.id.idCheckCompany)).setChecked(sPref.getBoolean(MainActivity.CHECK_COMPANY, false) );
        ((CheckBox) findViewById(R.id.idAddUserName)).setChecked(sPref.getBoolean(MainActivity.CHECK_ADDUSER, false) );
        ((CheckBox) findViewById(R.id.idCheckScan)).setChecked(sPref.getBoolean(MainActivity.CHECK_SCAN, false) );
    }

    public void SaveSetting() {
        SharedPreferences sPref = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(MainActivity.HOST_NAME, ((EditText) findViewById(R.id.idEditHost)).getText().toString() );
        ed.putString(MainActivity.SRV_NAME,  ((EditText) findViewById(R.id.idEditServerName)).getText().toString() );
        ed.putString(MainActivity.DB_NAME,   ((EditText) findViewById(R.id.idEditDataBase)).getText().toString() );
        ed.putBoolean(MainActivity.CHECK_USER, ((CheckBox) findViewById(R.id.idCheckUser)).isChecked() );
        ed.putBoolean(MainActivity.CHECK_COMPANY, ((CheckBox) findViewById(R.id.idCheckCompany)).isChecked() );
        ed.putBoolean(MainActivity.CHECK_ADDUSER, ((CheckBox) findViewById(R.id.idAddUserName)).isChecked() );
        ed.putBoolean(MainActivity.CHECK_SCAN, ((CheckBox) findViewById(R.id.idCheckScan)).isChecked() );
        ed.commit();
    }

}

package com.igr.pod.work;

/**
 * Created by igr on 08-Mar-17.
 */

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class PfdWork {
    private Context mContext = null;
    private Handler mHandler = null;
    private Exception mError = null;

    private File mFile = null;
    private String mFileName = null;
    private List<String> mList = new ArrayList<String>();
    private String mResult = new String();
    private Integer mResponseCode = 0;
    private String mUserData = null;
    private String mMailTo = null;
    // Settings variables
    private String mHostName;
    private String mServerName;
    private String mDataBase;
    private String mUserName;
    private String mPassword;
    private String mCompany;
    private boolean mCheckUser;
    private boolean mCheckCompany;
    // Constructor
    public PfdWork(Context context) {
        mContext = context;
        LoadSetting(MainActivity.TS_ALL);
    }
    // Public functions
    public File downloadFile(String sUrl, String sFilePath, String sFileNameTmp, Handler lHandler) {
        mError = null;
        mFileName = sFileNameTmp;
        mHandler  = lHandler;
        mFile = null;
        mResponseCode = 0;

        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        AsyncTask<String, Integer, File> mProgressTask = new AsyncTask<String, Integer, File>() {
            @Override
            protected File doInBackground(String... params) {
                String sUrl = params[0];
                String sFilePath = params[1];
                String sFileNameTmp = params[2];
                try {
                    String sUrlConv = sUrl.replace(" ", "%20");
                    URL lURL = new URL(sUrlConv);
                    HttpURLConnection lUrlConnection = (HttpURLConnection) lURL.openConnection();
                    lUrlConnection.setRequestMethod("GET");
                    lUrlConnection.setDoInput(true);
                    lUrlConnection.setConnectTimeout(5000);
                    lUrlConnection.setReadTimeout(5000);
                    lUrlConnection.connect();
                    mResponseCode = lUrlConnection.getResponseCode();
                    if(mResponseCode != HttpURLConnection.HTTP_OK) {
                        mError = new Exception(String.format("ResponseCode=%d(%s)",
                                mResponseCode, lUrlConnection.getResponseMessage()));
                        mFile = null;
                        return mFile;
                    }
                    InputStream lInputStream = lUrlConnection.getInputStream();
                    mFileName = retriveFileName(sUrl, lUrlConnection, sFileNameTmp);
                    mFile = new File(sFilePath, mFileName);
//                  if (mFile != null)
                    mFile.createNewFile();
                    mFile.setReadable(true, false);
                    FileOutputStream lOutputStream = new FileOutputStream(mFile);

                    int totalSize = lUrlConnection.getContentLength();
                    int downloadedSize = 0;
                    publishProgress(downloadedSize, totalSize);
                    byte[] buffer = new byte[1024];
                    int bufferLength = 0;
                    while( (bufferLength = lInputStream.read(buffer)) > 0) {
                        lOutputStream.write(buffer, 0, bufferLength);
                        downloadedSize += bufferLength;
                        publishProgress(downloadedSize, totalSize);
                    }
                    lOutputStream.close();
                    lInputStream.close();
                    return mFile;
                } catch (IOException e) {
                    e.printStackTrace();
                    mError = e;
                }
                if (mFile != null) {
                    mFile.delete();
                    mFile = null;
                }
                return mFile;
            }
            @Override
            protected void onProgressUpdate(Integer... values) {
                if (values[0] == 0) {
                    if ( mFileName != null)
                        progressDialog.setMessage(String.format("Downloading %s", mFileName));
                    else
                        progressDialog.setMessage("Downloading...");
                    progressDialog.setCancelable(false);
                    progressDialog.setMax(100);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                }
                progressDialog.setProgress((int) ((values[0] / (float) values[1]) * 100));
            }
            @Override
            protected void onPostExecute(File file) {
                progressDialog.hide();
                if ( mHandler != null )
                {
                    if ( mError != null )
                        mHandler.sendMessage(mHandler.obtainMessage(MainActivity.MSG_ERROR, mError));
                    else
                        mHandler.sendMessage(mHandler.obtainMessage(MainActivity.MSG_DOWNLOAD, mFile));
                }
            }
        };
/**/
        mProgressTask.execute(sUrl, sFilePath, sFileNameTmp);
        try {
            return mProgressTask.get();
        }
        catch(InterruptedException e) {
            mError = e;
            return null;
        }
        catch(ExecutionException e) {
            mError = e;
            return null;
        }
/*
        return mFile;
*/
    }
    public Integer uploadFile(String sUrl, String sFileName, String sUsedData, String sMailTo, Handler lHandler) {
        mError = null;
        mFileName = sFileName;
        mUserData = sUsedData;
        mMailTo = sMailTo;
        mHandler  = lHandler;

        mResponseCode = 0;
//        mList.clear();
        mResult = "";
        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        AsyncTask<String, Integer, Integer> mProgressTask = new AsyncTask<String, Integer, Integer>() {
            @Override
            protected Integer doInBackground(String... params) {
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";

                String sUrl = modifyUrl(params[0]);
                if ( mUserData!=null && !mUserData.isEmpty() ) {
                    String sUserData = mUserData.replace(" ", "%20");
                    sUrl += "&userdata=" + sUserData;
                }
                if ( mMailTo!=null && !mMailTo.isEmpty() ) {
                    String sMailTo = mMailTo.replace(" ", "%20");
                    sUrl += "&mailto=" + sMailTo;
                }

                String sFileName = params[1];
                try {
                    URL lURL = new URL(sUrl);
                    HttpURLConnection lUrlConnection = (HttpURLConnection) lURL.openConnection();
                    lUrlConnection.setDoInput(true); // Allow Inputs
                    lUrlConnection.setDoOutput(true); // Allow Outputs
                    lUrlConnection.setConnectTimeout(5000);
                    lUrlConnection.setReadTimeout(5000);
                    lUrlConnection.setUseCaches(false); // Don't use a Cached Copy
                    lUrlConnection.setRequestMethod("POST");
                    lUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                    lUrlConnection.setRequestProperty("ENCTYPE", "multipart/form-data");
                    lUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    lUrlConnection.setRequestProperty("uploaded_file", sFileName);
                    DataOutputStream lOutputStream = new DataOutputStream(lUrlConnection.getOutputStream());
                    lOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    lOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""+ sFileName + "\"" + lineEnd);
                    lOutputStream.writeBytes(lineEnd);

                    mFile = new File(sFileName);
                    FileInputStream lInputStream = new FileInputStream(mFile);
                    int totalSize = lInputStream.available();
                    int uploadedSize = 0;
                    publishProgress(uploadedSize , totalSize);
                    byte[] buffer = new byte[1024];
                    int bufferLength = 0;
                    // read file and write it into form...
                    while( (bufferLength = lInputStream.read(buffer)) > 0) {
                        lOutputStream.write(buffer, 0, bufferLength);
                        uploadedSize += bufferLength;
                        publishProgress(uploadedSize, totalSize);
                    }
                    // send multipart form data necesssary after file data...
                    lOutputStream.writeBytes(lineEnd);
                    lOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    // Responses from the server (code and message)
                    mResponseCode = lUrlConnection.getResponseCode();
                    String sResponseMessage = lUrlConnection.getResponseMessage();
                    if ( mResponseCode == HttpURLConnection.HTTP_OK ) {
                        InputStream lInputStreamUrl = lUrlConnection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(lInputStreamUrl, "UTF-8"), 8);
                        String line;
                        while ((line = reader.readLine()) != null)
                            mResult += line; // mList.add(line);
                        lInputStreamUrl.close();
                        if ( !mResult.endsWith(mContext.getResources().getString(R.string.idsPhpSuccess)) )
                        {
                            mError = new Exception(mResult);
                            mResult = "";
                        }
                    } else {
                        mError = new Exception(String.format("ResponseCode=%d(%s)",
                                mResponseCode, lUrlConnection.getResponseMessage()));
                    }
                    lInputStream.close();
                    lOutputStream.flush();
                    lOutputStream.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    mError = e;
                } catch (IOException e) {
                    e.printStackTrace();
                    mError = e;
                }
                return mResponseCode;
            }
            @Override
            protected void onProgressUpdate(Integer... values) {
                if (values[0] == 0) {
                    if ( mFileName != null)
                        progressDialog.setMessage(String.format("Uploading %s", mFileName));
                    else
                        progressDialog.setMessage("Uploading...");
                    progressDialog.setCancelable(false);
                    progressDialog.setMax(100);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                }
                progressDialog.setProgress((int) ((values[0] / (float) values[1]) * 100));
            }
            @Override
            protected void onPostExecute(Integer lResponseCode) {
                progressDialog.hide();
                if ( mHandler != null )
                {
                    if ( mError != null )
                        mHandler.sendMessage(mHandler.obtainMessage(MainActivity.MSG_ERROR, mError));
                    else
                        mHandler.sendMessage(mHandler.obtainMessage(MainActivity.MSG_UPLOAD, lResponseCode));
                }
            }
        };
/**/
        mProgressTask.execute(sUrl, sFileName);

        try {
            return mProgressTask.get();
        }
        catch(InterruptedException e) {
            mError = e;
            return null;
        }
        catch(ExecutionException e) {
            mError = e;
            return null;
        }
/*
        return mResponseCode;
*/
    }
    public List<String> getList(String sUrl, Handler lHandler) {
        mError = null;
        mHandler  = lHandler;

        mResponseCode = 0;
        mList.clear();
        AsyncTask<String, Void, List<String>> mProgressTask = new AsyncTask<String, Void, List<String>>() {
            private Dialog getlistDialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                getlistDialog = ProgressDialog.show(mContext, "Please wait", "Get List...");
            }
            @Override
            protected List<String> doInBackground(String... params) {
                String sUrl = modifyUrl(params[0]);
                try {
                    URL lURL = new URL(sUrl);
                    HttpURLConnection lUrlConnection = (HttpURLConnection) lURL.openConnection();
                    lUrlConnection.setRequestMethod("GET"); // lUrlConnection.setRequestMethod("POST");
                    lUrlConnection.setDoInput(true);
                    lUrlConnection.setDoOutput(true);
                    lUrlConnection.setConnectTimeout(5000);
                    lUrlConnection.setReadTimeout(5000);
                    lUrlConnection.connect();
                    mResponseCode = lUrlConnection.getResponseCode();
                    if(mResponseCode == HttpURLConnection.HTTP_OK){
                        InputStream lInputStream = lUrlConnection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(lInputStream, "UTF-8"), 8);
                        String line = null;
                        while ((line = reader.readLine()) != null)
                            mList.add(line);
                        lInputStream.close();
                    } else {
                        mError = new Exception(String.format("ResponseCode=%d(%s)",
                                mResponseCode, lUrlConnection.getResponseMessage()));
                    }
                    return mList;
                } catch (IOException e) {
                    e.printStackTrace();
                    mError = e;
                }
                mList.clear();
                return mList;
            }
            @Override
            protected void onPostExecute(List<String> lList){
                getlistDialog.dismiss();
                if ( mHandler != null )
                {
                    if ( mError != null )
                        mHandler.sendMessage(mHandler.obtainMessage(MainActivity.MSG_ERROR, mError));
                    else
                        mHandler.sendMessage(mHandler.obtainMessage(MainActivity.MSG_GETLIST, lList));
                }
            }
        };
        mProgressTask.execute(sUrl);

        try {
            return mProgressTask.get();
        }
        catch(InterruptedException e) {
            mError = e;
            mList.clear();
            return mList;
        }
        catch(ExecutionException e) {
            mError = e;
            mList.clear();
            return mList;
        }
    }
    public void LoadSetting(int type)
    {
        if ( mContext==null )
            return;
        SharedPreferences sPref = mContext.getSharedPreferences(MainActivity.PREF_NAME, mContext.MODE_PRIVATE);
        if ( (type&MainActivity.TS_LOGIN) > 0 )  {
            mUserName = sPref.getString(MainActivity.USER_NAME, "sa");
            mPassword = sPref.getString(MainActivity.PASSWORD, "igr");
            mCompany = sPref.getString(MainActivity.COMPANY, "FVJ");
        }
        if ( (type&MainActivity.TS_SETTING) > 0 ) {
            mHostName = sPref.getString(MainActivity.HOST_NAME, "http://10.0.2.2");
            mServerName = sPref.getString(MainActivity.SRV_NAME, ".\\SQLEXPRESS");
            mDataBase = sPref.getString(MainActivity.DB_NAME, "lox");
            mCheckUser = sPref.getBoolean(MainActivity.CHECK_USER, false);
            mCheckCompany = sPref.getBoolean(MainActivity.CHECK_COMPANY, false);
        }
    }
    // Private functions
    private String retriveFileName(String sUrl, URLConnection lUrlConnection, String sFileNameTmp)
    {
        if ( sUrl.lastIndexOf(".pdf") != -1 ) {
            int n = sUrl.lastIndexOf("/");
            return ( n != -1 ) ? sUrl.substring(n+1) : sUrl;
        }
        String sFileName = null;
        String sConnection = lUrlConnection.toString();
        if ( sConnection.lastIndexOf(".pdf") != -1 )
        {
            int n = sConnection.lastIndexOf("/");
            return ( n != -1 ) ? sConnection.substring(n+1) : sConnection;
        }

        String sDisposition = lUrlConnection.getHeaderField("Content-Disposition");
        if (sDisposition != null && sDisposition.indexOf("=") != -1) {
            sFileName = sDisposition.split("=")[1]; // getting value after '='
            return sFileName.replaceAll("\"", "").replaceAll("]", "");
        }

        return sFileNameTmp;
    }
    private String modifyUrl(final String sUrl)
    {
        String sAdd = String.format("servername=%s&database=%s&username=%s&password=%s", mServerName, mDataBase, mUserName, mPassword);
        if (mCheckCompany)
            sAdd += String.format("&company=%s", mCompany);
        return String.format("%s?%s", sUrl, sAdd);
    }
}
/*
    private File getFile(String sUrl, String sFilePath, String sFileNameTmp)
    {
        try {
            URL lURL = new URL(sUrl);
//          if ( lURL.getProtocol() == "https" )    HttpsURLConnection lUrlConnection = (HttpsURLConnection) lURL.openConnection();
//            else                                    HttpURLConnection lUrlConnection = (HttpURLConnection) lURL.openConnection();
            HttpURLConnection lUrlConnection = (HttpURLConnection) lURL.openConnection();
            lUrlConnection.setRequestMethod("GET");//lUrlConnection.setRequestMethod("POST");
            lUrlConnection.setDoInput(true);
            lUrlConnection.connect();
            int responseCode = lUrlConnection.getResponseCode();
            if(responseCode != HttpURLConnection.HTTP_OK) {
                mError = new Exception( String.format("ResponseCode = %d", responseCode) );
                mFile = null;
                return mFile;
            }

            InputStream lInputStream = lUrlConnection.getInputStream();
            mFileName = retriveFileName(lUrlConnection, sFileNameTmp);
            mFile = new File(sFilePath, mFileName);
            if (mFile != null)
                mFile.createNewFile();
            FileOutputStream lOutputStream = new FileOutputStream(mFile);

            int totalSize = lUrlConnection.getContentLength();
            int downloadedSize = 0;
            publishProgress(downloadedSize, totalSize);
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            while( (bufferLength = lInputStream.read(buffer)) > 0) {
                lOutputStream.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                publishProgress(downloadedSize, totalSize);
            }
            lOutputStream.close();
            lInputStream.close();
            return mFile;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            mError = e;
        } catch (IOException e) {
            e.printStackTrace();
            mError = e;
        }
        if (mFile != null) {
            mFile.delete();
            mFile = null;
        }
        return mFile;
    }
*/
/*
    public void downloadFiles(List<String> aUrls, String sFilePath, Handler lHandler) {
        mHandler  = lHandler;
        mError = null;
        mFileName = null;
        mFiles = null;

        final ProgressDialog progressDialog = new ProgressDialog(mContext);

        AsyncTask<List<String>, Integer, File[]> mProgressTask = new AsyncTask<List<String>, Integer, File[]>() {
            @Override
            protected void onPreExecute() {
                progressDialog.setMessage("Downloading...");
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.show();
            }
            @Override
            protected File[] doInBackground(List<String>... params) {
                List<String> aUrls = params[0];
                String sFilePath = params[1].get(0);
                for (int iFile = 0; iFile < aUrls.size(); iFile++) {
                    try {
                        URL lURL = new URL(sUrl);
                        HttpURLConnection lUrlConnection = (HttpURLConnection) lURL.openConnection();
                        lUrlConnection.setRequestMethod("GET");//lUrlConnection.setRequestMethod("POST");
                        lUrlConnection.setDoInput(true);
                        //                    lUrlConnection.setDoOutput(true);
                        lUrlConnection.connect();
                        int responseCode = lUrlConnection.getResponseCode();
                        if (responseCode != HttpURLConnection.HTTP_OK) {
                            mError = new Exception(String.format("ResponseCode = %d", responseCode));
                            mFile = null;
                            continue;
                        }

                        InputStream lInputStream = lUrlConnection.getInputStream();
                        retriveFileName(lUrlConnection, sFileNameTmp);
                        //                    mFile = File.createTempFile(sFilePath, sFileNameTmp);
                        mFile = new File(sFilePath, mFileName);
                        if (mFile != null)
                            mFile.createNewFile();
                        FileOutputStream lOutputStream = new FileOutputStream(mFile);

                        int totalSize = lUrlConnection.getContentLength();
                        int downloadedSize = 0;
                        publishProgress(downloadedSize, totalSize);
                        byte[] buffer = new byte[1024];
                        int bufferLength = 0;
                        while ((bufferLength = lInputStream.read(buffer)) > 0) {
                            lOutputStream.write(buffer, 0, bufferLength);
                            downloadedSize += bufferLength;
                            publishProgress(downloadedSize, totalSize);
                        }
                        lOutputStream.close();
                        lInputStream.close();
                        return mFile;
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        mError = e;
                    } catch (IOException e) {
                        e.printStackTrace();
                        mError = e;
                    }
                    if (mFile != null) {
                        mFile.delete();
                        mFile = null;
                    }
                }
                return mFiles;
            }
            @Override
            protected void onProgressUpdate(Integer... values) {
                if ( values[0] == 0 && mFileName != null)
                    progressDialog.setMessage(String.format("Downloading %s", mFileName));
                progressDialog.setProgress((int) ((values[0] / (float) values[1]) * 100));
            }
            @Override
            protected void onPostExecute(File file) {
                progressDialog.hide();
                if ( mHandler != null )
                {
                    if ( mError != null )
                        mHandler.sendMessage(mHandler.obtainMessage(-1, mError));
                    else
                        mHandler.sendMessage(mHandler.obtainMessage(0, mFile));
                }
            }
        };
        List<String> aFilePath = new ArrayList<String>();
        aFilePath.add(sFilePath);
        mProgressTask.execute(aUrls, aFilePath);
    }
 */
/*
                    lUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
*/
/*
                    DataOutputStream lOutputStream = new DataOutputStream( lUrlConnection.getOutputStream() );
                    String sParams;
*/
/*
                    OutputStream lOutputStream = lUrlConnection.getOutputStream();
                    String sParams = String.format(
                            "servername=%s&database=%s&username=%s&password=%s", sServerName, sDataBase, sUserName, sPassword);
                    BufferedWriter lBufferedWriter = new BufferedWriter(new OutputStreamWriter(lOutputStream, "utf-8"));
                    lBufferedWriter.write(sParams);
*/
/*
                    try {

                        JSONObject obj = new JSONObject();
                        obj.put("servername" , sServerName);
                        obj.put("database" , sDataBase);
                        obj.put("username" , sUserName);
                        obj.put("password" , sPassword);
                        sParams = obj.toString();

                    } catch (JSONException e) {
                        mError = e;
                        return null;
                    }

                    lOutputStream.writeChars(sParams);
                    lOutputStream.flush();
                    lOutputStream.close();
*/
//                    http://localhost/login.php?servername=IGR-PC\SQLEXPRESS&database=lox&username=sa&password=igr&company=FVJ
//                    sUrl = String.format("%s?servername=%s&database=%s&username=%s&password=%s", sUrl, sServerName, sDataBase, sUserName, sPassword);
/*
            @Override
            protected void onPreExecute() {
                progressDialog.setMessage("Downloading...");
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.show();
            }
*/

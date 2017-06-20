package com.igr.pod.work;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

/**
 * Created by igr on 22-Mar-17.
 */

public class ListViewEx extends  ListView
{
    private ArrayAdapter<File> mAdapterView = null;
    private AdapterView.OnItemClickListener mListener = null;

    public ListViewEx(Context context) {
        super(context);
        InitList(context);
    }
    public ListViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        InitList(context);
    }
    public ListViewEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        InitList(context);
    }
// Public functions
    public boolean AddItem(File lFile, boolean bChecked)
    {
        boolean bRet = false;
        if ( lFile == null )
            return bRet;
        int position = mAdapterView.getPosition(lFile);
        if ( position < 0 ) {
            mAdapterView.add(lFile);
            position = mAdapterView.getCount() - 1;
            bRet = true;
        }
        setItemChecked(position, bChecked);
        return bRet;
    }
    public int RemItem(File lFile)
    {
        int position = 0;
        if ( lFile != null ) {
            position = mAdapterView.getPosition(lFile);
            if (position >= 0) {
                setItemChecked(position, false);
                mAdapterView.remove(lFile);
            }
        } else
            mAdapterView.clear();
        return position;
    }
// Private functions
    private void InitList(Context context) {
        mAdapterView = new ArrayAdapter<File>(context, android.R.layout.simple_list_item_checked)
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View lView = super.getView(position, convertView, parent);
                final File lFile = getItem(position);
                ((TextView)lView).setText(lFile.getName());
                return lView;
            }
        };
        setAdapter(mAdapterView);
/*
        mListener = new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        };
        setOnItemClickListener(mListener);
 */
    }
}
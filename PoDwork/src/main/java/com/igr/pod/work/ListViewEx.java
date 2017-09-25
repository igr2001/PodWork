package com.igr.pod.work;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by igr on 22-Mar-17.
 */

public class ListViewEx extends  ListView
{
    public FileArrayAdapter mAdapterView = null;
/*
    // Callback
    private OnListListener mOnListListener = null;
    public interface OnListListener{
        public void OnItemClick(int position);
        public void OnItemLongClick(int position);
        public void OnItemDelete(int position);
    }
    public void setOnListListener(OnListListener listListener){
        mOnListListener = listListener;
    }
*/
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
    public int AddItem(Rec lRec)
    {
        if ( lRec == null )
            return -1;
        int position = mAdapterView.getPosition(lRec);
        if ( position < 0 ) {
            mAdapterView.add(lRec);
            position = mAdapterView.getCount() - 1;
        }
        setItemChecked(position, lRec.mIsSigned);
        return position;
    }
    public int AddItem(File lFile, boolean lIsSigned)
    {
        return AddItem(new Rec(lFile, lIsSigned));
    }
    public void RemItem(int position)
    {
        if ( position < 0 ) {
            mAdapterView.clear();
        } else {
            Rec lRec = mAdapterView.getItem(position);
            mAdapterView.remove(lRec);
            for (int i = position; i < mAdapterView.getCount(); i++)
                setItemChecked(i, mAdapterView.getItem(i).mIsSigned);
        }
    }
    public int getPositionFromString(String lName)
    {
        return mAdapterView.getPositionFromString(lName);
    }
    public int getPositionFromFile(File lFile)
    {
        return mAdapterView.getPositionFromFile(lFile);
    }
    // Private functions
    private void InitList(Context context) {
        mAdapterView = new FileArrayAdapter(context, android.R.layout.simple_list_item_checked)
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View lView = super.getView(position, convertView, parent);
                final Rec lRec = getItem(position);
                ((TextView)lView).setText(lRec.mFile.getName());
                return lView;
            }
        };
        mAdapterView.setNotifyOnChange(true);
        setAdapter(mAdapterView);
/*
        setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String sClassName = parent.getClass().getName();
                ListViewEx mListViewEx = (ListViewEx)parent;
                if ( mListViewEx.mOnListListener!=null )
                    mListViewEx.mOnListListener.OnItemClick(position);
            }
        });
*/
    }
    // FileArrayAdapter class
    public class FileArrayAdapter extends ArrayAdapter<Rec>
    {
        public FileArrayAdapter(Context context, int resource) {
            super(context, resource);
        }
        public FileArrayAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }
        public FileArrayAdapter(Context context, int resource, Rec[] objects) {
            super(context, resource, objects);
        }
        public FileArrayAdapter(Context context, int resource, int textViewResourceId, Rec[] objects) {
            super(context, resource, textViewResourceId, objects);
        }
        public FileArrayAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);
        }
        public FileArrayAdapter(Context context, int resource, int textViewResourceId, List objects) {
            super(context, resource, textViewResourceId, objects);
        }
/*
        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }
        @Override
        public boolean hasStableIds() {
            //return true;
            return  super.hasStableIds();
        }
*/
        public int getPosition(Rec lRec) {
            Rec lRecPos;
            int position;
            for (position=getCount()-1; position>=0; position--) {
                lRecPos = getItem(position);
                if (lRecPos.mFile.equals(lRec.mFile) && lRecPos.mIsSigned == lRec.mIsSigned)
                    break;
            }
            return position;
        }
        public int getPositionFromFile(File lFile ) {
            if (lFile == null)
                return -1;
            Rec lRecPos;
            int position;
            for(position=getCount()-1; position>=0; position--) {
                lRecPos = getItem(position);
                if ( lRecPos.mFile.equals(lFile) )
                    break;
            }
            return position;
        }
        public int getPositionFromString(String lName) {
            if (lName == null)
                return -1;
            return getPositionFromFile(new File(lName));
        }
    }

    // Classes: ListViewEx.Rec ListViewEx.ArrayRec ListViewEx.RecEx ListViewEx.ArrayRecEx
    public static class Rec {
        // Constants
        public final static String mSep = " ";
        // Fields
        public File mFile = null;
        public boolean mIsSigned = false;

        public Rec() {
        }
        public Rec(File lFile) {
            this.mFile = lFile;
        }
        public Rec(File lFile, boolean lIsSigned) {
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
        }
    }
    public static class ArrayRec extends ArrayList<Rec> {
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
                Rec lRec = new Rec();
                lRec.fromString(aStringList.get(i));
                this.add(lRec);
            }
        }
    }
    public static class RecEx extends Rec {
        public String mUserData = null;
        public ArrayRec mScanList = null;

        public RecEx() {
        }
        public RecEx(File lFile) {
            super(lFile);
        }
        public RecEx(File lFile, boolean lIsSigned) {
            super(lFile, lIsSigned);
        }
        public RecEx(File lFile, boolean lIsSigned, String lUserData) {
            super(lFile, lIsSigned);
            mUserData = lUserData;
        }
        public RecEx(File lFile, boolean lIsSigned, String lUserData, ArrayRec lScanList) {
            super(lFile, lIsSigned);
            mUserData = lUserData; mScanList = lScanList;
        }
    }
    public static class ArrayRecEx extends ArrayList<RecEx> {
    }

}

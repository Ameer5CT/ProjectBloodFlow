package com.example.bloodflow.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.bloodflow.DatabaseHandler;
import com.example.bloodflow.FormatCorrector;
import com.example.bloodflow.R;
import com.example.bloodflow.model.SingleDb;
import com.example.bloodflow.model.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DbListAdapter extends BaseAdapter {

    Context c;
    ArrayList<SingleDb> arrayList;
    DatabaseHandler db;

    public DbListAdapter(Context c) {
        this.c = c;
        arrayList = new ArrayList<>();

        db = new DatabaseHandler(c);

        List<Test> testList = db.getAllTests();

        for (Test m : testList) {
            Date d = m.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            arrayList.add(new SingleDb(Integer.toString(m.getBpm()), Integer.toString(m.getSpo2()), new FormatCorrector().formatTime(cal), new FormatCorrector().formatDate(cal)));
        }
    }
    static class ViewHolder {
        private TextView b1;
        private TextView s1;
        private TextView t1;
        private TextView d1;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View row = null;
        if (view == null) {
            ViewHolder mViewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.db_listview, viewGroup, false);
            mViewHolder.b1 = row.findViewById(R.id.dbListBpm);
            mViewHolder.s1 = row.findViewById(R.id.dbListSpo2);
            mViewHolder.t1 = row.findViewById(R.id.dbListTime);
            mViewHolder.d1 = row.findViewById(R.id.dbListDate);
            SingleDb temp_obj2 = arrayList.get(i);
            mViewHolder.b1.setText(temp_obj2.dbBpm);
            mViewHolder.s1.setText(temp_obj2.dbSpo2);
            mViewHolder.t1.setText(temp_obj2.dbTime);
            mViewHolder.d1.setText(temp_obj2.dbDate);
        }
        return row;
    }

    @Override
    public Object getItem(int i) {
        return arrayList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }
}

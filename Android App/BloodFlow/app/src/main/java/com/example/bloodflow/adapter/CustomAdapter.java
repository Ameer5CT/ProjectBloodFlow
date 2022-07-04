package com.example.bloodflow.adapter;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.example.bloodflow.R;
import com.example.bloodflow.model.SingleRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CustomAdapter extends BaseAdapter {

    Context c;
    ArrayList<SingleRow> arrayList;

    public CustomAdapter(Context c) {
        this.c = c;
        arrayList = new ArrayList<>();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { System.out.println("NOT_GRANTED"); }
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        List<String> sNames = new ArrayList<>();
        for(BluetoothDevice bt : pairedDevices) {
            sNames.add(bt.getName());
        }

        List<String> sMacs = new ArrayList<>();
        for(BluetoothDevice bt : pairedDevices) {
            sMacs.add(bt.getAddress());
        }
        String[] names = sNames.toArray(new String[0]);
        String[] macs = sMacs.toArray(new String[0]);

        for (int i=0; i<names.length; i++) {
            arrayList.add(new SingleRow(names[i], macs[i]));
        }
    }
    static class ViewHolder {
        private TextView n1;
        private TextView m1;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View row = null;
        if (view == null) {
            ViewHolder mViewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.custom_listview, viewGroup, false);
            mViewHolder.n1 = row.findViewById(R.id.listDeviceName);
            mViewHolder.m1 = row.findViewById(R.id.listDeviceMAC);
            SingleRow temp_obj = arrayList.get(i);
            mViewHolder.n1.setText(temp_obj.name);
            mViewHolder.m1.setText(temp_obj.mac);
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

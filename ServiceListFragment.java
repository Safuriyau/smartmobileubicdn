package com.example.safur.smartshare;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by safur on 10/06/2017.
 */

public class ServiceListFragment extends ListFragment {

    WiFiDevicesAdapter listAdapter;

    public static String getDeviceStatus(int statusCode) {
        Log.d(TAG, "getDeviceStatus: slf");
        switch (statusCode) {
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: servicelistfragment started");
        return inflater.inflate(R.layout.devices_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated: servicelistfrag");
        super.onActivityCreated(savedInstanceState);
        listAdapter = new WiFiDevicesAdapter(this.getActivity(),
                android.R.layout.simple_list_item_2, android.R.id.text1,
                new ArrayList<WiFiP2pService>());
        setListAdapter(listAdapter);
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "onListItemClick: slf");
        ((DeviceActionListener) getActivity()).connect((WiFiP2pService) l
                .getItemAtPosition(position));
        ((TextView) v.findViewById(android.R.id.text2)).setText("Connecting");

    }

    interface DeviceActionListener {
        public void connect(WiFiP2pService wifiP2pService);
    }

    public class WiFiDevicesAdapter extends ArrayAdapter<WiFiP2pService> {

        private List<WiFiP2pService> items;

        public WiFiDevicesAdapter(Context context, int resource,
                                  int textViewResourceId, List<WiFiP2pService> items) {
            super(context, resource, textViewResourceId, items);
            this.items = items;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "getView: slf");
            View myView = convertView;
            if (myView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getActivity().
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                myView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null);
            }
            WiFiP2pService wifiP2pService = items.get(position);
            if (wifiP2pService != null) {
                TextView nameText = (TextView) myView.findViewById(android.R.id.text1);
                if (nameText != null) {
                    nameText.setText(wifiP2pService.device.deviceName + " - " +
                            wifiP2pService.instanceName);
                }
                TextView statusText = (TextView)
                        myView.findViewById(android.R.id.text2);
                statusText.setText(getDeviceStatus(wifiP2pService.device.status));

            }
            return myView;
        }
    }
}



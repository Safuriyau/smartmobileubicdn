package com.example.safur.smartshare;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }


//    <fragment
//    android:id="@+id/frag_list"
//    class="com.example.safur.smartshare.ServiceListFragment"
//    android:layout_width="wrap_content"
//    android:layout_height="match_parent"
//    tools:layout="@layout/devices_list"
//    tools:layout_editor_absoluteX="8dp"
//    tools:layout_editor_absoluteY="8dp">
//        <!-- Preview: layout=@layout/row_devices -->
//    </fragment>
}

package net.alegen.android.netclip.ui;

import android.app.Fragment;

import android.os.Bundle;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.alegen.android.netclip.R;

public class HelpFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("netclip", "HelpFragment.onCreateView");
        return inflater.inflate(R.layout.help_view, container, false);
    }
}

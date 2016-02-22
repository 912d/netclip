package net.alegen.android.netclip.ui;

import android.app.Fragment;

import android.os.Bundle;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import net.alegen.android.netclip.util.Clipboard;
import net.alegen.android.netclip.R;

public class AboutFragment
    extends Fragment
    implements View.OnClickListener {

    private TextView lblBitcoinAddress;
    private TextView lblLitecoinAddress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("netclip", "AboutFragment.onCreateView");
        View view = inflater.inflate(R.layout.about_view, container, false);

        this.lblBitcoinAddress = (TextView)view.findViewById(R.id.lblBitcoinAddress);
        this.lblLitecoinAddress = (TextView)view.findViewById(R.id.lblLitecoinAddress);
        this.lblBitcoinAddress.setOnClickListener(this);
        this.lblLitecoinAddress.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        String address = "";
        if (v == this.lblBitcoinAddress)
            address = this.getActivity().getString(R.string.bitcoin_address);
        else if (v == this.lblLitecoinAddress)
            address = this.getActivity().getString(R.string.litecoin_address);
        Clipboard.getInstance( this.getActivity() ).setClipboardText(address);
    }
}

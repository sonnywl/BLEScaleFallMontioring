
package com.ble.fragments.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.ble.adapters.BluetoothScanDeviceListAdapter;
import com.ics234.scalefalldetectionble.R;
import com.ics234.scalefalldetectionble.FallService;

public class BluetoothScanDialog extends DialogFragment implements
        OnClickListener,
        OnItemClickListener {
    public final String TAG = BluetoothScanDialog.class.getSimpleName();
    private IBluetoothScanDialogListener mListener;
    private FallService mService;

    private BluetoothServiceHandler mHandler;
    private BluetoothScanDeviceListAdapter mAdapter;

    private Button btnScan;
    private Button btnCancel;
    private View noDeviceText;

    public BluetoothScanDialog(FallService service) {
        mService = service;
        mHandler = new BluetoothServiceHandler();
        mHandler.sendEmptyMessage(BluetoothServiceHandler.START_SCAN);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the
            // host
            mListener = (IBluetoothScanDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement IBluetoothScanDialogListener");
        }
        mAdapter = new BluetoothScanDeviceListAdapter(getActivity());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View v = inflater.inflate(R.layout.activity_bluetooth_devices, null);
        ListView ls = (ListView) v.findViewById(R.id.list_view_test);
        noDeviceText = v.findViewById(R.id.text_view_test);
        btnScan = (Button) v.findViewById(R.id.btn_scan);
        btnCancel = (Button) v.findViewById(R.id.btn_cancel);
        btnScan.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        ls.setOnItemClickListener(this);
        ls.setAdapter(mAdapter);
        builder.setView(v);

        return builder.create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cancel:
                mHandler.sendEmptyMessage(BluetoothServiceHandler.STOP_SCAN);
                break;
            case R.id.btn_scan:
                mHandler.sendEmptyMessage(BluetoothServiceHandler.START_SCAN);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListener.onDialogDeviceClick((BluetoothDevice) mAdapter.getItem(position));
        dismiss();
    }

    class BluetoothServiceHandler extends Handler {
        public static final int START_SCAN = 0;
        public static final int STOP_SCAN = 1;
        public static final int INTERVAL = 1000;
        private int messageState = STOP_SCAN;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_SCAN:
                    messageState = START_SCAN;
                    Log.i(TAG, "Starting Discovery");
                    mService.startLeDiscovery();
                    sendEmptyMessageDelayed(STOP_SCAN, INTERVAL);
                    break;
                case STOP_SCAN:
                    messageState = STOP_SCAN;
                    Log.i(TAG, "Stopping Discovery");
                    mService.stopLeDiscovery();
                    BluetoothDevice[] devices = mService.getDiscoveredDevices();
                    if (devices.length > 0) {
                        noDeviceText.setVisibility(View.GONE);
                    } else {
                        noDeviceText.setVisibility(View.VISIBLE);
                    }
                    mAdapter.updateList(devices);
                    break;
            }
            super.handleMessage(msg);
        }

        public int getMessageState() {
            return messageState;
        }

    }

    public interface IBluetoothScanDialogListener {
        public void onDialogDeviceClick(BluetoothDevice device);
    }
}

package com.mga.mobileguard;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final int VPN_PERMISSION_REQUEST = 1001;
    private static final String TAG = "MGA-MAIN";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void requestVpnPermission() {
        Intent intent = VpnService.prepare(this);

        if (intent == null) {
            Log.i(TAG, "VPN izni zaten verilmiş.");
            return;
        }

        startActivityForResult(intent, VPN_PERMISSION_REQUEST);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_PERMISSION_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "VPN izni kullanıcı tarafından verildi.");
            } else {
                Log.w(TAG, "VPN izni kullanıcı tarafından reddedildi.");
            }
        }
    }
}

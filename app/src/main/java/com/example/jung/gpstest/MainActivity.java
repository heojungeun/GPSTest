package com.example.jung.gpstest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private GpsTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;

    //private Context context;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private TextView contentTxt;
    private TextView sndTxt;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!checkLocationServicesStatus()){
            showDialogForLocationServiceSetting();
        }else{
            checkRunTimePermission();
        }

        // 현 주소를 알려주는 텍스트뷰 등록
        final TextView textview_address = (TextView)findViewById(R.id.textview);

        // 현 주소와, 볼륨 상태를 갱신하는 버튼 등록
        Button ShowLocationButton = (Button)findViewById(R.id.button);

        // 배터리 상태를 알려주는 텍스트뷰 등록, 알아서 바뀜
        contentTxt = (TextView) this.findViewById(R.id.monospaceTxt);
        this.registerReceiver(this.mBatInfoReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        audioManager = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        final int nMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        final int nMaxCall = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        sndTxt = (TextView)this.findViewById(R.id.sndcheckTxt);


        ShowLocationButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0){
                gpsTracker = new GpsTracker(MainActivity.this);

                double latitude = gpsTracker.getLatitude();
                double longitude = gpsTracker.getLongitude();

                //int btryStaus = getBatteryRemain(context);

                String address = getCurrentAddress(latitude, longitude);
                textview_address.setText(address);

                //For Normal mode
                //audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                //snd_mode_check();

                String snd = "현재소리상태:"+ snd_mode_check() + "\nVolume:"+getcrtRingVolume()+"\nmaxVol:"+nMax
                        + "\ncallVol:"+getcrtCallVolume()+"\ncallmax:"+nMaxCall;
                sndTxt.setText(snd);

                Toast.makeText(MainActivity.this, "현재위치\n위도"+latitude+"\n경도"+longitude, Toast.LENGTH_SHORT).show();



            }
        });
    }

    /*
    * ActivityCompat.requestPermission을 사용한 permission 요청 결과를 리턴받는 메소드
    */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                          @NonNull String[] permissions,
                                          @NonNull int[] grandResults)
    {
        //super.onRequestPermissionsResult(permsRequestCode,permissions,grandResults);
        if(permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length){
            // 요청 코드가 퍼미션요청코드이고 요청한 퍼미션 개수만큼 수신되었다면
            boolean check_result = true;

            //모든 퍼미션을 허용했는지 체크
            for(int result:grandResults){
                if(result!= PackageManager.PERMISSION_GRANTED){
                    check_result = false;
                    break;
                }
            }

            if(check_result){
                // 위치 값을 가져올 수 있음
                ;
            }
            else{
                //거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다. 2가지 경우 존재
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])){
                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요",Toast.LENGTH_LONG).show();
                    finish();
                }else{
                    Toast.makeText(MainActivity.this,"퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야합니다.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkRunTimePermission(){
        // 런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarselLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarselLocationPermission == PackageManager.PERMISSION_GRANTED){
            // 2-1. 이미 퍼미션을 가지고 있다면 (android6.0 이하버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된걸로 인식함

            // 3. 위치값을 가져올 수 있음

        }else{
            //2-2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요함. 2가지 경우가 필요하다
            // 3-1 사용자가 퍼미션 거부를 한 적이 있는 경우
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])){

                //3-2 요청을 진행하기 전에 사용자에게 퍼미션이 필요한 이유를 설명해줌
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                //3-3 사용자에게 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신된다
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }else{
                // 4-1 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 한다.
                //요청 결과는 onRequestPermissionResult에서 수신된다
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    public String getCurrentAddress(double latitude, double longitude){
        // Geocoder : gps 를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;

        try{
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7
                    );
        }catch(IOException ioException){
            //network problem
            Toast.makeText(this, "Geocoder service 사용불가", Toast.LENGTH_LONG).show();
            return "Geocoder service 사용불가";

        }catch(IllegalArgumentException illegalArgumentException){
            Toast.makeText(this, "잘못된 gps 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 gps 좌표";
        }

        if(addresses == null || addresses.size() == 0){
            Toast.makeText(this,"주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";
    }

    // GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" +
                "위치 설정을 수정하시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent callGPSSettingIntent
                        = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        switch (requestCode){
            case GPS_ENABLE_REQUEST_CODE:
                // 사용자가 GPS활성 시켰는지 검사
                if(checkLocationServicesStatus()){
                    if(checkLocationServicesStatus()){
                        Log.d("@@@", "onActivityResult: GPS 활성화 되어있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus(){
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(locationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // 배터리 확인 메소드
    public static int getBatteryRemain(Context context) {

        Intent intentBattery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = intentBattery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intentBattery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale;

        return (int)(batteryPct * 100);

    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            int level = intent.getIntExtra("level", 0);
            contentTxt.setText(String.valueOf(level) + "%");
        }
    };

    private String snd_mode_check(){
        String sndmode="";
        switch (audioManager.getRingerMode()){
            case AudioManager.RINGER_MODE_NORMAL:
                sndmode = "Ring";
                break;
            case AudioManager.RINGER_MODE_SILENT:
//                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
//                snd_mode_check();
                sndmode = "SILENT";
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
//                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
//                snd_mode_check();
                sndmode = "VIBRATE";
                break;
        }
        return sndmode;
    }

    //Ring
    private int getcrtRingVolume(){
        int nCurrentVolumn = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        return nCurrentVolumn;
    }

    //STREAM_VOICE_CALL
    private int getcrtCallVolume(){
        int nCurrentVolumn = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        return nCurrentVolumn;
    }

}

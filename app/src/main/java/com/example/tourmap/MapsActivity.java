package com.example.tourmap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AppComponentFactory;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, AutoPermissionsListener {
    Spinner spSeoulTour;
    private GoogleMap mMap;
    String seoulTour[] = {"국립중앙박물관", "남산골한옥마을", "예술의전당", "청계천", "63빌딩",
            "남산타워", "경복궁", "김치문화체험관", "서울올림픽기념관", "국립민속박물관",
            "서대문형무소역사관", "창덕궁"};
    double[] lat = {37.5240867, 37.5591447, 37.4785361, 37.5696512, 37.5198158, 37.5511147, 37.5788408,
            37.5629457, 37.5202976, 37.5815645, 37.5742887, 37.5826041}; // 관광지 위도
    double[] lon = {126.9803881, 126.9936826, 127.0107423, 127.0056375, 126.9403139, 126.9878596, 126.9770162, 126.9851652,
            127.1159236, 126.9789313, 126.9562269, 126.9919376}; // 관광지 경도
    double latlon[] = new double[2]; // 위도 경도
    double mylatlon[] = new double[2]; // 내 위치의 위도 경도
    ArrayAdapter<String> adapter;
    int pos; //position을 담을 값
    LocationManager myLocation; // 위치를 가져온다
    LocationListener myListener; // 상태값을 가져온다
    boolean myCheck = false; // 내 위치가 관광지와 경로가 겹치지 않게 하기 위함


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        AutoPermissions.Companion.loadAllPermissions(this, 101);
        //내 앱에 필요한 퍼미션이 있으면 알아서 다 처리해준다.
        setTitle("서울 관광 안내");
        spSeoulTour = (Spinner) findViewById(R.id.spSeoulTour);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map); // 지도 캐스팅 하는 법
        mapFragment.getMapAsync(this); // onCreate실행 후에 자동으로 초기값을 잡아주는 것
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, seoulTour);
        spSeoulTour.setAdapter(adapter);
        spSeoulTour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                latlon[0] = lat[position];
                latlon[1] = lon[position];
                pos = position;
                myCheck = false;
                moveMap(latlon);
//                mMap.addMarker(new MarkerOptions().position(TourPos).title(seoulTour[position])); // 표시되는 곳
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        AutoPermissions.Companion.parsePermissions(this, requestCode, permissions, this);
    }

    void setMylatlon() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // 내 위치를 찾겠다고 경로를 얻어온다.
        try {
            Location location = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            //마지막 내 위치 정보를 가져와둔다.
            if (location != null) {
                mylatlon[0] = location.getLatitude();//위도 가져온다
                mylatlon[1] = location.getLongitude();//경도 가져온다
            } else {
                showToast("내 위치 찾는 중....");
            }
        } catch (SecurityException e) {
            showToast("위치를 찾을 수 없습니다.");
        }
        GPSListener gpsListener = new GPSListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                10000, 1, gpsListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,1,0,"일반지도");
        menu.add(0,2,0,"위성지도");
        menu.add(0,3,0,"내위치정보");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case 1: // 일반지도
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case 2: // 위성지도
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case 3: // 내위치정보
                myCheck=true;
                moveMap(mylatlon);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) { // 실행하자마자 바로 뜬다. OnMapReadyCallback의 오버라이딩
        mMap = googleMap;
    }
    public void moveMap(double locationLatLon[]) {
        String[] address = {"서울특별시 용산구 서빙고로 137 국립중앙박물관",
        "서울특별시 중구 퇴계로34길 28 남산한옥마을",
        "서울특별시 서초구 남부순환로 2364 국립국악원",
        "서울특별시 종로구 창신동",
        "서울특별시 영등포구 63로 50 한화금융센터_63",
        "서울특별시 용산구 남산공원길 105 N서울타워",
        "서울특별시 종로구 삼청로 37 국립민속박물관",
        "서울특별시 중구 명동2가 32-2",
        "서울특별시 송파구 올림픽로 448 서울올림픽파크텔",
        "서울특별시 종로구 삼청로 37 국립민속박물관",
        "서울특별시 서대문구 통일로 251 독립공원",
        "서울특별시 종로구 율곡로 99"};
        String[] tel = {
            "02-2077-9000",
            "02-2264-4412",
            "02-580-1300",
            "02-2290-6114",
            "02-789-5663",
            "02-3455-9277",
            "02-3700-3900",
            "02-318-7051",
            "02-410-1354",
            "02-3704-3114",
            "02-360-8590",
            "02-762-8261"
        };
        String[] homepage = {"http://www.museum.go.kr",
                "http://hanokmaeul.seoul.go.kr",
                "http://www.sac.or.kr",
                "http://www.cheonggyecheon.or.kr",
                "http://www.63.co.kr",
                "http://www.nseoultower.com",
                "http://www.royalpalace.go.kr",
                "http://www.visitseoul.net/kr/article/article.do?_method=view&art_id=49160&lang=kr&m=0004003002009&p=03",
                "http://www.88olympic.or.kr",
                "http://www.nfm.go.kr",
                "http://www.sscmc.or.kr/culture2",
                "http://www.cdg.go.kr"
        };
        LatLng tourPos = new LatLng(locationLatLon[0], locationLatLon[1]); //위도와 경도를 받아오는 클래스
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tourPos, 15)); // 지도가 이 위치로 이동한다.
        //newLatLngZoom 확대 값을 줄 수 있다.
        MarkerOptions markOpt = new MarkerOptions();// 마크를 맘대로 설정할 수 있다.
        markOpt.position(tourPos);
        if(myCheck == true){ // 관광지와 내 위치를 같은 메소드로 가져오기 때문에 설명을 다르게 주어야 한다.
            markOpt.title("내가 서있는 곳");
            markOpt.snippet("위도 : " + locationLatLon[0] + " 경도 : " +locationLatLon[1]);
        }else{
            markOpt.title(address[pos]); // 마크의 제목
            markOpt.snippet(tel[pos]); // 제목 밑의 설명
        }
        markOpt.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker));// 마크 이미지 바꾸기
        mMap.addMarker(markOpt).showInfoWindow();// 화면에 보여주다
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            // 마크를 클릭하면 해당 홈페이지가 나온다.
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(myCheck==false){
                    Uri uri = Uri.parse(homepage[pos]);
                    Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                    startActivity(intent);
                }
                return false;
            }
        });
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            // 마크의 제목을 클릭하면 전화걸기 화면으로 넘어간다.
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (myCheck == false) {
                    Uri uri = Uri.parse("tel:" + tel[pos]);// "tel:"은 무조건 써줘야 한다.
                    Intent intent = new Intent(Intent.ACTION_DIAL,uri);
                    startActivity(intent);
                }
            }
        });
    }
    //토스트 전용 메소드
    void showToast(String msg){
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDenied(int i, String[] strings) {
        //showTosat("퍼미션을 거부했습니다.");
    }

    @Override
    public void onGranted(int i, String[] strings) {
        showToast("사용 가능");
        setMylatlon();
    }

    public class GPSListener implements LocationListener {// 상태값을 체크하는 것

        @Override
        public void onLocationChanged(@NonNull Location location) {//location이 내 위치정보를 가져온다.
            mylatlon[0] = location.getLatitude();
            mylatlon[1] = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //상태가 변화될 때마다 변화된 정보를 가져온다.
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE: // 경로가 잡히지 않는 곳에 들어갈 때
                    showToast("위치 찾기 불가능지역");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE://신호가 약할 때
                    showToast("위치 찾기 일시 불가능");
                    break;
                case LocationProvider.AVAILABLE:
                    showToast("위치 찾기 서비스 사용 가능");
                    break;
            }
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            showToast("현재 위치 서비스 가능 상태입니다.");
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            showToast("현재 위치 서비스 불가능 상태(켜주세요)");
        }
    }
}
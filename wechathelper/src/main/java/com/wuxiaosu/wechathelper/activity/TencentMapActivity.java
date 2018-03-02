package com.wuxiaosu.wechathelper.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.jaeger.library.StatusBarUtil;
import com.tencent.lbssearch.TencentSearch;
import com.tencent.lbssearch.httpresponse.BaseObject;
import com.tencent.lbssearch.httpresponse.HttpResponseListener;
import com.tencent.lbssearch.object.param.SearchParam;
import com.tencent.lbssearch.object.result.SearchResultObject;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.tencentmap.mapsdk.maps.CameraUpdate;
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.CameraPosition;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.wuxiaosu.wechathelper.R;
import com.wuxiaosu.wechathelper.bean.LocationSearchSuggestions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TencentMapActivity extends AppCompatActivity {

    @BindView(R.id.mapview)
    MapView mMapView;
    @BindView(R.id.floating_search_view)
    FloatingSearchView floatingSearchView;
    @BindView(R.id.fb_location)
    FloatingActionButton fbLocation;

    private TencentMap mTencentMap;

    private TencentLocationListener mLocationListener;
    private TencentLocationManager mLocationManager;

    private boolean isLocation = false;
    private Marker mLocationMarker;
    private Marker mChooseMarker;

    public final static String LAT_KEY = "lat";
    public final static String LON_KEY = "lon";
    public final static int REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tencent_map);
        ButterKnife.bind(this);
        StatusBarUtil.setTranslucentForImageView(this, findViewById(R.id.view_need_offset));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        initMap();
        initLocation();
    }

    private void initMap() {
        mTencentMap = mMapView.getMap();
        Intent intent = getIntent();
        String lat = intent.getStringExtra(LAT_KEY);
        String lon = intent.getStringExtra(LON_KEY);
        LatLng latLng = new LatLng(39.908860, 116.397390); //天安门

        if (!TextUtils.isEmpty(lat) && !TextUtils.isEmpty(lon)) {
            latLng = new LatLng(Double.valueOf(lat), Double.valueOf(lon));
            addMarker(latLng, false, null);
        }
        cameraUpdate(latLng);

        mTencentMap.setOnMapClickListener(new TencentMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                addMarker(latLng, false, null);
            }
        });

        floatingSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                LocationSearchSuggestions suggestions = (LocationSearchSuggestions) searchSuggestion;
                addMarker(suggestions.getLatLng(), false, null);
                cameraUpdate(suggestions.getLatLng());
                floatingSearchView.clearSuggestions();
                floatingSearchView.clearSearchFocus();
            }

            @Override
            public void onSearchAction(String currentQuery) {
            }
        });

        floatingSearchView.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {
                poiSearch(floatingSearchView.getQuery());
            }
        });

        floatingSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            public void onBindSuggestion(View suggestionView,
                                         ImageView leftIcon,
                                         TextView textView,
                                         SearchSuggestion item,
                                         int itemPosition) {

                LocationSearchSuggestions suggestions = (LocationSearchSuggestions) item;
                leftIcon.setImageDrawable(ContextCompat.getDrawable(TencentMapActivity.
                        this, R.drawable.ic_current_location));
                String key = floatingSearchView.getQuery().split(" ")[1];
                String text = suggestions.getTitle()
                        .replaceFirst(key, "<font color=\"#009688\">" + key + "</font>")
                        + "<br/>" + suggestions.getBody();
                textView.setText(Html.fromHtml(text));
            }
        });
    }

    private void poiSearch(String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        final String[] strings = key.trim().split(" ");
        if (strings.length != 2) {
            Toast.makeText(this, "搜索格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        SearchParam param = new SearchParam().keyword(strings[1]).boundary(new SearchParam.Region().poi(strings[0]));
        TencentSearch tencentSearch = new TencentSearch(this);
        tencentSearch.search(param, new HttpResponseListener() {
            @Override
            public void onSuccess(int i, BaseObject baseObject) {
                if (baseObject != null &&
                        ((SearchResultObject) baseObject).data != null &&
                        ((SearchResultObject) baseObject).data.size() != 0) {

                    SearchResultObject obj = (SearchResultObject) baseObject;
                    floatingSearchView.swapSuggestions(searchResultData2SearchSuggestion(obj.data));
                } else {
                    Toast.makeText(TencentMapActivity.this, "在“" +
                            strings[0] + "”搜索不到“" + strings[1] + "”", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int i, String s, Throwable throwable) {
                Toast.makeText(TencentMapActivity.this, "搜索错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<LocationSearchSuggestions> searchResultData2SearchSuggestion(List<SearchResultObject.SearchResultData> data) {
        List<LocationSearchSuggestions> result = new ArrayList<>();
        for (SearchResultObject.SearchResultData datum : data) {
            result.add(new LocationSearchSuggestions(datum.title, datum.address, new LatLng(datum.location.lat, datum.location.lng)));
        }
        return result;
    }

    private void cameraUpdate(LatLng latLng) {
        CameraUpdate cameraSigma =
                CameraUpdateFactory.newCameraPosition(
                        new CameraPosition(latLng, 14, 0, 0));
        mTencentMap.moveCamera(cameraSigma);
    }

    private void initLocation() {
        mLocationManager = TencentLocationManager.getInstance(this);
        mLocationListener = new TencentLocationListener() {
            @Override
            public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
                if (TencentLocation.ERROR_OK == i) {
                    LatLng latLng = new LatLng(tencentLocation.getLatitude(), tencentLocation.getLongitude());
                    addMarker(latLng, isLocation, tencentLocation.getName());
                    cameraUpdate(latLng);
                } else {
                    Toast.makeText(TencentMapActivity.this, "定位失败", Toast.LENGTH_SHORT).show();
                }
                stopLocation();
            }

            @Override
            public void onStatusUpdate(String s, int i, String s1) {
            }
        };
    }

    private void startLocation() {
        isLocation = true;
        int error = mLocationManager.requestLocationUpdates(TencentLocationRequest.create(), mLocationListener);
        if (error != 0) {
            Toast.makeText(TencentMapActivity.this, "定位失败", Toast.LENGTH_SHORT).show();
            isLocation = false;
        }
    }


    private void stopLocation() {
        isLocation = false;
        mLocationManager.removeUpdates(mLocationListener);
    }

    /**
     * 添加标记 并移除旧的
     *
     * @param latLng
     * @param isLocationMarker
     * @return
     */
    private void addMarker(LatLng latLng, boolean isLocationMarker, String name) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromResource(isLocationMarker
                ? R.drawable.ic_location_on : R.drawable.ic_current_location));
        markerOptions.position(latLng);
        markerOptions.snippet(latLng.latitude + "," + latLng.longitude);
        if (isLocationMarker) {
            markerOptions.title(TextUtils.isEmpty(name) ? "未知" : name);
        }
        Marker marker = mTencentMap.addMarker(markerOptions);
        if (isLocationMarker) {
            if (mLocationMarker != null) {
                mLocationMarker.remove();
            }
            mLocationMarker = marker;
        } else {
            if (mChooseMarker != null) {
                mChooseMarker.remove();
            }
            mChooseMarker = marker;
        }
    }

    @OnClick(R.id.fb_location)
    public void onLocation(View view) {
        if (!isLocation) {
            startLocation();
        }
    }

    @OnClick(R.id.fb_done)
    public void chooseDone(View view) {
        if (mChooseMarker != null) {
            Intent result = new Intent();
            result.putExtra(LAT_KEY, String.valueOf(mChooseMarker.getPosition().latitude));
            result.putExtra(LON_KEY, String.valueOf(mChooseMarker.getPosition().longitude));
            setResult(RESULT_OK, result);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onStop() {
        mMapView.onStop();
        super.onStop();
    }

    public static void actionStart(Activity context,
                                   String lat, String lon) {
        Intent intent = new Intent(context, TencentMapActivity.class);
        intent.putExtra(TencentMapActivity.LAT_KEY, lat);
        intent.putExtra(TencentMapActivity.LON_KEY, lon);
        context.startActivityForResult(intent, REQUEST_CODE);
    }
}

package com.wuxiaosu.wechathelper.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.CameraPosition;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.CameraUpdate;
import com.tencent.tencentmap.mapsdk.map.CameraUpdateFactory;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;
import com.wuxiaosu.wechathelper.R;
import com.wuxiaosu.wechathelper.bean.LocationSearchSuggestions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TencentMapLiteActivity extends AppCompatActivity {

    @BindView(R.id.mapview)
    MapView mMapView;
    @BindView(R.id.floating_search_view)
    FloatingSearchView floatingSearchView;

    private TencentMap mTencentMap;

    private Marker mChooseMarker;

    public final static String LAT_KEY = "lat";
    public final static String LON_KEY = "lon";
    public final static int REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tencent_map_lite);
        ButterKnife.bind(this);
        StatusBarUtil.setTranslucentForImageView(this, findViewById(R.id.view_need_offset));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        initMap();
    }

    private void initMap() {
        mTencentMap = mMapView.getMap();
        Intent intent = getIntent();
        String lat = intent.getStringExtra(LAT_KEY);
        String lon = intent.getStringExtra(LON_KEY);
        LatLng latLng = new LatLng(39.908860, 116.397390); //天安门

        if (!TextUtils.isEmpty(lat) && !TextUtils.isEmpty(lon)) {
            latLng = new LatLng(Double.valueOf(lat), Double.valueOf(lon));
            addMarker(latLng, null);
        }
        cameraUpdate(latLng);

        mTencentMap.setOnMapClickListener(new TencentMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                addMarker(latLng, null);
            }
        });

        floatingSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                LocationSearchSuggestions suggestions = (LocationSearchSuggestions) searchSuggestion;
                addMarker(suggestions.getLatLng(), suggestions.getTitle());
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
                leftIcon.setImageDrawable(ContextCompat.getDrawable(TencentMapLiteActivity.
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
                    Toast.makeText(TencentMapLiteActivity.this, "在“" +
                            strings[0] + "”搜索不到“" + strings[1] + "”", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int i, String s, Throwable throwable) {
                Toast.makeText(TencentMapLiteActivity.this, "搜索错误", Toast.LENGTH_SHORT).show();
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
                        new CameraPosition(latLng, 14));
        mTencentMap.moveCamera(cameraSigma);
    }

    /**
     * 添加标记 并移除旧的
     *
     * @param latLng
     * @return
     */
    private void addMarker(LatLng latLng, String name) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_location));
        markerOptions.position(latLng);
        markerOptions.snippet(latLng.getLatitude() + "," + latLng.getLongitude());
        markerOptions.title(TextUtils.isEmpty(name) ? "未知" : name);
        Marker marker = mTencentMap.addMarker(markerOptions);

        if (mChooseMarker != null) {
            mChooseMarker.remove();
        }
        mChooseMarker = marker;
    }

    @OnClick(R.id.fb_done)
    public void chooseDone(View view) {
        if (mChooseMarker != null) {
            Intent result = new Intent();
            result.putExtra(LAT_KEY, String.valueOf(mChooseMarker.getPosition().getLatitude()));
            result.putExtra(LON_KEY, String.valueOf(mChooseMarker.getPosition().getLongitude()));
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
        Intent intent = new Intent(context, TencentMapLiteActivity.class);
        intent.putExtra(TencentMapLiteActivity.LAT_KEY, lat);
        intent.putExtra(TencentMapLiteActivity.LON_KEY, lon);
        context.startActivityForResult(intent, REQUEST_CODE);
    }
}
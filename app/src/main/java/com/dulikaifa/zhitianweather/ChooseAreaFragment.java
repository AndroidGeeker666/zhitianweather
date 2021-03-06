package com.dulikaifa.zhitianweather;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dulikaifa.zhitianweather.db.City;
import com.dulikaifa.zhitianweather.db.County;
import com.dulikaifa.zhitianweather.db.Province;
import com.dulikaifa.zhitianweather.http.JsonRequestCallback;
import com.dulikaifa.zhitianweather.http.NetStatusUtil;
import com.dulikaifa.zhitianweather.http.OkHttpUtil;
import com.dulikaifa.zhitianweather.http.Url;
import com.dulikaifa.zhitianweather.util.HandleJsonUtil;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Author:李晓峰 on 2017/4/22 22:16
 * E-mail:chaate@163.com
 * Copyright(c)2017,All rights reserved.
 * Usage :
 */

public class ChooseAreaFragment extends Fragment {

    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    List<String> dataList = new ArrayList<>();

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;
    private static final String COUNTRY="中国";
    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;
    private TextView setting;
    private SweetAlertDialog pDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.select_city);
        backButton = (Button) view.findViewById(R.id.btn_back4);
        setting = (TextView) view.findViewById(R.id.setting);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initLstener();
        queryAndShowProvinces();
    }

    private void initLstener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryAndShowCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryAndShowCounties();
                } else if (currentLevel == LEVEL_COUNTY) {
                    String fragmentCountyName = countyList.get(position).getCountyName();
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("fragmentCountyName", fragmentCountyName);
                        intent.putExtra("COUNTRY", COUNTRY);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.requesWeather(fragmentCountyName, COUNTRY);

                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryAndShowCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryAndShowProvinces();
                }
            }
        });

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SettingActivity.class);
                getActivity().startActivity(intent);

            }
        });
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询，并将结果显示出来
     */
    private void queryAndShowProvinces() {
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SettingActivity.class);
                getActivity().startActivity(intent);
            }
        });
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0 && provinceList != null) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }

            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;

        } else {
            queryFromServer(Url.PROVINCE_URL, "province");
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询,并将结果显示出来
     */
    private void queryAndShowCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid=?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String cityUrl = Url.PROVINCE_URL + provinceCode;
            queryFromServer(cityUrl, "city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询,并将结果显示出来
     */
    private void queryAndShowCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid=?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String countyUrl = Url.PROVINCE_URL + provinceCode + "/" + cityCode;
            queryFromServer(countyUrl, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据。
     */
    private void queryFromServer(String url, final String type) {
        if (NetStatusUtil.isNetworkAvailable(getActivity())) {
            pDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.PROGRESS_TYPE);
            pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            pDialog.setTitleText("加载中...");
            pDialog.setCancelable(false);
            pDialog.show();
            OkHttpUtil.getInstance().getAsync(url, new JsonRequestCallback() {
                @Override
                public void onRequestSucess(String result) {

                    boolean isHandleSuccess = false;
                    if ("province".equals(type)) {
                        isHandleSuccess = HandleJsonUtil.handleProvinceResponse(result);
                    } else if ("city".equals(type)) {
                        isHandleSuccess = HandleJsonUtil.handleCityResponse(result, selectedProvince.getId());
                    } else if ("county".equals(type)) {
                        isHandleSuccess = HandleJsonUtil.handleCountyResponse(result, selectedCity.getId());
                    }
                    if (isHandleSuccess) {
                        if ("province".equals(type)) {
                            queryAndShowProvinces();
                        } else if ("city".equals(type)) {
                            queryAndShowCities();
                        } else if ("county".equals(type)) {
                            queryAndShowCounties();
                        }
                    }
                    pDialog.dismiss();
                }

                @Override
                public void onRequestFailure(String result) {
                    pDialog.dismiss();
                    Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            SweetAlertDialog pDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("网络未连接")
                    .setContentText("请检查网络设置")
                    .setConfirmText("我知道了,去设置网络")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                            sweetAlertDialog.dismissWithAnimation();
                        }
                    });
            pDialog.setCancelable(false);
            pDialog.show();
            setting.setText("刷新");
            setting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.startLocation();
                    queryAndShowProvinces();
                }
            });
        }
    }
}

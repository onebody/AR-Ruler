package com.phonecl.armeasure.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.phonecl.armeasure.settings.model.FeedbackRequest;
import com.phonecl.armeasure.settings.model.PrivacyInfo;
import com.phonecl.armeasure.settings.model.VersionInfo;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SettingsRepository {
    private static final String TAG = "SettingsRepository";
    private static final String BASE_URL = "https://api.example.com";
    private static final String PREFS_NAME = "ARMeasureSettings";
    private static final String KEY_PRIVACY_AGREED = "privacy_agreed";
    private static final String KEY_VERSION_CACHE = "version_cache";
    private static final String KEY_PRIVACY_CACHE = "privacy_cache";

    private ApiService apiService;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public SettingsRepository(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public VersionInfo fetchVersionInfo(String currentVersion) {
        try {
            Call<VersionInfo> call = apiService.checkVersion(currentVersion);
            Response<VersionInfo> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                String cache = gson.toJson(response.body());
                sharedPreferences.edit().putString(KEY_VERSION_CACHE, cache).apply();
                return response.body();
            }
        } catch (IOException e) {
            Log.e(TAG, "fetchVersionInfo failed: " + e.getMessage());
        }
        return getCachedVersionInfo();
    }

    public boolean sendFeedback(FeedbackRequest request) {
        try {
            Call<ApiService.BaseResponse> call = apiService.submitFeedback(request);
            Response<ApiService.BaseResponse> response = call.execute();
            return response.isSuccessful() && response.body() != null && response.body().isSuccess();
        } catch (IOException e) {
            Log.e(TAG, "sendFeedback failed: " + e.getMessage());
            return false;
        }
    }

    public PrivacyInfo fetchPrivacyInfo() {
        try {
            Call<PrivacyInfo> call = apiService.getPrivacyInfo();
            Response<PrivacyInfo> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                String cache = gson.toJson(response.body());
                sharedPreferences.edit().putString(KEY_PRIVACY_CACHE, cache).apply();
                return response.body();
            }
        } catch (IOException e) {
            Log.e(TAG, "fetchPrivacyInfo failed: " + e.getMessage());
        }
        return getCachedPrivacyInfo();
    }

    public void savePrivacyAgreement(boolean agreed) {
        sharedPreferences.edit().putBoolean(KEY_PRIVACY_AGREED, agreed).apply();
    }

    public boolean getPrivacyAgreement() {
        return sharedPreferences.getBoolean(KEY_PRIVACY_AGREED, true);
    }

    private VersionInfo getCachedVersionInfo() {
        String cache = sharedPreferences.getString(KEY_VERSION_CACHE, null);
        if (cache != null) {
            try {
                return gson.fromJson(cache, VersionInfo.class);
            } catch (Exception e) {
                Log.e(TAG, "parse cached version info failed: " + e.getMessage());
            }
        }
        return null;
    }

    private PrivacyInfo getCachedPrivacyInfo() {
        String cache = sharedPreferences.getString(KEY_PRIVACY_CACHE, null);
        if (cache != null) {
            try {
                return gson.fromJson(cache, PrivacyInfo.class);
            } catch (Exception e) {
                Log.e(TAG, "parse cached privacy info failed: " + e.getMessage());
            }
        }
        return getDefaultPrivacyInfo();
    }

    private PrivacyInfo getDefaultPrivacyInfo() {
        return new PrivacyInfo();
    }
}
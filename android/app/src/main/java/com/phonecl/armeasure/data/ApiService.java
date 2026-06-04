package com.phonecl.armeasure.data;

import com.phonecl.armeasure.settings.model.FeedbackRequest;
import com.phonecl.armeasure.settings.model.PrivacyInfo;
import com.phonecl.armeasure.settings.model.VersionInfo;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @GET("/api/version/check")
    Call<VersionInfo> checkVersion(@Query("currentVersion") String currentVersion);

    @POST("/api/feedback/submit")
    Call<BaseResponse> submitFeedback(@Body FeedbackRequest request);

    @GET("/api/privacy/info")
    Call<PrivacyInfo> getPrivacyInfo();

    class BaseResponse {
        private boolean success;
        private String message;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
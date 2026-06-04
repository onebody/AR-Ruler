package com.phonecl.armeasure.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.phonecl.armeasure.R;

public class FeedbackActivity extends AppCompatActivity {

    private SettingsPresenter presenter;
    private String selectedType = "experience";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        presenter = new SettingsPresenter(this);
        presenter.attachView(new FeedbackView());

        initViews();
    }

    private void initViews() {
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        Button btnExperience = findViewById(R.id.btn_experience);
        Button btnFeature = findViewById(R.id.btn_feature);
        Button btnOther = findViewById(R.id.btn_other);

        btnExperience.setOnClickListener(v -> selectType("experience"));
        btnFeature.setOnClickListener(v -> selectType("feature"));
        btnOther.setOnClickListener(v -> selectType("other"));

        Button btnSubmit = findViewById(R.id.btn_submit);
        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void selectType(String type) {
        selectedType = type;
        Button btnExperience = findViewById(R.id.btn_experience);
        Button btnFeature = findViewById(R.id.btn_feature);
        Button btnOther = findViewById(R.id.btn_other);

        btnExperience.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnFeature.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnOther.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        switch (type) {
            case "experience":
                btnExperience.setBackgroundColor(getResources().getColor(R.color.orange));
                break;
            case "feature":
                btnFeature.setBackgroundColor(getResources().getColor(R.color.blue));
                break;
            case "other":
                btnOther.setBackgroundColor(getResources().getColor(R.color.gray));
                break;
        }
    }

    private void submitFeedback() {
        EditText etContent = findViewById(R.id.et_content);
        EditText etContact = findViewById(R.id.et_contact);

        String content = etContent.getText().toString().trim();
        String contact = etContact.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "请输入反馈内容", Toast.LENGTH_SHORT).show();
            return;
        }

        presenter.submitFeedback(selectedType, content, contact);
    }

    private class FeedbackView implements SettingsContract.View {
        @Override
        public void showLoading() {
            Toast.makeText(FeedbackActivity.this, "提交中...", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void hideLoading() {
        }

        @Override
        public void showVersionInfo(com.phonecl.armeasure.settings.model.VersionInfo versionInfo) {
        }

        @Override
        public void showUpdateAvailable(com.phonecl.armeasure.settings.model.VersionInfo versionInfo) {
        }

        @Override
        public void showNoUpdate() {
        }

        @Override
        public void showFeedbackSuccess() {
            Toast.makeText(FeedbackActivity.this, "反馈提交成功，感谢您的意见！", Toast.LENGTH_SHORT).show();
            finish();
        }

        @Override
        public void showFeedbackFailed() {
            Toast.makeText(FeedbackActivity.this, "反馈提交失败，请稍后重试", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void showPrivacyInfo(com.phonecl.armeasure.settings.model.PrivacyInfo privacyInfo) {
        }

        @Override
        public void showPrivacyRevoked() {
        }

        @Override
        public void showError(String message) {
            Toast.makeText(FeedbackActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView();
    }
}
package com.phonecl.armeasure.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.phonecl.armeasure.R;
import com.phonecl.armeasure.settings.model.InfoItem;
import com.phonecl.armeasure.settings.model.PrivacyInfo;
import com.phonecl.armeasure.settings.model.VersionInfo;

public class SettingsActivity extends AppCompatActivity implements SettingsContract.View {

    private SettingsPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        presenter = new SettingsPresenter(this);
        presenter.attachView(this);

        initViews();
    }

    private void initViews() {
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        LinearLayout llCheckUpdate = findViewById(R.id.ll_check_update);
        llCheckUpdate.setOnClickListener(v -> presenter.checkVersion());

        LinearLayout llFeedback = findViewById(R.id.ll_feedback);
        llFeedback.setOnClickListener(v -> showFeedbackDialog());

        LinearLayout llContact = findViewById(R.id.ll_contact);
        llContact.setOnClickListener(v -> showContactDialog());

        LinearLayout llAbout = findViewById(R.id.ll_about);
        llAbout.setOnClickListener(v -> showAboutDialog());

        LinearLayout llPrivacyList = findViewById(R.id.ll_privacy_list);
        llPrivacyList.setOnClickListener(v -> presenter.getPrivacyInfo());

        LinearLayout llThirdParty = findViewById(R.id.ll_third_party);
        llThirdParty.setOnClickListener(v -> showThirdPartyDialog());

        LinearLayout llRevokePrivacy = findViewById(R.id.ll_revoke_privacy);
        llRevokePrivacy.setOnClickListener(v -> showRevokeConfirmDialog());
    }

    private void showFeedbackDialog() {
        Intent intent = new Intent(this, FeedbackActivity.class);
        startActivity(intent);
    }

    private void showContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("联系客服");
        builder.setMessage("客服电话：400-123-4567\n客服邮箱：support@armeasure.com");
        builder.setPositiveButton("拨打电话", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:4001234567"));
            startActivity(intent);
        });
        builder.setNegativeButton("发送邮件", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@armeasure.com"));
            startActivity(intent);
        });
        builder.setNeutralButton("关闭", null);
        builder.show();
    }

    private void showAboutDialog() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("关于我们");
            builder.setMessage(
                "AR测量尺 v" + versionName + "\n\n" +
                "一款基于AR技术的精准测量工具，支持直线距离测量和垂直高度测量。\n\n" +
                "开发者：PhoneCL Team\n" +
                "版权所有 © 2024"
            );
            builder.setPositiveButton("确定", null);
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showThirdPartyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("第三方信息共享清单");
        builder.setMessage(
            "Google ARCore\n" +
            "共享信息：设备传感器数据、相机画面特征点\n" +
            "共享目的：用于AR空间定位和测量计算\n\n" +
            "所有数据仅在本地处理，不上传云端"
        );
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    private void showRevokeConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("撤销隐私政策");
        builder.setMessage("撤销后将影响部分功能使用，确定要撤销吗？");
        builder.setPositiveButton("确定", (dialog, which) -> {
            presenter.revokePrivacyAgreement();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    @Override
    public void showLoading() {
        Toast.makeText(this, "加载中...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void showVersionInfo(VersionInfo versionInfo) {
    }

    @Override
    public void showUpdateAvailable(VersionInfo versionInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("发现新版本 v" + versionInfo.getVersionName());
        builder.setMessage(versionInfo.getUpdateContent());
        builder.setPositiveButton("立即更新", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(versionInfo.getDownloadUrl()));
            startActivity(intent);
        });
        builder.setNegativeButton("稍后更新", null);
        builder.show();
    }

    @Override
    public void showNoUpdate() {
        Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showFeedbackSuccess() {
        Toast.makeText(this, "反馈提交成功，感谢您的意见！", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showFeedbackFailed() {
        Toast.makeText(this, "反馈提交失败，请稍后重试", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showPrivacyInfo(PrivacyInfo privacyInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("个人信息收集清单\n\n");
        if (privacyInfo.getPersonalInfoList() != null) {
            for (int i = 0; i < privacyInfo.getPersonalInfoList().size(); i++) {
                InfoItem item = privacyInfo.getPersonalInfoList().get(i);
                sb.append(i + 1).append(". ").append(item.getName()).append("\n");
                sb.append("   用途：").append(item.getPurpose()).append("\n");
                sb.append("   存储期限：").append(item.getStoragePeriod()).append("\n\n");
            }
        } else {
            sb.append("相机权限\n   用途：用于AR测量功能，采集画面进行空间建模\n   存储期限：仅在使用时临时存储\n\n");
            sb.append("存储权限\n   用途：用于保存测量截图和数据\n   存储期限：用户主动删除前永久保存\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("个人信息收集清单");
        builder.setMessage(sb.toString());
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    @Override
    public void showPrivacyRevoked() {
        Toast.makeText(this, "已撤销隐私政策同意", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView();
    }
}
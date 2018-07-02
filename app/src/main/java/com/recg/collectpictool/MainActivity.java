package com.recg.collectpictool;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.recg.collectpictool.listener.GlidePauseOnScrollListener;
import com.recg.collectpictool.utils.GlideImageLoader;

import java.io.File;
import java.util.List;

import cn.finalteam.galleryfinal.CoreConfig;
import cn.finalteam.galleryfinal.FunctionConfig;
import cn.finalteam.galleryfinal.GalleryFinal;
import cn.finalteam.galleryfinal.PauseOnScrollListener;
import cn.finalteam.galleryfinal.ThemeConfig;
import cn.finalteam.galleryfinal.model.PhotoInfo;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private final int REQUEST_CODE_CAMERA = 1000;
    private final int REQUEST_CODE_GALLERY = 1001;
    private final int REQUEST_CODE_CROP = 1002;
    private final int REQUEST_CODE_EDIT = 1003;

    Button btChinese;
    Button btMath;
    Button btEnglish;
    Button btPolitics;
    Button btHistory;
    Button btPhysics;
    Button btChemistry;
    Button btGeography;
    Spinner spGrade;

    FunctionConfig functionConfig;
    FunctionConfig.Builder functionConfigBuilder;
    cn.finalteam.galleryfinal.ImageLoader imageLoader;
    ThemeConfig themeConfig;
    PauseOnScrollListener pauseOnScrollListener = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initTheme();

        initView();
        initData();
    }

    private void initTheme() {
        //公共配置都可以在application中配置，这里只是为了代码演示而写在此处
        themeConfig = ThemeConfig.DEFAULT;

        functionConfigBuilder = new FunctionConfig.Builder();
        imageLoader = new GlideImageLoader();
        pauseOnScrollListener = new GlidePauseOnScrollListener(false, true);

        boolean muti = false;
        muti = true;
        int maxSize = 9;
        functionConfigBuilder.setMutiSelectMaxSize(maxSize);

        final boolean mutiSelect = muti;
        functionConfigBuilder.setEnableEdit(true);

        functionConfigBuilder.setEnableCrop(true);
        int width = 800;
        functionConfigBuilder.setCropWidth(width);
        int height = 1000;
        functionConfigBuilder.setCropHeight(height);
        functionConfigBuilder.setCropSquare(false);
        functionConfigBuilder.setCropReplaceSource(false);
        functionConfigBuilder.setForceCrop(false);
        functionConfigBuilder.setForceCropEdit(false);
        functionConfigBuilder.setEnableCamera(true);
        functionConfigBuilder.setEnablePreview(false);

        functionConfig = functionConfigBuilder.build();
    }

    private void initView() {
        btChinese = (Button) findViewById(R.id.bt_chinese);
        btMath = (Button) findViewById(R.id.bt_math);
        btEnglish = (Button) findViewById(R.id.bt_english);
        btPolitics = (Button) findViewById(R.id.bt_politics);
        btHistory = (Button) findViewById(R.id.bt_history);
        btPhysics = (Button) findViewById(R.id.bt_physics);
        btChemistry = (Button) findViewById(R.id.bt_chemistry);
        btGeography = (Button) findViewById(R.id.bt_geography);
        spGrade = (Spinner) findViewById(R.id.sp_grade);
    }

    private void initData() {
        btChinese.setOnClickListener(this);
        btMath.setOnClickListener(this);
        btEnglish.setOnClickListener(this);
        btPolitics.setOnClickListener(this);
        btHistory.setOnClickListener(this);
        btPhysics.setOnClickListener(this);
        btChemistry.setOnClickListener(this);
        btGeography.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String subject = "chinese";
        int grade = spGrade.getSelectedItemPosition() + 1;
        switch (view.getId()){
            case R.id.bt_chinese:
                takePic(subject,grade);
                break;
            case R.id.bt_math:
                subject = "math";
                takePic(subject, grade);
                break;
            case R.id.bt_english:
                subject = "english";
                takePic(subject, grade);
                break;
            case R.id.bt_politics:
                subject = "politics";
                takePic(subject, grade);
                break;
            case R.id.bt_history:
                subject = "history";
                takePic(subject, grade);
                break;
            case R.id.bt_physics:
                subject = "physics";
                takePic(subject, grade);
                break;
            case R.id.bt_chemistry:
                subject = "chemistry";
                takePic(subject, grade);
                break;
            case R.id.bt_geography:
                subject = "geography";
                takePic(subject, grade);
                break;
        }
    }

    private void takePic(String subject, int grade) {
        File takePhotoFolder = new File(Environment.getExternalStorageDirectory(), "/DCIM/" +subject+ "/");
        CoreConfig coreConfig = new CoreConfig.Builder(MainActivity.this, imageLoader, themeConfig)
                .setFunctionConfig(functionConfig)
                .setPauseOnScrollListener(pauseOnScrollListener)
                .setTakePhotoFolder(takePhotoFolder)
                .setEditPhotoCacheFolder(takePhotoFolder)
                .setNoAnimcation(false)
                .build();
        coreConfig.setGrade(""+grade);
        coreConfig.setSubject(subject);
        GalleryFinal.init(coreConfig);
        GalleryFinal.openGalleryMuti(REQUEST_CODE_GALLERY, functionConfig, mOnHanlderResultCallback);
    }

    private GalleryFinal.OnHanlderResultCallback mOnHanlderResultCallback = new GalleryFinal.OnHanlderResultCallback() {
        @Override
        public void onHanlderSuccess(int reqeustCode, List<PhotoInfo> resultList) {
            if (resultList != null) {

            }
        }

        @Override
        public void onHanlderFailure(int requestCode, String errorMsg) {
            Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    };
}

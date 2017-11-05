package com.example.xdf.expressionsubtitle;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.unisound.client.SpeechConstants;
import com.unisound.client.SpeechUnderstander;
import com.unisound.client.SpeechUnderstanderListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import android.os.Environment;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private SpeechUnderstander mUnderstander;
    private ImageButton beginListen_bt;
    private boolean beginListen = false;

    private ImageButton addExpression_bt;
    private boolean addExpression = false;

    private Button output_bt;
    private boolean outputToFile = false;
    private File file;
    private String path;

    private boolean suspendWindow = false;
    private Button suspend_bt;
    private Button suspendedView;
    private Toast reminders;
    private WindowManager wm;
    private WindowManager.LayoutParams layoutParams;

    private TextView tv;//提示信息文本框



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        //WindowManager.LayoutParams.TYPE_SYSTEM_ALERT |
        layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        suspendedView = new Button(this);
        suspendedView.setText("弹幕~~~~~~~~~~~~~~~~~~~~~");
        suspendedView.setGravity(Gravity.CENTER);
        suspendedView.setTextColor(Color.argb(200, 250, 250,250));
        suspendedView.setTextSize(20);
        suspendedView.setBackgroundColor(Color.argb(100,  0, 0,0));
        suspendedView.setOnTouchListener(this);

        layoutParams.gravity = Gravity.LEFT|Gravity.BOTTOM;
        wm.addView(suspendedView, layoutParams);
        wm.removeView(suspendedView);

        suspend_bt = (Button) findViewById(R.id.suspend_bt);

        suspend_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!suspendWindow) {
                    suspendWindow = true;
                    reminders = Toast.makeText(getApplicationContext(),"悬浮窗已开启",Toast.LENGTH_SHORT);
//                    reminders.setText("开启悬浮窗");
                    if((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0){
                        finish();
                        return;
                    }
                    wm.addView(suspendedView, layoutParams);
                    layoutParams.gravity = Gravity.LEFT|Gravity.BOTTOM;
                    wm.updateViewLayout(suspendedView,layoutParams);
                    reminders.show();
                } else {
                    suspendWindow = false;
                    reminders = Toast.makeText(getApplicationContext(),"悬浮窗已关闭",Toast.LENGTH_SHORT);
                    wm.removeView(suspendedView);
                    reminders.show();
                }
            }
        });

        //tv.setText(getFilePath(getApplicationContext()));

        output_bt = (Button) findViewById(R.id.button3);
        output_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (outputToFile) {
                    reminders = Toast.makeText(getApplicationContext(),"输出到文件停止",Toast.LENGTH_SHORT);
                    reminders.show();
                    outputToFile = false;
                }
                else {
                    reminders = Toast.makeText(getApplicationContext(),"输出到文件开始",Toast.LENGTH_SHORT);
                    reminders.show();
                    outputToFile = true;
                }
            }
        });

        tv = (TextView) findViewById(R.id.textView2);


        beginListen_bt = (ImageButton) findViewById(R.id.imageIns);
        beginListen_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!beginListen) {
                    beginListen = true;
                    reminders = Toast.makeText(getApplicationContext(),"监听已经开始",Toast.LENGTH_SHORT);
                    reminders.show();
                    beginListen();
                } else {
                    beginListen = false;
                    reminders = Toast.makeText(getApplicationContext(),"监听已经关闭",Toast.LENGTH_SHORT);
                    reminders.show();
                    cancelListen();
                }
            }
        });

        addExpression_bt = (ImageButton) findViewById(R.id.addExp_bt);
        addExpression_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (addExpression) {
                    addExpression = false;
                    addExpression_bt.setImageDrawable(getResources().getDrawable(R.drawable.unclicked));
                    reminders = Toast.makeText(getApplicationContext(),"添加颜文字功能已关闭",Toast.LENGTH_SHORT);
                    reminders.show();
                } else {
                    addExpression = true;
                    addExpression_bt.setImageDrawable(getResources().getDrawable(R.drawable.icon1));
                    reminders = Toast.makeText(getApplicationContext(),"添加颜文字功能已经开启",Toast.LENGTH_SHORT);
                    reminders.show();
                }
            }
        });
        //先用一个文本框和按钮代替语音输入
    }



    protected void beginListen() {
        mUnderstander = new SpeechUnderstander(this, Config.appKey, Config.secret);
        mUnderstander.setOption(SpeechConstants.ASR_SERVICE_MODE, SpeechConstants.ASR_SERVICE_MODE_NET);
        mUnderstander.setListener(new SpeechUnderstanderListener() {
            public void onResult(int type, String jsonResult) {
                switch (type) {
                    case SpeechConstants.ASR_RESULT_NET:
                        try {
                            Log.i("json" , jsonResult);
                            JSONObject result = new JSONObject(jsonResult);

                            String net_asr = result.get("net_asr").toString();

                            JSONArray jsonArray = new JSONArray(net_asr);
                            JSONObject child = new JSONObject(jsonArray.getString(0));

                            String logStr = child.get("recognition_result").toString();

                            Log.i("result" , logStr);
                            tv.setText(logStr);

                            //这里输出到悬浮窗
                            outputToSuspendedWindow(logStr);
                            //输入到文件夹
                            if (outputToFile) {
                                outToFile(logStr);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }

            public void onEvent(int type, int timeMs) {
                if (type ==  SpeechConstants.ASR_EVENT_RECORDING_STOP){
                    beginListen();
                }
            }

            public void onError(int type, String errorMSG) {
            }
        });
        mUnderstander.init(null);
        mUnderstander.start();
    }

    protected void cancelListen(){
        mUnderstander.cancel();
    }


    protected void outputToSuspendedWindow(String str) throws IOException {
        if (addExpression) {
            str = wrapTheResult(str);
        }
        suspendedView.setText(str);
        tv.setText(str);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                layoutParams.x = (int) event.getRawX();
                layoutParams.y = (int) event.getRawY();
                wm.updateViewLayout(suspendedView, layoutParams);
                break;
            case MotionEvent.ACTION_MOVE:
                layoutParams.x = (int) event.getRawX();
                layoutParams.y = (int) event.getRawY();
                wm.updateViewLayout(suspendedView, layoutParams);
                break;
        }
        return false;
    }



    protected String wrapTheResult(String result) throws IOException {
        //循环，拿到情绪
        InputStream inputStream = getResources().openRawResource(R.raw.all);
        BufferedReader read = new BufferedReader(new InputStreamReader(inputStream));
        String wrapResult = result;
        String line;
        while ((line = read.readLine()) != null) {
            String exp[] = line.split(" ", 2);
            String pattern = ".*" + exp[0] + ".*";
            boolean isMatch = Pattern.matches(pattern, result);
            if (isMatch) {
                wrapResult = result + exp[1];
                break;
            }
        }

        return wrapResult;

    }

    protected void outToFile(String result) {
        if (file == null) {
            file = new File("default.txt");
        }
        try {
            PrintWriter pw = new PrintWriter(file);
            pw.append(result);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getFilePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()) {//如果外部储存可用
            return context.getExternalFilesDir(null).getPath();//获得外部存储路径,默认路径为 /storage/emulated/0/Android/data/com.waka.workspace.logtofile/files/Logs/log_2016-03-14_16-15-09.log
        } else {
            return context.getFilesDir().getPath();//直接存在/data/data里，非root手机是看不到的
        }
    }


}

package com.example.user.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.support.annotation.WorkerThread;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.naverspeech.client.utils.AudioWriterPCM;
import com.naver.speech.clientapi.SpeechConfig;
import com.naver.speech.clientapi.SpeechRecognitionException;
import com.naver.speech.clientapi.SpeechRecognitionListener;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.naver.speech.clientapi.SpeechRecognizer;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CLIENT_ID = "6kz23w16n6";

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;
    private TextView tetResult;
    private Button btnStrart;
    private String mResult;
    private AudioWriterPCM writer;


    private void handleMessage(Message msg) {
        switch (msg.what) {
            //음성인식을 시작할 준비가 완료된 경우
            case R.id.clientReady:
                writer = new AudioWriterPCM(Environment.getExternalStorageDirectory().getAbsolutePath()+" /NaverSpeechTest");
                writer.open("Test");
                break;

            //현재 음성인식이 진행되고 있는 경우
            case R.id.audioRecordint:
                writer.write((short[]) msg.obj);
                break;

            //처리가 되고 있는 도중에 결과를 받은 경우
            case R.id.partialResult:
                mResult = (String) (msg.obj);
                tetResult.setText(mResult);
                break;

            //최종인식이 완료되면 유사 결과를 모두 보여준다.
            case R.id.finalResult:
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                StringBuilder strBuf = new StringBuilder();

                for(String result : results) {
                    strBuf.append(result);
                    strBuf.append("\n");
                }
                mResult = strBuf.toString();
                tetResult.setText(mResult);
                break;

            //인식 오류가 발생한 경우
            case R.id.recognitionError:
                if(writer != null) {
                    writer.close();
                }
                mResult = "Error code: "+msg.obj.toString();
                tetResult.setText(mResult);
                btnStrart.setText(R.string.str_start);
                btnStrart.setEnabled(true);
                break;

            //음성 인식 비활성화 상태인 경우
            case R.id.clientInactive:
                if(writer != null) {
                    writer.close();
                }
                btnStrart.setText(R.string.str_start);
                btnStrart.setEnabled(true);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tetResult = (TextView) findViewById(R.id.txt_result);
        btnStrart = (Button) findViewById(R.id.btn_start);
        handler = new RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);
        btnStrart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*사용자의 os버전이 마시멜로우 이상인지 체크합니다. */
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    /*사용자 단말기의 권환 중 권한이 허용되어 있는지 체크합니다. */
                    int permissionResult = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
                    /*권한이 없을 때 */
                    if (permissionResult == PackageManager.PERMISSION_DENIED) {
                        /*사용자가 권한을 한번이라도 거부한 적이 있는 지 확인합니다. */
                        if(shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("권한이 필요합니다.")
                                    .setMessage("이 기능을 사용하기 위해서는 권한이 필요합니다. 계속하시겠습니까?")
                                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
                                            }
                                        }
                                    })
                                    .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(MainActivity.this, "기능을 취소했습니다.",Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .create()
                                    .show();
                        }
                        //최초로 권한을 요청하는 경우
                        else {
                            //권한을 요청합니다.
                            requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, 1000);
                        }

                    }
                    /*권한이 있는 경우*/
                    else {
                        /*음성 인식 기능을 처리합니다. */
                        if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                            mResult = "";
                            tetResult.setText("Connecting...");
                            btnStrart.setText(R.string.str_stop);
                            naverRecognizer.recognize();
                        } else {
                            Log.d(TAG, "stop and wait Final Result");
                            btnStrart.setEnabled(false);
                            naverRecognizer.getSpeechRecognizer().stop();
                        }
                    }
                }
                /*사용자의 os버전이 마시멜로우 이하일 때 */
                else {
                    /*음성 인식 기능을 처리합니다. */
                    if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                        mResult = "";
                        tetResult.setText("Connecting");
                        naverRecognizer.recognize();
                    } else {
                        Log.d(TAG, "stop and wait Final Result");
                        btnStrart.setEnabled(false);
                        naverRecognizer.getSpeechRecognizer().stop();
                    }
                }
            }
        });
    }

    @Override
    protected  void onStart() {
        super.onStart();
        //음성인식 서버 초기화를 진행합니다.
        naverRecognizer.getSpeechRecognizer().initialize();
    }
    @Override
    protected  void onResume() {
        super.onResume();
        mResult = "";
        tetResult.setText("");
        btnStrart.setText(R.string.str_start);
        btnStrart.setEnabled(true);
    }
    @Override
    protected void onStop() {
        super.onStop();
        //음성인식 서버를 종료합니다.
        naverRecognizer.getSpeechRecognizer().release();
    }
    //SpeechRecognizer 쓰레드의 메시지를 처리하는 핸들러를 정의합니다.
    static class RecognitionHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        RecognitionHandler (MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if(activity != null) {
                activity.handleMessage(msg);
            }
        }
    }
}

//SpeechRecognitionListener 를 상속한 클래스를 정의한다.
class  NaverRecognizer implements SpeechRecognitionListener {
    private final static String TAG = NaverRecognizer.class.getSimpleName();
    private Handler mHandler;
    private SpeechRecognizer mRecognizer;

    public NaverRecognizer(Context context, Handler handler, String clientId) {
        this.mHandler = handler;
        try {
            mRecognizer = new SpeechRecognizer(context, clientId);
        } catch (SpeechRecognitionException e) {
            e.printStackTrace();
        }
        mRecognizer.setSpeechRecognitionListener(this);
    }

    public SpeechRecognizer getSpeechRecognizer() {
        return mRecognizer;
    }

    public void recognize() {
        try {
            mRecognizer.recognize(new SpeechConfig(SpeechConfig.LanguageType.KOREAN, SpeechConfig.EndPointDetectType.AUTO));
        } catch (SpeechRecognitionException e) {
            e.printStackTrace();
        }
    }

    @Override
    @WorkerThread
    public void onInactive() {
        Message msg = Message.obtain(mHandler, R.id.clientInactive);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onReady() {
        Message msg = Message.obtain(mHandler, R.id.clientReady);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onRecord(short[] speech) {
        Message msg = Message.obtain(mHandler, R.id.audioRecordint, speech);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onPartialResult(String result) {
        Message msg = Message.obtain(mHandler, R.id.partialResult, result);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onEndPointDetected() {
        Log.d(TAG, "Event occured : EndPointDetected");
    }

    @Override
    @WorkerThread
    public void onResult(SpeechRecognitionResult result) {
        Message msg = Message.obtain(mHandler, R.id.finalResult, result);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onError(int errorCode) {
        Message msg = Message.obtain(mHandler, R.id.recognitionError, errorCode);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onEndPointDetectTypeSelected(SpeechConfig.EndPointDetectType epdType) {
        Message msg = Message.obtain(mHandler, R.id.endPointDetectTypeSelected, epdType);
        msg.sendToTarget();
    }
}


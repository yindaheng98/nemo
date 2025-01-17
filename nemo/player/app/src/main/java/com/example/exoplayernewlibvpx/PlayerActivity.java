package com.example.exoplayernewlibvpx;
/*
在ContentSelection.java定义的界面上选好参数之后就跳转到这个页面上播放视频
这里只有控制界面的代码，编解码的代码在com.google.android.exoplayer2里
作者修改了com.google.android.exoplayer2的代码，选好的参数都将传入至com.google.android.exoplayer2.DefaultRenderersFactory
*/
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.example.exoplayernewlibvpx.Constants.MESSAGE_EXO_STOP;
import static com.example.exoplayernewlibvpx.Constants.DATA_ROOT_PATH;


public class PlayerActivity extends AppCompatActivity {

    SimpleExoPlayer mSimpleExoPlayer;
    ExoPlayerHandler mExoPlayerHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        setupExoPlayer( //收集配置信息设置ExoPlayer
                getIntent().getStringExtra("content"),
                getIntent().getStringExtra("index"),
                getIntent().getStringExtra("quality"),
                getIntent().getStringExtra("resolution"),
                getIntent().getStringExtra("mode"),
                getIntent().getStringExtra("algorithm")
                );

//        int loopback = Integer.parseInt(getIntent().getStringExtra("loopback"));
        String loopback = getIntent().getStringExtra("loopback");
        if(loopback.matches("None")) {
            Log.e("loopback", "no loopback");
        }
        else{
            loopExoPlayer(Integer.parseInt(loopback)); //启动ExoPlayer循环播放
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSimpleExoPlayer != null) {
            mSimpleExoPlayer.stop(true);
            mSimpleExoPlayer.release();
        }
    }

    //TODO: video relative path, content path

    private void setupExoPlayer(String content, String index, String quality, String resolution, String mode, String algorithm) {
        String contentPath = DATA_ROOT_PATH + File.separator + content; // TODO: remove index
        String videoName ="";
        int decodeMode = 0;

        Log.e("loopback", contentPath);
        Log.e("loopback", quality);
        Log.e("loopback", resolution);
        Log.e("loopback", mode);
        Log.e("loopback", algorithm);

        //视频界面模式
        if (mode.equals("Decode")) {
            decodeMode = 0;
        } else if (mode.equals("Decode-SR")) {
            decodeMode = 1;
        } else if (mode.equals("Decode-Cache")) {
            decodeMode = 2;
        }

        //不同的清晰度代表不同的视频
        if (resolution.equals("240")) {
            videoName = "240p_512kbps_s0_d300.webm";
        } else if (resolution.equals("360")) {
            videoName = "360p_1024kbps_s0_d300.webm";
        } else if (resolution.equals("480")) {
            videoName = "480p_1600kbps_s0_d300.webm";
        }


        PlayerView playerView = findViewById(R.id.player);

        //Renderer渲染器，用于渲染媒体文件。当创建播放器的时候，Renderers被注入
        //这里，参数通过DefaultRenderersFactory传入到com.google.android.exoplayer2中
        DefaultRenderersFactory renderFactory = new DefaultRenderersFactory(this, contentPath, quality,  Integer.parseInt(resolution), decodeMode, algorithm);
        //而com.google.android.exoplayer2里有作者自己实现的编码器

        renderFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        mSimpleExoPlayer =
                ExoPlayerFactory.newSimpleInstance(this,
                        renderFactory,
                        new DefaultTrackSelector(),
                        new DefaultLoadControl(),
                        null);
        playerView.setPlayer(mSimpleExoPlayer);

        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT);

        MediaSource mediaSource = createLocalMediaSource(contentPath, videoName); //准备媒体数据
        //MediaSource媒体资源，用于定义要播放的媒体，加载媒体，以及从哪里加载媒体。简单的说，MediaSource就是代表我们要播放的媒体文件，可以是本地资源，可以是网络资源。

        mSimpleExoPlayer.prepare(mediaSource); //准备媒体数据
        mSimpleExoPlayer.setPlayWhenReady(true);

        mSimpleExoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    mSimpleExoPlayer.stop(true);
                }
            }
        });
    }

    private void loopExoPlayer(int seconds) {
        mSimpleExoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        mExoPlayerHandler = new ExoPlayerHandler();
        Message message = mExoPlayerHandler.obtainMessage();
        message.what = MESSAGE_EXO_STOP;
        mExoPlayerHandler.sendMessageDelayed(message, seconds * 1000);
    }

    private MediaSource createLocalMediaSource(String contentPath, String videoName) {
        File file = new File(contentPath + File.separator + "video" + File.separator + videoName);
        Uri uri = Uri.fromFile(file);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "ExoPlayer"));
        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        return videoSource;
    }


    private void setupDirectories() {
        //测试相关数据全部放在Android/data/android.example.testlibvpx下
        File dataDir = new File("/storage/emulated/0/Android/data", "android.example.testlibvpx");
        if (!dataDir.exists()) {
            dataDir.mkdir();

            File fileDir = new File("/storage/emulated/0/Android/data/android.example.testlibvpx", "files");
            if (!fileDir.exists()) {
                fileDir.mkdir();

                //Make inner directory structures
                File videoDir = new File("/storage/emulated/0/Android/data/android.example.testlibvpx/files", "video");
                File checkpointDir = new File("/storage/emulated/0/Android/data/android.example.testlibvpx/files", "checkpoint");
                File imageDir = new File("/storage/emulated/0/Android/data/android.example.testlibvpx/files", "image");
                File logDir = new File("/storage/emulated/0/Android/data/android.example.testlibvpx/files", "log");
                videoDir.mkdir();
                checkpointDir.mkdir();
                imageDir.mkdir();
                logDir.mkdir();

                //Add model and video from android resources
                File edsr64Dir = new File("/storage/emulated/0/Android/data/android.example.testlibvpx/files/checkpoint", "EDSR_S_B8_F64_S4");
                edsr64Dir.mkdir();
                File model = new File("/storage/emulated/0/Android/data/android.example.testlibvpx/files/checkpoint/EDSR_S_B8_F64_S4", "ckpt-100.dlc"); //SR模型数据文件

                File video = new File("/storage/emulated/0/Android/data/android.example.testlibvpx/files/video", "240p_s0_d60_encoded.webm"); //待播放的视频

                try {
                    InputStream modelInputStream = getResources().openRawResource(R.raw.ckpt_100); //打开模型数据文件测试写入
                    OutputStream modelOutputStream = new FileOutputStream(model);
                    byte[] data = new byte[modelInputStream.available()];
                    modelInputStream.read(data);
                    modelOutputStream.write(data);
                    modelInputStream.close();
                    modelOutputStream.close();

                    InputStream videoInputStream = getResources().openRawResource(R.raw.video); //打开视频文件测试写入
                    OutputStream videoOutputStream = new FileOutputStream(video);
                    byte[] videoData = new byte[videoInputStream.available()];
                    videoInputStream.read(videoData);
                    videoOutputStream.write(videoData);
                    videoInputStream.close();
                    videoOutputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //This version should be used when libvpx is changed to receive file paths from java side.
    private void setupDirectoriesSafe() {

        File fileDir = new File(this.getExternalFilesDir(null), "files");
        //Files have not been initialized yet.
        if (!fileDir.exists()) {
            fileDir.mkdir();

            //Make all directory structures
            File videoDir = new File(this.getExternalFilesDir("files"), "video");
            File checkpointDir = new File(this.getExternalFilesDir("files"), "checkpoint");
            File imageDir = new File(this.getExternalFilesDir("files"), "image");
            File logDir = new File(this.getExternalFilesDir("files"), "log");
            videoDir.mkdir();
            checkpointDir.mkdir();
            imageDir.mkdir();
            logDir.mkdir();

            //Add model from android resources
            InputStream inputStream = getResources().openRawResource(R.raw.ckpt_100);

        }


    }

    private class ExoPlayerHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_EXO_STOP:
                    mSimpleExoPlayer.stop(true);
                    finish();
                    break;
                default:
                    break;
            }
        }
    }
}
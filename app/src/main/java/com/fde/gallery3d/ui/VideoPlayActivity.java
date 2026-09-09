/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fde.gallery3d.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.documentfile.provider.DocumentFile;

import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseActivity;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.utils.StringUtils;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
//import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

public class VideoPlayActivity extends BaseActivity {
//    private StyledPlayerView mStyledPlayerView;

    private  SimpleExoPlayer simpleExoPlayer;
    private PlayerView playerView;
    private DefaultTrackSelector.Parameters mDefaultTrackSelectorParameters;
    private Multimedia videoData;

    ImageView imgDetails;

    SimpleExoPlayer player;

    Context context ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        context = this ;
        playerView = findViewById(R.id.player_view);
        imgDetails = (ImageView) findViewById(R.id.imgDetails);
        videoData = (Multimedia) getIntent().getSerializableExtra("video_data");
        if(videoData == null){
            Uri imageUri = getIntent().getData();
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, imageUri);
            String realPath = StringUtils.ToString(documentFile.getUri());
            videoData = FileUtils.getMultimediaByPath(context,documentFile.getName());
            if(videoData == null){
                videoData = new Multimedia();
                videoData.setPath(realPath);
            }
        }
        initializePlayer();
//        initPlayer();

        imgDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context,com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
                builder.setTitle(R.string.details);
                builder.setMessage("title:  " + videoData.getTitle() + "\n"
//                        + "width:  " + videoData.getWidth() + "\n"
//                        + "height:  " + videoData.getHeight() + "\n"
                        + "date:  " + StringUtils.conversionTime(1000 * videoData.getDateTaken()) + "\n"
                        + "duration:  " + videoData.getDuration()/1000 + "s\n"
                        + "path:  " + videoData.getPath() + "\n"
                );
                builder.show();
            }
        });
    }


    private void initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(context);
        playerView.setPlayer(player);

        RawResourceDataSource rawResourceDataSource =
                new RawResourceDataSource(context);





        try {
//            Uri rawResourceUri =
//                    RawResourceDataSource.buildRawResourceUri(R.raw.aa);
//            rawResourceDataSource.open(
//                    new DataSpec(rawResourceUri)
//            );
//
//            DataSource dataSource = rawResourceDataSource;

//            MediaSource mediaSource =
//                    new ExtractorMediaSource.Factory(() -> dataSource)
//                            .createMediaSource(rawResourceUri);

            Uri videoUri = null ;
            if(videoData.getPath() !=null && videoData.getPath().contains("content://")){
                videoUri = Uri.parse(videoData.getPath());
            }else {
                videoUri = Uri.fromFile(new File(videoData.getPath()));
            }

            MediaSource mediaSource =
                    new ExtractorMediaSource.Factory(
                            new DefaultDataSourceFactory(
                                    context,
                                    Util.getUserAgent(context, "Gallery3D")
                            )
                    ).createMediaSource(videoUri);

            if (player != null) {
                player.prepare(mediaSource);
                player.setPlayWhenReady(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * release player
     */
    private void releasePlayer() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerView.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
         LogTools.i("----------onDestroy---------");
        if (player != null) {
            player.release();
        }
        player = null ;
    }
}
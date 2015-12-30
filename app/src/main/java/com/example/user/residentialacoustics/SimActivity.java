package com.example.user.residentialacoustics;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jjoe64.graphview.*;

// RESOURCES:
// http://developer.android.com/guide/topics/media/audio-capture.html
// http://www.android-graphview.org/documentation

public class SimActivity extends Activity{

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    final int START_PLAY_ID = 0;
    final int STOP_PLAY_ID = 1;
    final int START_REC_ID = 2;
    final int STOP_REC_ID = 3;

    private AsyncTask button_task = null;

    private Button finish_button = null;
    private ImageButton rec_stop_button = null;
    private ImageButton play_pause_button = null;
    private ImageButton curtain_button = null;
    private TextView db_display = null;
    private TextView max_db_display = null;

    private final double REFERENCE_AMP = 0;
    private double decibels = 0;
    private double max_decibels = 0;

    int arr_index_rec = 0;
    int arr_index_dis = 0;
    private double[] db_arr = new double[1000];     // fixed size: either stop record when full
    private double[] db_reduced = new double[1000]; //             OR reallocate

    private boolean init_recording_state = false;   // has there been an initial recording
    private boolean button_play_state = false;
    private boolean button_record_state = false;
    private boolean curtain_closed = false;

    String datapath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sim_data.3gp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim);

        finish_button = (Button) findViewById(R.id.finish_button);
        rec_stop_button = (ImageButton) findViewById(R.id.rec_stop_button);
        play_pause_button = (ImageButton) findViewById(R.id.play_pause_button);
        curtain_button = (ImageButton) findViewById(R.id.curtain_button);
        db_display = (TextView) findViewById(R.id.db_readout);
        max_db_display = (TextView) findViewById(R.id.db_max);

        button_task = new PlayRecTask();

        rec_stop_button.setOnClickListener(new View.OnClickListener() {
            // called when user clicks rec/stop button
            public void onClick(View v) {
                updateRecordButton();
            }
        });

        play_pause_button.setOnClickListener(new View.OnClickListener() {
            // called when user clicks play/pause button
            public void onClick(View v) {
                updatePlayButton();
            }
        });

        curtain_button.setOnClickListener(new View.OnClickListener() {
            // called when user clicks curtain button
            public void onClick(View v) {
                updateCurtainButton();
            }
        });

        finish_button.setOnClickListener(new View.OnClickListener() {
            // called when user clicks the Finish button
            public void onClick(View v) {
                if (button_record_state) updateRecordButton();
                if (button_play_state) updatePlayButton();
                finish();
            }
        });
    }

    private void updateRecordButton() {
        if (!button_play_state && (button_task.getStatus() != AsyncTask.Status.RUNNING)) {
            if (button_record_state) {
                rec_stop_button.setImageResource(R.drawable.ic_action_mic);
                button_record_state = false;
            } else {
                if (!init_recording_state) init_recording_state = true;
                rec_stop_button.setImageResource(R.drawable.ic_action_stop);
                button_record_state = true;
            }
            onRecord(button_record_state);
        }
    }

    private void updatePlayButton() {
        if (!button_record_state && init_recording_state && (button_task.getStatus() != AsyncTask.Status.RUNNING)) {
            if (button_play_state) {
                play_pause_button.setImageResource(R.drawable.ic_action_play);
                button_play_state = false;
            } else {
                play_pause_button.setImageResource(R.drawable.ic_action_pause);
                button_play_state = true;
            }
            onPlay(button_play_state);
        }
    }

    private void updateCurtainButton() {
        if (curtain_closed) {
            curtain_button.setImageResource(R.drawable.open_curtain);
            curtain_closed = false;
        } else {
            curtain_button.setImageResource(R.drawable.closed_curtain);
            curtain_closed = true;
        }
    }

    private void onRecord(boolean start) {
        if (start) {
            button_task = new PlayRecTask().execute(START_REC_ID);
        } else {
            button_task = new PlayRecTask().execute(STOP_REC_ID);
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            button_task = new PlayRecTask().execute(START_PLAY_ID);
        } else {
            button_task = new PlayRecTask().execute(STOP_PLAY_ID);
        }
    }

    private class PlayRecTask extends AsyncTask <Integer, Void, String> {

        @Override
        protected  void onPreExecute() {}

        @Override
        protected String doInBackground(Integer... opId) {
            switch (opId[0]) {
                case START_PLAY_ID: start_play();   return "play started";
                case STOP_PLAY_ID:  stop_play();    return "play stopped";
                case START_REC_ID:  start_rec();    return "rec started";
                case STOP_REC_ID:   stop_rec();     return "rec stopped";
                default: return "invalid id";
            }
        }

        @Override
        protected void onPostExecute(String result) {}

        protected void start_play () {
            System.out.println("start_play()");
            mPlayer = new MediaPlayer();
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while ((mPlayer != null) && mPlayer.isPlaying()) { // sleep allows mPlayer to move to non-null state
                                try {                                          // where isPlaying() function is not valid
                                    Thread.sleep(750);                         // temp fix by increasing sleep time, wasteful solution
                                } catch (Exception e) {
                                    Log.e("MediaPlayer", "isPlayingCheck failed()");
                                }
                            }
                            if (mPlayer != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updatePlayButton();
                                    }
                                });
                            }
                        }
                    }).start();
                }
            });

            try {
                mPlayer.setDataSource(datapath);
                mPlayer.prepareAsync();
            } catch (Exception e) {
                Log.e("Media_StartPlaying", "prepare() failed");
            }

            // reset decibel array index to playback from beginning
            arr_index_rec = 0;
            arr_index_dis = 0;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mPlayer != null) {
                        try {
                            Thread.sleep(200); // 250 ms between each measurement
                        } catch (Exception e) {
                            Log.e("Thread_dBCalc", "sleep() failed");
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (button_play_state) {
                                    if (db_reduced[arr_index_dis] >= 0) {
                                        if (curtain_closed) db_display.setText("dB: " + Double.toString(db_reduced[arr_index_dis]));
                                        else db_display.setText("dB: " + Double.toString(db_arr[arr_index_dis]));
                                    }
                                    max_db_display.setText("MAX: " + Double.toString(max_decibels));
                                    ++arr_index_dis;
                                }
                            }
                        });
                    }
                }
            }).start();


        }

        protected void stop_play () {
            System.out.println("stop_play()");
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
        }

        protected void start_rec () {
            System.out.println("start_rec()");
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(datapath);

            try {
                mRecorder.prepare();
                mRecorder.start();
            } catch (Exception e) {
                Log.e("Media_StartRecording", "prepare() failed");
            }

            // reset decibel array index to store new values
            arr_index_rec = 0;
            arr_index_dis = 0;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mRecorder != null) { // while recording
                        try {
                            Thread.sleep(200); // 250 ms between each measurement
                        } catch (Exception e) {
                            Log.e("Thread_dBCalc", "sleep() failed");
                        }
                        if (mRecorder != null) { // avoids race condition
                            decibels = mRecorder.getMaxAmplitude();
                            db_arr[arr_index_rec] = 20 * Math.log10( decibels );
                            db_reduced[arr_index_rec] = 20 * Math.log10( decibels * .20 );
                            if (db_arr[arr_index_rec] > max_decibels) max_decibels = db_arr[arr_index_rec];
                            ++arr_index_rec;
                        }

                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (button_record_state) {
                                    if (db_reduced[arr_index_dis] >= 0) {
                                        if (curtain_closed) db_display.setText("dB: " + Double.toString(db_reduced[arr_index_dis]));
                                        else db_display.setText("dB: " + Double.toString(db_arr[arr_index_dis]));
                                    }
                                    max_db_display.setText("MAX: " + Double.toString(max_decibels));
                                    ++arr_index_dis;
                                }
                            }
                        });
                    }
                }
            }).start();
        }

        protected void stop_rec () {
            System.out.println("stop_rec()");
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
                max_decibels = 0;
            }
        }
    }

    @Override
    protected void onPause() { // activity pause
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

package com.icaynia.dmxario.Activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;

import com.icaynia.dmxario.Bluetooth.Bluetooth;
import com.icaynia.dmxario.Data.PositionManager;
import com.icaynia.dmxario.Data.ViewID;
import com.icaynia.dmxario.GlobalVar;
import com.icaynia.dmxario.Model.Position;
import com.icaynia.dmxario.R;
import com.icaynia.dmxario.Scene;
import com.icaynia.dmxario.VerticalSeekBar;
import com.icaynia.dmxario.View.ControllerDisplayView;
import com.icaynia.dmxario.View.PositionButton;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by icaynia on 2016. 12. 14..
 */

public class ControllerActivity extends AppCompatActivity {
    GlobalVar global;

    private ArrayList<PositionButton> arrayButtons = new ArrayList<PositionButton>();
    private ArrayList<VerticalSeekBar> arraySeekbar = new ArrayList<VerticalSeekBar>();
    private PositionButton prevFrame;
    private PositionButton nextFrame;
    private PositionButton recordButton;
    private PositionButton editButton;

    public ControllerDisplayView controllerDisplayView;

    private Scene mainScene = new Scene(this);

    /* FOR EDIT MODE */
    private boolean EDIT_MODE = false;
    private int EDIT_MODE_SELECTED_POSITION = 0;
    private int row;
    private ArrayList<String> seekBarData = new ArrayList<String>();

    /* FOR RECORD MODE */
    private boolean RECORD_MODE = false;
    private Scene tmpScene;
    private Timer mTimer;
    private boolean ismTimerRunning;
    private String tmpStr;
    private Handler handler;
    private ViewID viewID;

    /* FOR SEEKBAR */
    private PositionButton seekbarCustomizing;
    private ArrayList<PositionButton> selChannelButtons = new ArrayList<PositionButton>();

    /* FOR POSITION */
    private ArrayList<Position> position = new ArrayList<Position>();
    private PositionManager positionManager;

    /* FOR POSITION : DIALOG */
    private View dialogV;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller_new);

        viewInitialize();
        dataInitialize();
    }

    private void viewInitialize() {
        viewID = new ViewID();

        /* seekbar initialize */
        for (int row = 0; row < viewID.controller.seekbar.length; row++) {
            arraySeekbar.add(row, (VerticalSeekBar) findViewById(viewID.controller.seekbar[row]));
            arraySeekbar.get(row).setMax(255);
            arraySeekbar.get(row).setTag(row+"");
            arraySeekbar.get(row).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    /* key */
                    int key = Integer.parseInt(seekBar.getTag().toString());

                    if (selChannelButtons.get(0).isSwitchOn()) {
                        sendData("+e:"+(key+1)+":"+ progress+"#");
                        seekBarData.set(key, progress+"");
                    }
                    if (selChannelButtons.get(1).isSwitchOn()) {
                        sendData("+e:"+(key+17)+":"+ progress+"#");
                        seekBarData.set(key+16, progress+"");
                    }
                    if (selChannelButtons.get(2).isSwitchOn()) {
                        sendData("+e:"+(key+33)+":"+ progress+"#");
                        seekBarData.set(key+32, progress+"");
                    }
                    if (selChannelButtons.get(3).isSwitchOn()) {
                        sendData("+e:"+(key+49)+":"+ progress+"#");
                        seekBarData.set(key+48, progress+"");
                    }
                    if (EDIT_MODE) {
                        controllerDisplayView.setPositionScript(getPositionScript());
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        for (int row = 0; row < viewID.controller.seekbarSelectChannelButton.length; row++) {
            selChannelButtons.add(row, (PositionButton) findViewById(viewID.controller.seekbarSelectChannelButton[row]));
            selChannelButtons.get(row).setSwitchMode(true);
            selChannelButtons.get(row).setText((row+1)+"");
        }
        seekbarCustomizing = (PositionButton) findViewById(viewID.controller.seekbar_customizing);
        seekbarCustomizing.setText("Custom");

        seekBarDataInitialize();

        /* position initialize */
        for (int row = 0; row < viewID.controller.position.length; row++) {
            arrayButtons.add(row, (PositionButton) findViewById(viewID.controller.position[row]));

            arrayButtons.get(row).v.setTag(row+"");
            arrayButtons.get(row).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendData(position.get(Integer.parseInt(v.getTag().toString())).action_press);
                }
            });
        }

        prevFrame = (PositionButton) findViewById(viewID.controller.prevFrameButton);
        prevFrame.setText("<");
        prevFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToFrame(mainScene.getSceneNowFrame()-1, true);
            }
        });
        nextFrame = (PositionButton) findViewById(viewID.controller.nextFrameButton);
        nextFrame.setText(">");
        nextFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToFrame(mainScene.getSceneNowFrame()+1, true);
            }
        });
        recordButton = (PositionButton) findViewById(viewID.controller.recordButton);
        recordButton.setText("REC");
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RECORD_MODE) {
                    recordMode(false);
                } else {
                    recordMode(true);
                    editMode(false);
                }
            }
        });
        editButton = (PositionButton) findViewById(viewID.controller.editButton);
        editButton.setText("EDIT");
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EDIT_MODE) {
                    editMode(false);
                } else {
                    editMode(true);
                    recordMode(false);
                }
            }
        });

        /* controller initialize */
        controllerDisplayView = (ControllerDisplayView) findViewById(R.id.content_display);
        controllerDisplayView.setFrameNumber(mainScene.getSceneNowFrame(), false);
        controllerDisplayView.setMaxFrame(1);

        controllerDisplayView.setFrameSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.e("seekBar", progress+"");
                goToFrame(progress+1, false);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        controllerDisplayView.setEditPositionOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    private void seekBarDataInitialize() {
        seekBarData = new ArrayList<String>();
        for (int row = 0; row < 64; row++) {
            seekBarData.add(row, "");
        }
    }

    private void dataInitialize() {
        global = (GlobalVar) getApplicationContext();
        global.bluetooth = new Bluetooth(this);
        /* new scene */
        mainScene.setSceneLength(1);
        mainScene.setSceneName("New Scene");
        positionInitialize();
    }

    private void positionInitialize() {
        /* position */
        positionManager = new PositionManager(this);
        for (int i = 0; i < 48; i++) {
            position.add(i, positionManager.getPosition(i));
            arrayButtons.get(i).setText(position.get(i).name);
        }
    }

    private void sendData(String data) {
        tmpStr += data;
        Log.e("sendToBluetooth", data);
    }

    private void goToFrame(int frame, boolean progressMoving) {
        controllerDisplayView.setMaxFrame(mainScene.getSceneLength());
        if (frame > mainScene.getSceneLength()) {
            mainScene.setSceneLength(frame);
            controllerDisplayView.setMaxFrame(frame);
        }
        /* Data */
        if (frame > 0) {
            mainScene.setSceneNowFrame(frame);
        /* View */
            controllerDisplayView.setFrameNumber(frame, progressMoving);
        }
    }

    private void editMode(boolean SWITCH) {
        EDIT_MODE = SWITCH;
        if (SWITCH) {
            editButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_orange));
            controllerDisplayView.setEditPositionVisiblie(View.VISIBLE);
            activeEditMode();
        } else {
            editButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.selector_button_green));
            controllerDisplayView.setEditPositionVisiblie(View.GONE);
            deactiveEditMode();
        }
    }

    private void recordMode(boolean SWITCH) {
        RECORD_MODE = SWITCH;
        if (SWITCH) {
            recordButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_orange));
            recordSceneStart();
        } else {
            recordButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.selector_button_green));
            if (ismTimerRunning) {
                mTimer.cancel();
                ismTimerRunning = false;
            }
        }
    }

    private void setPositionScript(int id, String str) {

        position.get(id).action_press= str;
        positionManager.setPosition(id, position.get(id));
        positionInitialize();
    }

    public void recordSceneStart()
    {
        handler = new Handler();
        tmpScene = new Scene(this);
        tmpStr = ""; // 비우고 시작
        if (ismTimerRunning) {
            mTimer.cancel();
            ismTimerRunning = false;
        }
        mTimer = new Timer();
        mTimer.schedule(
                new TimerTask(){
                    int i = 1;
                    @Override
                    public void run()
                    {
                        handler.post(
                                new Runnable()
                                {
                                    public void run()
                                    {
                                        ismTimerRunning = true;
                                        //fileStr += i+"#"+"0=0;"+tmpStr+"-\n";
                                        Log.e("Controller/rec..start()", i+"#:"+tmpStr);
                                        tmpScene.putFrame(i, tmpStr);
                                        tmpStr = "";

                                        if (i == 200)
                                        {
                                            tmpScene.setSceneLength(i);
                                            tmpScene.setSceneName("scene0.scn");
                                            //setDisplayText(""); dialog
                                            // 알림창 객체 생성
                                            mTimer.cancel();
                                            Log.e("Controller/rec..start()", "recordTimer stopped.");
                                            //showSavesceneDialog();

                                            recordMode(false);
                                        }
                                        i++;
                                        goToFrame(i, true);
                                    }
                                });
                    }
                }, 0, 20
        );
    }

    private void activeEditMode() {
        positionInitialize();
        for (row = 0; row < viewID.controller.position.length; row++) {
            arrayButtons.get(row).v.setTag(row+"");
            arrayButtons.get(row).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    savePositionScript();
                    /* before */
                    setSelectPosition(Integer.parseInt(v.getTag().toString()));
                    }
            });
        }
        setSelectPosition(EDIT_MODE_SELECTED_POSITION);
    }

    private void deactiveEditMode() {
        savePositionScript();
        arrayButtons.get(EDIT_MODE_SELECTED_POSITION).setBackgroundDrawable(getResources().getDrawable(R.drawable.selector_button_green));
        viewInitialize();
    }

    private void setSelectSeekbarChannel(final int id) {

    }

    private void savePositionScript() {
        if (!getPositionScript().isEmpty())
        setPositionScript(EDIT_MODE_SELECTED_POSITION, getPositionScript());
    }

    private String getPositionScript() {
        String str = "";
        for (int i = 0; i < seekBarData.size(); i++) {
            if (!seekBarData.get(i).isEmpty())
                str += "+e:"+(i + 1)+":"+ seekBarData.get(i) +"#\n";
        }
        return str;
    }
    private void setSelectPosition(final int id) {
        arrayButtons.get(EDIT_MODE_SELECTED_POSITION).setBackgroundDrawable(getResources().getDrawable(R.drawable.selector_button_green));

        arrayButtons.get(id).setBackgroundDrawable(getResources().getDrawable(R.drawable.button_orange));
        EDIT_MODE_SELECTED_POSITION = id;
        Log.e("SELECTED_POSITION", EDIT_MODE_SELECTED_POSITION+"");

        /* display setting */
        controllerDisplayView.setPositionName(position.get(id).name);
        controllerDisplayView.setPositionScript(position.get(id).action_press);

        controllerDisplayView.setEditPositionOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPositionEditNameDialog(id);
            }
        });

        seekBarDataInitialize();
    }
    public void showPositionEditNameDialog(final int id) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        dialogV = getLayoutInflater().inflate(R.layout.dialog_position_editname, null);

        final EditText position_name = (EditText) dialogV.findViewById(R.id.position_name);


        builder.setView(dialogV);
        builder.setTitle("Edit position : "+ id);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = position_name.getEditableText().toString();
                position.get(id).name = name;
                positionManager.setPosition(id, position.get(id));
                positionInitialize();
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);

        alert.show();    // 알림창 띄우기
    }
}
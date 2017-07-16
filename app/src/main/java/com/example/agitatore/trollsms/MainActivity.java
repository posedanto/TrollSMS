package com.example.agitatore.trollsms;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CONTACT = 0;
    private static final String DIALOG_SEND = "0";
    private static final String DIALOG_CANCEL = "1";

    Button buttonSelectContact, buttonSend, buttonCancel;
    SeekBar seekBar, seekBarDelay;
    EditText editTextMessage, editTextPeriod, editTextDelay;
    TextView textViewInfo, textViewInfo2;
    ProgressBar progressBar;
    LinearLayout onSendingBlock;

    String myContactNumber, myContactName;
    char[] myMessage;
    Timer myTimer;
    int myCurrentPos = 0;
    long myPeriod, myDelay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSelectContact = (Button) findViewById(R.id.button_select_contact);
        buttonSelectContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                pickContact.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        buttonCancel = (Button) findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Отменяем?")
                        .setMessage("Отправлено: " + myCurrentPos + " СМС" +
                        "\nОсталось: " + (myMessage.length-myCurrentPos) + " СМС")
                        .setPositiveButton("ОК",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        myTimer.cancel();
                                        resetUI();
                                    }
                                })
                        .setNegativeButton("Отмена",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel(); }
                                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        buttonSend = (Button) findViewById(R.id.button_send);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myMessage = editTextMessage.getText().toString().toCharArray();
                myPeriod = (seekBar.getProgress() + 1) * 1000;
                myDelay = seekBarDelay.getProgress() * 1000;
                if (myMessage.length == 0) {
                    Toast.makeText(getApplicationContext(),
                            "Введи текст сообщения!",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (myContactNumber == null) {
                    Toast.makeText(getApplicationContext(),
                            "Выбери жертву!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String dialogQuestion = "Жертва: " + myContactName +
                        "\nСообщение: " + editTextMessage.getText().toString() +
                        "\nИнтервал отправки СМС: " + myPeriod /1000 + " сек" +
                        "\nОтправка начнется через: " + myDelay /1000 + " сек" +
                        "\n\nВНИМАНИЕ\nБудет отправлено " + myMessage.length + " СМС!";
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Спамим?")
                        .setMessage(dialogQuestion)
                        .setPositiveButton("ОК",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        sendMessages();
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton("Отмена",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel(); }
                                });
                AlertDialog alert = builder.create();
                alert.show();

            }
        });

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editTextPeriod.setText("" + (float) (1 + progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarDelay = (SeekBar) findViewById(R.id.seekBarDelay);
        seekBarDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editTextDelay.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        editTextPeriod = (EditText) findViewById(R.id.editTextPeriod);
        editTextDelay = (EditText) findViewById(R.id.editTextDelay);

        editTextMessage = (EditText) findViewById(R.id.editText_message);
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                textViewInfo.setText("Количество СМС для отправки: " + s.length());
            }
        });

        textViewInfo = (TextView) findViewById(R.id.textView_info);
        textViewInfo2 = (TextView) findViewById(R.id.textViewInfo2);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        onSendingBlock = (LinearLayout) findViewById(R.id.onSendBlock);
        onSendingBlock.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();

            String[] queryFields = new String[] {ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER};
            Cursor cursor = getContentResolver().query(contactUri, queryFields, null, null, null);
            try {
                if (cursor.getCount() == 0)
                    return;
                cursor.moveToFirst();
                myContactName = cursor.getString(0);
                myContactNumber = cursor.getString(1);
                buttonSelectContact.setText(myContactName + " (" + myContactNumber + ")");
            }
            finally {
                cursor.close();
            }
        }
    }

    private void resetUI() {
        myCurrentPos = 0;
        buttonSelectContact.setEnabled(true);
        buttonSend.setEnabled(true);
        onSendingBlock.setVisibility(View.INVISIBLE);
        progressBar.setProgress(0);
        textViewInfo2.setText("");
        myTimer.cancel();
    }

    private void sendMessages() {
        buttonSelectContact.setEnabled(false);
        buttonSend.setEnabled(false);
        onSendingBlock.setVisibility(View.VISIBLE);
        final Handler uiHandler = new Handler();
        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(myContactNumber, null,
                            "" + myMessage[myCurrentPos], null, null);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again later!",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

                myCurrentPos++;
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(myCurrentPos * 100 /myMessage.length);
                        textViewInfo2.setText(textViewInfo2.getText().toString() +
                                myMessage[myCurrentPos-1]);
                        if (myCurrentPos == myMessage.length) {
                            resetUI();
                        }
                    }
                });
            }
        }, myDelay, myPeriod);
    }
}

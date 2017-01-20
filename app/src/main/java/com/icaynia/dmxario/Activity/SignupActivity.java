package com.icaynia.dmxario.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.icaynia.dmxario.Global;
import com.icaynia.dmxario.R;

/**
 * Created by icaynia on 19/01/2017.
 */

public class SignupActivity extends AppCompatActivity {
    Global global;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        global = (Global) getApplicationContext();
        TextView signupText = (TextView) findViewById(R.id.signupText);
        signupText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText email, pw1, pw2;
                email = (EditText) findViewById(R.id.input_email);
                pw1 = (EditText)findViewById(R.id.input_password);
                pw2 = (EditText)findViewById(R.id.input_password_again);
                if (email.getText().toString().isEmpty())
                    onToast("이메일을 입력하세요.");
                else if (pw1.getText().toString().isEmpty())
                    onToast("비밀번호를 입력하세요.");
                else if (!pw1.getText().toString().equals(pw2.getText().toString())) {
                    onToast("비밀번호가 일치하지 않습니다.");
                } else {
                    addUser(email.getText().toString(), pw1.getText().toString());
                    onMainActivity();
                }
            }
        });


    }

    private void onMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void addUser(String id, String pw) {
        global.getAuth().createUserWithEmailAndPassword(id, pw)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("Authentication", "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(SignupActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });

        }
    private void onToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

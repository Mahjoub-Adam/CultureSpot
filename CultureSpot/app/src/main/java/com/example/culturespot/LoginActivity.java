package com.example.culturespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView register;
    private Button login;
    private EditText editTextEmail,editTextPassword;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private Switch switch1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth=FirebaseAuth.getInstance();
        login=(Button) findViewById(R.id.login);
        login.setOnClickListener(this);
        editTextEmail=(EditText) findViewById(R.id.email);
        editTextPassword=(EditText) findViewById(R.id.password);
        switch1=(Switch) findViewById(R.id.switch1);
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // (un)show password
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) editTextPassword.setTransformationMethod(null);
                else editTextPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });
        register=(TextView) findViewById(R.id.register);
        register.setOnClickListener(this);
        progressBar=(ProgressBar) findViewById(R.id.progressBar);
    }
    @Override
    public void onBackPressed(){
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.register:
                startActivity(new Intent(this,RegisterActivity.class)); // get to register activity
                break;
            case R.id.login:
                loginUser();
                break;
            default:
                break;
        }
    }
    private boolean empty(String title,String str,EditText edit){
        if(str.isEmpty()){
            edit.setError(title+" is required!");
            return true;
        }
        return false;
    }
    private boolean emailMatches(String email,EditText editTextEmail){
        if(empty("Email",email,editTextEmail)) return true;
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){  //valid email
            editTextEmail.setError("Please provide a valid email");
            return true;
        }
        return false;
    }
    private boolean passwordMatches(String password,EditText editTextPassword){
        if(empty("Password",password,editTextPassword)) return true;
        if(password.length()<6) {  //valid password
            editTextPassword.setError("Minimum password length should be 6 characters!");
            return true;
        }
        return false;
    }
    private void loginUser(){
        String email=editTextEmail.getText().toString().trim();
        String password=editTextPassword.getText().toString().trim();
        boolean boolEmail=emailMatches(email,editTextEmail);
        boolean boolPassword=passwordMatches(password,editTextPassword);
        if(boolEmail || boolPassword) return;
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){ // user exists and password is correct
                    startActivity(new Intent(LoginActivity.this,BottomNavigationActivity.class)); // show user main activity
                    progressBar.setVisibility(View.GONE);
                }
                else {
                    Toast.makeText(LoginActivity.this,"Invalid credentials,try again!",Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.FirebaseDatabase;
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth mAuth;
    private Button register;
    private EditText editTextUsername,editTextEmail,editTextFirstName,editTextSurname,editTextPassword;
    private ProgressBar progressBar;
    private boolean bool;
    private Switch switch1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        register=(Button) findViewById(R.id.register);
        register.setOnClickListener(this);
        switch1=(Switch) findViewById(R.id.switch1);

        editTextUsername=(EditText) findViewById(R.id.username);
        editTextEmail=(EditText) findViewById(R.id.email);
        editTextFirstName=(EditText) findViewById(R.id.first_name);
        editTextSurname=(EditText) findViewById(R.id.surname);
        editTextPassword=(EditText) findViewById(R.id.password);
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { // (un)show password
                if(isChecked) editTextPassword.setTransformationMethod(null);
                else editTextPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });
        progressBar=(ProgressBar) findViewById(R.id.progressBar);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.register:
                registerUser();
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
    private boolean emailMatches(String email,EditText editTextEmail){ //valid email
        if(empty("Email",email,editTextEmail)) return true;
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
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
    private boolean emailExists(String email) { //see if email already exists
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                if (task.getResult().getSignInMethods().size()!=0)  {
                    Toast.makeText(RegisterActivity.this,"Failed to register,email already exists!",Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    bool=true;
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                bool=false;
                e.printStackTrace();
            }
        });
        return bool;
    }
    private void registerUser(){
        String username=editTextUsername.getText().toString().trim();
        String email=editTextEmail.getText().toString().trim();
        String first_name=editTextFirstName.getText().toString().trim();
        String surname=editTextSurname.getText().toString().trim();
        String password=editTextPassword.getText().toString().trim();
        boolean boolUsername=empty("Username",username,editTextUsername);
        boolean boolFirstName=empty("First Name",first_name,editTextFirstName);
        boolean boolEmail=emailMatches(email,editTextEmail);
        boolean boolPassword=passwordMatches(password,editTextPassword);
        boolean boolSurname=empty("Surname",surname,editTextSurname);
        if(boolUsername  || boolFirstName || boolEmail || boolSurname || boolPassword) return;
        progressBar.setVisibility(View.VISIBLE);
        if(!emailExists(email)) mAuth.createUserWithEmailAndPassword(email,password) //create user to firebase
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            User user=new User(username,email,first_name,surname);
                            FirebaseDatabase.getInstance().getReference("Users") // create new document on user collection
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        progressBar.setVisibility(View.VISIBLE);
                                        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() { // sigin in user
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if(task.isSuccessful()){
                                                    startActivity(new Intent(RegisterActivity.this,BottomNavigationActivity.class)); // show user to main activity
                                                    progressBar.setVisibility(View.GONE);
                                                }
                                                else progressBar.setVisibility(View.GONE);
                                            }
                                        });
                                    }
                                    else {
                                        Toast.makeText(RegisterActivity.this,"Failed to register,please try again!",Toast.LENGTH_LONG).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }
                        else{
                            Toast.makeText(RegisterActivity.this,"Failed to register,please try again!",Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }
}
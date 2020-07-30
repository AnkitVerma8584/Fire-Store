package com.example.serverdatatransfer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

import dataStorageClasses.Constants;
import dataStorageClasses.MySingleton;
import dataStorageClasses.SharedPrefManager;


public class UserLogin extends AppCompatActivity {
    private EditText username, password;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);
        username = findViewById(R.id.et_username);
        password = findViewById(R.id.et_password);
        Button signIn = findViewById(R.id.go);
        TextView register = findViewById(R.id.tv_register);
        progressDialog = new ProgressDialog(this);

        signIn.setOnClickListener(v -> userLogin());

        register.setOnClickListener(v -> {
            startActivity(new Intent(UserLogin.this, UserRegister.class));
            finish();
        });
    }

    private void userLogin() {
        final String userName = username.getText().toString().trim();
        final String passWord = password.getText().toString().trim();
        if (userName.equals("") || passWord.equals("")) {
            Toast.makeText(getApplicationContext(), "Empty parameters", Toast.LENGTH_LONG).show();
        } else {
            progressDialog.setCancelable(false);
            progressDialog.setTitle("Logging in...");
            progressDialog.show();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.URL_LOGIN, response -> {
                progressDialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (!jsonObject.getBoolean("error")) {
                        SharedPrefManager.getInstance(getApplicationContext()).userLogin(jsonObject.getInt("id"), jsonObject.getString("username"),passWord);
                        startActivity(new Intent(UserLogin.this, DashBoard.class));
                        finish();
                    }
                    Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }, error -> {
                progressDialog.hide();
                Toast.makeText(getApplicationContext(), "Error! " + error.getMessage(), Toast.LENGTH_LONG).show();
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("username", userName);
                    params.put("password", passWord);
                    return params;
                }
            };
            MySingleton.getInstance(this).addToRequestQueue(stringRequest);
        }
    }
}
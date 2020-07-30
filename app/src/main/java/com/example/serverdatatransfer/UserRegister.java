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
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import dataStorageClasses.Constants;
import dataStorageClasses.MySingleton;
import dataStorageClasses.SharedPrefManager;

public class UserRegister extends AppCompatActivity {
    private TextInputLayout t1, t2, t3;
    private EditText username, password, conPass;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        username = findViewById(R.id.et_username);
        password = findViewById(R.id.et_password);
        conPass = findViewById(R.id.et_con_password);

        Button register = findViewById(R.id.btn_register);
        t1 = findViewById(R.id.textInputLayout);
        t2 = findViewById(R.id.textInputLayout2);
        t3 = findViewById(R.id.textInputLayout3);

        TextView signIn = findViewById(R.id.tv_sign_in);
        progressDialog = new ProgressDialog(this);

        register.setOnClickListener(v -> registerUser());
        signIn.setOnClickListener(v -> {
            startActivity(new Intent(UserRegister.this, UserLogin.class));
            finish();
        });
    }

    private void registerUser() {
        final String userName = username.getText().toString().trim();
        final String passWord = password.getText().toString().trim();
        final String conPassword = conPass.getText().toString().trim();

        if (userName.equals(""))
            t1.setError("Username cannot be empty");
        if (passWord.equals(""))
            t2.setError("Password cannot be empty");
        if (conPassword.equals(""))
            t2.setError("Password cannot be empty");
        if (userName.length() > 15) {
            t1.setError("Username exceeded limit");
        }
        if (passWord.length() > 15) {
            t2.setError("Password exceeded limit");
        }
        if (!conPassword.equals(passWord)) {
            t3.setError("Password does not match");
        }
        if(userName.length()>0&&userName.length()<16&&passWord.length()>0&&passWord.length()<16&&conPassword.equals(passWord)) {
            t1.setError(null);
            t2.setError(null);
            t3.setError(null);
            progressDialog.setTitle("Registering user");
            progressDialog.show();
            StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.URL_REGISTER, response -> {
                progressDialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (!jsonObject.getBoolean("error")) {
                        username.setText(null);
                        password.setText(null);
                        conPass.setText(null);
                        loginUser(userName, passWord);
                    }
                    Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
                progressDialog.hide();
                Toast.makeText(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
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

    private void loginUser(String u, String p) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.URL_LOGIN, response -> {
            progressDialog.dismiss();
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (!jsonObject.getBoolean("error")) {
                    SharedPrefManager.getInstance(getApplicationContext()).userLogin(jsonObject.getInt("id"), jsonObject.getString("username"), jsonObject.getString("password"));
                    startActivity(new Intent(UserRegister.this, DashBoard.class));
                    finish();
                } else
                    Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            progressDialog.hide();
            Toast.makeText(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", u);
                params.put("password", p);
                return params;
            }
        };
        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }
}
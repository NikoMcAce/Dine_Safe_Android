package com.example.dine_safe_android;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UsersData extends AppCompatActivity {
    private EditText etName, etPhoneNumber, etRole, etNewPassword;
    private Button btnDeleteAccount, btnDownloadData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_data);

        etName = findViewById(R.id.etName1);
        etPhoneNumber = findViewById(R.id.etPhoneNumber1);
        etRole = findViewById(R.id.etRole1);
        etNewPassword = findViewById(R.id.etNewPassword1);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount1);
        btnDownloadData = findViewById(R.id.btnDownloadData1);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);

        // Load user data
        if (username != null) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);
                    String role = dataSnapshot.child("role").getValue(String.class);

                    etName.setText(name);
                    etPhoneNumber.setText(phone);
                    etRole.setText(role);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(UsersData.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(UsersData.this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete your account?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (username != null) {
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
                                userRef.removeValue().addOnSuccessListener(aVoid -> {
                                    Toast.makeText(UsersData.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.clear();
                                    editor.apply();
                                    Intent intent = new Intent(UsersData.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnDownloadData.setOnClickListener(v -> {
            downloadUserData();
        });

        etNewPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (!etNewPassword.getText().toString().isEmpty()) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
                userRef.child("password").setValue(etNewPassword.getText().toString());
                Toast.makeText(UsersData.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void downloadUserData() {
        // Implement data download logic here
        Toast.makeText(this, "Download feature not implemented yet.", Toast.LENGTH_SHORT).show();
    }
}
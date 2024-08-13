package com.example.dine_safe_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText loginUsername, loginPassword;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if the user is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String savedUsername = sharedPreferences.getString("username", null);
        if (savedUsername != null) {
            // User is already logged in, redirect to the appropriate dashboard
            navigateToDashboard(sharedPreferences.getString("role", null));
            return;  // End the activity here
        }

        setContentView(R.layout.activity_login);

        loginUsername = findViewById(R.id.loginUsername);
        loginPassword = findViewById(R.id.LoginPassword);
        loginButton = findViewById(R.id.button2);

        loginButton.setOnClickListener(v -> checkLogin());
    }

    private void checkLogin() {
        String username = loginUsername.getText().toString();
        String password = loginPassword.getText().toString();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference restaurantRef = database.getReference("Restaurants");
        DatabaseReference usersRef = database.getReference("Users");

        // Check in Restaurants first
        restaurantRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean found = false;
                for (DataSnapshot restaurantSnapshot : task.getResult().getChildren()) {
                    String adminUsername = restaurantSnapshot.child("admin_username").getValue(String.class);
                    String adminPassword = restaurantSnapshot.child("admin_password").getValue(String.class);

                    if (username.equals(adminUsername) && password.equals(adminPassword)) {
                        saveToSharedPreferences(username, "admin", restaurantSnapshot.getKey());
                        navigateToDashboard("admin");
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Check in Users if not found in Restaurants
                    checkUsers(usersRef, username, password);
                }
            } else {
                Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUsers(DatabaseReference usersRef, String username, String password) {
        usersRef.child(username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String storedPassword = task.getResult().child("password").getValue(String.class);
                String role = task.getResult().child("role").getValue(String.class);
                String restaurantName = task.getResult().child("restaurant_name").getValue(String.class);

                if (password.equals(storedPassword)) {
                    saveToSharedPreferences(username, role, restaurantName);
                    navigateToDashboard(role);
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid password.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(LoginActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveToSharedPreferences(String username, String role, String restaurantName) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("role", role);
        editor.putString("restaurant_name", restaurantName);
        editor.apply();
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        switch (role) {
            case "admin":
                intent = new Intent(LoginActivity.this, AdminDashboard.class);
                break;
            case "chef":
                intent = new Intent(LoginActivity.this, ChefDashboard.class);
                break;
            case "waiter":
                intent = new Intent(LoginActivity.this, WaiterDashboard.class);
                break;
            default:
                return;  // No valid role found, do nothing
        }
        startActivity(intent);
        finish();  // End the LoginActivity so the user can't go back to it
    }
}
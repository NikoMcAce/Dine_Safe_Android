package com.example.dine_safe_android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Pattern;

public class AddEmployee extends AppCompatActivity {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[^\\s\\.$#\\[\\]/]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+44\\s?\\d{10}$");

    private Spinner roleSpinner;
    private EditText nameEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText phoneEditText;
    private Button saveButton;
    public String restaurantName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");

        roleSpinner = findViewById(R.id.roleSpinner);
        nameEditText = findViewById(R.id.nameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);

        // Populate Spinner with roles
        String[] roles = {"Chef", "Waiter"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        saveButton.setOnClickListener(v -> saveEmployee());
    }

    private void saveEmployee() {
        String role = roleSpinner.getSelectedItem().toString();
        String name = nameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(role) || TextUtils.isEmpty(name) || TextUtils.isEmpty(username)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate username and phone
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            Toast.makeText(this, "Username is not valid. No spaces, dots, or special characters allowed.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            Toast.makeText(this, "Phone number is not valid. Use format +44 followed by 10 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reference to the Users node
        DatabaseReference employeesRef = FirebaseDatabase.getInstance().getReference("Users");

        // Check if username already exists
        employeesRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Username is already taken
                    Toast.makeText(AddEmployee.this, "Username is already taken. Please choose a different username.", Toast.LENGTH_SHORT).show();
                } else {
                    // Username is available, save the employee details
                    DatabaseReference userRef = employeesRef.child(username);
                    userRef.child("name").setValue(capitalizeWords(name));
                    userRef.child("username").setValue(username);
                    userRef.child("password").setValue(password);
                    userRef.child("phone").setValue(phone);
                    userRef.child("restaurant_name").setValue(restaurantName); // Set appropriate restaurant name
                    userRef.child("role").setValue(toLowerCase(role));

                    Toast.makeText(AddEmployee.this, "Employee added successfully.", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after saving
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors
                Toast.makeText(AddEmployee.this, "Failed to check username. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder capitalized = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                capitalized.append(c);
            } else if (capitalizeNext) {
                capitalized.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                capitalized.append(c);
            }
        }

        return capitalized.toString();
    }

    private String toLowerCase(String input) {
        if (input == null) {
            return null; // Handle null input
        }
        return input.toLowerCase();
    }
}
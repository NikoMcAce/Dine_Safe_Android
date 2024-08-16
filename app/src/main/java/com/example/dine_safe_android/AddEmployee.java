package com.example.dine_safe_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
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

    private ListView employeeListView;
    private ArrayAdapter<String> employeeAdapter;
    private ArrayList<String> employeeList = new ArrayList<>();
    private HashMap<String, HashMap<String, String>> employeeDetails = new HashMap<>();

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
        String[] roles = {"chef", "waiter"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        saveButton.setOnClickListener(v -> saveEmployee());


        employeeListView = findViewById(R.id.employeeListView);
        employeeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, employeeList);
        employeeListView.setAdapter(employeeAdapter);

        fetchEmployees();
        employeeListView.setOnItemClickListener((parent, view, position, id) -> {
            String fullUserInfo = employeeList.get(position); // This might be a string like "username-role"
            String[] parts = fullUserInfo.split("-"); // Split the string by hyphen
            if (parts.length > 0) {
                String username = parts[0];
                username = username.replaceAll("\\s+$", "");
                showEmployeeDialog(username);
                //Toast.makeText(this, username, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid user data.", Toast.LENGTH_SHORT).show();
            }
        });
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
    private void fetchEmployees() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.orderByChild("restaurant_name").equalTo(restaurantName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                employeeList.clear();
                employeeDetails.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String username = snapshot.child("username").getValue(String.class);
                    if (username != null) {
                        String name = snapshot.child("name").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String role = snapshot.child("role").getValue(String.class);
                        HashMap<String, String> details = new HashMap<>();
                        details.put("name", name);
                        details.put("phone", phone);
                        details.put("role", role);
                        details.put("username", username);
                        employeeDetails.put(username, details);
                        //employeeList.add(name + " (" + role + ")");
                        employeeList.add(username+" - "+name+ " (" + role + ")");
                    }
                }
                employeeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AddEmployee.this, "Failed to load employees.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showEmployeeDialog(String username) {
        HashMap<String, String> details = employeeDetails.get(username);
        if (details == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_employee, null);
        builder.setView(dialogView);

        EditText nameEditText = dialogView.findViewById(R.id.dialogNameEditText);
        EditText phoneEditText = dialogView.findViewById(R.id.dialogPhoneEditText);
        Spinner roleSpinner = dialogView.findViewById(R.id.dialogRoleSpinner);
        EditText usernameEditText = dialogView.findViewById(R.id.dialogUsernameEditText);
        Button updateButton = dialogView.findViewById(R.id.dialogUpdateButton);
        Button deleteButton = dialogView.findViewById(R.id.dialogDeleteButton);

        nameEditText.setText(details.get("name"));
        phoneEditText.setText(details.get("phone"));
        usernameEditText.setText(username);
        usernameEditText.setEnabled(false); // Username is not editable

        // Setup role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"chef", "waiter"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);
        int rolePosition = roleAdapter.getPosition(details.get("role"));
        roleSpinner.setSelection(rolePosition);

        AlertDialog dialog = builder.create();

        updateButton.setOnClickListener(v -> {
            // Update logic
            String newName = nameEditText.getText().toString().trim();
            String newPhone = phoneEditText.getText().toString().trim();
            String newRole = roleSpinner.getSelectedItem().toString();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
            userRef.child("name").setValue(newName);
            userRef.child("phone").setValue(newPhone);
            userRef.child("role").setValue(newRole);
            dialog.dismiss();
            Toast.makeText(this, "Employee updated.", Toast.LENGTH_SHORT).show();

            Intent intent = getIntent();
            finish();
            startActivity(intent);

        });

        deleteButton.setOnClickListener(v -> {
            // Delete logic
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
            userRef.removeValue();
            dialog.dismiss();
            Toast.makeText(this, "Employee deleted.", Toast.LENGTH_SHORT).show();

            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });

        //cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
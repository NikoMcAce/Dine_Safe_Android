package com.example.dine_safe_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText adminUserName, adminPassword, adminName, restaurantName, addressLine1, addressLine2, postCode, tableCount;
    private EditText waiterName, waiterUserName, waiterPassword, waiterPhone;
    private EditText chefName, chefUserName, chefPassword, chefPhone;
    private Button createRestaurantButton;
    private LinearLayout formLayout;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[^\\s\\.$#\\[\\]/]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+44\\s?\\d{10}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MIN_TABLE_COUNT = 1;
    private static final int MAX_TABLE_COUNT = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize fields
        adminUserName = findViewById(R.id.adminUserName);
        adminPassword = findViewById(R.id.adminPassword);
        adminName = findViewById(R.id.adminName);
        restaurantName = findViewById(R.id.restaurantName);
        addressLine1 = findViewById(R.id.addressLine1);
        addressLine2 = findViewById(R.id.addressLine2);
        postCode = findViewById(R.id.postCode);
        tableCount = findViewById(R.id.tableCount);

        waiterName = findViewById(R.id.waiterName);
        waiterUserName = findViewById(R.id.waiterUserName);
        waiterPassword = findViewById(R.id.waiterPassword);
        waiterPhone = findViewById(R.id.waiterPhone);

        chefName = findViewById(R.id.chefName);
        chefUserName = findViewById(R.id.chefUserName);
        chefPassword = findViewById(R.id.chefPassword);
        chefPhone = findViewById(R.id.chefPhone);

        createRestaurantButton = findViewById(R.id.createRestaurantButton);
//        formLayout = findViewById(R.id.formLayout); // Make sure to add this ID in XML

        createRestaurantButton.setOnClickListener(v -> validateAndCreateRestaurant());
    }

    private void validateAndCreateRestaurant() {
        boolean isValid = true;

        // Reset previous errors
        resetErrors();

        // Check if any required field is empty
        isValid &= validateRequiredFields();

        // Validate passwords
        isValid &= validatePasswords();

        // Validate table count and phone numbers
        isValid &= validateTableCount();
        isValid &= validatePhoneNumbers();

        // Validate usernames
        isValid &= validateUsernames();

        if (isValid) {
            // Proceed with creating restaurant in Firebase
            saveDataToFirebase();
        }
    }

    private boolean validateRequiredFields() {
        boolean isValid = true;

        // Check if any required field is empty
        if (isEmpty(adminUserName)) isValid = false;
        if (isEmpty(adminPassword)) isValid = false;
        if (isEmpty(adminName)) isValid = false;
        if (isEmpty(restaurantName)) isValid = false;
        if (isEmpty(addressLine1)) isValid = false;
        if (isEmpty(postCode)) isValid = false;
        if (isEmpty(tableCount)) isValid = false;
        if (isEmpty(waiterName)) isValid = false;
        if (isEmpty(waiterUserName)) isValid = false;
        if (isEmpty(waiterPassword)) isValid = false;
        if (isEmpty(waiterPhone)) isValid = false;
        if (isEmpty(chefName)) isValid = false;
        if (isEmpty(chefUserName)) isValid = false;
        if (isEmpty(chefPassword)) isValid = false;
        if (isEmpty(chefPhone)) isValid = false;

        return isValid;
    }

    private boolean validatePasswords() {
        boolean isValid = true;

        // Check if passwords have more than 8 characters
        if (adminPassword.getText().toString().length() < MIN_PASSWORD_LENGTH) {
            adminPassword.setError("Password must be at least 8 characters");
            isValid = false;
        }
        if (waiterPassword.getText().toString().length() < MIN_PASSWORD_LENGTH) {
            waiterPassword.setError("Password must be at least 8 characters");
            isValid = false;
        }
        if (chefPassword.getText().toString().length() < MIN_PASSWORD_LENGTH) {
            chefPassword.setError("Password must be at least 8 characters");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateTableCount() {
        boolean isValid = true;

        String tableCountStr = tableCount.getText().toString();
        try {
            int tableCountValue = Integer.parseInt(tableCountStr);
            if (tableCountValue < MIN_TABLE_COUNT || tableCountValue > MAX_TABLE_COUNT) {
                tableCount.setError("Table count must be between 1 and 99");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            tableCount.setError("Invalid table count");
            isValid = false;
        }

        return isValid;
    }

    private boolean validatePhoneNumbers() {
        boolean isValid = true;

        // Validate phone numbers
        if (!PHONE_PATTERN.matcher(waiterPhone.getText().toString()).matches()) {
            waiterPhone.setError("Invalid phone number");
            isValid = false;
        }
        if (!PHONE_PATTERN.matcher(chefPhone.getText().toString()).matches()) {
            chefPhone.setError("Invalid phone number");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateUsernames() {
        boolean isValid = true;

        // Validate usernames
        if (!USERNAME_PATTERN.matcher(adminUserName.getText().toString()).matches()) {
            adminUserName.setError("Invalid username");
            isValid = false;
        }
        if (!USERNAME_PATTERN.matcher(waiterUserName.getText().toString()).matches()) {
            waiterUserName.setError("Invalid username");
            isValid = false;
        }
        if (!USERNAME_PATTERN.matcher(chefUserName.getText().toString()).matches()) {
            chefUserName.setError("Invalid username");
            isValid = false;
        }

        return isValid;
    }

    private boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    private void resetErrors() {
        adminUserName.setError(null);
        adminPassword.setError(null);
        adminName.setError(null);
        restaurantName.setError(null);
        addressLine1.setError(null);
        addressLine2.setError(null);
        postCode.setError(null);
        tableCount.setError(null);
        waiterName.setError(null);
        waiterUserName.setError(null);
        waiterPassword.setError(null);
        waiterPhone.setError(null);
        chefName.setError(null);
        chefUserName.setError(null);
        chefPassword.setError(null);
        chefPhone.setError(null);
    }

    private void saveDataToFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference restaurantRef = database.getReference("Restaurants/" + restaurantName.getText().toString());
        DatabaseReference usersRef = database.getReference("Users");

        // Add restaurant data directly under the restaurant node
        restaurantRef.child("admin_username").setValue(adminUserName.getText().toString());
        restaurantRef.child("admin_password").setValue(adminPassword.getText().toString());
        restaurantRef.child("address_line1").setValue(addressLine1.getText().toString());
        restaurantRef.child("address_line2").setValue(addressLine2.getText().toString());
        restaurantRef.child("postcode").setValue(postCode.getText().toString());
        restaurantRef.child("table_count").setValue(Integer.parseInt(tableCount.getText().toString()));
        restaurantRef.child("maxorderno").setValue(0);  // Add maxorderno field with initial value 0

        // Create waiter data directly under the Users node
        DatabaseReference waiterDataRef = usersRef.child(waiterUserName.getText().toString());
        waiterDataRef.child("name").setValue(waiterName.getText().toString());
        waiterDataRef.child("username").setValue(waiterUserName.getText().toString());
        waiterDataRef.child("password").setValue(waiterPassword.getText().toString());
        waiterDataRef.child("phone").setValue(waiterPhone.getText().toString());
        waiterDataRef.child("restaurant_name").setValue(restaurantName.getText().toString());
        waiterDataRef.child("role").setValue("waiter");

        // Create chef data directly under the Users node
        DatabaseReference chefDataRef = usersRef.child(chefUserName.getText().toString());
        chefDataRef.child("name").setValue(chefName.getText().toString());
        chefDataRef.child("username").setValue(chefUserName.getText().toString());
        chefDataRef.child("password").setValue(chefPassword.getText().toString());
        chefDataRef.child("phone").setValue(chefPhone.getText().toString());
        chefDataRef.child("restaurant_name").setValue(restaurantName.getText().toString());
        chefDataRef.child("role").setValue("chef");

        Toast.makeText(this, "Restaurant created successfully!", Toast.LENGTH_SHORT).show();

        // Redirect to login page
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
    }
}

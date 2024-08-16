package com.example.dine_safe_android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ViewMenu extends AppCompatActivity {

    public String restaurantName="";
    private EditText searchBar;
    private LinearLayout menuContainer;
    private TextView emptyMenuTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_menu);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        String adminUsername = sharedPreferences.getString("username", "Admin Username");
        searchBar = findViewById(R.id.searchBar);
        menuContainer = findViewById(R.id.menuContainer);
        emptyMenuTextView = findViewById(R.id.emptyMenuTextView);

        FloatingActionButton fabAddFood = findViewById(R.id.fabAddFood);
        fabAddFood.setOnClickListener(v -> showAddFoodDialog());

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMenu(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });

        loadMenu();
    }

    private void loadMenu() {
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Menu");
        menuRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                LinearLayout menuContainer = findViewById(R.id.menuContainer);
                menuContainer.removeAllViews(); // Clear previous items
                boolean isMenuEmpty = true;

                for (DataSnapshot foodSnapshot : dataSnapshot.getChildren()) {
                    isMenuEmpty = false;
                    View cardView = LayoutInflater.from(ViewMenu.this).inflate(R.layout.menu_card, menuContainer, false);

                    TextView dishNameTextView = cardView.findViewById(R.id.dishNameTextView);
                    TextView priceTextView = cardView.findViewById(R.id.priceTextView);
                    View editButton = cardView.findViewById(R.id.editButton);
                    View deleteButton = cardView.findViewById(R.id.deleteButton);

                    String dishName = foodSnapshot.child("name").getValue(String.class);
                    String price = foodSnapshot.child("price").getValue(String.class);

                    dishNameTextView.setText(dishName);
                    priceTextView.setText(price);

                    // Set click listeners for edit and delete buttons
                    editButton.setOnClickListener(v -> showEditDialog(foodSnapshot));

                    deleteButton.setOnClickListener(v -> {
                        // Handle delete button click
                        // Remove the food item from Firebase
                        foodSnapshot.getRef().removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ViewMenu.this, "Food item deleted.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ViewMenu.this, "Failed to delete food item.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });

                    menuContainer.addView(cardView);
                }

                // Show empty menu message if no items found
                TextView emptyMenuTextView = findViewById(R.id.emptyMenuTextView);
                emptyMenuTextView.setVisibility(isMenuEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ViewMenu.this, "Failed to load menu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog(DataSnapshot foodSnapshot) {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(ViewMenu.this).inflate(R.layout.dialog_edit_food, null);
        EditText editDishName = dialogView.findViewById(R.id.editDishName);
        EditText editPrice = dialogView.findViewById(R.id.editPrice);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set initial values
        String currentDishName = foodSnapshot.child("name").getValue(String.class);
        String currentPrice = foodSnapshot.child("price").getValue(String.class);
        editDishName.setText(currentDishName);
        editPrice.setText(currentPrice);

        // Create the dialog
        Dialog dialog = new Dialog(ViewMenu.this);
        dialog.setContentView(dialogView);
        dialog.setTitle("Edit Food Item");

        saveButton.setOnClickListener(v -> {
            String newDishName = editDishName.getText().toString();
            String newPrice = editPrice.getText().toString();

            // Validate input
            if (newDishName.isEmpty() || newPrice.isEmpty()) {
                Toast.makeText(ViewMenu.this, "Please fill in both fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save changes to Firebase
            foodSnapshot.getRef().child("name").setValue(newDishName);
            foodSnapshot.getRef().child("price").setValue(newPrice).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ViewMenu.this, "Food item updated.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ViewMenu.this, "Failed to update food item.", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAddFoodDialog() {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(ViewMenu.this).inflate(R.layout.dialog_add_food, null);
        EditText editDishName = dialogView.findViewById(R.id.editDishName);
        EditText editPrice = dialogView.findViewById(R.id.editPrice);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Create the dialog
        Dialog dialog = new Dialog(ViewMenu.this);
        dialog.setContentView(dialogView);
        dialog.setTitle("Add New Food Item");

        saveButton.setOnClickListener(v -> {
            final String dishName = capitalizeWords(editDishName.getText().toString());
            final String price = editPrice.getText().toString();

            // Validate input
            if (dishName.isEmpty() || price.isEmpty()) {
                Toast.makeText(ViewMenu.this, "Please fill in both fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Retrieve the current menu items to determine the next food_no
            DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Menu");
            menuRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Find the maximum food_no
                    int maxFoodNo = 0;
                    for (DataSnapshot foodSnapshot : dataSnapshot.getChildren()) {
                        try {
                            int currentFoodNo = Integer.parseInt(foodSnapshot.getKey());
                            if (currentFoodNo > maxFoodNo) {
                                maxFoodNo = currentFoodNo;
                            }
                        } catch (NumberFormatException e) {
                            // Handle exception if the key is not a valid number
                            e.printStackTrace();
                        }
                    }

                    // Increment food_no
                    int newFoodNo = maxFoodNo + 1;

                    // Save the new food item to Firebase
                    DatabaseReference newFoodRef = menuRef.child(String.valueOf(newFoodNo));
                    newFoodRef.child("name").setValue(dishName);
                    newFoodRef.child("price").setValue(price).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ViewMenu.this, "Food item added.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ViewMenu.this, "Failed to add food item.", Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(ViewMenu.this, "Failed to retrieve menu data.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
    private void filterMenu(String query) {
        for (int i = 0; i < menuContainer.getChildCount(); i++) {
            CardView card = (CardView) menuContainer.getChildAt(i);
            TextView dishNameTextView = card.findViewById(R.id.dishNameTextView);
            String dishName = dishNameTextView.getText().toString().toLowerCase();
            card.setVisibility(dishName.contains(query.toLowerCase()) ? View.VISIBLE : View.GONE);
        }

        // Show empty menu message if no items match the query
        boolean isMenuEmpty = true;
        for (int i = 0; i < menuContainer.getChildCount(); i++) {
            CardView card = (CardView) menuContainer.getChildAt(i);
            if (card.getVisibility() == View.VISIBLE) {
                isMenuEmpty = false;
                break;
            }
        }
        emptyMenuTextView.setVisibility(isMenuEmpty ? View.VISIBLE : View.GONE);
    }
}
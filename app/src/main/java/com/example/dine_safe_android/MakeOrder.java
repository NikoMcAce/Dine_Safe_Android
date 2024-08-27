package com.example.dine_safe_android;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MakeOrder extends AppCompatActivity {
    public String restaurantName = "";
    private LinearLayout menuList;
    private int tableIndex;
    private List<OrderItem> orderItemList = new ArrayList<>();
    private List<View> allMenuItems = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_make_order);

        menuList = findViewById(R.id.menu_list);
        SearchView searchMenu = findViewById(R.id.search_menu);
        Button confirmButton = findViewById(R.id.confirm_button);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");

        // Fetch the menu from Firebase
        fetchMenuFromFirebase();
        tableIndex= getIntent().getIntExtra("TABLE_INDEX", -1);
        Toast.makeText(this, "Table Number: " + tableIndex, Toast.LENGTH_SHORT).show();

        searchMenu.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterMenu(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMenu(newText);
                return false;
            }
        });

        confirmButton.setOnClickListener(v -> {
            showConfirmDialog();
        });
    }
    private void filterMenu(String query) {
        query = query.toLowerCase();

        // Clear the menu list before adding filtered items
        menuList.removeAllViews();

        for (View itemView : allMenuItems) {
            TextView foodNameTextView = itemView.findViewById(R.id.food_name);
            String foodName = foodNameTextView.getText().toString().toLowerCase();

            if (foodName.contains(query)) {
                menuList.addView(itemView);
            }
        }
    }

    private void fetchMenuFromFirebase() {
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference()
                .child("Restaurants")
                .child(restaurantName)
                .child("Menu");

        menuRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                menuList.removeAllViews();  // Clear previous items if any
                allMenuItems.clear();  // Clear the list before repopulating

                for (DataSnapshot foodSnapshot : dataSnapshot.getChildren()) {
                    String foodName = foodSnapshot.child("name").getValue(String.class);
                    String foodPrice = foodSnapshot.child("price").getValue(String.class);

                    if (foodName != null && foodPrice != null) {
                        addMenuItemCard(foodName, foodPrice);
                        //allMenuItems.add(menuItemView);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors
                Toast.makeText(MakeOrder.this, "Failed to load menu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMenuItemCard(String name, String price) {
        // Inflate a card view for each menu item
        View menuItemView = LayoutInflater.from(this).inflate(R.layout.menu_item_card, menuList, false);

        // Set the food name and price
        TextView foodNameTextView = menuItemView.findViewById(R.id.food_name);
        TextView foodPriceTextView = menuItemView.findViewById(R.id.food_price);

        foodNameTextView.setText(name);
        foodPriceTextView.setText("£" + price);

        // Set an onClickListener to show a toast with food name and price
        menuItemView.setOnClickListener(v -> {
            // Inflate the dialog layout
            LayoutInflater inflater = LayoutInflater.from(MakeOrder.this);
            View dialogView = inflater.inflate(R.layout.dialog_quantity_selector, null);

            // Initialize the dialog elements
            TextView numberOfCustomers = dialogView.findViewById(R.id.numberOfCustomers);
            Button minusButton = dialogView.findViewById(R.id.minusButton);
            Button plusButton = dialogView.findViewById(R.id.plusButton);
            Button assignButton = dialogView.findViewById(R.id.assignButton);

            // Set initial quantity
            int[] quantity = {1}; // using an array to allow modification inside lambda

            // Set click listeners for minus and plus buttons
            minusButton.setOnClickListener(view -> {
                if (quantity[0] > 1) {
                    quantity[0]--;
                    numberOfCustomers.setText(String.valueOf(quantity[0]));
                }
            });

            plusButton.setOnClickListener(view -> {
                quantity[0]++;
                numberOfCustomers.setText(String.valueOf(quantity[0]));
            });

            // Create and show the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(MakeOrder.this);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            // Set click listener for assignButton
            assignButton.setOnClickListener(view -> {
                // Handle adding the item with quantity to the order
                OrderItem orderItem = new OrderItem(name, quantity[0], price);
                orderItemList.add(orderItem);
                addOrderItemToList(orderItem);
                // Show a toast to confirm
                Toast.makeText(MakeOrder.this, "Added " + quantity[0] + " x " + name + " to the order.", Toast.LENGTH_SHORT).show();

                dialog.dismiss();
            });

            dialog.show();
        });

        // Add the card to the LinearLayout
        menuList.addView(menuItemView);
        // Add the view to allMenuItems for filtering purposes
        allMenuItems.add(menuItemView);
    }
    private void addOrderItemToList(OrderItem orderItem) {
        // Find the order_list LinearLayout
        LinearLayout orderList = findViewById(R.id.order_list);

        // Inflate the layout for each order item
        LayoutInflater inflater = LayoutInflater.from(this);
        View orderItemView = inflater.inflate(R.layout.order_item_view, orderList, false);

        // Set the food name and total price (quantity x price)
        TextView foodNameTextView = orderItemView.findViewById(R.id.food_name);
        TextView totalPriceTextView = orderItemView.findViewById(R.id.total_price);

        foodNameTextView.setText(orderItem.getName());

        // Calculate the total price
        double price = Double.parseDouble(orderItem.getPrice());
        double totalPrice = price * orderItem.getQuantity();
        totalPriceTextView.setText(String.format("£%.2f", totalPrice));

        // Add the view to the order list
        orderList.addView(orderItemView);
    }
    private void showConfirmDialog() {
        // Calculate the total price
        double totalAmount = 0;
        for (OrderItem item : orderItemList) {
            double price = Double.parseDouble(item.getPrice());
            totalAmount += price * item.getQuantity();
        }

        // Create and display a confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MakeOrder.this);
        builder.setTitle("Confirm Order");
        builder.setMessage("Total: £" + String.format("%.2f", totalAmount) + "\n\nDo you want to confirm the order?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference()
                    .child("Restaurants")
                    .child(restaurantName)
                    .child("orders");

            // Query to find the current order for the table
            ordersRef.orderByChild("table_no").equalTo(tableIndex).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean orderFound = false;
                    for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                        String status = orderSnapshot.child("status").getValue(String.class);
                        if ("occupied".equals(status)) {  // Assuming "occupied" means the order is still being taken
                            String orderNo = orderSnapshot.getKey();
                            double currentTotalPrice = orderSnapshot.child("total_price").getValue(Double.class);

                            // Update total price
                            double newTotalPrice = currentTotalPrice + calculateOrderTotal();

                            // Update the order in Firebase
                            ordersRef.child(orderNo).child("total_price").setValue(newTotalPrice);

                            // Add ordered food items
                            for (OrderItem item : orderItemList) {
                                DatabaseReference foodRef = ordersRef.child(orderNo)
                                        .child("food_ordered")
                                        .child(item.getName());

                                double unitPrice = Double.parseDouble(item.getPrice());

                                // Retrieve the current quantity
                                foodRef.child("qty").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        int currentQty = 0;
                                        if (dataSnapshot.exists()) {
                                            currentQty = dataSnapshot.getValue(Integer.class); // Get current quantity
                                        }

                                        int newQty = currentQty + item.getQuantity(); // Add the new quantity to the current one
                                        foodRef.child("qty").setValue(newQty); // Update the quantity
                                        foodRef.child("unit_price").setValue(unitPrice); // Set the unit price
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        // Handle possible errors
                                        Toast.makeText(MakeOrder.this, "Failed to update quantity.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            // Show confirmation toast
                            Toast.makeText(MakeOrder.this, "Order confirmed! Total: £" + String.format("%.2f", newTotalPrice), Toast.LENGTH_SHORT).show();
                            orderFound = true;
                            break;
                        }
                    }

                    if (!orderFound) {
                        Toast.makeText(MakeOrder.this, "No active order found for this table.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle possible errors
                    Toast.makeText(MakeOrder.this, "Failed to update order.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private double calculateOrderTotal() {
        double total = 0;
        for (OrderItem item : orderItemList) {
            double price = Double.parseDouble(item.getPrice());
            total += price * item.getQuantity();
        }
        return total;
    }
}
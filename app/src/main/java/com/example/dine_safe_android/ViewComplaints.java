package com.example.dine_safe_android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
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

public class ViewComplaints extends AppCompatActivity {
    private String restaurantName;
    private SearchView searchView;
    private LinearLayout complaintsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_complaints);

        complaintsContainer = findViewById(R.id.complaintsContainer);
        searchView = findViewById(R.id.searchView);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView complaintsrestaurant = findViewById(R.id.complaintsrestaurant);
        TextView comaplintsusername = findViewById(R.id.comaplintsusername);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        String adminUsername = sharedPreferences.getString("username", "Admin Username");
        complaintsrestaurant.setText(restaurantName);
        comaplintsusername.setText(adminUsername);


        loadComplaints();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterComplaints(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterComplaints(newText);
                return false;
            }
        });
    }

    private void loadComplaints() {
        DatabaseReference complaintsRef = FirebaseDatabase.getInstance()
                .getReference("Restaurants")
                .child(restaurantName)
                .child("Complaints");

        complaintsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                complaintsContainer.removeAllViews();

                for (DataSnapshot complaintSnapshot : dataSnapshot.getChildren()) {
                    String orderNo = complaintSnapshot.child("order_no").getValue(String.class);
                    String raisedOn = complaintSnapshot.child("raised_on").getValue(String.class);
                    String complain = complaintSnapshot.child("complain").getValue(String.class);
                    String raisedBy = complaintSnapshot.child("raised_by").getValue(String.class);
                    String raisedAt = complaintSnapshot.child("raised_at").getValue(String.class);

                    StringBuilder foodDetails = new StringBuilder();
                    for (DataSnapshot foodItem : complaintSnapshot.child("food_returned").getChildren()) {
                        String foodName = foodItem.getKey();
                        Integer qty = foodItem.child("qty").getValue(Integer.class);
                        String unitPrice = foodItem.child("unit_price").getValue(String.class);


                        foodDetails.append(foodName)
                                .append(" - Qty: ").append(qty)
                                .append(", Price: £").append(unitPrice)
                                .append("\n");
                    }

                    addComplaintCard(orderNo, raisedOn, complain, raisedBy, raisedAt, foodDetails.toString().trim());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ViewComplaints.this, "Failed to load complaints.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addComplaintCard(String orderNo, String raisedOn, String complain, String raisedBy, String raisedAt, String foodDetails) {
        View complaintCard = LayoutInflater.from(this).inflate(R.layout.complaint_card, complaintsContainer, false);

        TextView tvOrderNumber = complaintCard.findViewById(R.id.tvOrderNumber);
        TextView tvRaisedOnDate = complaintCard.findViewById(R.id.tvRaisedOnDate);
        TextView tvFoodDetails = complaintCard.findViewById(R.id.tvFoodDetails);
        TextView tvComplaint = complaintCard.findViewById(R.id.tvComplaint);
        TextView tvRaisedBy = complaintCard.findViewById(R.id.tvRaisedBy);

        tvOrderNumber.setText("Order #" + orderNo);
        tvRaisedOnDate.setText("Date: " + raisedOn);
        tvFoodDetails.setText(foodDetails.isEmpty() ? "No food returned." : foodDetails);
        tvComplaint.setText("Complaint: " + complain);
        tvRaisedBy.setText("Raised By: " + raisedBy + " at " + raisedAt);

        complaintsContainer.addView(complaintCard);
    }
    private void filterComplaints(final String query) {
        final String searchQuery = query.toLowerCase().trim();  // Declare a final variable
        if (TextUtils.isEmpty(searchQuery)) {
            loadComplaints();  // Reset to show all complaints if the query is empty
            return;
        }

        complaintsContainer.removeAllViews();

        DatabaseReference complaintsRef = FirebaseDatabase.getInstance()
                .getReference("Restaurants")
                .child(restaurantName)
                .child("Complaints");

        complaintsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot complaintSnapshot : dataSnapshot.getChildren()) {
                    String orderNo = complaintSnapshot.child("order_no").getValue(String.class);
                    if (orderNo != null && orderNo.toLowerCase().contains(searchQuery)) {
                        String raisedOn = complaintSnapshot.child("raised_on").getValue(String.class);
                        String complain = complaintSnapshot.child("complain").getValue(String.class);
                        String raisedBy = complaintSnapshot.child("raised_by").getValue(String.class);
                        String raisedAt = complaintSnapshot.child("raised_at").getValue(String.class);

                        StringBuilder foodDetails = new StringBuilder();
                        for (DataSnapshot foodItem : complaintSnapshot.child("food_returned").getChildren()) {
                            String foodName = foodItem.getKey();
                            Integer qty = foodItem.child("qty").getValue(Integer.class);
                            String unitPrice = foodItem.child("unit_price").getValue(String.class);

                            foodDetails.append(foodName)
                                    .append(" - Qty: ").append(qty)
                                    .append(", Price: £").append(unitPrice)
                                    .append("\n");
                        }

                        addComplaintCard(orderNo, raisedOn, complain, raisedBy, raisedAt, foodDetails.toString().trim());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ViewComplaints.this, "Failed to search complaints.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
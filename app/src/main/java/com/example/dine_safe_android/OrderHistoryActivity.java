package com.example.dine_safe_android;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class OrderHistoryActivity extends AppCompatActivity {
    private String restaurantName;
    private String username, billedBy ;
    private LinearLayout ordersList;
    private FireDetectionManager fireDetectionManager;
    public interface OnUserNameFetchedListener {
        void onUserNameFetched(String name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_history);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        fireDetectionManager = new FireDetectionManager(this);
        fireDetectionManager.checkForFireAndHandle(true);
        ordersList = findViewById(R.id.orders_list);
        TextView tvRestaurantName = findViewById(R.id.tvRestaurantName);
        TextView tvUserName = findViewById(R.id.tvUserName);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        username = sharedPreferences.getString("username", "Username");

        tvRestaurantName.setText(restaurantName);
        tvUserName.setText("Logged in as: " + username);

        loadOrderHistory();
    }

    private void loadOrderHistory() {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference()
                .child("Restaurants")
                .child(restaurantName)
                .child("orders");

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ordersList.removeAllViews();  // Clear previous views

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String status = orderSnapshot.child("status").getValue(String.class);
                    if ("billed".equals(status)) {
                        addOrderCard(orderSnapshot);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(OrderHistoryActivity.this, "Failed to load orders.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addOrderCard(DataSnapshot orderSnapshot) {
        // Inflate the layout for each order card
        View orderCardView = LayoutInflater.from(this).inflate(R.layout.order_card, ordersList, false);

        TextView orderNumberTextView = orderCardView.findViewById(R.id.order_number);
        TextView customersTextView = orderCardView.findViewById(R.id.customers);
        TextView tableNumberTextView = orderCardView.findViewById(R.id.table_number);
        TextView totalPriceTextView = orderCardView.findViewById(R.id.total_price);
        Button makeBillButton = orderCardView.findViewById(R.id.makeBill);

        String orderNumber = orderSnapshot.getKey();
        String noOfCustomers = orderSnapshot.child("no_of_customers").getValue(String.class);
        Integer tableNo = orderSnapshot.child("table_no").getValue(Integer.class);
        Double totalPrice = orderSnapshot.child("total_price").getValue(Double.class);

        // Set order details
        orderNumberTextView.setText("Order #" + orderNumber);
        customersTextView.setText("Customers: " + noOfCustomers);
        tableNumberTextView.setText("Table: " + tableNo);
        totalPriceTextView.setText("Total: £" + totalPrice);

        // Handle makeBill button click
        makeBillButton.setOnClickListener(v -> generatePDF(orderNumber, noOfCustomers, tableNo, totalPrice, orderSnapshot));

        // Add the card to the orders list
        ordersList.addView(orderCardView);
    }
    public void generatePDF(String orderNumber, String noOfCustomers, Integer tableNo, Double totalPrice, DataSnapshot orderSnapshot) {
        // Fetch the billed date, time, and billed_by
        String billedDate = orderSnapshot.child("billed_date").getValue(String.class);
        String billedTime = orderSnapshot.child("billed_time").getValue(String.class);
        String billedByUsername = orderSnapshot.child("billed_by").getValue(String.class);

        fetchUserName(billedByUsername, new OnUserNameFetchedListener() {
            @Override
            public void onUserNameFetched(String billedByName) {
                PdfDocument pdfDocument = new PdfDocument();
                Paint paint = new Paint();
                Paint titlePaint = new Paint();

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Load and draw the logo
                Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo); // Replace with your actual drawable resource name
                int logoWidth = 300; // Set desired width for the logo
                int logoHeight = (int) (logo.getHeight() * ((float) logoWidth / logo.getWidth())); // Maintain aspect ratio
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, true);
                canvas.drawBitmap(scaledLogo, (pageInfo.getPageWidth() - logoWidth) / 2, 40, paint); // Center the logo, 40 for top padding

                // Adjust the Y position for the rest of the content
                int contentStartY = 40 + logoHeight + 40; // 40 for padding below the logo

                // Draw the restaurant name below the logo
                titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                titlePaint.setTextSize(24);
                titlePaint.setColor(Color.BLACK);
                canvas.drawText(restaurantName, (pageInfo.getPageWidth() - titlePaint.measureText(restaurantName)) / 2, contentStartY, titlePaint);

                // Draw the title "Invoice"
                canvas.drawText("Invoice", (pageInfo.getPageWidth() - titlePaint.measureText("Invoice")) / 2, contentStartY + 40, titlePaint);  // Adjust positioning as needed

                paint.setTextSize(14);
                canvas.drawText("Table Number: " + tableNo, 20, contentStartY + 80, paint);
                canvas.drawText("Number of Customers: " + noOfCustomers, 20, contentStartY + 100, paint);
                canvas.drawText("Order Number: " + orderNumber, 20, contentStartY + 120, paint);
                canvas.drawText("Billed Date: " + billedDate, 20, contentStartY + 140, paint);
                canvas.drawText("Billed Time: " + billedTime, 20, contentStartY + 160, paint);
                canvas.drawText("Billed By: " + billedByName, 20, contentStartY + 180, paint);

                int yPosition = contentStartY + 220;
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("Items", 20, yPosition, paint);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

                yPosition += 20;
                for (DataSnapshot foodSnapshot : orderSnapshot.child("food_ordered").getChildren()) {
                    String foodName = foodSnapshot.getKey();
                    Integer qty = foodSnapshot.child("qty").getValue(Integer.class);
                    Double unitPrice = foodSnapshot.child("unit_price").getValue(Double.class);

                    double itemTotal = qty * unitPrice;

                    // Draw food name and quantity
                    canvas.drawText(foodName + " (" + qty + " x £" + unitPrice + ")", 20, yPosition, paint);

                    // Calculate the x-position for right-aligned prices
                    String itemTotalStr = "£" + String.format("%.2f", itemTotal);
                    float priceXPosition = pageInfo.getPageWidth() - 20 - paint.measureText(itemTotalStr);

                    // Draw right-aligned price
                    canvas.drawText(itemTotalStr, priceXPosition, yPosition, paint);

                    yPosition += 20;
                }

                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                String totalAmountStr = "Total Amount: £" + String.format("%.2f", totalPrice);
                float totalPriceXPosition = pageInfo.getPageWidth() - 20 - paint.measureText(totalAmountStr);
                canvas.drawText(totalAmountStr, totalPriceXPosition, yPosition + 40, paint);

                pdfDocument.finishPage(page);

                // Save the document to the Downloads directory
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Order_" + orderNumber + ".pdf");

                try {
                    pdfDocument.writeTo(new FileOutputStream(file));
                    Toast.makeText(OrderHistoryActivity.this, "PDF saved to Downloads folder", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(OrderHistoryActivity.this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                pdfDocument.close();
            }
        });
    }

    private void fetchUserName(String username, final OnUserNameFetchedListener listener) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(username);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    listener.onUserNameFetched(name);  // Pass the name back to the caller
                } else {
                    listener.onUserNameFetched("Unknown");  // Username not found
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onUserNameFetched("Unknown");  // Handle the error scenario
            }
        });
    }
}
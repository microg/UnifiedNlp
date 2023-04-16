package org.microg.nlp.backend.dejavu.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.microg.nlp.backend.dejavu.R;
import com.google.android.material.snackbar.Snackbar;

import org.microg.nlp.backend.dejavu.database.Database;
import org.microg.nlp.backend.dejavu.database.RfEmitter;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView dbNumberOfRows;
    private Button deleteDB;

    private Database db;

    private final static int BACKGROUND_LOCATION_PERMISSION_CODE = 333;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                String message = "Background";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    message += getPackageManager().getBackgroundPermissionOptionLabel();
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(message)
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
                            }
                        })
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Snackbar.make(findViewById(android.R.id.content), "Not granted", Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .create().show();
        } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
        }

        db = new Database(this);

        dbNumberOfRows = findViewById(R.id.db_number_of_rows);
        dbNumberOfRows.setText(String.valueOf(db.getRowsCount()));

        deleteDB = findViewById(R.id.delete_database);
        deleteDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.clearDatabase();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        }

        return super.onOptionsItemSelected(item);
    }
}

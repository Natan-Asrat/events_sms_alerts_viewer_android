package com.emicasolutions.eventsmsviewer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText forwardNameEditText, forwardNumbersEditText;
    private Switch forwardSmsSwitch;
    private Button saveButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize UI elements
        forwardNameEditText = findViewById(R.id.forward_name);
        forwardNumbersEditText = findViewById(R.id.forward_numbers);
        forwardSmsSwitch = findViewById(R.id.forward_sms_switch);
        saveButton = findViewById(R.id.save_button);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);

        // Load existing settings, if available
        loadSettings();

        // Save button click listener
        saveButton.setOnClickListener(v -> saveSettings());
    }

    // Load settings from SharedPreferences
    private void loadSettings() {
        String forwardName = sharedPreferences.getString("forward_name", "");
        String forwardNumbers = sharedPreferences.getString("forward_numbers", "");
        boolean forwardSms = sharedPreferences.getBoolean("forward_sms", false);

        forwardNameEditText.setText(forwardName);
        forwardNumbersEditText.setText(forwardNumbers);
        forwardSmsSwitch.setChecked(forwardSms);
    }

    // Save settings to SharedPreferences
    private void saveSettings() {
        String forwardName = forwardNameEditText.getText().toString();
        String forwardNumbers = forwardNumbersEditText.getText().toString();
        boolean forwardSms = forwardSmsSwitch.isChecked();

        // Validate inputs
        if (forwardName.isEmpty()) {
            Toast.makeText(this, "Please enter a forward name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (forwardNumbers.isEmpty()) {
            Toast.makeText(this, "Please enter forward numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save settings
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("forward_name", forwardName);
        editor.putString("forward_numbers", forwardNumbers);
        editor.putBoolean("forward_sms", forwardSms);
        editor.apply();

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }
}

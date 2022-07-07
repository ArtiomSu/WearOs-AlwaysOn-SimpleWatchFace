package terminal_heat_sink.simplewatchface;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.drawable.DrawableCompat;

import com.madrapps.pikolo.ColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;

public class ColorPickerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_picker);

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                "terminal_heat_sink.simplewatchface_preferences", Context.MODE_PRIVATE);

        ColorPicker colorPicker = findViewById(R.id.colorPicker);

        int currentColor = prefs.getInt("font_color", Color.RED);

        Button back = findViewById(R.id.back_button);
        Drawable buttonDrawable = back.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);
        DrawableCompat.setTint(buttonDrawable, currentColor);
        back.setBackground(buttonDrawable);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        colorPicker.setColor(currentColor);
        colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override
            public void onColorSelected(int color) {
                // Do whatever you want with the color
                prefs.edit().putInt("font_color", color).apply();
            }
        });
        
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

}
package com.jawad.wifihotspotfinder;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

// This is section of the setting page where the details
// about the code is mentioned, i.e the About Page.
public class AboutPage extends AppCompatActivity {

    // The first and only method carried out with sets up
    // the toolbar and layout of the About Section.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}




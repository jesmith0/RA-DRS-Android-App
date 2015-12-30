package com.example.user.residentialacoustics;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Button;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton start_button = (ImageButton) findViewById(R.id.start_button);
        ImageButton order_button = (ImageButton) findViewById(R.id.order_button);

        start_button.setOnClickListener(new OnClickListener() {

            // Called when user clicks the Order button
            public void onClick(View v) {
                try {

                    // Create Intent object for starting simulation activity
                    Intent start_simIntent = new Intent(MainActivity.this, SimActivity.class);
                    startActivity(start_simIntent);

                } catch (Exception e) { Log.e("StartButton", e.toString()); }
            }
        });

        order_button.setOnClickListener(new OnClickListener() {

            // Called when user clicks the Order button
            public void onClick(View v) {
                try {

                    // Create Intent object for starting RA order form in web browser
                    Intent order_webIntent = new Intent(
                            android.content.Intent.ACTION_VIEW,
                            Uri.parse("http://residential-acoustics.com/shop/soundproofing/acousticurtain/"));

                    startActivity(order_webIntent);

                } catch (Exception e) { Log.e("OrderButton", e.toString()); }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

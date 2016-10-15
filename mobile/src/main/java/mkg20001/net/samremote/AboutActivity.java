package mkg20001.net.samremote;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        //Intent intent = getIntent();
        FloatingActionButton fab=(FloatingActionButton) findViewById(R.id.stateFAB);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AboutActivity.this, Remote.class);
                startActivity(intent);
            }
        });

        Drawable draw= ContextCompat.getDrawable(AboutActivity.this,R.drawable.close);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fab.setForeground(draw);
        } else {
            fab.setImageDrawable(draw);
        }
        /*String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        TextView textView = new TextView(this);
        textView.setTextSize(40);
        textView.setText(message);

        ViewGroup layout = (ViewGroup) findViewById(R.id.activity_display_message);
        layout.addView(textView);*/
    }
}

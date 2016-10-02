package mkg20001.net.samremote;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        View.OnClickListener on=new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AboutActivity.this, Remote.class);
                startActivity(intent);
            }
        };
        for (View v:new View[]{findViewById(R.id.toolbar),findViewById(R.id.textView3)}) {
            v.setOnClickListener(on);
        }
    }
}

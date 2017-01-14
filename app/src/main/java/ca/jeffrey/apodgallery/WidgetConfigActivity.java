package ca.jeffrey.apodgallery;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;

/**
 * Created by jeffrey on 2017-01-07.
 */

public class WidgetConfigActivity extends Activity {
    private int widgetId;
    private WidgetConfigActivity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);
        setResult(RESULT_CANCELED);
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
        // initListViews();
        Button b2 = (Button) findViewById(R.id.button2);

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://google.ca"));
                PendingIntent pending = PendingIntent.getActivity(context, 0, intent, 0);
                // views.setOnClickPendingIntent(R.id);
                widgetManager.updateAppWidget(widgetId, views);

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
    }
}

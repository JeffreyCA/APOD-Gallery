package ca.jeffrey.apodgallery;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by jeffrey on 2017-01-07.
 */

public class WidgetConfigActivity extends AppCompatActivity {
    private int widgetId;
    private WidgetConfigActivity context;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_widget_config);

        // Initialize toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);

        if (myToolbar != null) {
            myToolbar.showOverflowMenu();
        }

        myToolbar.setTitle("Widget Configuration");

        setSupportActionBar(myToolbar);

        ListView listView = (ListView) findViewById(android.R.id.list);

        // Defined array values to show in ListView
        String[] values = new String[] { "Check Permissions", "Option 2"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Toast.makeText(WidgetConfigActivity.this, R.string.error_cache, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);

                MenuItem menuItem = menu.findItem(R.id.action_widget_save);
                menuItem.setEnabled(true);
                Drawable drawable = menuItem.getIcon();

                if (drawable != null) {
                    drawable.mutate();
                    drawable.setColorFilter(ContextCompat.getColor(WidgetConfigActivity.this, R.color.colorWhite), PorterDuff
                            .Mode.SRC_ATOP);
                }
            }
        });

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        // final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
        // initListViews();
        // Button b2 = (Button) findViewById(R.id.button2);

        /*
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
        */
    }


    // Inflate options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config_menu, menu);

        // Set colour of share icon to white (from black)
        MenuItem menuItem = menu.findItem(R.id.action_widget_save);
        Drawable drawable = menuItem.getIcon();

        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(ContextCompat.getColor(this, R.color.colorGray), PorterDuff
                    .Mode.SRC_ATOP);
        }

        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu (Menu menu){
        super.onPrepareOptionsMenu(menu);
        this.menu = menu;
        MenuItem menuItem = menu.findItem(R.id.action_widget_save);
        menuItem.setEnabled(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_widget_save:
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                setResult(RESULT_OK, resultValue);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

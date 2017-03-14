package ca.jeffrey.apodgallery.widget;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by jeffrey on 2017-01-20.
 */

public class WidgetItem {
    private String date;

    public WidgetItem(String date) {
        this.date = date;
    }

    public String getFormattedDate() {
        SimpleDateFormat fileNameFormat = new SimpleDateFormat("y-MM-dd");
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("MMM dd, yyyy");
        String formattedDate = "";

        try {
            formattedDate = shortDateFormat.format(fileNameFormat.parse(date));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return formattedDate;
    }

    public String getDate() {
        return date;
    }
}
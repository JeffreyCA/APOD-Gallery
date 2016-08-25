package ca.jeffrey.apodgallery;

/**
 * Taken from http://stackoverflow.com/questions/5033012/auto-scale-textview-text-to-fit-within
 * -bounds/17782522#17782522
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.widget.TextView;

public class AutoResizeTextView extends TextView {

    private static final int NO_LINE_LIMIT = -1;

    private RectF textRect = new RectF();
    private RectF availableSpaceRect;
    private SparseIntArray textCachedSizes;
    private TextPaint paint;
    private float maxTextSize;
    private float spacingMult = 1.0f;
    private float spacingAdd = 0.0f;
    private float minTextSize = 20;
    private int widthLimit;
    private int maxLines;

    private final SizeTester sizeTester = new SizeTester() {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public int onTestSize(int suggestedSize, RectF availableSPace) {
            paint.setTextSize(suggestedSize);
            String text = getText().toString();
            boolean singleline = getMaxLines() == 1;
            if (singleline) {
                textRect.bottom = paint.getFontSpacing();
                textRect.right = paint.measureText(text);
            }
            else {
                StaticLayout layout = new StaticLayout(text, paint, widthLimit, Alignment
                        .ALIGN_NORMAL, spacingMult, spacingAdd, true);
                // return early if we have more lines
                if (getMaxLines() != NO_LINE_LIMIT && layout.getLineCount() > getMaxLines()) {
                    return 1;
                }
                textRect.bottom = layout.getHeight();
                int maxWidth = -1;
                for (int i = 0; i < layout.getLineCount(); i++) {
                    if (maxWidth < layout.getLineWidth(i)) {
                        maxWidth = (int) layout.getLineWidth(i);
                    }
                }
                textRect.right = maxWidth;
            }

            textRect.offsetTo(0, 0);
            if (availableSPace.contains(textRect)) {
                // may be too small, don't worry we will find the best match
                return -1;
            }
            else {
                // too big
                return 1;
            }
        }
    };
    private boolean mEnableSizeCache = true;
    private boolean mInitiallized;

    public AutoResizeTextView(Context context) {
        super(context);
        initialize();
    }

    public AutoResizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private static int binarySearch(int start, int end, SizeTester sizeTester, RectF
            availableSpace) {
        int lastBest = start;
        int lo = start;
        int hi = end - 1;
        int mid = 0;
        while (lo <= hi) {
            mid = (lo + hi) >>> 1;
            int midValCmp = sizeTester.onTestSize(mid, availableSpace);
            if (midValCmp < 0) {
                lastBest = lo;
                lo = mid + 1;
            }
            else if (midValCmp > 0) {
                hi = mid - 1;
                lastBest = hi;
            }
            else {
                return mid;
            }
        }
        // make sure to return last best
        // this is what should always be returned
        return lastBest;

    }

    private void initialize() {
        paint = new TextPaint(getPaint());
        maxTextSize = getTextSize();
        availableSpaceRect = new RectF();
        textCachedSizes = new SparseIntArray();
        if (maxLines == 0) {
            // no value was assigned during construction
            maxLines = NO_LINE_LIMIT;
        }
        mInitiallized = true;
    }

    @Override
    public void setText(final CharSequence text, BufferType type) {
        super.setText(text, type);
        adjustTextSize(text.toString());
    }

    @Override
    public void setTextSize(float size) {
        maxTextSize = size;
        textCachedSizes.clear();
        adjustTextSize(getText().toString());
    }

    public int getMaxLines() {
        return maxLines;
    }

    @Override
    public void setMaxLines(int maxlines) {
        super.setMaxLines(maxlines);
        maxLines = maxlines;
        reAdjust();
    }

    @Override
    public void setSingleLine() {
        super.setSingleLine();
        maxLines = 1;
        reAdjust();
    }

    @Override
    public void setSingleLine(boolean singleLine) {
        super.setSingleLine(singleLine);
        if (singleLine) {
            maxLines = 1;
        }
        else {
            maxLines = NO_LINE_LIMIT;
        }
        reAdjust();
    }

    @Override
    public void setLines(int lines) {
        super.setLines(lines);
        maxLines = lines;
        reAdjust();
    }

    @Override
    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources res;

        if (c == null)
            res = Resources.getSystem();
        else
            res = c.getResources();
        maxTextSize = TypedValue.applyDimension(unit, size, res.getDisplayMetrics());
        textCachedSizes.clear();
        adjustTextSize(getText().toString());
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        spacingMult = mult;
        spacingAdd = add;
    }

    /**
     * Set the lower text size limit and invalidate the view
     *
     * @param minTextSize
     */
    public void setMinTextSize(float minTextSize) {
        this.minTextSize = minTextSize;
        reAdjust();
    }

    private void reAdjust() {
        adjustTextSize(getText().toString());
    }

    private void adjustTextSize(String string) {
        if (!mInitiallized) {
            return;
        }
        int startSize = (int) minTextSize;
        int heightLimit = getMeasuredHeight() - getCompoundPaddingBottom() -
                getCompoundPaddingTop();
        widthLimit = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        availableSpaceRect.right = widthLimit;
        availableSpaceRect.bottom = heightLimit;
        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, efficientTextSizeSearch(startSize, (int)
                maxTextSize, sizeTester, availableSpaceRect));
    }

    /**
     * Enables or disables size caching, enabling it will improve performance
     * where you are animating a value inside TextView. This stores the font
     * size against getText().length() Be careful though while enabling it as 0
     * takes more space than 1 on some fonts and so on.
     *
     * @param enable enable font size caching
     */
    public void enableSizeCache(boolean enable) {
        mEnableSizeCache = enable;
        textCachedSizes.clear();
        adjustTextSize(getText().toString());
    }

    private int efficientTextSizeSearch(int start, int end, SizeTester sizeTester, RectF
            availableSpace) {
        if (!mEnableSizeCache) {
            return binarySearch(start, end, sizeTester, availableSpace);
        }
        String text = getText().toString();
        int key = text == null ? 0 : text.length();
        int size = textCachedSizes.get(key);
        if (size != 0) {
            return size;
        }
        size = binarySearch(start, end, sizeTester, availableSpace);
        textCachedSizes.put(key, size);
        return size;
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before,
                                 final int after) {
        super.onTextChanged(text, start, before, after);
        reAdjust();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        textCachedSizes.clear();
        super.onSizeChanged(width, height, oldwidth, oldheight);
        if (width != oldwidth || height != oldheight) {
            reAdjust();
        }
    }

    private interface SizeTester {
        /**
         * @param suggestedSize  Size of text to be tested
         * @param availableSpace available space in which text must fit
         *
         * @return an integer < 0 if after applying {@code suggestedSize} to
         * text, it takes less space than {@code availableSpace}, > 0
         * otherwise
         */
        int onTestSize(int suggestedSize, RectF availableSpace);
    }
}
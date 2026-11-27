package com.koc.hatirlatici;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

/** Sözü saydam bir katman olarak çizer; arka plan (resim/video/animasyon) bunun altında kalır. */
public class QuoteView extends View {
    final String text;
    final String author;
    Bitmap layer;

    public QuoteView(Context c, String text, String author) {
        super(c);
        this.text = text;
        this.author = author;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w <= 0 || h <= 0) return;
        if (layer != null) layer.recycle();
        layer = Wallpaper.quoteLayer(text, author, w, h);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (layer != null) c.drawBitmap(layer, 0, 0, null);
    }
}

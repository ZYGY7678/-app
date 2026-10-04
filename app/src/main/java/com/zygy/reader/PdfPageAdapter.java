package com.zygy.reader;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.Holder> {
    private final PdfRenderer renderer;
    private final int pageBackground;

    public PdfPageAdapter(PdfRenderer renderer, int pageBackground) {
        this.renderer = renderer;
        this.pageBackground = pageBackground;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView image = new ImageView(parent.getContext());
        image.setBackgroundColor(Color.rgb(45, 45, 45));
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(dp(parent, 5), dp(parent, 9), dp(parent, 5), dp(parent, 9));
        return new Holder(image);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        try {
            synchronized (renderer) {
                PdfRenderer.Page page = renderer.openPage(position);
                int width = Math.max(600,
                        holder.image.getResources().getDisplayMetrics().widthPixels - dp(holder.image, 10));
                float ratio = (float) page.getHeight() / Math.max(1, page.getWidth());
                int height = Math.max(800, (int) (width * ratio));
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(pageBackground);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                holder.image.setImageBitmap(bitmap);
            }
        } catch (Exception ignored) {
            holder.image.setImageDrawable(null);
        }
    }

    @Override
    public int getItemCount() {
        return renderer.getPageCount();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView image;
        Holder(ImageView image) {
            super(image);
            this.image = image;
        }
    }

    private static int dp(ViewGroup v, int x) {
        return (int) (x * v.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static int dp(ImageView v, int x) {
        return (int) (x * v.getResources().getDisplayMetrics().density + 0.5f);
    }
}

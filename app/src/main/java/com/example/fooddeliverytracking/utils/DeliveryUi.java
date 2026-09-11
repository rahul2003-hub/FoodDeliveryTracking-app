package com.example.fooddeliverytracking.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.models.Order;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Locale;

public final class DeliveryUi {
    private DeliveryUi() { }

    public static int statusLabel(String status) {
        if (Constants.STATUS_DELIVERED.equals(status)) return R.string.delivered;
        if (Constants.STATUS_OUT_FOR_DELIVERY.equals(status)) return R.string.out_for_delivery_label;
        if (Constants.STATUS_PICKED_UP.equals(status)) return R.string.picked_up;
        if (Constants.STATUS_ACCEPTED.equals(status)) return R.string.accepted;
        return R.string.confirmed;
    }

    public static String arrival(android.content.Context context, Order order) {
        if (Constants.STATUS_DELIVERED.equals(order.getStatus())) return context.getString(R.string.delivered);
        Long arrival = order.getEstimatedArrivalAt();
        if (arrival == null || arrival <= System.currentTimeMillis()) return context.getString(R.string.unknown_value);
        long minutes = (arrival - System.currentTimeMillis() + 59999) / 60000;
        return context.getString(R.string.eta_minutes, minutes);
    }

    public static void badge(TextView view, boolean success) {
        int color = ContextCompat.getColor(view.getContext(), success ? R.color.accent_green : R.color.primary_orange);
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(view.getContext(), success ? R.color.success_surface : R.color.primary_orange_light));
        background.setCornerRadius(24 * view.getResources().getDisplayMetrics().density);
        view.setBackground(background);
        view.setTextColor(color);
    }

    public static void avatar(TextView view, String name) {
        String initials = "";
        if (name != null && !name.trim().isEmpty()) {
            String[] words = name.trim().split("\\s+");
            initials = words[0].substring(0, 1);
            if (words.length > 1) initials += words[words.length - 1].substring(0, 1);
        }
        view.setText(initials.toUpperCase(Locale.getDefault()));
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(ContextCompat.getColor(view.getContext(), R.color.text_secondary));
        view.setBackground(background);
    }

    public static void progress(View root, String status, boolean driver) {
        boolean picked = Constants.STATUS_PICKED_UP.equals(status) || Constants.STATUS_OUT_FOR_DELIVERY.equals(status)
                || Constants.STATUS_DELIVERED.equals(status);
        boolean delivered = Constants.STATUS_DELIVERED.equals(status);
        int step = delivered ? 2 : picked ? 1 : 0;
        int[] ids = {R.id.stepOne, R.id.stepTwo, R.id.stepThree};
        for (int i = 0; i < ids.length; i++) {
            TextView circle = root.findViewById(ids[i]);
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            int orange = ContextCompat.getColor(root.getContext(), R.color.primary_orange);
            background.setColor(ContextCompat.getColor(root.getContext(), i <= step ? R.color.primary_orange : R.color.muted_surface));
            background.setStroke(2, i <= step ? orange : ContextCompat.getColor(root.getContext(), R.color.text_secondary));
            circle.setBackground(background);
            circle.setText(i <= step ? root.getContext().getString(R.string.check_mark) : "");
        }
        ((TextView) root.findViewById(R.id.stepOneLabel)).setText(driver ? R.string.accepted : R.string.confirmed);
        root.findViewById(R.id.stepLineOne).setBackgroundColor(ContextCompat.getColor(root.getContext(), picked ? R.color.primary_orange : R.color.outline));
        root.findViewById(R.id.stepLineTwo).setBackgroundColor(ContextCompat.getColor(root.getContext(), delivered ? R.color.primary_orange : R.color.outline));
    }

    public static void contact(Activity activity, View button, String uid) {
        button.setEnabled(false);
        button.setAlpha(0.35f);
        button.setContentDescription(activity.getString(R.string.phone_unavailable));
        if (uid == null || uid.isEmpty()) return;
        FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS).child(uid).child("phone").get()
                .addOnSuccessListener(snapshot -> {
                    if (activity.isDestroyed()) return;
                    String phone = snapshot.getValue(String.class);
                    if (phone == null || phone.trim().isEmpty()) return;
                    button.setEnabled(true);
                    button.setAlpha(1f);
                    button.setContentDescription(activity.getString(R.string.call_contact));
                    button.setOnClickListener(v -> {
                        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                        try { activity.startActivity(dial); }
                        catch (android.content.ActivityNotFoundException ignored) { }
                    });
                });
    }

    public static BitmapDescriptor marker(android.content.Context context, int icon, String label) {
        float density = context.getResources().getDisplayMetrics().density;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(11 * density);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        String text = label == null ? "" : label;
        if (text.length() > 24) text = text.substring(0, 23) + "…";
        int width = (int) Math.max(44 * density, paint.measureText(text) + 20 * density);
        Bitmap bitmap = Bitmap.createBitmap(width, (int) (62 * density), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        paint.setColor(android.graphics.Color.WHITE);
        canvas.drawRoundRect(new RectF(0, 0, width, 58 * density), 9 * density, 9 * density, paint);
        Drawable drawable = ContextCompat.getDrawable(context, icon);
        if (drawable != null) {
            int left = (width - (int) (30 * density)) / 2;
            drawable.setBounds(left, (int) (3 * density), left + (int) (30 * density), (int) (33 * density));
            drawable.draw(canvas);
        }
        paint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, width / 2f, 48 * density, paint);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}

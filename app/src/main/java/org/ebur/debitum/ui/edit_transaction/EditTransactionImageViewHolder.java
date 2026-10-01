package org.ebur.debitum.ui.edit_transaction;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.ebur.debitum.R;

import java.io.File;

class EditTransactionImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private static final String TAG = "EditTransactionImageViewHolder";

    private final ImageView imgView;
    private final View checkmarkView;
    private final Drawable placeholderDrawable;
    private final AddImageCallback addImageCallback;
    private final DeleteImageCallback deleteCallback;
    @Nullable private File imageFile;

    private ActionMode actionMode;

    private EditTransactionImageViewHolder(View itemView, AddImageCallback addImageCallback, DeleteImageCallback deleteCallback) {
        super(itemView);
        imgView = itemView.findViewById(R.id.image);
        checkmarkView = itemView.findViewById(R.id.image_selected_checkmark);

        this.addImageCallback = addImageCallback;
        this.deleteCallback = deleteCallback;

        Context context = itemView.getContext();
        placeholderDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_add_photo_64);
    }

    public void bind(@Nullable File imageFile) {
        this.imageFile = imageFile;
        checkmarkView.setVisibility(View.INVISIBLE);
        if (imageFile != null) {
            imgView.setImageBitmap(decodeSampledBitmap(imageFile, imgView));
            itemView.setOnClickListener(view -> {
                showImage();
            });
            itemView.setOnLongClickListener(view -> {
                if(itemView.isSelected()) {
                    actionMode.finish();
                    return true;
                } else {
                    if (actionMode != null) {
                        return false;
                    }
                    actionMode = itemView.startActionMode(actionModeCallback);
                    return true;
                }
            });
        } else {
            imgView.setImageDrawable(placeholderDrawable);
            // the placeholder offers both sources (gallery and camera), so it delegates the
            // choice to the fragment which then presents the respective launcher
            itemView.setOnClickListener(view -> addImage());
            itemView.setOnLongClickListener(view -> {
                addImage();
                return true;
            });
        }
    }

    /**
     * Decodes a downsampled version of the image for display in the thumbnail.
     * ImageView.setImageURI() would decode the full bitmap, which for a multi-megapixel camera
     * photo would need tens of megabytes of heap and crash on low-memory devices.
     *
     * @return the decoded thumbnail, or null if the image could not be decoded
     */
    @Nullable
    private static Bitmap decodeSampledBitmap(@NonNull File imageFile, @NonNull ImageView imageView) {
        try {
            // first pass: read the image dimensions only
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bounds);

            int reqWidth = imageView.getWidth() > 0 ? imageView.getWidth() : imageView.getLayoutParams().width;
            int reqHeight = imageView.getHeight() > 0 ? imageView.getHeight() : imageView.getLayoutParams().height;

            // second pass: decode with a sample size that keeps the result close to the
            // requested thumbnail size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight);
            return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        } catch (OutOfMemoryError e) {
            // getMessage() may be null here, so it must not be passed to Log.e unguarded
            Log.e(TAG, "Out of memory while decoding image thumbnail");
            return null;
        }
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (reqWidth <= 0 || reqHeight <= 0 || width <= 0 || height <= 0) {
            return inSampleSize;
        }
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        while (halfWidth / inSampleSize >= reqWidth && halfHeight / inSampleSize >= reqHeight) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    private void showImage() {
        if (imageFile != null) {
            Context context = imgView.getContext();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // using the file:// uri directly would cause a android.os.FileUriExposedException
            Uri contentUri = FileProvider.getUriForFile(context, fileProviderAuthority(context), imageFile);
            intent.setData(contentUri);
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(itemView.getContext(),
                        itemView.getResources().getString(R.string.edit_transaction_image_error_app_not_found),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void addImage() {
        addImageCallback.onAddImage();
    }

    private void deleteImage() {
         if (imageFile != null) deleteCallback.onDelete(imageFile);
    }

    static EditTransactionImageViewHolder create(ViewGroup parent,
                                                 AddImageCallback addImageCallback,
                                                 DeleteImageCallback deleteCallback) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_list, parent, false);
        return new EditTransactionImageViewHolder(view, addImageCallback, deleteCallback);
    }

    // the FileProvider authority is declared as ${applicationId}.fileprovider, so it has to be
    // derived from the runtime package name rather than hardcoded (the debug build uses a
    // different applicationId suffix)
    @NonNull
    static String fileProviderAuthority(@NonNull Context context) {
        return context.getPackageName() + ".fileprovider";
    }

    // without having an onClickListener the view will not get the pressed state when clicked
    // and thus won't reflect the backgroundTint change from list_item_bg_selector.xml
    @Override
    public void onClick(View v) {
    }



    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_edit_transaction_image, menu);
            itemView.setSelected(true);
            checkmarkView.setVisibility(View.VISIBLE);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(R.string.edit_transaction_actionmode_title);
            return true; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.miDeleteImage) {
                deleteImage();
                mode.finish(); // Action picked, so close the CAB
                return true;
            } else {
                return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            itemView.setSelected(false);
            checkmarkView.setVisibility(View.INVISIBLE);
        }
    };

    // this must be passed by the fragment using the adapter/viewHolder to handle image deletion
    public interface DeleteImageCallback {
        void onDelete(@NonNull File imagefile);
    }

    // this must be passed by the fragment using the adapter/viewHolder to handle image creation,
    // which requires an ActivityResultLauncher that only the fragment can register
    public interface AddImageCallback {
        void onAddImage();
    }

}


package org.ebur.debitum.ui.edit_transaction;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.io.File;

public class EditTransactionImageAdapter
        extends ListAdapter<File, EditTransactionImageViewHolder> {

    private final EditTransactionImageViewHolder.AddImageCallback addImageCallback;
    private final EditTransactionImageViewHolder.DeleteImageCallback deleteCallback;

    public EditTransactionImageAdapter(@NonNull DiffUtil.ItemCallback<File> diffCallback,
                                       @NonNull EditTransactionImageViewHolder.AddImageCallback addImageCallback,
                                       @NonNull EditTransactionImageViewHolder.DeleteImageCallback deleteCallback) {
        super(diffCallback);
        this.addImageCallback = addImageCallback;
        this.deleteCallback = deleteCallback;
        //setHasStableIds(true);
    }

    @NonNull
    @Override
    public EditTransactionImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return EditTransactionImageViewHolder.create(parent, addImageCallback, deleteCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull EditTransactionImageViewHolder holder, int position) {
        File current = getItem(position);
        holder.bind(current);
    }

    /*@Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }*/

    static class Diff extends DiffUtil.ItemCallback<File> {

        // the last element of the list is a null placeholder for adding images, so both callbacks
        // have to tolerate null values
        @Override
        public boolean areItemsTheSame(@Nullable File oldItem, @Nullable File newItem) {
            if (oldItem == null || newItem == null) return oldItem == null && newItem == null;
            return oldItem.getName().equals(newItem.getName());
        }

        // image filenames are unique and immutable while the transaction is being edited, so
        // comparing name and length is sufficient. Deliberately not comparing file contents,
        // which would read whole (potentially multi-megabyte) camera images on every list update.
        @Override
        public boolean areContentsTheSame(@Nullable File oldItem, @Nullable File newItem) {
            if (oldItem == null || newItem == null) return oldItem == null && newItem == null;
            return oldItem.getName().equals(newItem.getName()) && oldItem.length() == newItem.length();
        }
    }
}

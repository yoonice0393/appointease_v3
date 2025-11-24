package com.example.sttherese.adapters;

/**
 * Interface to communicate the data count changes from a RecyclerView Adapter
 * back to the hosting Activity/Fragment for managing UI elements like empty states.
 */
public interface OnItemCountChangeListener {

    void onLoadMore();

    /**
     * Called by the adapter whenever the data set changes.
     * @param count The current number of items in the adapter.
     */
    void onCountChange(int count);
}
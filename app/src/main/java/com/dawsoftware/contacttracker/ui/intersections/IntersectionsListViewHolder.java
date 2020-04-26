package com.dawsoftware.contacttracker.ui.intersections;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.dawsoftware.contacttracker.R;

public class IntersectionsListViewHolder extends ViewHolder implements OnClickListener {
	
	public ImageView icon;
	public TextView time;
	public TextView status;
	public TextView lat;
	public TextView lng;
	
	public OnHolderClickListener listener;
	
	public IntersectionsListViewHolder(@NonNull final View itemView, final OnHolderClickListener listener) {
		super(itemView);
		
		this.listener = listener;
		
		icon = itemView.findViewById(R.id.intersection_item_icon);
		time = itemView.findViewById(R.id.intersection_item_time);
		status = itemView.findViewById(R.id.intersection_item_status);
		
		lat = itemView.findViewById(R.id.intersection_item_lat);
		lat.setOnClickListener(this);
		
		lng = itemView.findViewById(R.id.intersection_item_lng);
		lng.setOnClickListener(this);
	}
	
	@Override
	public void onClick(final View v) {
		if (listener != null) {
			listener.onItemClicked(getAdapterPosition());
		}
	}
	
	public interface OnHolderClickListener {
		void onItemClicked(int position);
	}
}

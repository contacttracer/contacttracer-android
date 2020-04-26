package com.dawsoftware.contacttracker.ui.survey;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.dawsoftware.contacttracker.R;

public class SymptomViewHolder extends ViewHolder implements OnClickListener {
	
	private CheckedListener listener;
	
	public ImageView icon;
	public TextView title;
	public TextView text;
	public CheckBox checkBox;
	
	public SymptomViewHolder(@NonNull final View itemView, CheckedListener listener) {
		super(itemView);
		
		this.listener = listener;
		
		icon = itemView.findViewById(R.id.symptom_item_icon);
		title = itemView.findViewById(R.id.symptom_item_title);
		text = itemView.findViewById(R.id.symptom_item_text);
		checkBox = itemView.findViewById(R.id.symptom_item_checkbox);
		
		checkBox.setOnClickListener(this);
	}
	
	@Override
	public void onClick(final View v) {
		if (listener != null) {
			listener.checkedChanged(checkBox.isChecked(), getAdapterPosition());
		}
	}
	
	public interface CheckedListener {
		void checkedChanged(boolean isChecked, int position);
	}
}

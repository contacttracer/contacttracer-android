package com.dawsoftware.contacttracker.ui.survey;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;

import androidx.annotation.NonNull;
import androidx.core.widget.CompoundButtonCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.ui.main.MainFragmentViewModel;
import com.dawsoftware.contacttracker.ui.survey.SymptomViewHolder.CheckedListener;

public class SymptomsAdapter extends RecyclerView.Adapter<SymptomViewHolder> implements CheckedListener {
	
	private static final int VIEW_TYPE_DEFAULT = 0;
	
	public static final String NOTHING_SELECTED = "nothing";
	
	private final Context context;
	
	private String mode;
	private boolean isActive;
	private String userSelectionStatus = NOTHING_SELECTED;
	private boolean isSharedChecked = true;
	
	private int[] iconsWell = {R.drawable.ic_thermometer,
	                       R.drawable.ic_cough,
	                       R.drawable.ic_breath,
	                       R.drawable.ic_share_black_24dp};
	
	private int[] titlesWell = {R.string.fever_title,
	                        R.string.cough_title,
	                        R.string.breath_title,
	                        R.string.share_title};
	
	private int[] descriptionsWell = {R.string.fever_description,
	                              R.string.cough_description,
	                              R.string.breath_description,
	                              R.string.share_description};
	
	private int[] iconsInfected = {R.drawable.ic_hotel,
	                           R.drawable.ic_share_black_24dp};
	
	private int[] titlesInfected = {R.string.sick_status,
	                                R.string.share_title};
	
	private int[] descriptionsInfected = {R.string.sick_status_text,
	                                      R.string.share_description};
	
	private int[] iconsSick = {R.drawable.ic_sentiment_satisfied_black_24dp,
	                           R.drawable.ic_share_black_24dp};
	
	private int[] titlesSick = {R.string.well_status,
	                            R.string.share_title};
	
	private int[] descriptionsSick = {R.string.well_status_text,
	                                  R.string.share_description};
	
	private HashSet<Integer> checkedIndexes;
	
	private ArrayList<String> selectedSymptoms;
	
	public SymptomsAdapter(final String mode, final Context context, final boolean isActive) {
		checkedIndexes = new HashSet<>();
		selectedSymptoms = new ArrayList<>();
		
		this.context = context;
		this.mode = mode;
		this.isActive = isActive;
	}
	
	private int getIconDrawable(int position) {
		int result;
		
		switch (mode) {
			default:
			case MainFragmentViewModel.STATUS_HEALTHY: {
				result = iconsWell[position];
				break;
			}
			
			case MainFragmentViewModel.STATUS_INFECTED: {
				result = iconsInfected[position];
				break;
			}
			
			case MainFragmentViewModel.STATUS_SICK: {
				result = iconsSick[position];
				break;
			}
		}
		
		return result;
	}
	
	private int getTitle(int position) {
		int result;
		
		switch (mode) {
			default:
			case MainFragmentViewModel.STATUS_HEALTHY: {
				result = titlesWell[position];
				break;
			}
			
			case MainFragmentViewModel.STATUS_INFECTED: {
				result = titlesInfected[position];
				break;
			}
			
			case MainFragmentViewModel.STATUS_SICK: {
				result = titlesSick[position];
				break;
			}
		}
		
		return result;
	}
	
	private int getText(int position) {
		int result;
		
		switch (mode) {
			default:
			case MainFragmentViewModel.STATUS_HEALTHY: {
				result = descriptionsWell[position];
				break;
			}
			
			case MainFragmentViewModel.STATUS_INFECTED: {
				result = descriptionsInfected[position];
				break;
			}
			
			case MainFragmentViewModel.STATUS_SICK: {
				result = descriptionsSick[position];
				break;
			}
		}
		
		return result;
	}
	
	private int getListSize() {
		int result;
		
		switch (mode) {
			default:
			case MainFragmentViewModel.STATUS_HEALTHY: {
				result = iconsWell.length;
				break;
			}
			
			case MainFragmentViewModel.STATUS_INFECTED: {
				result = iconsInfected.length;
				break;
			}
			
			case MainFragmentViewModel.STATUS_SICK: {
				result = iconsSick.length;
				break;
			}
		}
		
		return result;
	}
	
	private int getCheckboxColor() {
		if (isActive) {
			switch (mode) {
				case MainFragmentViewModel.STATUS_HEALTHY: {
					return context.getResources()
					              .getColor(R.color.status_healthy_background_end);
				}
				case MainFragmentViewModel.STATUS_INFECTED: {
					return context.getResources()
					              .getColor(R.color.status_potentially_sick_background_end);
				}
				case MainFragmentViewModel.STATUS_SICK: {
					return context.getResources()
					              .getColor(R.color.status_sick_background_end);
				}
				default: {
					return context.getResources()
					              .getColor(R.color.unavailable_gray);
				}
			}
		} else {
			return context.getResources()
			              .getColor(R.color.unavailable_gray);
		}
	}
	
	@Override
	public int getItemViewType(final int position) {
		return VIEW_TYPE_DEFAULT;
	}
	
	@NonNull
	@Override
	public SymptomViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		final Context context = parent.getContext();
		final LayoutInflater inflater = LayoutInflater.from(context);
		
		SymptomViewHolder viewHolder;
		
		switch (viewType) {
			default:
			case VIEW_TYPE_DEFAULT: {
				final View view = inflater.inflate(R.layout.layout_symptom_item, parent, false);
				viewHolder = new SymptomViewHolder(view, this);
			}
		}
		
		return viewHolder;
	}
	
	@Override
	public void onBindViewHolder(@NonNull final SymptomViewHolder holder, final int position) {
		final Resources res = holder.itemView.getResources();
		
		holder.icon.setImageDrawable(res.getDrawable(getIconDrawable(position)));
		holder.title.setText(res.getString(getTitle(position)));
		holder.text.setText(res.getString(getText(position)));
		
		holder.checkBox.setChecked(checkedIndexes.contains(position));
		
		if (position == getItemCount()-1) {
			holder.checkBox.setChecked(isSharedChecked);
		}
		
		ColorFilter colorFilter = new PorterDuffColorFilter(getCheckboxColor(), PorterDuff.Mode.SRC_ATOP);
		Drawable drawable = CompoundButtonCompat.getButtonDrawable(holder.checkBox);
		if (drawable != null) {
			drawable.setColorFilter(colorFilter);
		}
	}
	
	@Override
	public int getItemCount() {
		return getListSize();
	}
	
	@Override
	public void checkedChanged(final boolean isChecked, final int position) {
		
		if (isChecked) {
			checkedIndexes.add(position);
		} else {
			checkedIndexes.remove(position);
		}
		
		if (position == getItemCount()-1) {
			isSharedChecked = isChecked;
			return;
		}
		
		switch (mode) {
			case MainFragmentViewModel.STATUS_HEALTHY: {
				if (isChecked) {
					userSelectionStatus = MainFragmentViewModel.STATUS_INFECTED;
					addSelectedSymptom(position);
				} else {
					int sizeModifier = isSharedChecked ? 1 : 0;
					if (position < getItemCount()-1) {
						if (checkedIndexes.size() - sizeModifier <= 0) {
							userSelectionStatus = MainFragmentViewModel.STATUS_HEALTHY;
						}
						removeUnelectedSymptom(position);
					}
				}
				break;
			}
			
			case MainFragmentViewModel.STATUS_INFECTED: {
				if (isChecked) {
					userSelectionStatus = MainFragmentViewModel.STATUS_SICK;
				} else {
					userSelectionStatus = MainFragmentViewModel.STATUS_HEALTHY;
				}
				break;
			}
			
			default:
			case MainFragmentViewModel.STATUS_SICK: {
				if (isChecked) {
					userSelectionStatus = MainFragmentViewModel.STATUS_HEALTHY;
				} else {
					userSelectionStatus = MainFragmentViewModel.STATUS_SICK;
				}
				break;
			}
		}
		
		Log.i("wuttt", "user selects: " + userSelectionStatus);
	}
	
	private void addSelectedSymptom(int position) {
		switch (position) {
			case 0: {
				selectedSymptoms.add(MainFragmentViewModel.SYMPTOM_FEVER);
				break;
			}
			
			case 1: {
				selectedSymptoms.add(MainFragmentViewModel.SYMPTOM_COUGH);
				break;
			}
			
			case 2: {
				selectedSymptoms.add(MainFragmentViewModel.SYMPTOM_DISPNEA);
				break;
			}
		}
		
		Log.i("wutt", "Symptoms: " + selectedSymptoms.toString());
	}
	
	private void removeUnelectedSymptom(int position) {
		switch (position) {
			case 0: {
				selectedSymptoms.remove(MainFragmentViewModel.SYMPTOM_FEVER);
				break;
			}
			
			case 1: {
				selectedSymptoms.remove(MainFragmentViewModel.SYMPTOM_COUGH);
				break;
			}
			
			case 2: {
				selectedSymptoms.remove(MainFragmentViewModel.SYMPTOM_DISPNEA);
				break;
			}
		}
		
		Log.i("wutt", "Symptoms: " + selectedSymptoms.toString());
	}
	
	public String getUserSelectedStatus() {
		return userSelectionStatus;
	}
	
	public ArrayList<String> getSelectedSymptoms() {
		return selectedSymptoms;
	}
	
	public boolean getIsSharedChecked() {
		return isSharedChecked;
	}
}

package com.dawsoftware.contacttracker.ui.intersections;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.data.network.models.Intersection;
import com.dawsoftware.contacttracker.ui.intersections.IntersectionsListFragment.CoordinateClickListener;
import com.dawsoftware.contacttracker.ui.intersections.IntersectionsListViewHolder.OnHolderClickListener;
import com.dawsoftware.contacttracker.ui.main.MainFragmentViewModel;
import com.dawsoftware.contacttracker.util.StringUtil;
import com.dawsoftware.contacttracker.util.ViewUtil;

public class IntersectionsListAdapter extends RecyclerView.Adapter<ViewHolder> implements OnHolderClickListener {
	
	private static final int VIEW_TYPE_DEFAULT = 0;
	private static final int VIEW_TYPE_DIVIDER = 1;
	
	private List<Intersection> list;
	
	private Context context;
	
	private String hourString;
	private String minuteString;
	private String dayString;
	
	private String infectedStatusString;
	private String sickStatusString;
	
	private int dividerPosition = -1;
	
	private final CoordinateClickListener mCoordinateClickListener;
	
	public IntersectionsListAdapter(final Context context, final CoordinateClickListener coordinateClickListener) {
		this.context = context;
		
		mCoordinateClickListener = coordinateClickListener;
		
		list = new ArrayList<>();
		
		prepareResources();
	}
	
	private void prepareResources() {
		final Resources res = context.getResources();
		
		hourString = res.getString(R.string.hours_short);
		minuteString = res.getString(R.string.minutes_short);
		dayString = res.getString(R.string.days_short);
		
		infectedStatusString = res.getString(R.string.intersections_list_status_infected);
		sickStatusString = res.getString(R.string.intersections_list_status_sick);
	}
	
	@Override
	public int getItemViewType(final int position) {
		if (dividerPosition > 0 && dividerPosition == position) {
			return VIEW_TYPE_DIVIDER;
		}
		
		return VIEW_TYPE_DEFAULT;
	}
	
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		final Context context = parent.getContext();
		final LayoutInflater inflater = LayoutInflater.from(context);
		
		ViewHolder viewHolder;
		
		switch (viewType) {
			case VIEW_TYPE_DIVIDER: {
				final View view = inflater.inflate(R.layout.layout_intersection_item_divider, parent, false);
				viewHolder = new DividerViewHolder(view);
				break;
			}
			
			default:
			case VIEW_TYPE_DEFAULT: {
				final View view = inflater.inflate(R.layout.layout_intersection_item, parent, false);
				viewHolder = new IntersectionsListViewHolder(view, this);
				break;
			}
		}
		
		return viewHolder;
	}
	
	@Override
	public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int position) {
		
		if (list.size() == 0) {
			return;
		}
		
		if (viewHolder instanceof DividerViewHolder) {
			return;
		}
		
		IntersectionsListViewHolder holder = (IntersectionsListViewHolder) viewHolder;
		
		int itemPosition = 0;
		
		if (dividerPosition > 0 && dividerPosition < getItemCount()) {
			itemPosition = position > dividerPosition ? position - 1 : position;
		} else {
			itemPosition = position;
		}
		
		final Intersection item = list.get(itemPosition);
		
		if (item.status.equals(MainFragmentViewModel.STATUS_INFECTED)) {
			ViewUtil.setTint(context, holder.icon, R.color.infected_yellow);
			holder.icon.setImageResource(R.drawable.ic_sentiment_neutral_black_24dp);
		} else {
			ViewUtil.setTint(context, holder.icon, R.color.sick_red);
			holder.icon.setImageResource(R.drawable.ic_sentiment_dissatisfied_black_24dp);
		}
		
		holder.time.setText(unixtimeToTimeAgo(item.time));
		holder.status.setText(convertStatus(item.status));
		
		holder.lat.setText(String.format(Locale.US, "%.7f", item.lat) + ", ");
		holder.lng.setText(String.format(Locale.US,"%.7f", item.lng));
		
		holder.listener = this;
	}
	
	private String unixtimeToTimeAgo(long unixtime) {
		long diff = unixtime <= 0 ? 0 : TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - unixtime;
		
		if (diff <= 0) {
			return "0 " + minuteString;
		}
		
		long days = TimeUnit.SECONDS.toDays(diff);
		long hours = TimeUnit.SECONDS.toHours(diff)%24;
		long minutes = TimeUnit.SECONDS.toMinutes(diff)%60;
		
		StringBuilder sb = new StringBuilder();
		
		if (days > 0) {
			sb.append(days);
			sb.append(" ");
			sb.append(dayString);
			sb.append(" ");
		}
		
		if (hours > 0) {
			sb.append(hours);
			sb.append(" ");
			sb.append(hourString);
			sb.append(" ");
		} else {
			if (days > 0) {
				sb.append(hours);
				sb.append(" ");
				sb.append(hourString);
				sb.append(" ");
			}
		}
		
		if (minutes >= 0) {
			sb.append(minutes);
			sb.append(" ");
			sb.append(minuteString);
		}
		
		sb.append("");
		
		return sb.toString();
	}
	
	private String convertStatus(final String itemStatus) {
		if (StringUtil.isEmpty(itemStatus)) {
			return "";
		}
		
		switch (itemStatus) {
			case MainFragmentViewModel.STATUS_INFECTED: {
				return infectedStatusString;
			}
			case MainFragmentViewModel.STATUS_SICK: {
				return sickStatusString;
			}
			default: {
				return "";
			}
		}
	}
	
	@Override
	public int getItemCount() {
		return dividerPosition > 0 ? list.size() + 1 : list.size();
	}
	
	public void setListOfIntersections(final List<Intersection> list) {
		if (list != null) {
			
			// for screenshoting
//			addMockedData(list);
			
			this.list = list;
			notifyDataSetChanged();
		}
	}
	
	// for screenshoting
//	private void addMockedData(final List<Intersection> list) {
//		// for screenshoting
//		long ut = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
//		list.clear();
//		list.add(new Intersection("possible_infected", ut-TimeUnit.MINUTES.toSeconds(14), "1", 55.821491f, 37.641791f));
//		list.add(new Intersection("possible_infected", ut-TimeUnit.MINUTES.toSeconds(25), "2", 55.812023f, 37.625011f));
//		list.add(new Intersection("infected", ut-TimeUnit.MINUTES.toSeconds(69), "3", 55.818446f, 37.603583f));
//		list.add(new Intersection("possible_infected", ut-TimeUnit.MINUTES.toSeconds(62*15), "4", 55.845552f, 37.640876f));
//		list.add(new Intersection("infected", ut-TimeUnit.MINUTES.toSeconds(63*24), "5", 55.805164f, 37.514774f));
//		list.add(new Intersection("possible_infected", ut-TimeUnit.MINUTES.toSeconds(62*24*2), "6", 55.751621f, 37.626499f));
//	}
	
	@Override
	public void onItemClicked(final int position) {
		if (position >= 0 && position != dividerPosition) {
			if (list.size() > position) {
				Intersection item = list.get(position);
				if (item != null && mCoordinateClickListener != null) {
					mCoordinateClickListener.onCoordinateClicked(item.lat, item.lng);
				}
			}
		}
	}

	public void setDividerPosition(int position) {
		if (position > 0 && position < getItemCount() - 1) {
			dividerPosition = position;
			notifyItemInserted(dividerPosition);
		} else {
			notifyItemRemoved(position);
			dividerPosition = -1;
		}
	}
}

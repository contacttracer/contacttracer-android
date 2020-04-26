package com.dawsoftware.contacttracker.ui.intersections;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dawsoftware.contacttracker.MainActivity;
import com.dawsoftware.contacttracker.MainActivityViewModel;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.analytics.Analytics;
import com.dawsoftware.contacttracker.ui.main.MainFragmentViewModel;
import com.dawsoftware.contacttracker.util.IntentUtil;

public class IntersectionsListFragment extends Fragment {
	
	private MainFragmentViewModel mMainFragmentViewModel;
	private MainActivityViewModel mMainActivityViewModel;
	
	private RecyclerView recycler;
	private IntersectionsListAdapter adapter;
	private MapInvoker mMapInvoker;
	
	private FrameLayout noDataPlaceholder;
	
	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mMapInvoker = new MapInvoker();
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
	                         @Nullable final Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_intersections_list, container, false);
		
		initViewModels();
		initViews(view);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Analytics.intersectionsScreenOpened(getActivity());
		
		boolean showBadge = mMainActivityViewModel.getMustShowBottomBadge().getValue();
		
		if (showBadge) {
			mMainActivityViewModel.getMustShowBottomBadge().setValue(false);
		}
		
		if (adapter.getItemCount() > 0) {
			recycler.setVisibility(View.VISIBLE);
			noDataPlaceholder.setVisibility(View.INVISIBLE);
		} else {
			recycler.setVisibility(View.INVISIBLE);
			noDataPlaceholder.setVisibility(View.VISIBLE);
		}
		
		mMainFragmentViewModel.getAllIntersectionsFromDb();
	}
	
	private void initViewModels() {
		final MainActivity activity = (MainActivity) getActivity();
		if (activity == null) {
			return;
		}
		
		mMainFragmentViewModel = ViewModelProviders.of(activity).get(MainFragmentViewModel.class);
		mMainFragmentViewModel.getSnapshotOfIntersections().observe(this, intersections -> {
			
			mMainFragmentViewModel.uniqueIntersections = 0;
			
			if (intersections == null || intersections.size() == 0) {
				noDataPlaceholder.setVisibility(View.VISIBLE);
				recycler.setVisibility(View.INVISIBLE);
				return;
			}
			
			if (adapter != null) {
				adapter.setListOfIntersections(intersections);
				adapter.setDividerPosition(mMainFragmentViewModel.uniqueIntersections);
				
				noDataPlaceholder.setVisibility(View.INVISIBLE);
				recycler.setVisibility(View.VISIBLE);
			}
		});
		
		mMainActivityViewModel = ViewModelProviders.of(activity).get(MainActivityViewModel.class);
	}
	
	private void initViews(final View root) {
		noDataPlaceholder = root.findViewById(R.id.no_data);
		
		adapter = new IntersectionsListAdapter(getContext(), mMapInvoker);
		
		recycler = root.findViewById(R.id.intersections_list_recycler);
		recycler.setAdapter(adapter);
		recycler.setLayoutManager(new LinearLayoutManager(getContext()));
	}
	
	public interface CoordinateClickListener {
		void onCoordinateClicked(final Float latitude, final Float longtitude);
	}
	
	private class MapInvoker implements CoordinateClickListener {
		
		@Override
		public void onCoordinateClicked(final Float latitude, final Float longitude) {
			final Intent intent = IntentUtil.getGeoShareIntent(latitude, longitude);
			if (intent != null) {
				startActivity(intent);
			}
		}
	}
}

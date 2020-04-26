package com.dawsoftware.contacttracker;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.dawsoftware.contacttracker.ui.intersections.IntersectionsListFragment;
import com.dawsoftware.contacttracker.ui.main.MainFragment;
import com.dawsoftware.contacttracker.ui.statistics.WorldStatistics;

public class CustomFragmentPagerAdapter extends FragmentPagerAdapter {
	
	public static final int INTERSECTIONS_PAGE_INDEX = 0;
	public static final int STATISTICS_PAGE_INDEX = 1;
	public static final int HEALTH_PAGE_INDEX = 2;
	
	private static final int PAGE_COUNT = 3;
	
	public CustomFragmentPagerAdapter(FragmentManager fm) {
		super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
	}

	@Override
	public Fragment getItem(int position) {
		switch (position) {
			case INTERSECTIONS_PAGE_INDEX: {
				return new IntersectionsListFragment();
			}
			
			case STATISTICS_PAGE_INDEX: {
				return new WorldStatistics();
			}
			
			case HEALTH_PAGE_INDEX: {
				return new MainFragment();
			}
		}
		
		return null;
	}

	@Override
	public int getCount() {
		return PAGE_COUNT;
	}

}

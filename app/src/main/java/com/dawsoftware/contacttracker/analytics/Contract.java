package com.dawsoftware.contacttracker.analytics;

public class Contract {
	private Contract() {}
	
	public static final String ONBOARDING_OPENED = "onboarding_opened";
	public static final String ONBOARDING_DISMISSED = "onboarding_dismissed";
	public static final String ONBOARDING_FINISHED = "onboarding_finished";
	
	public static final String MAIN_SCREEN_OPENED = "main_screen_opened";
	public static final String INTERSECTIONS_SCREEN_OPENED = "intersections_screen_opened";
	public static final String SURVEY_SCREEN_OPENED = "survey_screen_opened";
	public static final String STATISTICS_SCREEN_OPENED = "statistics_screen_opened";
	
	public static final String APP_START_MANUAL = "app_start_manual";
	public static final String APP_START_SERVICE = "app_start_service";
	public static final String APP_START_SURVEY_NOTIF = "app_start_survey_notif";
	public static final String APP_START_INTERSECTION_NOTIF = "app_start_intersection_notif";
	public static final String APP_START_REMINDER_NOTIF = "app_start_reminder_notif";
	
	public static final String START_SCANNING_BUTTON = "start_scanning_button";
	public static final String START_SCANNING_TOTAL = "start_scanning_total";
	
	public static final String SEND_SURVEY_BUTTON = "send_survey_button";
	
	public static final String SHARED_FROM_SURVEY = "shared_from_survey";
	
	public static final String STATE_ACTIVE_TO_INACTIVE = "state_active_to_inactive";
	public static final String STATE_INACTIVE_TO_ACTIVE = "state_inactive_to_active";
	
	public static final String STATUS_HEALTHY_TO_INFECTED_CLIENT = "client_status_healthy_to_infected";
	public static final String STATUS_HEALTHY_TO_INFECTED_SERVER = "server_status_healthy_to_infected";
	
	public static final String STATUS_INFECTED_TO_HEALTHY_CLIENT = "client_status_infected_to_healthy";
	public static final String STATUS_INFECTED_TO_HEALTHY_SERVER = "server_status_infected_to_healthy";
	
	public static final String STATUS_INFECTED_TO_SICK_CLIENT = "client_status_infected_to_sick";
	public static final String STATUS_INFECTED_TO_SICK_SERVER = "server_status_infected_to_sick";
	
	public static final String STATUS_SICK_TO_HEALTHY_CLIENT = "client_status_sick_to_healthy";
	public static final String STATUS_SICK_TO_HEALTHY_SERVER = "server_status_sick_to_healthy";
}

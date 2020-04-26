package com.dawsoftware.contacttracker.data;

import android.content.Context;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;

import com.dawsoftware.contacttracker.BuildConfig;
import com.dawsoftware.contacttracker.data.db.AppDB;
import com.dawsoftware.contacttracker.data.db.BeaconEntity;
import com.dawsoftware.contacttracker.data.db.CoordinateEntity;
import com.dawsoftware.contacttracker.data.db.IntersectionCounterEntity;
import com.dawsoftware.contacttracker.data.db.IntersectionEntity;
import com.dawsoftware.contacttracker.data.db.SpotEntity;
import com.dawsoftware.contacttracker.data.db.SurroundingEntity;
import com.dawsoftware.contacttracker.data.mappers.BeaconMappers;
import com.dawsoftware.contacttracker.data.mappers.CoordinateMappers;
import com.dawsoftware.contacttracker.data.mappers.IntersectionMappers;
import com.dawsoftware.contacttracker.data.mappers.SpotMappers;
import com.dawsoftware.contacttracker.data.network.CommonApi;
import com.dawsoftware.contacttracker.data.network.models.AppLinkResponse;
import com.dawsoftware.contacttracker.data.network.models.Intersection;
import com.dawsoftware.contacttracker.data.network.models.PushToken;
import com.dawsoftware.contacttracker.data.network.models.RegisterIds;
import com.dawsoftware.contacttracker.data.network.models.RegisterResponse;
import com.dawsoftware.contacttracker.data.network.models.Status;
import com.dawsoftware.contacttracker.data.network.models.StatusResponse;
import com.dawsoftware.contacttracker.data.network.models.UserData;
import com.dawsoftware.contacttracker.data.network.models.UserDataResponse;
import com.dawsoftware.contacttracker.data.network.models.location.Beacon;
import com.dawsoftware.contacttracker.data.network.models.location.Coordinates;
import com.dawsoftware.contacttracker.data.network.models.location.Spot;
import com.dawsoftware.contacttracker.data.network.models.location.SurroundingsMeta;
import com.dawsoftware.contacttracker.data.network.models.location.SurroundingsResponse;
import com.dawsoftware.contacttracker.services.BluetoothHelper.BluetoothDeviceHolder;
import com.dawsoftware.contacttracker.services.LocationCollectedData;
import com.dawsoftware.contacttracker.util.LogUtil;
import com.dawsoftware.contacttracker.util.PreferencesUtil;
import com.dawsoftware.contacttracker.util.StringUtil;
import com.dawsoftware.contacttracker.util.executors.WorkerThreadExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import okhttp3.logging.HttpLoggingInterceptor.Logger;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Repository {
	
	private static Repository instance = null;
	
	public static Repository getInstance(@NonNull final Context applicationContext) {
		
		if (instance == null) {
			synchronized (Repository.class) {
				if (instance == null) {
					instance = new Repository(applicationContext);
				}
			}
		}
		
		return instance;
	}
	
	private static final int DEFAULT_NETWORK_TRIES = 3;
	
	public static final String BUNDLE_INFECTED_NEAR = "BUNDLE_INFECTED_NEAR";
	public static final String BUNDLE_INFECTED_TOTAL = "BUNDLE_INFECTED_TOTAL";
	public static final String BUNDLE_INFECTED_24H = "BUNDLE_INFECTED_24H";
	public static final String BUNDLE_STATUS = "BUNDLE_STATUS";
	public static final String BUNDLE_LAST_INTERSECTION_TIME_S = "BUNDLE_LAST_INTERSECTION_TIME_S";
	public static final String BUNDLE_UNIQUE_INTERSECTIONS = "BUNDLE_UNIQUE_INTERSECTIONS";
	public static final String BUNDLE_SEND_SUCCESS = "BUNDLE_SEND_SUCCESS";
	
	private Context context;
	
	private final WorkerThreadExecutor executors;
	
	private final CommonApi api;
	private OkHttpClient okHttpClient;
	private Retrofit retrofit;
	private HttpLoggingInterceptor httpLogging;
	
	private AppDB db;
	
	private PreferencesUtil prefs;
	
	private volatile boolean isJanitorRunning = false;
	
	private Repository(@NonNull final Context applicationContext) {
		context = applicationContext;
		executors = new WorkerThreadExecutor();
		api = initRetrofit();
		db = AppDB.getInstance(applicationContext);
		prefs = new PreferencesUtil(applicationContext);
	}
	
	private CommonApi initRetrofit() {
		httpLogging = new HttpLoggingInterceptor(initLogger());
		httpLogging.setLevel(Level.BODY);
		
		okHttpClient = new OkHttpClient().newBuilder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .addInterceptor(httpLogging)
                        .build();
		
		retrofit = new Retrofit.Builder()
				.baseUrl(BuildConfig.URL)
				.client(okHttpClient)
				.addConverterFactory(GsonConverterFactory.create())
				.build();
		
		return retrofit.create(CommonApi.class);
	}
	
	private Logger initLogger() {
		return s -> {
			LogUtil.writeToFile(s, context, LogUtil.NET);
		};
	}
	
	public void register(final String deviceId, final String btMac, final RegisterCallback callback) {
		
		if (StringUtil.isEmpty(deviceId)) {
			callback.fail();
			return;
		}
		
		executors.execute(() -> {
			boolean isSuccessful = false;
			
			for (int i = 0; i < DEFAULT_NETWORK_TRIES; i++) {
				Call<RegisterResponse> call = api.register(new RegisterIds(deviceId, btMac));
				Response<RegisterResponse> response = null;
				
				try {
					response = call.execute();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (response != null && response.code() == 200) {
					if (response.body() != null && response.body().status == 200) {
						callback.success(response.body().userId.userID);
						isSuccessful = true;
						break;
					}
				}
			}
			
			if (!isSuccessful) {
				callback.fail();
			}
		});
	}
	
	public interface RegisterCallback {
		void success(final String userId);
		void fail();
	}
	
	public void sendStatus(final String status, final ArrayList<String> symptoms, final String userId, final String deviceId,
	                       final SendStatusCallback callback) {
		
		if (StringUtil.isEmpty(status)) {
			return;
		}
		
		if (isIDSIncorrect(userId, deviceId)) {
			callback.fail();
			return;
		}
		
		executors.execute(() -> {
			
			boolean isSuccessful = false;
			
			for (int i = 0; i < DEFAULT_NETWORK_TRIES; i++) {
				
				Call<StatusResponse> call;
				
//				if (BuildConfig.DEBUG && "infected".equals(status)) {
//					// для отладки - сколько секунд назад заболел
//					long past = 14 * 86400; // дней
//
//					call = api.updateStatusDebug(userId, deviceId, new StatusDebug(status, symptoms, past));
//				} else {
					call = api.updateStatus(userId, deviceId, new Status(status, symptoms));
//				}
				
				Response<StatusResponse> response = null;
				
				try {
					response = call.execute();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (response != null && response.code() == 200) {
					if (response.body() != null && (response.body().status == 200 || response.body().status == 503)) {
						callback.success();
						isSuccessful = true;
						break;
					}
				}
			}
			
			if (!isSuccessful) {
				callback.fail();
			}
		});
	}
	
	public interface SendStatusCallback {
		void success();
		void fail();
	}
	
	public void getData(final String userId, final String deviceId, final GetDataCallback callback) {
		
		if (isIDSIncorrect(userId, deviceId)) {
			callback.fail();
			return;
		}
		
		executors.execute(() -> {
			boolean isSuccessful = false;
			
			Response<UserDataResponse> response = null;
			
			for (int i = 0; i < DEFAULT_NETWORK_TRIES; i++) {
				Call<UserDataResponse> call = api.getUserData(userId, deviceId);
				
				try {
					response = call.execute();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (response != null && response.code() == 200) {
					if (response.body() != null && response.body().status == 200) {
						isSuccessful = true;
						break;
					}
				}
			}
			
			if (isSuccessful) {
				
				long lastIntersectionTimeS = 0;
				
				IntersectionCounterEntity counters = null;
				
				if (response.body().userData.infectedTotal != null && response.body().userData.active) {
					counters = db.intersectionDao().getIntersectionCounters();
					
					boolean creation = false;
					if (counters == null) {
						counters = new IntersectionCounterEntity();
						creation = true;
					}
					
					lastIntersectionTimeS = calculateLastIntersectionTimeAgoSec(response.body().userData.intersections);
					
					counters.totalIntersections = response.body().userData.infectedTotal;
					
					if (response.body().userData.infected24h != null) {
						counters.last24hIntersections = response.body().userData.infected24h;
					} else {
						counters.last24hIntersections = 0;
					}
					
					counters.lastIntersectionTimeS = lastIntersectionTimeS;
					counters.status = response.body().userData.status;
					counters.isActive = response.body().userData.active;
					
					if (creation) {
						db.intersectionDao().insertCounters(counters);
					} else {
						db.intersectionDao().updateCounters(counters);
					}
				} else {
					if (!response.body().userData.active) {
						
						counters = db.intersectionDao().getIntersectionCounters();
						
						boolean creation = false;
						if (counters == null) {
							counters = new IntersectionCounterEntity();
							creation = true;
						}
						
						counters.totalIntersections = 0;
						counters.last24hIntersections = 0;
						counters.lastIntersectionTimeS = 0;
						counters.status = "healthy";
						counters.isActive = response.body().userData.active;
						
						if (creation) {
							db.intersectionDao().insertCounters(counters);
						} else {
							db.intersectionDao().updateCounters(counters);
						}
					}
				}
				
				final List<IntersectionEntity> currentIntersections = db.intersectionDao().getAllIntersectionsDESC();
				if (currentIntersections != null && currentIntersections.size() > 0) {
					db.intersectionDao().deleteIntersections(currentIntersections);
				}
				
				final List<IntersectionEntity> newIntersections =
						IntersectionMappers.intersectionsToDb(response.body().userData.intersections);
				if (newIntersections != null && newIntersections.size() > 0) {
					db.intersectionDao().insertIntersections(newIntersections);
					
					if (BuildConfig.DEBUG) {
						for (IntersectionEntity i : newIntersections) {
							LogUtil.writeToFile("Intersect: "+i.userId, context, LogUtil.DB);
						}
					}
				}
				
				callback.success(response.body().userData, lastIntersectionTimeS);
			} else {
				callback.fail();
			}
		});
	}
	
	private long calculateLastIntersectionTimeAgoSec(final List<Intersection> data) {
		
		long max = 0;
		
		if (data == null || data.isEmpty()) {
			return max;
		}
		
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).time > max) {
				max = data.get(i).time;
			}
		}
		
		return max;
	}
	
	public interface GetDataCallback {
		void success(UserData userData, long lastIntersectionTimeS);
		void fail();
	}
	
	public void getAppLink(final GetAppLinkCallback callback) {
		
		executors.execute(() -> {
			boolean isSuccessful = false;
			
			for (int i = 0; i < DEFAULT_NETWORK_TRIES; i++) {
				Call<AppLinkResponse> call = api.getAppLink();
				Response<AppLinkResponse> response = null;
				
				try {
					response = call.execute();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (response != null && response.code() == 200) {
					if (response.body() != null && response.body().status == 200) {
						callback.success(response.body().result.appLink);
						isSuccessful = true;
						break;
					}
				}
			}
			
			if (!isSuccessful) {
				callback.fail();
			}
		});
	}
	
	public interface GetAppLinkCallback {
		void success(String appLink);
		void fail();
	}
	
	public void saveSurroundingData(final LocationCollectedData data,
	                                final boolean mustCalculateIntersections, final SendSurroundingCallback callback) {
		
		if (data == null) {
			return;
		}
		
		executors.execute(() -> {
			Log.i("wuttt", "calculate intersections? " + mustCalculateIntersections);
			
			saveSurroundingDataToDB(data);
			sendSurroundingData(callback, mustCalculateIntersections);
		});
	}
	
	@WorkerThread
	public void saveSurroundingDataToDB(final LocationCollectedData data) {
		
		boolean beaconsWritten = false;
		boolean spotsWritten = false;
		boolean coordinateWritten = false;

		long timeSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

		final SurroundingEntity surround = new SurroundingEntity();
		surround.time = timeSec;

		final HashSet<BluetoothDeviceHolder> btDevices = new HashSet<>(data.getBluetoothDevices());
		final List<ScanResult> wifiDevices = new ArrayList<>(data.getWifiSpots());
		final Location location = data.getLocation();

		BeaconEntity[] beacons = null;
		if (btDevices != null) {
			beacons = BeaconMappers.beaconsToDB(btDevices, timeSec);
		}
		
		if (beacons != null && beacons.length > 0) {
			db.beaconsDao().insert(beacons);
			beaconsWritten = true;
			
			if (BuildConfig.DEBUG) {
				for (BeaconEntity b : beacons) {
					LogUtil.writeToFile("Beacon: "+b.mac, context, LogUtil.DB);
				}
			}
		}

		SpotEntity[] spots = null;
		if (wifiDevices != null) {
			spots = SpotMappers.spotsToDB(wifiDevices, timeSec);
			spotsWritten = true;
			
			if (BuildConfig.DEBUG) {
				for (SpotEntity s : spots) {
					LogUtil.writeToFile("Spot: "+s.mac, context, LogUtil.DB);
				}
			}
		}
		
		if (spots != null && spots.length > 0) {
			db.spotsDao().insert(spots);
		}

		final CoordinateEntity coordinate = CoordinateMappers.coordinatesToDbWithTime(location, timeSec);
		
		if (coordinate != null) {
			db.coordinatesDao().insert(coordinate);
			coordinateWritten = true;
			
			if (BuildConfig.DEBUG) {
				LogUtil.writeToFile("Coordinate: "+coordinate.lat+" "+coordinate.lng, context, LogUtil.DB);
			}
		}
		
		if (beaconsWritten || spotsWritten || coordinateWritten) {
			db.surroundingDao().insert(surround);
			if (BuildConfig.DEBUG) {
				LogUtil.writeToFile("-----------", context, LogUtil.DB);
			}
		}
	}
	
	@WorkerThread
	public void sendSurroundingData(final SendSurroundingCallback callback, final boolean mustCalculateIntersections) {
		sendSurroundingData(null, callback, mustCalculateIntersections);
	}
	
	@WorkerThread
	public void sendSurroundingData(final SurroundingEntity surrounding, final SendSurroundingCallback callback,
	                                final boolean mustCalculateIntersections) {
		
		final SurroundingEntity surroundingData = surrounding == null ? db.surroundingDao().getLast() : surrounding;
		
		if (surroundingData == null) {
			if (callback != null) {
				callback.nothingToSend();
			}
			return;
		}
		
		List<SurroundingEntity> es = db.surroundingDao().getAll();
		Log.i("wutt", "left in DB: " + es.size());
		
		long time = surroundingData.time;
		
		final List<BeaconEntity> beaconEntities = db.beaconsDao().selectByTime(time);
		final List<SpotEntity> spotEntities = db.spotsDao().getByTime(time);
		final List<CoordinateEntity> coordinateEntity = db.coordinatesDao().getByTime(time);
		
		final ArrayList<Beacon> beacons = BeaconMappers.beaconsFromDB(beaconEntities);
		final ArrayList<Spot> spots = SpotMappers.spotsFromDB(spotEntities);
		
		final ArrayList<Coordinates> coordinates = CoordinateMappers.coordinatesListFromDb(coordinateEntity);
		
		long timeSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		
		final SurroundingsMeta meta = new SurroundingsMeta(timeSec, mustCalculateIntersections, beacons, spots, coordinates);
		
		final SurroundingsResponse response = sendSurrounding(meta, prefs.getUserId(), prefs.getInstallationId());
		
		boolean isSentSuccessfully = response != null && response.status == 200;
		
		final Bundle bundle = new Bundle();
		
		if (isSentSuccessfully) {
			db.coordinatesDao().deleteAll(coordinateEntity);
			db.spotsDao().deleteAll(spotEntities);
			db.beaconsDao().deleteAll(beaconEntities);
			db.surroundingDao().delete(surroundingData);
			
			if (response.result != null) {
				long newLastIntersectionTime = 0;
				int uniqueIntersectionsCount = 0;
				
				IntersectionCounterEntity counters = db.intersectionDao().getIntersectionCounters();
				
				if (response.result.intersections != null && response.result.intersections.size() > 0) {
					
					final List<Intersection> oldIntersections = IntersectionMappers.intersectionsFromDb(db.intersectionDao().getAllIntersectionsDESC());
					final List<Intersection> newIntersections = response.result.intersections;
					
					uniqueIntersectionsCount = filterNonUniqueIntersections(oldIntersections, newIntersections);
					
					if (counters != null) {
						long currentLastIntersectionTime = counters.lastIntersectionTimeS;
						long newIntersectionsLastTime = calculateLastIntersectionTimeAgoSec(newIntersections);
						
						newLastIntersectionTime = Math.max(currentLastIntersectionTime, newIntersectionsLastTime);
						
						counters.lastIntersectionTimeS = newLastIntersectionTime;
						
						if (response.result.infected24h != null) {
							counters.last24hIntersections = response.result.infected24h;
						} else {
							counters.last24hIntersections = 0;
						}
						
						counters.totalIntersections = response.result.infectedTotal;
						counters.status = response.result.status;
						
						db.intersectionDao().updateCounters(counters);
					} else {
						counters = new IntersectionCounterEntity();
						counters.lastIntersectionTimeS = newLastIntersectionTime;
						
						if (response.result.infected24h != null) {
							counters.last24hIntersections = response.result.infected24h;
						} else {
							counters.last24hIntersections = 0;
						}
						
						counters.totalIntersections = response.result.infectedTotal;
						counters.status = response.result.status;
						
						db.intersectionDao().insertCounters(counters);
					}
				}
				
				bundle.putString(BUNDLE_STATUS, response.result.status);
				bundle.putInt(BUNDLE_INFECTED_TOTAL, response.result.infectedTotal);
				
				if (response.result.infected24h != null) {
					bundle.putInt(BUNDLE_INFECTED_24H, response.result.infected24h);
				} else {
					bundle.putInt(BUNDLE_INFECTED_24H, counters.last24hIntersections);
				}
				
				if (response.result.infectedNear != null) {
					bundle.putInt(BUNDLE_INFECTED_NEAR, response.result.infectedNear);
				}
				
				bundle.putLong(BUNDLE_LAST_INTERSECTION_TIME_S, newLastIntersectionTime);
				bundle.putInt(BUNDLE_UNIQUE_INTERSECTIONS, uniqueIntersectionsCount);
			} else {
				isSentSuccessfully = false;
			}
		}
		
		if (callback != null) {
			if (isSentSuccessfully) {
				callback.success(bundle);
			} else {
				callback.fail();
			}
		}
	}
	
	/**
	 * Добавляет в текущий список пересечений только те, которые в нём не содержатся.
	 * В качестве критерия отсева используется userID.
	 *
	 * @param oldList текущий список пересечений
	 * @param newList список пересечений, полученный при сканировании
	 *
	 * @return число добавленных пересечений
	 */
	private int filterNonUniqueIntersections(final List<Intersection> oldList, final List<Intersection> newList) {
		
		if (oldList == null || newList.isEmpty()) {
			return 0;
		}
		
		final ArrayList<Intersection> uniqueList = new ArrayList<>();
		
		for (int i = 0; i < newList.size(); i++) {
			Intersection currentNew = newList.get(i);
			boolean found = false;
			
			for (int j = 0; j < oldList.size(); j++) {
				Intersection currentOld = oldList.get(j);
				
				if (currentOld.userId.equals(currentNew.userId)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				uniqueList.add(currentNew);
			}
		}
		
		final List<IntersectionEntity> newEntities = IntersectionMappers.intersectionsToDb(uniqueList);
		if (!newEntities.isEmpty()) {
			db.intersectionDao().insertIntersections(newEntities);
		}
		
		if (BuildConfig.DEBUG) {
			for (IntersectionEntity i : newEntities) {
				LogUtil.writeToFile("Scan found unique: "+i.userId, context, LogUtil.DB);
			}
		}
		
		return newEntities.size();
	}
	
	@WorkerThread
	private SurroundingsResponse sendSurrounding(final SurroundingsMeta surroundingsMeta, final String userId, final String deviceId) {
		
		if (isIDSIncorrect(userId, deviceId)) {
			return null;
		}
		
		Response<SurroundingsResponse> response = null;
		
		for (int i = 0; i < DEFAULT_NETWORK_TRIES; i++) {
			
			final Call<SurroundingsResponse> call = api.sendSurroundData(userId, deviceId, surroundingsMeta);
			
			try {
				response = call.execute();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (response != null && response.code() == 200) {
				if (response.body() != null && response.body().status == 200) {
					break;
				}
			}
		}
		
		return response == null ? null : response.body();
	}
	
	public interface SendSurroundingCallback {
		void success(final Bundle data);
		void fail();
		void nothingToSend();
	}
	
	public void checkAndRunJanitor() {
		executors.execute(() -> {
			if (isJanitorRunning) {
				return;
			}
			
			runJanitor();
		});
	}
	
	@WorkerThread
	private void runJanitor() {
		isJanitorRunning = true;
		
		final List<SurroundingEntity> surroundingEntities = db.surroundingDao().getAll();
		if (surroundingEntities == null || surroundingEntities.isEmpty()) {
			isJanitorRunning = false;
			return;
		}
		
		for (SurroundingEntity e: surroundingEntities) {
			
			long time = e.time;
			
			final List<BeaconEntity> beaconEntities = db.beaconsDao().selectByTime(time);
			final List<SpotEntity> spotEntities = db.spotsDao().getByTime(time);
			final List<CoordinateEntity> coordinateEntity = db.coordinatesDao().getByTime(time);
			
			final ArrayList<Beacon> beacons = BeaconMappers.beaconsFromDB(beaconEntities);
			final ArrayList<Spot> spots = SpotMappers.spotsFromDB(spotEntities);
			
			final ArrayList<Coordinates> coordinates = CoordinateMappers.coordinatesListFromDb(coordinateEntity);
			
			long timeSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
			
			final SurroundingsMeta meta = new SurroundingsMeta(timeSec, false, beacons, spots, coordinates);
			
			SurroundingsResponse response = sendSurrounding(meta, prefs.getUserId(), prefs.getInstallationId());
			
			if (response != null && response.status == 200) {
				db.coordinatesDao().deleteAll(coordinateEntity);
				db.spotsDao().deleteAll(spotEntities);
				db.beaconsDao().deleteAll(beaconEntities);
				db.surroundingDao().delete(e);
			} else {
				break;
			}
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		
		isJanitorRunning = false;
	}
	
	public void getAllIntersectionsFromDbByDESC(final GetIntersectionsFromDbCallback callback) {
		executors.execute(() -> {
			final List<IntersectionEntity> entities = db.intersectionDao().getAllIntersectionsDESC();
			if (entities == null) {
				callback.fail();
				return;
			}
			
			final List<Intersection> resultList = IntersectionMappers.intersectionsFromDb(entities);
			callback.success(resultList);
		});
	}
	
	public interface GetIntersectionsFromDbCallback {
		void success(final List<Intersection> intersections);
		void fail();
	}
	
	public void getCountersFromDb(final GetCountersFromDbCallback callback) {
		executors.execute(() -> {
			final IntersectionCounterEntity entity = db.intersectionDao().getIntersectionCounters();
			if (entity == null) {
				callback.fail();
				return;
			}
			
			callback.success(entity.totalIntersections, entity.last24hIntersections, entity.status, entity.isActive);
		});
	}
	
	public interface GetCountersFromDbCallback {
		void success(final Integer total, final Integer last24h, final String status, final Boolean isActive);
		void fail();
	}
	
	public void sendPushToken(final String token, final SendPushTokenCallback callback) {

		if (StringUtil.isEmpty(token)) {
			return;
		}
		
		final String userId = prefs.getUserId();
		final String deviceId = prefs.getInstallationId();

		if (isIDSIncorrect(userId, deviceId)) {
			return;
		}

		executors.execute(() -> {

			boolean isSuccessful = false;

			for (int i = 0; i < DEFAULT_NETWORK_TRIES; i++) {
				Call<StatusResponse> call = api.updateToken(userId, deviceId, new PushToken(token));
				Response<StatusResponse> response = null;

				try {
					response = call.execute();
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (response != null && response.code() == 200) {
					if (response.body() != null && (response.body().status == 200 || response.body().status == 503)) {
						isSuccessful = true;
						callback.success();
						break;
					}
				}
			}

			if (!isSuccessful) {
				callback.fail();
				return;
			}
		});
	}
	
	public interface SendPushTokenCallback {
		void success();
		void fail();
	}

	private boolean isIDSIncorrect(final String userId, final String deviceId) {
		return StringUtil.isEmpty(userId) || StringUtil.isEmpty(deviceId);
	}
}

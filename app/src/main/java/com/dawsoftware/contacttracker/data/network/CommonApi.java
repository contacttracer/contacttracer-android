package com.dawsoftware.contacttracker.data.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import com.dawsoftware.contacttracker.data.network.models.AppLinkResponse;
import com.dawsoftware.contacttracker.data.network.models.PushToken;
import com.dawsoftware.contacttracker.data.network.models.RegisterIds;
import com.dawsoftware.contacttracker.data.network.models.RegisterResponse;
import com.dawsoftware.contacttracker.data.network.models.Status;
import com.dawsoftware.contacttracker.data.network.models.StatusDebug;
import com.dawsoftware.contacttracker.data.network.models.StatusResponse;
import com.dawsoftware.contacttracker.data.network.models.UserDataResponse;
import com.dawsoftware.contacttracker.data.network.models.location.SurroundingsMeta;
import com.dawsoftware.contacttracker.data.network.models.location.SurroundingsResponse;

public interface CommonApi {
	
	@POST("v1/user")
	Call<RegisterResponse> register(@Body RegisterIds deviceId);
	
	@GET("v1/user/{user_id}")
	Call<UserDataResponse> getUserData(@Path("user_id") String userId,
	                                   @Header("Authorization") String deviceId);
	
	@POST("v1/user/{user_id}/status")
	Call<StatusResponse> updateStatus(@Path("user_id") String userId,
	                                  @Header("Authorization") String deviceId,
	                                  @Body Status status);
	
	@POST("v1/user/{user_id}/status")
	Call<StatusResponse> updateStatusDebug(@Path("user_id") String userId,
	                                  @Header("Authorization") String deviceId,
	                                  @Body StatusDebug status);
	
	@GET("v1/app/link")
	Call<AppLinkResponse> getAppLink();
	
	@POST("v1/user/{user_id}/surroundings")
	Call<SurroundingsResponse> sendSurroundData(@Path("user_id") String userId,
	                                            @Header("Authorization") String deviceId,
	                                            @Body SurroundingsMeta surroundData);
	
	@POST("/v1/user/{user_id}/messaging")
	Call<StatusResponse> updateToken(@Path("user_id") String userId,
	                                 @Header("Authorization") String deviceId,
	                                 @Body PushToken token);
	
}

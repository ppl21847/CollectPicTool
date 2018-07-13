package cn.finalteam.galleryfinal.utils;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by liupaipai on 2018/7/13.
 */

public interface UploadImageAPI {
    @FormUrlEncoded
    @POST("/HWWebPost.asmx/UploadImg")
    Call<JsonObject> postImg(@Field("json_input") String json_input);
}

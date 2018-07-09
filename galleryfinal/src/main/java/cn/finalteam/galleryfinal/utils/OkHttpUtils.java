package cn.finalteam.galleryfinal.utils;


import java.io.File;
import java.util.List;
import java.util.Map;

import cn.finalteam.toolsfinal.io.FilenameUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by liupaipai on 2018/7/9.
 */

public class OkHttpUtils {
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

    /**
     * 通过上传的文件的完整路径生成RequestBody
     * @param fileNames 完整的文件路径
     * @return
     */
    private RequestBody getRequestBody(Map<String, String> params,String pic_key,List<String> fileNames) {
        //创建MultipartBody.Builder，用于添加请求的数据
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);

        //遍历map中所有参数到builder
        if (params != null){
            for (String key : params.keySet()) {
                builder.addFormDataPart(key, params.get(key));
            }
        }

        //遍历paths中所有图片绝对路径到builder，并约定key如“upload”作为后台接受多张图片的key
        for (int i = 0; i < fileNames.size(); i++) { //对文件进行遍历
            File file = new File(fileNames.get(i)); //生成文件
            //根据文件的后缀名，获得文件类型
            String fileType = getMimeType(file.getName());
            builder.addFormDataPart( //给Builder添加上传的文件
                    pic_key,  //请求的名字
                    file.getName(), //文件的文字，服务器端用来解析的
                    RequestBody.create(MediaType.parse(fileType), file) //创建RequestBody，把上传的文件放入
            );
        }
        return builder.build(); //根据Builder创建请求
    }

    /**
     * 获得Request实例
     * @param url
     * @param fileNames 完整的文件路径
     * @return
     */
    public Request getRequest(String url, Map<String, String> params,String pic_key,List<String> fileNames) {
        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .post(getRequestBody(params,pic_key,fileNames));
        return builder.build();
    }

    /**
     * 获取文件后缀
     * */
    private String getMimeType(String fileName){
        String ext = FilenameUtils.getExtension(fileName);
        return "image/"+ext;
    }

}

package med.base.server.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultResponse<T> {
    /**
     * true是有错误,false是没有错误
     */
    private Boolean status;

    private T data;

    private String statusText;


    /**
     * 返回数据
     */
    public static String success(Object object) {

        return JSON.toJSONString(DefaultResponse.builder().status(true).data(object).build(), SerializerFeature.WriteNullNumberAsZero
                , SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.DisableCircularReferenceDetect);
    }

    /**
     * 返回数据
     */
    public static String successNoData(Object object) {

        return JSON.toJSONString(object, SerializerFeature.WriteNullNumberAsZero
                , SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.DisableCircularReferenceDetect);
    }


    /**
     * 返回数据
     */
    public static String success(){

        return JSON.toJSONString(DefaultResponse.builder().status(true).build());
    }

    /**
     * 返回数据
     */
    public static String error(String message) {

        return JSON.toJSONString(DefaultResponse.builder().status(false).statusText(message).build());
    }


    public static String error(Object object) {

        return JSON.toJSONString(DefaultResponse.builder().status(false).data(object).build(),SerializerFeature.WriteNullNumberAsZero
                , SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.DisableCircularReferenceDetect);
    }
}

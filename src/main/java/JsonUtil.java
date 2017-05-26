import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

/**
 * Created by Administrator on 2017/5/16.
 */
public class JsonUtil {
    public static String model2Json(Blog blog) {
        String jsonData = null;
        try {
            XContentBuilder jsonBuild = XContentFactory.jsonBuilder();
            jsonBuild.startObject().field("id", blog.getId()).field("title", blog.getTitle())
                    .field("posttime", blog.getPosttime()).field("content",blog.getContent()).endObject();
            jsonData = jsonBuild.string();
            System.out.println(jsonData);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonData;
    }

}

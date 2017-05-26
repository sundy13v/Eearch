import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.*;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.alibaba.fastjson.JSONArray;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class Elasticsearch {

    private TransportClient client;
    private IndicesAdminClient adminClient;
    /**
     * 集群配置初始化方法
     * @throws Exception
     */
    private void init() throws Exception{
        Properties properties = readElasticsearchConfig();
        String clusterName = properties.getProperty("clusterName");
        String[] inetAddresses = properties.getProperty("hosts").split(",");
        Settings esSettings = Settings.builder()
                .put("cluster.name", clusterName) //设置ES实例的名称
                .put("client.transport.sniff", true) //自动嗅探整个集群的状态，把集群中其他ES节点的ip添加到本地的客户端列表中
                .build();
        client = new PreBuiltTransportClient(esSettings);//初始化client较老版本发生了变化，此方法有几个重载方法，初始化插件等。
        //此步骤添加IP，至少一个，其实一个就够了，因为添加了自动嗅探配置
//        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
        for (int i = 0; i < inetAddresses.length; i++) {
            String[] tmpArray = inetAddresses[i].split(":");
            String ip = tmpArray[0];
            int port = Integer.valueOf(tmpArray[1]);
            client = client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ip), port));
        }
//        Map<String,Object> infoMap = new HashMap<String, Object>();
//        infoMap.put("name", "zk");
//        infoMap.put("title", "zookper");
//        infoMap.put("createTime", new Date());
//        infoMap.put("count", 1022);
//        IndexResponse indexResponse = client.prepareIndex("test", "info","100").setSource(infoMap).execute().actionGet();
//        System.out.println("id:"+indexResponse.getId());
//        client.close();
    }
    /**
     * 构造方法
     */
    public Elasticsearch() {
        try {
            init();
        } catch (Exception e) {
            System.out.println("init() exception!");
            e.printStackTrace();
        }
    }
    /**
     * 判断集群中{index}是否存在
     * @param index
     * @return 存在（true）、不存在（false）
     */
    public boolean indexExists(String index){
        IndicesExistsRequest request = new IndicesExistsRequest(index);
        IndicesExistsResponse response = client.admin().indices().exists(request).actionGet();
        if (response.isExists()) {
            return true;
        }
        return false;
    }

    /***
     * 创建index索引，插入数据
     */
    public void CreateIndex(){
        List<String> jsonData = DataFactory.getInitJsonData();
        BulkRequestBuilder brb = client.prepareBulk();
        for (int i = 0; i < jsonData.size(); i++) {
            brb.add(Requests.indexRequest("blog").type("java").id(i+"").source(jsonData.get(i)));
        }
        BulkResponse br = brb.execute().actionGet();
        if(br.hasFailures()){
            System.out.println("失败");
        }else{
            System.out.println("成功");
        }

    }

    /**
     * 读取es配置信息
     * @return
     */
    private Properties readElasticsearchConfig() {
        Properties properties = new Properties();
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("elasticsearch.properties");
            properties.load(new InputStreamReader(is,"UTF-8"));
        } catch (IOException e) {
            System.out.println("readEsConfig exception!");
            e.printStackTrace();
        }
        return properties;
    }
    /**
     * 读取json配置文件
     * @return JSONArray
     * @throws IOException
     */
    public JSONArray readJosnFile() throws IOException{
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("index.json");
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuffer sb = new StringBuffer();
        String tmp = null;
        while((tmp=br.readLine()) != null){
            sb.append(tmp);
        }
        JSONArray result = JSONArray.parseArray(sb.toString());
        return result;
    }

    /***
     * 读取信息
     * @throws Exception
     */
    public void query()  {
        if(indexExists("test")) {
            QueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("name").gt(50);
            SearchResponse searchResponse = client.prepareSearch("test")
                    .setTypes("info")
                    .setQuery(rangeQueryBuilder)
                    .addSort("createTime", SortOrder.DESC)
                    .setSize(20)
                    .execute()
                    .actionGet();
            SearchHits hits = searchResponse.getHits();
            System.out.println("查到记录数：" + hits.getTotalHits());
            SearchHit[] searchHists = hits.getHits();
            if (searchHists.length > 0) {
                for (SearchHit hit : searchHists) {
                    String name = (String) hit.getSource().get("name");
                    String title = (String) hit.getSource().get("title");
                    System.out.format("name:%s ,age :%s \n", name, title);
                }
            }
        }
    }

    public static void main(String[] args) {
        Elasticsearch es = new Elasticsearch();
        es.CreateIndex();
    }
}

package vertx.geocoding;

import java.util.concurrent.TimeUnit;
import static java.util.Arrays.asList;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.OverDailyLimitException;
import com.google.maps.errors.OverQueryLimitException;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class GeocodingVerticle extends AbstractVerticle {
	private GeoApiContext context;
	private EventBus eb;
	boolean isChangingKey = false;
	private MongoClient mongoClient;
	private MongoDatabase db;
	private MongoCollection<Document> notGeoCol;
	private GraphDatabaseService graphDb;
	private Label label;
	private Label provinceLabel;
	private long timerID;
	
	@SuppressWarnings("deprecation")
	public void start(Future<Void> fut) throws Exception{
		
		label = DynamicLabel.label("geo");										//label chung cho tất cả các nút
		provinceLabel = DynamicLabel.label("province");							//label cho mức Tỉnh
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("GeoDb");		
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase("test");
		notGeoCol = db.getCollection("DungTestMongo");							//DB có sẵn cần geocode
		db.getCollection("doneDB");
		db.getCollection("failDB");

		//Init context for geocoding context
		initContext();
		this.context.setApiKey(this.config().getString("key"));
		
		//Get on the event bus
		eb = vertx.eventBus();
		eb.consumer("geocoder", 
				new Handler<Message<JsonObject>>(){
					@Override
					public void handle(Message<JsonObject> message) {
						// TODO Auto-generated method stub
						if (message.body().getString("type").equals("set key"))
							resetApiKey(message.body().getString("api key"));
						}
					});
		//Thực hiện việc geocode
		timerID = this.setVertxTimer();
	}
	
	public void stop() {
		eb.send("scheduler", new JsonObject().put("type", "geocoder undeployed"));
		mongoClient.close();
 		graphDb.shutdown();
	}
	
	//Hàm thực hiện tuần hoàn việc geocode
	public long setVertxTimer(){
		return vertx.setPeriodic(200, new Handler<Long>(){
			@Override
			public void handle(Long arg0) {
				// TODO Auto-generated method stub
				try {
					reverseGeocoding();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	public void initContext(){
		context = new GeoApiContext()
				.setQueryRateLimit(5)
				.setReadTimeout(1, TimeUnit.SECONDS)
				.setConnectTimeout(1, TimeUnit.SECONDS)
				.setWriteTimeout(1, TimeUnit.SECONDS)
				.setRetryTimeout(1, TimeUnit.SECONDS);
	}
	
	//Đổi key sau mỗi lần over query limit
	public void resetApiKey(String apiKey){
			context.setApiKey(apiKey);
	}

	//Thực hiện geocode, nhận vào một String dạng "21,000546, 65,154864"
	public void reverseGeocoding() throws Exception {
		//Neu dang doi key thi return luon
		Document doc;
		if(isChangingKey)
			return;
		
		// Lấy dữ liệu từ DB
		doc = notGeoCol.find(new Document("$or", asList(new Document("status", false), new Document("status", null))))
				.limit(1).iterator().next();
		ObjectId id = doc.get("_id", ObjectId.class);
		String idString = id.toHexString();
		
		//Bắt đầu geocode
		try {
			double lat = Double.valueOf(doc.getString("geo_lat"));
			double lng = Double.valueOf(doc.getString("geo_long"));
			
			//Gửi request và nhận lại một mảng
			GeocodingResult[] results = GeocodingApi.newRequest(context)
						.latlng(new LatLng(lat, lng))
						.await();
			
			//Geocoding result là một mảng nên ta sẽ add vào từng phần tử của mảng đó
			for(int i = 0; i < results.length; i++){
				this.addToNeo4j(results[i], idString);
			}
		} catch (NullPointerException e){
			return;
		} catch (OverQueryLimitException e){
			//Neu dang doi key thi bo qua luon
			if(!isChangingKey){
				vertx.cancelTimer(timerID);
				isChangingKey = true;
				
				//Gửi scheduler yêu cầu lấy key
				//Nếu lấy được key thì set lại, khởi động lại timer
				//Nếu không thì stop
				eb.send("scheduler", new JsonObject().put("type", "need change key"),
						res -> {
								if (res.succeeded()){
									context.setApiKey(((JsonObject)res.result().body()).getString("api key"));
									isChangingKey = false;
									timerID = setVertxTimer();
								}
								else this.stop();
						});
			}
			return;
		} catch (OverDailyLimitException e) {
			// Nếu hết quota của một ngày thì sẽ dừng lại luôn, để scheduler deploy lại sau
			this.stop();
			return;
		} catch (NumberFormatException e) {
			// Nếu collection bị lỗi sẽ delete khỏi db
			notGeoCol.deleteOne(new Document("_id", id));
			return;
		}
		notGeoCol.findOneAndUpdate(new Document("_id", id), 
				new Document("$set", new Document("status", true)));
		System.out.println("Done something");
	}
	
	public AddressComponent getComponent(GeocodingResult res, AddressComponentType type) {
		//Tìm kiếm thông tin trong geocoding result
		for (AddressComponent component : res.addressComponents){
			for (AddressComponentType componentType : component.types){
				if(componentType == type)
					return component;
			}
		}
		throw new NullPointerException(null);
	}
	
	public Node searchOrCreateNode(Node source, GeocodingResult res, AddressComponentType type){
		//Tìm kiếm theo chiều rộng nút có nội dung như trong result
		try (Transaction tx = graphDb.beginTx()){
			for(Node childNode : graphDb.traversalDescription()
									.breadthFirst()
									.evaluator(Evaluators.atDepth(1))
									.relationships(RelTypes.INCLUDE, Direction.OUTGOING)
									.traverse(source)
									.nodes()){
				if (getComponent(res, type).longName.equals(childNode.getProperty("name"))){
					tx.success();
					return childNode;
				}
			}
			tx.success();
		}
		
		//Nếu không tìm thấy thì tạo nút mới có nội dung cần tìm
		Node newNode;
		try (Transaction tx = graphDb.beginTx()){
			newNode = graphDb.createNode(label);
			newNode.setProperty("type", type.toString());
			newNode.setProperty("name", getComponent(res, type).longName);
			source.createRelationshipTo(newNode, RelTypes.INCLUDE);
			tx.success();
		}
		
		return newNode;
	}
	
	public void addToNeo4j(GeocodingResult res, String idString){
		//Dang o muc county
		try (Transaction tx = graphDb.beginTx())
		{
			try {
				Node province, admlv2, sublocal1, route;
				//Tìm trước nút chưa thông tin về tỉnh. Đây là nút có mức ADMINISTRATIVE_AREA_LEVEL_1
				province = graphDb.findNode(provinceLabel
						, "name", this.getComponent(res, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1).longName);
				if (province == null){
					Node provNode = graphDb.createNode(provinceLabel);
					provNode.addLabel(label);
					provNode.setProperty("name", 
							this.getComponent(res, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1).longName);
					province = provNode;
				}
				
				//Tiếp tục đi theo các mức đến mức Đường (ROUTE)
				admlv2 = this.searchOrCreateNode(province, res, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2);
				sublocal1 = this.searchOrCreateNode(admlv2, res, AddressComponentType.SUBLOCALITY_LEVEL_1);
				route = this.searchOrCreateNode(sublocal1, res, AddressComponentType.ROUTE);
				
				//Tạo một lá và add vào cây
				Node newNode = graphDb.createNode(label);
				newNode.setProperty("geo_lat", res.geometry.location.lat);
				newNode.setProperty("id", idString);
				String routeNumber = getComponent(res, AddressComponentType.STREET_NUMBER).longName;
				if (routeNumber != null)
					newNode.setProperty("number", routeNumber);
				route.createRelationshipTo(newNode, RelTypes.INCLUDE);
				newNode.setProperty("geo_long", res.geometry.location.lng);
			} catch (NullPointerException e) {
				//Nếu một trong các mức của geocoding result bị thiếu, result đó sẽ bị bỏ qua ngay
				tx.failure();
				return;
			}
			tx.success();
		}
	}
}

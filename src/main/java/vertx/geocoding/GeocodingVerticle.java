package vertx.geocoding;

import static java.util.Arrays.asList;

import java.util.concurrent.TimeUnit;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.util.FastNoSuchElementException;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.bson.Document;
import org.bson.types.ObjectId;

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
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;

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
	private long timerID;
	private TitanGraph graph;
	private GraphTraversalSource g;
	private Vertex vietNamVertex;
	private boolean status;
	
	
	public void start(Future<Void> fut) throws Exception{
		graph = TitanFactory.open("vertxgeocoding.properties");
		vietNamVertex = graph.traversal().V().has("name", "Vietnam").next();
		/*label = DynamicLabel.label("geo");										
		provinceLabel = DynamicLabel.label("province");							
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("GeoDb");*/		
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase("test");
		notGeoCol = db.getCollection("DungTestMongo");							
		db.getCollection("doneDB");
		db.getCollection("failDB");

		initContext();
		this.context.setApiKey(this.config().getString("key"));
		
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
		timerID = this.setVertxTimer();
	}
	
	public void stop() {
		eb.send("scheduler", new JsonObject().put("type", "geocoder undeployed"));
		mongoClient.close();
		graph.close();
 		//graphDb.shutdown();
	}
	
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
	
	public void resetApiKey(String apiKey){
			context.setApiKey(apiKey);
	}

	public void reverseGeocoding() throws Exception {
		Document doc;
		if(isChangingKey)
			return;
		
		doc = notGeoCol.find(new Document("$or", asList(new Document("status", false), new Document("status", null))))
				.limit(1).iterator().next();
		ObjectId id = doc.get("_id", ObjectId.class);
		String idString = id.toHexString();
		
		try {
			double lat = Double.valueOf(doc.getString("geo_lat"));
			double lng = Double.valueOf(doc.getString("geo_long"));
			
			GeocodingResult[] results = GeocodingApi.newRequest(context)
						.latlng(new LatLng(lat, lng))
						.await();
			
			for(int i = 0; i < results.length; i++){
				this.addToTitan(results[i], idString, status);
				if (status) 
					notGeoCol.findOneAndUpdate(new Document("_id", id), 
							new Document("$set", new Document("status", true)));
			}
		} catch (NullPointerException e){
			return;
		} catch (OverQueryLimitException e){
			if(!isChangingKey){
				vertx.cancelTimer(timerID);
				isChangingKey = true;
				
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
			this.stop();
			return;
		} catch (NumberFormatException e) {
			notGeoCol.deleteOne(new Document("_id", id));
			return;
		}
		System.out.println("Done something");
	}
	
	public AddressComponent getComponent(GeocodingResult res, AddressComponentType type) {
		for (AddressComponent component : res.addressComponents){
			for (AddressComponentType componentType : component.types){
				if(componentType == type)
					return component;
			}
		}
		throw new NullPointerException(null);
	}
	
	public Vertex searchOrCreateNode(Vertex source, GeocodingResult res, AddressComponentType type){
		TitanTransaction tx = graph.newTransaction();
		String componentName = this.getComponent(res, type).longName;
		Vertex expected;
		try {
			expected = g.V(source.id()).out("include").has("name", componentName).next();
		} catch (FastNoSuchElementException e) {
			expected = tx.addVertex(T.label, type.toString(), "name", componentName);
			source.addEdge("include", expected);
			tx.commit();
			return expected;
		}
		
		tx.commit();
		return expected;
		/*try (Transaction tx = graphDb.beginTx()){
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
		
		Node newNode;
		try (Transaction tx = graphDb.beginTx()){
			newNode = graphDb.createNode(label);
			newNode.setProperty("type", type.toString());
			newNode.setProperty("name", getComponent(res, type).longName);
			source.createRelationshipTo(newNode, RelTypes.INCLUDE);
			tx.success();
		}
		
		return newNode;*/
	}
	
	public void addToTitan(GeocodingResult res, String idString, boolean status){
		TitanTransaction tx = graph.newTransaction();
		g = graph.traversal();
		try {
			Vertex province, admlv2, sublocal1, route;
			province = this.searchOrCreateNode(vietNamVertex, res, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1);
			admlv2 = this.searchOrCreateNode(province, res, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2);
			sublocal1 = this.searchOrCreateNode(admlv2, res, AddressComponentType.SUBLOCALITY_LEVEL_1);
			route = this.searchOrCreateNode(sublocal1, res, AddressComponentType.ROUTE);
			
			Vertex leaf = tx.addVertex(T.label, "place", "mongoID", idString);
			String routeNumber = getComponent(res, AddressComponentType.STREET_NUMBER).longName;
			if (routeNumber != null)
				leaf.property("route number", routeNumber);
			leaf.property("lat", res.geometry.location.lat, "long", res.geometry.location.lng);
			route.addEdge("include", leaf);
		} catch (NullPointerException e) {
			tx.rollback();
			status = false;
			return;
		}
		
		tx.commit();
		status = true;
		return;
		/*try (Transaction tx = graphDb.beginTx())
		{
			try {
				Node province, admlv2, sublocal1, route;
				province = graphDb.findNode(provinceLabel
						, "name", this.getComponent(res, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1).longName);
				if (province == null){
					Node provNode = graphDb.createNode(provinceLabel);
					provNode.addLabel(label);
					provNode.setProperty("name", 
							this.getComponent(res, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1).longName);
					province = provNode;
				}
				
				admlv2 = this.searchOrCreateNode(province, res, AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2);
				sublocal1 = this.searchOrCreateNode(admlv2, res, AddressComponentType.SUBLOCALITY_LEVEL_1);
				route = this.searchOrCreateNode(sublocal1, res, AddressComponentType.ROUTE);
				
				Node newNode = graphDb.createNode(label);
				newNode.setProperty("geo_lat", res.geometry.location.lat);
				newNode.setProperty("id", idString);
				String routeNumber = getComponent(res, AddressComponentType.STREET_NUMBER).longName;
				if (routeNumber != null)
					newNode.setProperty("number", routeNumber);
				route.createRelationshipTo(newNode, RelTypes.INCLUDE);
				newNode.setProperty("geo_long", res.geometry.location.lng);
			} catch (NullPointerException e) {
				tx.failure();
				return;
			}
			tx.success();
		}*/
	}
}

package vertx.integration.test;

import java.util.ArrayList;

import org.bson.Document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class LatLngVerticle extends AbstractVerticle  {
	private EventBus eb;
	MongoCollection<Document> notGeoCol;
	MongoCollection<Document> doneGeo;
	Gson gson;
	int index = 0;
	
	public void start() throws InterruptedException {
		new ArrayList<Document>();
		MongoClient mongoClient = new MongoClient();
		MongoDatabase db = mongoClient.getDatabase("test");
		gson = new GsonBuilder().create();
		
		notGeoCol = db.getCollection("DungTestMongo", Document.class);
		doneGeo = db.getCollection("doneGeoCol", Document.class);
		eb = vertx.eventBus();
		
		handleDB();
	}
	
	public void stop(){
		eb.send("scheduler", new JsonObject().put("type", "latlng off"));
	}
	
	
	
	@SuppressWarnings("static-access")
	public void handleDB() throws InterruptedException{
		while(index < 250){
			eb.send("geocoder", new JsonObject().put("type", "geocoding"),
					res -> {
						if (res.succeeded())
							System.out.println("latlng gui geocoder thanh cong");
						else {
							System.out.println("gui khong thanh cong");
						}
					});
			index++;
			Thread.currentThread().sleep(100);
		}
	}
}

package vertx.geocoding;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.bson.Document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class LatLngVerticle extends AbstractVerticle  {
	private EventBus eb;
	MongoCollection<Document> notGeoCol;
	MongoCollection<Document> doneGeo;
	Gson gson;
	
	
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
	
	public JsonObject getLatLng(Document doc){
		return new JsonObject().put("lat", doc.getString("geo_lat"))
							.put("lng", doc.getString("geo_long"));
	}
	
	@SuppressWarnings("static-access")
	public void handleDB() throws InterruptedException{
		while(notGeoCol.count() > 0){
			Document doc = notGeoCol.findOneAndDelete(null);
			eb.send("geocoder", getLatLng(doc).put("type", "geocoding"),
					res -> {
						if (res.succeeded())
							doneGeo.insertOne(doc);
						else {
							notGeoCol.insertOne(doc);
						}
					});
			Thread.currentThread().sleep(200);
		}
	}
}

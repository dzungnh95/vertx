package mongodb.test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static java.util.Arrays.asList;

public class FirstOne {
	
	
	public static void main(String[] args) throws ParseException{
		MongoClient mongoClient = new MongoClient();
		MongoDatabase db = mongoClient.getDatabase("test");
		
		MongoCollection<Document> col = db.getCollection("DungTestMongo", Document.class);
		
		Document doc = (Document) col.find().limit(1).iterator().next();
		
		ObjectId id = doc.getObjectId("_id");
		
		col.findOneAndUpdate(new Document("_id", id), new Document("$set", new Document("status", true)));
		
		System.out.println(id.toHexString());
		System.out.println(col.find(new Document("_id", id)).limit(1).iterator().next().getBoolean("status"));
		        
	}
}

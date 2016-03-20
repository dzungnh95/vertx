package vertx.integration.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.OverDailyLimitException;
import com.google.maps.errors.OverQueryLimitException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

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
	private Gson gson;
	int index = 0;
	public void start(Future<Void> fut){
		gson = new GsonBuilder().setPrettyPrinting().create();
		initContext();
		eb = vertx.eventBus();
		eb.consumer("geocoder", 
				new Handler<Message<JsonObject>>(){
					@Override
					public void handle(Message<JsonObject> message) {
						// TODO Auto-generated method stub
						if (message.body().getString("type").equals("geocoding"))
							try {
								reverseGeocoding(message);
								index++;
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						else if (message.body().getString("type").equals("set key"))
							resetApiKey(message.body().getString("api key"));
						}
					});
		this.context.setApiKey(this.config().getString("key"));
		System.out.println("Khoi tao cho geocoder");
		// Thông báo cho main verticle deploy thành công và gửi key để khởi tạo context
		fut.complete();
	}
	
	public void stop(){
		eb.send("scheduler", new JsonObject().put("type", "geocoder off"));
	}
	
	public void initContext(){
		
	}
	
	//Đổi key sau mỗi lần over query limit
	public void resetApiKey(String apiKey){
			context.setApiKey(apiKey);
	}
	
	//Thực hiện geocode, nhận vào một String dạng "21,000546, 65,154864"
	public void reverseGeocoding(Message<JsonObject> mes) throws Exception {
		if(isChangingKey) {
			mes.fail(0, null);
			return;
		}
		
		try {
			if (index == 249)
				throw new OverDailyLimitException(null);
			if (index % 50 == 0)
				throw new OverQueryLimitException(null);
			switch (index%5){
			case 1: throw new IOException();
			
			case 4: throw new Exception();
			}
		} catch (IOException e){
			mes.fail(0, null);
			System.err.println("IOException");
			return;
		} catch (OverQueryLimitException e){
			mes.fail(0, null);
			System.err.println("Over query limit");
			if(!isChangingKey){
				isChangingKey = true;
				eb.send("scheduler", new JsonObject().put("type", "need change key"),
						res -> {
							if(((JsonObject)res.result().body()).getBoolean("can switch")){
								context.setApiKey(((JsonObject)res.result().body()).getString("api key"));
								System.out.println(((JsonObject)res.result().body()).getString("api key"));
								isChangingKey = false;
							} else
								try {
									this.stop();
								} catch (Exception e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
						});
			}
			e.printStackTrace();
			return;
		} catch (OverDailyLimitException e) {
			System.err.println("Over daily limit");
			this.stop();
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			
			return;
		}
		mes.reply(null);
	}
}

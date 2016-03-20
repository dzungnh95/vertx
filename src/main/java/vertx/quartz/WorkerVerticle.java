package vertx.quartz;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class WorkerVerticle extends AbstractVerticle{
	private EventBus eb;
	
	public void start(){
		
		eb = vertx.eventBus();
		eb.consumer("worker", 
				new Handler<Message<JsonObject>>() {

					public void handle(Message<JsonObject> message) {
						// TODO Auto-generated method stub
						System.out.println("Receive message");
						if (message.body().getBoolean("ping"))
							System.out.println("Hello world!");
					}
			
		});
		System.out.println("Successful deployment!");	
	}
	
	@Override
	public void stop(){
		System.out.println("worker done");
	}
}
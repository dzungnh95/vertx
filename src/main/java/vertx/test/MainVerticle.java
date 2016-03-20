package vertx.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;


public class MainVerticle extends AbstractVerticle{
	private EventBus eb;
	
	@Override
	public void start(){
		eb = vertx.eventBus();
		eb.consumer("main verticle", 
				mes -> {
					mes.fail(0, "sth");
		});
		
	}
}

package vertx.test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class Main2 {
	public static void main(String[] args){
		Vertx vertx = Vertx.vertx();
		
		
		vertx.deployVerticle(Worker.class.getName(),
				res -> {
					if(res.succeeded())
						vertx.eventBus().send("worker", "something");
				});
				
	}
}

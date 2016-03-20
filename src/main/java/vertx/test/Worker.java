package vertx.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;

public class Worker extends AbstractVerticle {
	public void start(){
		vertx.eventBus().consumer("worker", 
				mes -> {
					long timerID = vertx.setPeriodic(300,
							new Handler<Long>(){

								@Override
								public void handle(Long arg0) {
									// TODO Auto-generated method stub
									System.out.println("something");
								}
						
					});
					System.out.println("Done");
				});
	}
}

package vertx.quartz;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import vertx.util.Runner;


public class TestVerticle extends AbstractVerticle{
	public static void main(String[] args) throws InterruptedException {
		Vertx vertx = Vertx.vertx();
		EventBus eb = vertx.eventBus();
		vertx.deployVerticle(SchedulerVerticle.class.getName());
		vertx.deployVerticle(WorkerVerticle.class.getName(), new DeploymentOptions().setWorker(true));
		Thread.currentThread().sleep(2000);
		
		eb.send("scheduler", new JsonObject()
				.put("cron", "*/5 * * * * ?")
				.put("triggerAddress", "worker"),
				new DeliveryOptions().setSendTimeout(5000));
		System.out.println("Sent");
	}
}
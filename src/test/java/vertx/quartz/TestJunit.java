package vertx.quartz;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestJunit {
	private Vertx vertx;
	private EventBus eb;
	
	@Before
	public void setUp(){
		vertx = Vertx.vertx();
		vertx.deployVerticle(Schedulers.class.getName());
		vertx.deployVerticle(Worker.class.getName());
		eb = vertx.eventBus();
	}
	
	@After
	public void tearDown(){
		vertx.close();
	}
	
	@Test
	public void testMyApp() {
		
		eb.send("scheduler", new JsonObject()
								.put("cron", "*/5 * * * * ?")
								.put("triggerAddress", "worker"));
	}
}
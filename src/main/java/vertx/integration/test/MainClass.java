package vertx.integration.test;

import io.vertx.core.Vertx;

public class MainClass {
	public static void main(String[] args){
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(SchedulerVerticle.class.getName());
	}
}

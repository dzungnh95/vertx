package vertx.geocoding;

import io.vertx.core.Vertx;

public class MainClass {
	public static void main(String[] args) throws InterruptedException{
		Vertx.vertx().deployVerticle(SchedulerVerticle.class.getName());
	}
}

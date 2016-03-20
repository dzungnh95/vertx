package vertx.integration.test;

import java.text.ParseException;

import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class SchedulerVerticle extends AbstractVerticle{
	private String dailyCron = "0 */1 * * * ?";
	private String[] apiKey;
	private int index = 0;
	private EventBus eb;
	private Scheduler scheduler;
	boolean isGeocoderDeployed = false;
	boolean isLatLngDeployed = false;
	String latlngVerticleID;
	
	public void start() throws SchedulerException, InterruptedException{
		apiKey = new String[10];
		for(int i = 0; i < 10; i++)
			apiKey[i] = String.valueOf(i + 1);
		try {
			this.scheduler = StdSchedulerFactory.getDefaultScheduler();
			this.scheduler.start();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		eb = vertx.eventBus();
		eb.send("geocoder", new JsonObject().put("type", "set key")
											.put("api key", apiKey[index]));
		eb.consumer("scheduler", 
				new Handler<Message<JsonObject>>(){

					@Override
					public void handle(Message<JsonObject> mes) {
						// TODO Auto-generated method stub
						if (mes.body().getString("type").equals("need change key")){
							index++;
							mes.reply(new JsonObject().put("api key", apiKey[index])
										.put("can switch", true));
						}
						else if (mes.body().getString("type").equals("daily switch")){
							dailySwitch();
						}
						else if (mes.body().getString("type").equals("geocoder off")){
							isGeocoderDeployed = false;
						}
						else if (mes.body().getString("type").equals("latlng off")){
							isLatLngDeployed = false;
						}
					}
			
		});
		
		vertx.deployVerticle(GeocodingVerticle.class.getName(),
				new DeploymentOptions().setConfig(new JsonObject().put("key", apiKey[0])),
				res -> {
					if (res.succeeded()) {
						vertx.deployVerticle(LatLngVerticle.class.getName(),
								res1 -> {
									if (res1.succeeded()){
										this.isLatLngDeployed = true;
										this.latlngVerticleID = res1.result();
										try {
											dailyScheduling();
										} catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								});
						this.isGeocoderDeployed = true;
					}
				});
		
		
		
	}
	
	private void dailyScheduling() throws SchedulerException{
		try {
			JobDetailImpl jobDetail = new JobDetailImpl();
			jobDetail.setJobClass(SwitchJob.class);
			JobKey jobKey = new JobKey("Daily reset key");
			jobDetail.setKey(jobKey);
			JobDataMap data = new JobDataMap();
			data.put("eventBus", vertx.eventBus());
			jobDetail.setJobDataMap(data);
			
			CronTriggerImpl cronTrigg = new CronTriggerImpl();
			cronTrigg.setName("Daily reset key");
			cronTrigg.setCronExpression(dailyCron);
			this.scheduler.scheduleJob(jobDetail, cronTrigg);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void dailySwitch(){
		index = 0;
		if (isGeocoderDeployed)
			eb.send("geocoder", new JsonObject().put("type", "reset key")
					.put("api key", apiKey[index]));
		else {
			if (!isLatLngDeployed)
				vertx.undeploy(latlngVerticleID);
			vertx.deployVerticle(GeocodingVerticle.class.getName(),
					new DeploymentOptions().setConfig(new JsonObject().put("key", apiKey[0])),
					res -> {
						if (res.succeeded()) {
							vertx.deployVerticle(LatLngVerticle.class.getName(),
									res1 -> {
										if (res1.succeeded()){
											isLatLngDeployed = true;
											latlngVerticleID = res1.result();
										}
									});
							isGeocoderDeployed = true;
						}
					});
		}
	}
}

package vertx.geocoding;

import java.text.ParseException;

import org.quartz.JobDataMap;
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
	private String dailyCron = "0 0 0 * * ?";
	private String[] apiKey;
	private int index = 0;
	private EventBus eb;
	private Scheduler scheduler;
	boolean isGeocoderDeployed = false;
	String geocoderVerticleID;
	
	public void start() throws SchedulerException{
		apiKey = new String[5];
		apiKey[0] = "AIzaSyBgmIynJKwgSzl7QEhD4skoIxl2Vnv28WY";
		apiKey[1] = "AIzaSyA5MacwhPs4ApHoO3qZoEPQgtsA_BAjR8U";
		apiKey[2] = "AIzaSyCi9sE7WOGNDCSw6HxSD547hHF3XZlIC6w";
		apiKey[3] = "AIzaSyDKeockc4HV3SPB0li6NAW0YVBQBKBcVcc";
		apiKey[4] = "AIzaSyDJZ46nCwOzq0GQvekOfjEt498o88LmNvc";
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
							switchKey(mes);
						}
						else if (mes.body().getString("type").equals("geocoder undeployed")){
							isGeocoderDeployed = false;
						}
						else if (mes.body().getString("type").equals("daily switch")){
							dailySwitch();
						}
					}
			
		});
		
		vertx.deployVerticle(GeocodingVerticle.class.getName(),
				new DeploymentOptions().setConfig(new JsonObject().put("key", apiKey[0])),
				res -> {
					if (res.succeeded()) {
						this.geocoderVerticleID = res.result();
					}
				});
		
		dailyScheduling();
		
	}
	
	public void dailyScheduling() throws SchedulerException{
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
	
	public void switchKey(Message<JsonObject> mes) {
		index++;
		if (index == 5) {
			vertx.undeploy(geocoderVerticleID);
			this.isGeocoderDeployed = false;
			mes.fail(0, "sth");;
		}
		else {
			mes.reply(new JsonObject().put("api key", apiKey[index]));
		}
	}
	
	public void dailySwitch(){
		index = 0;
		if (isGeocoderDeployed)
			eb.send("geocoder", new JsonObject().put("type", "reset key")
					.put("api key", apiKey[index]));
		else {
			vertx.deployVerticle(GeocodingVerticle.class.getName(),
					new DeploymentOptions().setConfig(new JsonObject().put("key", apiKey[0])),
					res -> {
						if (res.succeeded()) {
							this.geocoderVerticleID = res.result();
							isGeocoderDeployed = true;
						}
					});
		}
	}
}

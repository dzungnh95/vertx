package vertx.quartz;

import java.text.ParseException;

import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.*;

public class SchedulerVerticle extends AbstractVerticle{	
	private Scheduler scheduler;
	
	@Override
	public void start(){
		try {
			this.scheduler = StdSchedulerFactory.getDefaultScheduler();
			this.scheduler.start();
			
			vertx.eventBus().consumer("scheduler", 
					new Handler<Message<JsonObject>>() {

				public void handle(Message<JsonObject> message) {
					// TODO Auto-generated method stub
					System.out.println("sche receive");
					handleMessage(message);
					//message.reply(arg0);
				}
				
			});
		} catch(SchedulerException e){
			e.printStackTrace();
		}
		System.out.println("Succesful Deployment, too");
	}
	
	public void stop(){
		System.out.println("scheduler done");
		try{
			this.scheduler.shutdown();
			this.scheduler = null;
		} catch (SchedulerException e){
			e.printStackTrace();
		}
	}
	
	public void handleMessage(Message<JsonObject> message) {
		// TODO Auto-generated method stub
		if (message.body().getString("cron") != null){
			
			String triggerAddress = message.body().getString("triggerAddress");
			
			try {
				JobDetailImpl jobDetail = new JobDetailImpl();
				jobDetail.setJobClass(Ping.class);
				JobKey jobKey = new JobKey("pingJob");
				jobDetail.setKey(jobKey);
				JobDataMap jobDataMap = new JobDataMap();
				jobDataMap.put("eventBus", vertx.eventBus());
				jobDataMap.put("triggerAddress", triggerAddress);
				jobDetail.setJobDataMap(jobDataMap);
				
				CronTriggerImpl cronTrigg = new CronTriggerImpl();
				cronTrigg.setName("pingJob");
				cronTrigg.setCronExpression(message.body().getString("cron"));
				this.scheduler.scheduleJob(jobDetail, cronTrigg);
			} catch (ParseException e){
				e.printStackTrace();
			} catch (SchedulerException e){
				e.printStackTrace();
			}
			System.out.println("Sent message");
		}
	}
}

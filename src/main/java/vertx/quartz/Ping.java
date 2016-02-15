package vertx.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class Ping implements Job{

	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		EventBus eb = (EventBus) context.getMergedJobDataMap().get("eventBus");
		JsonObject ping = new JsonObject().put("ping", true);
		eb.send("worker", ping);
		System.out.println("Ping executed");
	}

}

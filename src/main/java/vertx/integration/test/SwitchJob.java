package vertx.integration.test;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class SwitchJob implements Job{
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		System.out.println("daily switching");
		EventBus eb = (EventBus) context.getMergedJobDataMap().get("eventBus");
		eb.send("scheduler", new JsonObject().put("type", "daily switch"));
	}

}

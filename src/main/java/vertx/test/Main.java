package vertx.test;

import java.text.ParseException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;


public class Main {
	public static int sth = 0;
	
	public static void main(String[] args) throws InterruptedException, SchedulerException, ParseException {
		doSth1();
		//doSth2();
	}
	
	public static void doSth1() throws SchedulerException{
		sth = 10;
		Scheduler scheduler;
		scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();
	}
	
	public static void doSth2() throws ParseException{
		JobDetailImpl jobDetail = new JobDetailImpl();
		jobDetail.setJobClass(ScheduleJob.class);
		JobKey jobKey = new JobKey("Daily reset key");
		
		CronTriggerImpl cronTrigg = new CronTriggerImpl();
		cronTrigg.setName("Daily reset key");
		cronTrigg.setCronExpression("*/5 * * * * ?");
	}
	
	public class ScheduleJob implements Job{

		@Override
		public void execute(JobExecutionContext arg0) throws JobExecutionException {
			System.out.println(sth);
		}
		
	}
}

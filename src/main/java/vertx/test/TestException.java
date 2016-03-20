package vertx.test;

public class TestException {
	public static void main(String[] args){
		System.out.println("start");
		try {
			String str = null;
			str.equals("sth");
			System.out.println("something");
		} catch (NullPointerException e){
			return;
		} finally {
			System.out.println("end");
		}
		 System.out.println("do sth");
	}
}

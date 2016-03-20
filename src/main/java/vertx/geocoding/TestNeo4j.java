package vertx.geocoding;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class TestNeo4j {
	public static void main(String[] args) throws InterruptedException{
		/*Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(SchedulerVerticle.class.getName());*/
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( "testDB" );
		registerShutdownHook( graphDb );
		Node firstNode;
		Node secondNode;
		Relationship relationship;
		
		try (Transaction tx = graphDb.beginTx())
		{
			tx.success();
		}
		firstNode = graphDb.createNode();
		firstNode.setProperty("message", "Hello, ");
		secondNode = graphDb.createNode();
		
		relationship = firstNode.createRelationshipTo(secondNode, RelTypes.KNOWS);
		relationship.setProperty("message", "brave Neo4j");
		System.out.println("sth");
		System.out.print( firstNode.getProperty( "message" ) );
		System.out.print( relationship.getProperty( "message" ) );
		System.out.print( secondNode.getProperty( "message" ) );
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		 // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running application).
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        @Override
	        public void run()
	        {
	            graphDb.shutdown();
	        }
	    } );
	}
}

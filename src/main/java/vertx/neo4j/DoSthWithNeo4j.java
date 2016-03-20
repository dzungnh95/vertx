package vertx.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class DoSthWithNeo4j {
	
	public static void main(String[] args){
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( "testDB" );
	}
}

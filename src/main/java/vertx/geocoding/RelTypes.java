package vertx.geocoding;

import org.neo4j.graphdb.RelationshipType;

public enum RelTypes implements RelationshipType{
	KNOWS,
	
	INCLUDE
}

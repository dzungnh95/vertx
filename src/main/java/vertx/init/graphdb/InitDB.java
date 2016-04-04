package vertx.init.graphdb;

import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.google.maps.model.AddressComponentType;
import com.thinkaurelius.titan.core.Multiplicity;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.schema.ConsistencyModifier;
import com.thinkaurelius.titan.core.schema.TitanGraphIndex;
import com.thinkaurelius.titan.core.schema.TitanManagement;

public class InitDB {

	public static void main(String[] args) {
		TitanGraph graph = TitanFactory.open("vertxgeocoding.properties");
		TitanManagement mgmt = graph.openManagement();
		PropertyKey name = mgmt.makePropertyKey("name").dataType(String.class).make();
		TitanManagement.IndexBuilder nameIndexBuilder = mgmt.buildIndex("name", Vertex.class)
				.addKey(name);
        TitanGraphIndex namei = nameIndexBuilder.buildCompositeIndex();
        mgmt.setConsistency(namei, ConsistencyModifier.LOCK);
        
		mgmt.makeEdgeLabel("include").multiplicity(Multiplicity.SIMPLE);
		
		mgmt.makeVertexLabel("country").make();
		mgmt.makeVertexLabel("province").make();
		mgmt.makeVertexLabel(AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2.toString()).make();
		mgmt.makeVertexLabel(AddressComponentType.SUBLOCALITY_LEVEL_1.toString()).make();
		mgmt.makeVertexLabel(AddressComponentType.ROUTE.toString()).make();

		mgmt.makePropertyKey("mongoID").dataType(String.class).make();
		mgmt.makePropertyKey("route number").dataType(String.class).make();
		mgmt.makePropertyKey("lat").dataType(Double.class).make();
		mgmt.makePropertyKey("long").dataType(Double.class).make();
		
		
		
		mgmt.commit();
		
		TitanTransaction tx = graph.newTransaction();
		tx.addVertex(T.label, "country", "name", "Vietnam");
		tx.commit();
		
		graph.close();
	}

}

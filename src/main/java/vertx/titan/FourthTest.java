package vertx.titan;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

public class FourthTest {
	public static void main(String[] args){
		TinkerGraph graph = TinkerFactory.createModern();
		GraphTraversalSource g = graph.traversal();
		g.V().forEachRemaining(item -> {
			System.out.println(item.id());
		});
		
	}
}

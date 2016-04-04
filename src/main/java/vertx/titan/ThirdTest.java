package vertx.titan;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;

public class ThirdTest {
	public static void main(String[] args){
		TitanGraph graph = TitanFactory.open("vertxgeocoding.properties");
		GraphTraversalSource g = graph.traversal();
		System.out.println(g.V(4144).next().value("name").toString());
				
	}		
}

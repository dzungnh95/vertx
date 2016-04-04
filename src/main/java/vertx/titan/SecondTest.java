package vertx.titan;

import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.Multiplicity;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.VertexLabel;
import com.thinkaurelius.titan.core.schema.TitanManagement;

public class SecondTest {
	public static void main(String[] args){
		TitanGraph graph = TitanFactory.open("vertxgeocoding.properties");
		TitanManagement mgmt = graph.openManagement();
		VertexLabel provinceLabel = mgmt.makeVertexLabel("province").make();
		VertexLabel admin2Label = mgmt.makeVertexLabel("adminLvl2").make();
		VertexLabel sub1Label = mgmt.makeVertexLabel("subLvl2").make();
		EdgeLabel include = mgmt.makeEdgeLabel("include").multiplicity(Multiplicity.SIMPLE).make();
		PropertyKey name = mgmt.makePropertyKey("name").dataType(String.class).make();
		
		TitanManagement.IndexBuilder nameIndexBuilder = mgmt.buildIndex("name", Vertex.class).addKey(name).unique();		
		mgmt.commit();
		
		TitanTransaction tx = graph.newTransaction();
		Vertex hcmCity = tx.addVertex(T.label, "province"
										,"name", "Ho Chi Minh City");
		Vertex hanoi = tx.addVertex(T.label, "province"
										,"name", "Ha Noi");
		//TP Ha Noi
		Vertex baDinh = tx.addVertex(T.label, "adminLvl2"
									,"name", "Ba Dinh");
		hanoi.addEdge("include", baDinh);
		
		Vertex hoanKiem = tx.addVertex(T.label, "adminLvl2"
									,"name", "Hoan Kiem");
		hanoi.addEdge("include", hoanKiem);
		
		Vertex cauGiay = tx.addVertex(T.label, "adminLvl2"
									,"name", "Cau Giay");
		hanoi.addEdge("include", cauGiay);
		
		//Quan Ba Dinh
		Vertex congVi = tx.addVertex(T.label, "subLvl2"
									,"name", "Cong Vi");
		Vertex lieuGiai = tx.addVertex(T.label, "subLvl2"
									,"name", "Lieu Giai");
		Vertex doiCan = tx.addVertex(T.label, "subLvl2"
				,"name", "Doi Can");
		baDinh.addEdge("include", congVi);
		baDinh.addEdge("include", doiCan);
		baDinh.addEdge("include", lieuGiai);
		
		//Quan Hoan Kiem
		Vertex hangBong = tx.addVertex(T.label, "subLvl2"
				,"name", "Hang Bong");
		Vertex hangBai = tx.addVertex(T.label, "subLvl2"
				,"name", "Hang Bai");
		Vertex dongXuan = tx.addVertex(T.label, "subLvl2"
				,"name", "Dong Xuan");
		hoanKiem.addEdge("include", hangBong);
		hoanKiem.addEdge("include", hangBai);
		hoanKiem.addEdge("include", dongXuan);
		
		//Quan Cau Giay
		Vertex dichVong = tx.addVertex(T.label, "subLvl2"
				,"name", "Dich Vong");
		Vertex nghiaTan = tx.addVertex(T.label, "subLvl2"
				,"name", "Nghia Tan");
		Vertex quanHoa = tx.addVertex(T.label, "subLvl2"
				,"name", "Quan Hoa");
		cauGiay.addEdge("include", dichVong);
		cauGiay.addEdge("include", nghiaTan);
		cauGiay.addEdge("include", quanHoa);
		
		tx.commit();
		graph.close();
	}
}

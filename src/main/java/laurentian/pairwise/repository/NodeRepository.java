package laurentian.pairwise.repository;

import laurentian.pairwise.request.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {


    Node findByNodeName(@Param("node_name") String nodeName);
}

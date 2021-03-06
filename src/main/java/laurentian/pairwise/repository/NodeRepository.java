package laurentian.pairwise.repository;

import laurentian.pairwise.request.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * This is JPA repo, has all the important method to fetch, save and delete from the database.
 * */
@Repository
@CrossOrigin
@RepositoryRestResource(collectionResourceRel = "node", path = "nodes")
public interface NodeRepository extends JpaRepository<Node, Long> {

    Node findByNodeName(@Param("node_name") String nodeName);
}

package laurentian.pairwise.bootstrap;

import laurentian.pairwise.repository.NodeRepository;
import laurentian.pairwise.request.Node;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Loader implements CommandLineRunner {

    private NodeRepository nodeRepository;

    public Loader(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Node rootNode = new Node();
        rootNode.setNodeName("Root");
        rootNode.setValue(100);
        nodeRepository.save(rootNode);
    }
}

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

    /**
     * Post construct of application, it will create a default node of ROOT. User always starts with a
     * default node to add on to.
     *   * @param args : Default args (Not using)
     *   * @throws Exception : Exception will be thrown if unable to persist the data on to the database.
     * */

    @Override
    public void run(String... args) throws Exception {
        //Creating an instance of Node class.
        Node rootNode = new Node();
        //Setting the nodeName to ROOT
        rootNode.setNodeName("Root");
        //Setting the ROOT value to 100
        rootNode.setValue(100);
        //Saving the ROOT node on to the DB.
        nodeRepository.save(rootNode);
    }
}

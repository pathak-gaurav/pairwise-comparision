package laurentian.pairwise.bootstrap;

import laurentian.pairwise.repository.NodeRepository;
import laurentian.pairwise.repository.UploadFlagRepository;
import laurentian.pairwise.request.Node;
import laurentian.pairwise.request.UploadFlag;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Loader implements CommandLineRunner {

    private NodeRepository nodeRepository;
    private UploadFlagRepository uploadFlagRepository;

    public Loader(NodeRepository nodeRepository, UploadFlagRepository uploadFlagRepository) {
        this.nodeRepository = nodeRepository;
        this.uploadFlagRepository = uploadFlagRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Node rootNode = new Node();
        rootNode.setNodeName("Root");
        rootNode.setValue(100);
        nodeRepository.save(rootNode);
        UploadFlag flag = new UploadFlag(false);
        uploadFlagRepository.save(flag);
    }
}

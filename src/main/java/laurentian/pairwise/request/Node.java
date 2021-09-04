package laurentian.pairwise.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String nodeName;
    private String parentNodeId;
    private double value;


    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "node")
    private List<Node> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    private Node node;

    public Node(String nodeName, String parentNodeId, double value, List<Node> children, Node node) {
        this.nodeName = nodeName;
        this.parentNodeId = parentNodeId;
        this.value = value;
        this.children = children;
        this.node = node;
    }

    public Node(String nodeName, String parentNodeId, double value, Node node) {
        this.nodeName = nodeName;
        this.parentNodeId = parentNodeId;
        this.value = value;
        this.node = node;
    }

    public Node(double value) {
        this.value = value;
    }

    /**
     * Added for Analyze if issue found we will delete it.
     * */
    public Node(Node node) {
        this.node = node;
    }

    @JsonIgnore
    @JsonProperty(value = "node")
    public Node getNode() {
        return node;
    }

    public String showChildren() {
        if (getChildren().isEmpty() || getChildren() == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (Node n : getChildren()) {
                sb.append("Node " + n.getId() + " ");
            }
            return " Children: " + sb.toString();
        }
    }

    @JsonIgnore
    @JsonProperty(value = "aleaf")
    public Boolean isALeaf() {
        if (getChildren().isEmpty() || getChildren() == null) {
            return true;
        } else {
            return false;
        }
    }
}

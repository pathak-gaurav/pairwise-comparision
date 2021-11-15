package laurentian.pairwise.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NodeModel {
    String nodeName;
    String parentName;
    double value;
}

package laurentian.pairwise.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VirusScanningResponse {

    @JsonProperty("CleanResult")
    public Boolean cleanResult;
    @JsonProperty("FoundViruses")
    public List<FoundViruse> foundViruses = null;

}

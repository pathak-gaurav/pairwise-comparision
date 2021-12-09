package laurentian.pairwise.repository;

import laurentian.pairwise.request.Node;
import laurentian.pairwise.request.UploadFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

@Repository
@CrossOrigin
public interface UploadFlagRepository extends JpaRepository<UploadFlag, Long> {
}

package laurentian.pairwise.configuration;

import laurentian.pairwise.request.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/***
 * This is the configuration class to Expose the id's of the NODE Class, by default it will be not visible.
 */
@Configuration
public class MyDataRestConfig implements RepositoryRestConfigurer {

    /***
     * @field: EntityManager interface is used to allow applications to manage and search for entities in the relational
     *          database. The EntityManager is an API that manages the lifecycle of entity instances.
     */
    private EntityManager entityManager;


    /**
     * Constructor: For Constructor based Autowiring.
     * */
    @Autowired
    public MyDataRestConfig(EntityManager theEntityManager) {
        entityManager = theEntityManager;
    }


    /***
     * This method is used to expose the ID's of the Node Class.
     * @param config : Spring Data REST configuration options.
     */
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {

        HttpMethod[] theUnsupportedActions = {HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE};

        // disable HTTP methods for Node: PUT, POST and DELETE
        config.getExposureConfiguration()
                .forDomainType(Node.class)
                .withItemExposure((metdata, httpMethods) -> httpMethods.disable(theUnsupportedActions))
                .withCollectionExposure((metdata, httpMethods) -> httpMethods.disable(theUnsupportedActions));

        // call an internal helper method
        exposeIds(config);

    }

    private void exposeIds(RepositoryRestConfiguration config) {

        // expose entity ids
        // - get a list of all entity classes from the entity manager
        Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();

        // - create an array of the entity types
        List<Class> entityClasses = new ArrayList<>();

        // - get the entity types for the entities
        for (EntityType tempEntityType : entities) {
            entityClasses.add(tempEntityType.getJavaType());
        }

        // - expose the entity ids for the array of entity/domain types
        Class[] domainTypes = entityClasses.toArray(new Class[0]);
        config.exposeIdsFor(domainTypes);
    }
}

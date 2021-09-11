package laurentian.pairwise.controller;

import laurentian.pairwise.repository.NodeRepository;
import laurentian.pairwise.request.Node;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "${app.version.v1}")
public class PairwiseController {

    private NodeRepository nodeRepository;

    public PairwiseController(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    /**
     * This is to create the root node and add the node to the existing node
     */
    @RequestMapping(value = "/pairwise", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<Object> pairwiseAddNode(@RequestBody Node node) {
        /**
         * If request body of node has no parentId which means it is null and node does not have name i.e 'ROOT'
         * then this is an error because a ROOT is top node and only it can have a condition where it's parent
         * node can be NULL.
         * */
        if (node.getParentNodeId() == null && !node.getNodeName().equalsIgnoreCase("ROOT")) {
            return new ResponseEntity("Root Node is Required", HttpStatus.BAD_REQUEST);
            /**
             *Now we are checking whether there is already a node whose name is similar to the node coming from
             * request body then we are going to throw an error as two node cannot have same name.
             * Name must be unique
             * */
        } else if (nodeRepository.findByNodeName(node.getNodeName()) != null) {
            return new ResponseEntity("Node Name Already Exist", HttpStatus.BAD_REQUEST);
        }
        /**
         * If the request body of node does not have any parentNodeId which means parentNodeId is null
         * and request body node name is equal to ROOT then we allow the creation of ROOT node.
         * */
        else if (node.getParentNodeId() == null && node.getNodeName().equalsIgnoreCase("ROOT")) {
            /**
             * saving the node in the database.
             * */
            node.setValue(100);
            nodeRepository.save(node);
        }
        /**
         * Add one more condition whether the parentNodeId exists or not and then allow to save node else
         * throw an error that 'invalid parentNodeId'
         * */
        /**
         * If all the above if else condition fails then it means we are adding a node under root node or may
         * be under some other node.
         * */
        else {
            /** Since we are adding a node under another node then we must also get an parentNodeId under which
             * this node coming from request body want's to get added.
             *
             * So fetching the parentNodeId from requestBody.
             * */
            String parentNodeId = node.getParentNodeId();
            /**
             * Once we get the parentNodeId then from the database we will retrieve the Object of that parentNodeId.
             * */
            Node parentNode = nodeRepository.findById(Long.parseLong(parentNodeId)).orElse(null);
            /**
             * We are create a new Node by passing the value in constructor from request body. At this moment
             * this node is not saved in the database.
             * */
            Node nodeToAdd = new Node(node.getNodeName(), node.getParentNodeId(), node.getValue(), parentNode);
            /**
             * It is also important to maintain the tree structure thus we have to add the created node in previous step
             * to the list of getChildren.
             * Every node has an ArrayList which is name Children, if root got a children then we will insert the new node
             * in Root's arraylist.
             * Consider a new node named 'A' want's to added under ROOT then we will create a node object for 'A'
             * and then that node object of 'A' will be added under the ArrayList of parentNode i.e. ROOT
             * */
            parentNode.getChildren().add(nodeToAdd);
            /**
             * Finally we save the node in the database.
             * */
            nodeRepository.save(nodeToAdd);
        }
        List<Node> all = nodeRepository.findAll();
        return new ResponseEntity<>(all, HttpStatus.CREATED);
    }

    /**
     * This is to delete the node and it's children
     */
    @RequestMapping(value = "/pairwise", method = RequestMethod.DELETE)
    public @ResponseBody
    ResponseEntity<Object> pairwiseDeleteNode(@RequestParam Long nodeId) {
        /**
         * Checking whether the node with the nodeId exist in the database that mean checking whether it is not null.
         * Also checking whether the node is not the ROOT node, the reason to check this condition because root node parentNodeId is NULL.
         * and if we pass null as parentNodeId in below code it's gonna throw number format exception.
         * So to delete root node we have a separate else condition. And to delete any other node except ROOT below 'if' condition will execute.
         * */
        if (nodeRepository.findById(nodeId) != null && !nodeRepository.findById(nodeId).orElse(null).getNodeName().equalsIgnoreCase("Root")) {
            /** Getting the node object from database using nodeId which is passed as the parameter.
             * */
            Node node = nodeRepository.findById(nodeId).orElse(null);
            /** Once we get the node based on the nodeId which is passed as param, now we are going to fetch the object of it's parent because we want to
             * remove the node object from it's parentNode ArrayList.
             * */
            Node parentNode = nodeRepository.findById(Long.parseLong(node.getParentNodeId())).orElse(null);
            /** Removing node from it's parent arraylist. Also parent can have many children so we want to remove specific node which
             * was passed in param. Thus we have found the object in above step and then we have passed it in remove.
             * */
            parentNode.getChildren().remove(node);
            /** Now we can delete the node itself as it now de-associated from it's parent. So it will get deleted from database.
             * */
            nodeRepository.delete(node);
        } else {
            /**
             * When nodeName is ROOT the we will delete it directly. Also notice we are not checking whether nodeName is ROOT because in previous
             * if condition we checked whether nodeName is "NOT ROOT"
             * All association will be removed when we delete it so no need to worry about it's ArrayList as we want it's children to also gets deleted.
             * */
            Node node = nodeRepository.findById(nodeId).orElse(null);
            nodeRepository.delete(node);
        }
        List<Node> all = nodeRepository.findAll();
        return new ResponseEntity<>(all, HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/pairwise", method = RequestMethod.PUT)
    public @ResponseBody
    ResponseEntity<Object> pairwiseModifyNode(@RequestBody Node node) {
        /** Primary key never gets changed but other attributes can get changed.
         * So here we are checking based on primary key i.e. node.getId() whether the requestBody nodeName is same as in database
         * or the user is trying to change the name of the node i.e. nodeName().
         * */
        if (!node.getNodeName().equalsIgnoreCase(nodeRepository.findById(node.getId()).orElse(null).getNodeName())) {
            /** Here we are checking whether the new nodeName which user trying to change already exists in database or not.
             * If it exists in database i.e. a node with such nodeName already available in the DB then we are going to throw an error.
             * */
            if (nodeRepository.findByNodeName(node.getNodeName()) != null) {
                return new ResponseEntity("Node Name Already Exist, Try Different", HttpStatus.BAD_REQUEST);
            }
        }
        /** Reteriving node based on nodeId which was available in RequestBody.
         * Once we have the Object from database we will update the object values using setter's.
         */
        Node nodeToModify = nodeRepository.findById(node.getId()).orElse(null);
        nodeToModify.setNodeName(node.getNodeName());
        nodeToModify.setParentNodeId(node.getParentNodeId());
        nodeToModify.setChildren(node.getChildren());
        nodeToModify.setValue(node.getValue());
        /** Once updating the value of Object we will persist back it in the database.
         * */
        nodeRepository.save(nodeToModify);

        List<Node> all = nodeRepository.findAll();
        return new ResponseEntity<>(all, HttpStatus.ACCEPTED);
    }

    /**
     * This is going to be the input which we will received as an array from UI or  API
     * Since array has 3 element so matrix size will be 3*3
     * If input size is 6 then it will be 4*4 matrix ( 4 half is 2 so 4*2 - 2)
     * If input size is 10 then it will be 5*5 matrix (5 * 2)
     * If input size is 15 then it will be 6*6 matrix (6 half is 3 so 6*3 - 3)
     * If input size is 21 then it will be 7*7 matrix (7 * 3)
     * If input size is 28 then it will be 8*8 matrix (8 half is 4 so 8*4 - 4)
     * If input size is 26 then it will be 9** matrix ( 9 * 4 )
     */
    @RequestMapping(value = "/analyze", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<Object> pairwiseAnalyze(@RequestBody Node node) {


        /** A node must have atleast three direct children. Inheritance children are not allowed.
         * Thus we will throw an error if a node has less than 3 nodes.
         * */
        if (node.getChildren().size() < 3) {
            return new ResponseEntity("Node must have at least 3 Child Node", HttpStatus.BAD_REQUEST);
        } else {

            /**
             * if there are 3 children then it will be 3*3 matrix, when 4 children then we will have 4*4 matrix
             * Since it is a square matrix so we will set rowcount equal to column count.
             * */
            int rowCount = node.getChildren().size();
            int colCount = rowCount;

            /**
             * This is to show a matrix with all elements as 1 because once clicking on Analyze
             * A Matrix should show up with all the 1 values.
             * */
            double[][] arr = new double[rowCount][colCount];
            Arrays.stream(arr).forEach(a -> Arrays.fill(a, 1));

            /** We will save result in double dimension array.
             * */
            double[][] result = new double[rowCount][colCount];
            /** If we have four input then it means we have to create a 4*4 matrix.
             * So total of 6 values are required in upper triangle but we have only 4 value as in input.
             * So rest two more input's we will create behind the scene and put a flag to showInTree as false.
             *  ShowInTree is yet to be implement as it will be a boolean field.
             * */
            if (rowCount == 4) {
                for (int i = 0; i < 2; i++) {
                    Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                    pairwiseAddNode(tempNode);
                }
            }
            if (rowCount == 5) {
                for (int i = 0; i < 5; i++) {
                    Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                    pairwiseAddNode(tempNode);
                }
            }
            if (rowCount == 6) {
                for (int i = 0; i < 9; i++) {
                    Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                    pairwiseAddNode(tempNode);
                }
            }
            if (rowCount == 7) {
                for (int i = 0; i < 14; i++) {
                    Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                    pairwiseAddNode(tempNode);
                }
            }
            if (rowCount == 8) {
                for (int i = 0; i < 20; i++) {
                    Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                    pairwiseAddNode(tempNode);
                }
            }
            if (rowCount == 9) {
                for (int i = 0; i < 27; i++) {
                    Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                    pairwiseAddNode(tempNode);
                }
            }
            return new ResponseEntity<>(arr, HttpStatus.ACCEPTED);
        }

    }

    @RequestMapping(value = "/update", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<Object> pairwiseUpdate(@RequestBody double[][] inputArray) {

        /**
         * Although it has to be a square matrix but the length of row and column is taken to check
         * whether matrix is a square matrix or not.
         * Also this row and column count will later use in loop for assignment.
         * */
        int rowCount = inputArray.length;
        int colCount = inputArray[0].length;


        /** This double result array will hold the result and will be sent to UI for show in a table.
         * */
        double[][] resultArray = new double[rowCount][colCount];
        /** Since it has to be a square matrix if row and column count is not matched then we will throw an error.
         * */
        if (rowCount != colCount || rowCount < 3 || colCount < 3) {
            return new ResponseEntity<>("Should be a square Matrix", HttpStatus.BAD_REQUEST);
        } else {
            /**
             * If row and column count are same and greater than 3 then we will perform action on the data.
             * */
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {

                    /** If row == col then it means we are talking about the diagonal elements which always has to be one.
                     * */
                    if (row == col) {
                        resultArray[row][col] = 1;

                        /** Here if row is smaller than column then we are talking about upper triangular matrix.
                         * result_array will fill with the input_array data of upper triangle element.
                         * */
                    } else if (row < col) {
                        resultArray[row][col] = round(inputArray[row][col], 2);

                        /** Here we update the lower triangular matrix.
                         * */
                    } else if (row > col) {
                        /** Note that on right hand side column and row position is switched. As this is required to
                         * update the element of lower triangle with (1/upper_triangle).
                         * So consider if we want to update lower triangle [2][0] element than the formula has to be
                         *  (1/[0][2]), here [0][2] element belongs to upper triangle.
                         * */
                        resultArray[row][col] = round(1 / inputArray[col][row], 2);
                    }
                }
            }
        }
        /** This will be used to save the product of rows which we receive from UI
         * */
        double product[] = new double[rowCount];
        /** This temp variable is declared because it helps to store the result of addition of each matrix row.
         * */
        double temp = 1;
        /** Since it is a double dimension resultArray thus we have used inner loop as well.
         * This will add element in each row and save it in temp variable.
         * */
        for (int row = 0; row < resultArray.length; row++) {
            for (int col = 0; col < resultArray[row].length; col++) {
                temp = temp * resultArray[row][col];
            }
            /** Here we have to calculate fraction power because using fraction directly in pow method will result in
             * ignoring the demonitor of rational number in this case 1/3 so it will ignore 3 and will consider only 1.
             * */
            double fractionPower = (double) 1 / rowCount;
            /** As per pairwise the addition should be multiplied with power of 1/rowCount to get production of each
             * matrix row.
             * */
            product[row] = round(Math.pow(temp, fractionPower), 2);
            /** Here we are just resetting the temp value so that for next iteration it does not have any other
             * garbage value from previous iteration.
             * */
            temp = 1;
        }
        /** Here we have declared the new variable to store the sum of all the elements in product array
         * Initially we have initialised it with zero.
         * */
        double additionOfProductArray = 0.0;
        /** Looping over thr product array so that it's element can be added and save in additionOfProductArray variable
         * */
        for (int element = 0; element < product.length; element++) {
            additionOfProductArray += product[element];
        }
        /** Pairwise asked us to calculate the productElement/SUM so for this we declare a new array to store the result.
         * for each row.
         * */
        double elementFromDivAndAddition[] = new double[rowCount];
        for(int element = 0 ; element < product.length ; element ++){
            elementFromDivAndAddition[element] = round(product[element] / additionOfProductArray , 2);
        }
        return new ResponseEntity<>(resultArray, HttpStatus.ACCEPTED);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

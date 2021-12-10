package laurentian.pairwise.service;

import laurentian.pairwise.controller.PairwiseController;
import laurentian.pairwise.repository.NodeRepository;
import laurentian.pairwise.request.Node;
import laurentian.pairwise.request.NodeModel;
import laurentian.pairwise.request.Triad;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Math.min;
import static laurentian.pairwise.controller.PairwiseController.round;

@Service
public class PairwiseService {

    private NodeRepository nodeRepository;

    public PairwiseService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public List<Node> addNode(Node node) {
        /**
         * If the request body of node does not have any parentNodeId which means parentNodeId is null
         * and request body node name is equal to ROOT then we allow the creation of ROOT node.
         * */
        if (node.getParentNodeId() == null && node.getNodeName().equalsIgnoreCase("ROOT")) {
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

            /** Getting the parentNode Children Count, Parent node can be any node let's say ROOT, A, B.
             * So to set the correct value of children parentNode children size is required.
             * */
            int parentNodeChildren = parentNode.getChildren().size();
            /** Here we are getting the parentNode value i.e. for Root it is 100 and let's say for A it is 100
             * if A is the only child of ROOT.
             * */
            double nodeValue = parentNode.getValue();
            /** If there are more than one child than root value will be divided among children.
             * */
            if (parentNodeChildren >= 1) {
                /** +1 is consider based on the current request. Let's say if it already has one Children Node and now
                 * a request is coming from UI to add another node so the request which is coming from UI has be considered
                 * as +1.
                 * */
                nodeValue = round(nodeValue / (parentNodeChildren + 1), 2);
                final double temp = nodeValue;
                /** Here we will be updating the existing child node value with the correct one as the new request
                 * has just added another children.
                 * */
                parentNode.getChildren().forEach(element -> element.setValue(temp));
            }

            /**
             * We are create a new Node by passing the value in constructor from request body. At this moment
             * this node is not saved in the database.
             * */
            Node nodeToAdd = new Node(node.getNodeName(), node.getParentNodeId(), nodeValue, parentNode);
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
        return nodeRepository.findAll();
    }

    public List<Node> deleteNode(Long nodeId) {
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
            parentNode = nodeRepository.findById(Long.parseLong(node.getParentNodeId())).orElse(null);
            int parentNodeChildren = parentNode.getChildren().size();
           if(parentNodeChildren!=0) {
               double nodeValue = round((parentNode.getValue() / parentNodeChildren), 2);
               final double temp = nodeValue;
               /** Here we will be updating the existing child node value with the correct one as we have just
                * deleted one of the children.
                * */
               parentNode.getChildren().forEach(element -> element.setValue(temp));
           }
            nodeRepository.save(parentNode);
        } else {
            /**
             * When nodeName is ROOT the we will delete it directly. Also notice we are not checking whether nodeName is ROOT because in previous
             * if condition we checked whether nodeName is "NOT ROOT"
             * All association will be removed when we delete it so no need to worry about it's ArrayList as we want it's children to also gets deleted.
             * */
            Node node = nodeRepository.findById(nodeId).orElse(null);
            nodeRepository.delete(node);

        }
        return nodeRepository.findAll();
    }

    public List<Node> modifyNode(Node node) {

        /** Reteriving node based on nodeId which was available in RequestBody.
         * Once we have the Object from database we will update the object values using setter's.
         */
        Node nodeToModify = nodeRepository.findById(node.getId()).orElse(null);
        nodeToModify.setNodeName(node.getNodeName());
//        nodeToModify.setParentNodeId(node.getParentNodeId());
//        nodeToModify.setChildren(node.getChildren());
//        nodeToModify.setValue(node.getValue());
        /** Once updating the value of Object we will persist back it in the database.
         * */
        nodeRepository.save(nodeToModify);
        return nodeRepository.findAll();
    }

    public double[][] analyze(Node node) {

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

        /** Below Code will be removed going forward
         * */
        if (rowCount == 4) {
            for (int i = 0; i < 2; i++) {
                Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                //addNode(tempNode);
            }
        }
        if (rowCount == 5) {
            for (int i = 0; i < 5; i++) {
                Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                // addNode(tempNode);
            }
        }
        if (rowCount == 6) {
            for (int i = 0; i < 9; i++) {
                Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                //addNode(tempNode);
            }
        }
        if (rowCount == 7) {
            for (int i = 0; i < 14; i++) {
                Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                //addNode(tempNode);
            }
        }
        if (rowCount == 8) {
            for (int i = 0; i < 20; i++) {
                Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                //addNode(tempNode);
            }
        }
        if (rowCount == 9) {
            for (int i = 0; i < 27; i++) {
                Node tempNode = new Node(UUID.randomUUID().toString(), "1", 1, null);
                //addNode(tempNode);
            }
        }
        return arr;
    }

    public double[][] updateAfterFinalize(double[][] inputArray, int numberPassed) {


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
            //return new ResponseEntity<>("Should be a square Matrix", HttpStatus.BAD_REQUEST);
        } else {

            long nodeLoopCounter = 2;
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
                        resultArray[row][col] = round(inputArray[row][col], 3);


                        // TODO
                        // TODO
                        // TODO
                        // TODO
                        // TODO
                        // TODO

                        /** This update logic WORK to update the nodeValues in database but need to ask
                         * what will be the calculation for it. and which value to be considered from output.
                         *
                         * */
//                        final int rowFinal = row;
//                        final int colFinal = col;
//                        Node node = nodeRepository.findById(nodeLoopCounter).get();
//                        node.setValue(round(inputArray[row][col], 3));
//                        nodeRepository.save(node);
//                        nodeLoopCounter++;


                        /** Here we update the lower triangular matrix.
                         * */
                    } else if (row > col) {
                        /** Note that on right hand side column and row position is switched. As this is required to
                         * update the element of lower triangle with (1/upper_triangle).
                         * So consider if we want to update lower triangle [2][0] element than the formula has to be
                         *  (1/[0][2]), here [0][2] element belongs to upper triangle.
                         * */
                        resultArray[row][col] = round(1 / inputArray[col][row], 3);
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
             * ignoring the denominator of rational number in this case 1/3 so it will ignore 3 and will consider only 1.
             * */
            double fractionPower = (double) 1 / rowCount;
            /** As per pairwise the addition should be multiplied with power of 1/rowCount to get production of each
             * matrix row.
             * */
            product[row] = round(Math.pow(temp, fractionPower), 3);
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
        for (int element = 0; element < product.length; element++) {
            elementFromDivAndAddition[element] = round(product[element] / additionOfProductArray, 3);
        }

        /** Calculation of GM Matrix Section Start
         * */

        /** We are creating an ArrayList to set the element in upper triangular GM Matrix
         * There is a formula for this that 1stElement - 2ndElement and then 1stElement - 3rdElement
         * So that's why we needed a for loop to perform this set of Action.
         * */
        ArrayList<Double> tempList = new ArrayList<>();
        for (int i = 0; i < elementFromDivAndAddition.length; i++) {
            for (int j = 0; j < elementFromDivAndAddition.length; j++) {
                if (i != j && i < j) {
                    tempList.add(round(elementFromDivAndAddition[i] / elementFromDivAndAddition[j], 3));
                }
            }
        }
        /** Converting the ArrayList to Array so that it will easy to insert element directly in GM as we have already
         * calculated it's value in previous Loop.
         * */
        double[] tempDouble = tempList.stream().mapToDouble(Double::doubleValue).toArray();

        /** As reconstructed matrix is also a double dimension matrix like our input matrix.
         * */
        double reconstructedGM[][] = new double[rowCount][colCount];
        /** This K will help to get the element from tempDouble and help to store in top
         * triangle of GM, K value will be increased inside the loop so that never a same value will gets stored.
         * */
        int k = 0;
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < colCount; col++) {
                if (row == col) {
                    /** Diagonal will be all 1
                     * */
                    reconstructedGM[row][col] = 1;
                } else if (row < col) {
                    reconstructedGM[row][col] = round(tempDouble[k], 3);
                    k++;
                } else if (row > col) {
                    /** Element of lower triangle with (1/upper_triangle).
                     * */
                    reconstructedGM[row][col] = round(1 / reconstructedGM[col][row], 3);
                }
            }
        }

        /** Excel output is Different than System output need to check whether Subtraction of Matrix is done properly
         * */
        String differenceMatrix[][] = new String[rowCount][colCount];
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < colCount; col++) {
                DecimalFormat decimalFormatter = new DecimalFormat("##.############");
                decimalFormatter.setMinimumFractionDigits(2);
                decimalFormatter.setMaximumFractionDigits(15);
                double tempDoubleToHold = round(resultArray[row][col] - reconstructedGM[row][col], 2);
                differenceMatrix[row][col] = decimalFormatter.format(round((Math.pow(tempDoubleToHold, 2)), 6)); //round(Math.pow(tempDoubleToHold,2),5);
            }
        }
        if (numberPassed == 0) {
            return resultArray;
        }
            return reconstructedGM;

    }

    public List<NodeModel> getTreeNode() {
        List<Node> nodeList = nodeRepository.findAll();
        List<NodeModel> nodeModels = new ArrayList<>();
        nodeList.forEach(element -> {
            NodeModel nodeModel = new NodeModel();
            nodeModel.setNodeName(element.getNodeName());
            //nodeModel.setId(element.getId());
            String parentNodeName = null;
            if (element.getParentNodeId() != null) {
                parentNodeName = nodeRepository.findById(Long.valueOf(element.getParentNodeId())).orElse(null).getNodeName();
            }
            nodeModel.setParentName(parentNodeName);
            nodeModel.setValue(element.getValue());
            nodeModels.add(nodeModel);
        });
        return nodeModels;
    }

    public ArrayList<Triad> getAllInconsistencyValues(double[][] inputArray, int rowCount) {
        ArrayList<Triad> list = new ArrayList<>();
        for (int i = 0; i < rowCount - 1; i++) {
            for (int j = i + 1; j < rowCount; j++) {
                double X = inputArray[i][j];
                if (j < rowCount) {
                    for (int k = j + 1; k < rowCount; k++) {
                        double Z = inputArray[j][k];
                        double Y = inputArray[i][k];

                        double kii = Double.valueOf(1) - min(round(Y / (X * Z), 2), round((X * Z) / Y, 2));
                        list.add(new Triad((double) round(i,3), (double) round(j,3), (double) round(k,3), round(X,3), round(Y,3), round(Z,3), round(kii,5)));
                    }
                }
            }
        }
        Collections.sort(list);
        Collections.reverse(list);
        return list;
    }

    public ArrayList<Triad> getTriads(double[][] inputArray) {
        ArrayList<Triad> allInconsistencyValuesAndTriad = getAllInconsistencyValues(inputArray, inputArray.length);
        while (allInconsistencyValuesAndTriad.get(0).getKii() > PairwiseController.inconsistencyTolerance) {
            for (int i = 0; i < inputArray.length; i++) {
                for (int k = i + 1; k < inputArray.length; k++) {
                    for (int j = k + 1; j < inputArray.length; j++) {

                        double a = inputArray[i][j];//obtain value from the matrix
                        double b = inputArray[i][k];//obtain value from the matrix
                        double c = inputArray[k][j];//obtain value from the matrix

                        double temp = round(Float.valueOf(Math.round(Math.min(Math.abs(1 - a / (b * c)), Math.abs(1 - (b * c) / a)) * 100) / 100f).doubleValue(), 2);//calculate the inconsistency value and store in the inconsistency array
                        if (allInconsistencyValuesAndTriad.get(0).getKii() == temp)//find the triple elements with the largest inconsistency
                        {
                            if ((b * c) < a) {
                                double A = (b * c) / ((a + b + c) * (a + b + c));
                                double B = (a + 2 * b * c) / (a + b + c);
                                double C = b * c - a;
                                double m = B * B - 4 * A * C;
                                if (m < 0) {
                                    break;
                                } else {
                                    double x1 = (-1 * B + Math.sqrt(m)) / (2 * A);
                                    double x2 = (-1 * B - Math.sqrt(m)) / (2 * A);
                                    if ((x1 > 0) && (x2 < 0)) {
                                        b = (float) (b + (b * x1) / (a + b + c));
                                        c = (float) (c + (c * x1) / (a + b + c));
                                        a = (float) (a - (a * x1) / (a + b + c));
                                    } else if ((x1 < 0) && (x2 > 0)) {
                                        b = (float) (b + (b * x2) / (a + b + c));
                                        c = (float) (c + (c * x2) / (a + b + c));
                                        a = (float) (a - (a * x2) / (a + b + c));
                                    } else if ((x1 > 0) && (x2 > 0)) {
                                        double x = Math.min((float) x1, (float) x2);
                                        b = (float) (b + (b * x) / (a + b + c));
                                        c = (float) (c + (c * x) / (a + b + c));
                                        a = (float) (a - (a * x) / (a + b + c));
                                    } else if ((x1 < 0) && (x2 < 0)) {
                                        break;
                                    }
                                }
                            } else if ((b * c) > a) {
                                double A = (b * c) / ((a + b + c) * (a + b + c));
                                double B = -1 * (a + 2 * b * c) / (a + b + c);
                                double C = b * c - a;
                                double m = B * B - 4 * A * C;
                                if (m < 0) {
                                    break;
                                } else {
                                    double x1 = (-1 * B + Math.sqrt(m)) / (2 * A);
                                    double x2 = (-1 * B - Math.sqrt(m)) / (2 * A);
                                    if ((x1 > 0) && (x2 < 0)) {
                                        b = (float) (b - (b * x1) / (a + b + c));
                                        c = (float) (c - (c * x1) / (a + b + c));
                                        a = (float) (a + (a * x1) / (a + b + c));
                                    } else if ((x1 < 0) && (x2 > 0)) {
                                        b = (float) (b - (b * x2) / (a + b + c));
                                        c = (float) (c - (c * x2) / (a + b + c));
                                        a = (float) (a + (a * x2) / (a + b + c));
                                    } else if ((x1 > 0) && (x2 > 0)) {
                                        double x = Math.min((float) x1, (float) x2);
                                        b = (float) (b - (b * x) / (a + b + c));
                                        c = (float) (c - (c * x) / (a + b + c));
                                        a = (float) (a + (a * x) / (a + b + c));
                                    } else if ((x1 < 0) && (x2 < 0)) {
                                        break;
                                    }
                                }
                            }
                        }
                        inputArray[i][j] = a;
                        inputArray[i][k] = b;
                        inputArray[k][j] = c;
                    }
                }
            }
            allInconsistencyValuesAndTriad = getAllInconsistencyValues(inputArray, inputArray.length);
        }
        return allInconsistencyValuesAndTriad;
    }

    public ArrayList<Triad> reduceInconsistency2(double[][] inputArray) {
        ArrayList<Triad> allInconsistencyValuesAndTriad = getAllInconsistencyValues(inputArray, inputArray.length);
        long N = allInconsistencyValuesAndTriad.stream().filter(element -> element.getKii() > PairwiseController.inconsistencyTolerance).count();
        int i = 0, j = 0, k = 0;
        double X = 0, Y = 0, Z = 0;

        while (N != 0) {
            i = Double.valueOf(allInconsistencyValuesAndTriad.get(0).getI()).intValue();
            j = Double.valueOf(allInconsistencyValuesAndTriad.get(0).getJ()).intValue();
            k = Double.valueOf(allInconsistencyValuesAndTriad.get(0).getK()).intValue();
            X = allInconsistencyValuesAndTriad.get(0).getX();
            Y = allInconsistencyValuesAndTriad.get(0).getY();
            Z = allInconsistencyValuesAndTriad.get(0).getZ();

            double new_x = (Math.pow(X, (double) 2 / 3) * Math.pow(Z, (double) -1 / 3) * Math.pow(Y, (double) 1 / 3));
            double new_y = (Math.pow(X, (double) 1 / 3) * Math.pow(Z, (double) 1 / 3) * Math.pow(Y, (double) 2 / 3));
            double new_z = (Math.pow(X, (double) -1 / 3) * Math.pow(Z, (double) 2 / 3) * Math.pow(Y, (double) 1 / 3));

            inputArray[i][j] = round(new_x, 4);
            inputArray[i][k] = round(new_y, 4);
            inputArray[j][k] = round(new_z, 4);
            inputArray[j][i] = round((double) 1 / new_x, 4);
            inputArray[k][i] = round((double) 1 / new_y, 4);
            inputArray[k][j] = round((double) 1 / new_z, 4);
            allInconsistencyValuesAndTriad = getAllInconsistencyValues(inputArray, inputArray.length);
            N = allInconsistencyValuesAndTriad.stream().filter(element -> element.getKii() > 0.333333).count();
        }
        return allInconsistencyValuesAndTriad;
    }
}

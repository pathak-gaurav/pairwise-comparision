package laurentian.pairwise.controller;

import com.opencsv.CSVWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import laurentian.pairwise.repository.NodeRepository;
import laurentian.pairwise.request.Node;
import laurentian.pairwise.request.NodeModel;
import laurentian.pairwise.request.Triad;
import laurentian.pairwise.request.VirusScanningResponse;
import laurentian.pairwise.rest.RestServiceClient;
import laurentian.pairwise.service.PairwiseService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/***
 * The class contains all the API, which will be used by frontend.
 */
@RestController
@Api(value = "Pairwise Microservice", description = "Operations pertaining to Node, File's and Virus Scan in Pairwise")
@RequestMapping(value = "${app.version.v1}")
@CrossOrigin(origins = "*")
public class PairwiseController {

    /***
     * Field for Object creation
     */
    private NodeRepository nodeRepository;
    private PairwiseService pairwiseService;
    private RestServiceClient restServiceClient;

    /***
     * Static Fields to hold the result
     */
    private static double[][] array;
    private static double[][] finalResult;

    /**
     * Default Inconsistency
     */
    public static double inconsistencyTolerance = 0.33;

    /***
     *
     * @param nodeRepository : Repository Contains the methods to perform certain operation on database.
     * @param pairwiseService : Service contains the Business Logic
     * @param restServiceClient : It's a Rest Client implementation to call File Scanning / Virus Scanning External API's
     */
    public PairwiseController(NodeRepository nodeRepository, PairwiseService pairwiseService, RestServiceClient restServiceClient) {
        this.nodeRepository = nodeRepository;
        this.pairwiseService = pairwiseService;
        this.restServiceClient = restServiceClient;
    }

    /**
     * This is to create the root node and add the node to the existing node
     */
    @CrossOrigin
    @ApiOperation(value = "API will Create the Node", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Request Accepted and response Sent"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "File cannot be processed as it contain VIRUS")})
    /***
     * This API will add node in a TreeMap
     * @param: Node is the class/Entity to hold TreeMap data.
     * @Return: ResponseEntity represents the whole HTTP response with the status Code of the Request
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
            /***
             * Checking whether node name is passed as NULL or a String is passed with empty spaces.
             */
        } else if (node.getNodeName().trim().length() == 0 || node.getNodeName().equalsIgnoreCase("NULL")) {
            return new ResponseEntity("White Space / NULL is Not Allowed", HttpStatus.BAD_REQUEST);
        }
        //Calling the Business Logic in Service to AddNode.
        List<Node> allNodes = pairwiseService.addNode(node);
        /***
         * Returning the response back to UI after adding the node.
         */
        return new ResponseEntity<>(allNodes, HttpStatus.CREATED);
    }

    /**
     * This is to delete the node and it's children
     */
    @CrossOrigin
    @ApiOperation(value = "API will Delete the Node", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Request Accepted and response Sent"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "File cannot be processed as it contain VIRUS")})
    @RequestMapping(value = "/pairwise", method = RequestMethod.DELETE)
    public @ResponseBody
    ResponseEntity<Object> pairwiseDeleteNode(@RequestParam Long nodeId) {
        if (nodeRepository.findById(nodeId) == null) {
            return new ResponseEntity("Refresh Page, Node is already Deleted", HttpStatus.BAD_REQUEST);
        }
        if (nodeRepository.findById(nodeId).orElse(null).getNodeName().equalsIgnoreCase("Root")) {
            return new ResponseEntity("Root Name cannot be deleted", HttpStatus.BAD_REQUEST);
        }
        List<Node> allNodes = pairwiseService.deleteNode(nodeId);
        return new ResponseEntity<>(allNodes, HttpStatus.ACCEPTED);
    }

    @CrossOrigin
    @ApiOperation(value = "API will Change the Node Name", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Request Accepted and response Sent"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "File cannot be processed as it contain VIRUS")})
    @RequestMapping(value = "/pairwise", method = RequestMethod.PUT)
    public @ResponseBody
    ResponseEntity<Object> pairwiseModifyNode(@RequestBody Node node) {
        /** Primary key never gets changed but other attributes can get changed.
         * So here we are checking based on primary key i.e. node.getId() whether the requestBody nodeName is same as in database
         * or the user is trying to change the name of the node i.e. nodeName().
         * */
        if (nodeRepository.findById(node.getId()) == null || nodeRepository.findById(node.getId()).isEmpty()) {
            return new ResponseEntity("Node Does Not Exist", HttpStatus.BAD_REQUEST);
        }
        if (nodeRepository.findById(node.getId()).orElse(null).getNodeName().equalsIgnoreCase("ROOT")) {
            return new ResponseEntity("Root node name cannot be changed", HttpStatus.BAD_REQUEST);
        }
        if (!node.getNodeName().equalsIgnoreCase(nodeRepository.findById(node.getId()).orElse(null).getNodeName())) {
            /** Here we are checking whether the new nodeName which user trying to change already exists in database or not.
             * If it exists in database i.e. a node with such nodeName already available in the DB then we are going to throw an error.
             * */
            if (nodeRepository.findByNodeName(node.getNodeName()) != null) {
                return new ResponseEntity("Node Name Already Exist, Try Different", HttpStatus.BAD_REQUEST);
            }
        }
        if (node.getNodeName().trim().length() == 0 || node.getNodeName().equalsIgnoreCase("NULL")) {
            return new ResponseEntity("White Space / NULL is Not Allowed", HttpStatus.BAD_REQUEST);
        }
        List<Node> allNodes = pairwiseService.modifyNode(node);
        return new ResponseEntity<>(allNodes, HttpStatus.ACCEPTED);
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
    /**
     * Once you add all the nodes in the application, then the reduce inconsistency button will be used to create the matrix
     * with all values of 1.
     */

    @CrossOrigin
    @ApiOperation(value = "API will produce matrix with value of 1", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Request Accepted and response Sent"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "File cannot be processed as it contain VIRUS")})
    @RequestMapping(value = "/analyze", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<Object> pairwiseAnalyze(@RequestParam Long nodeId, @RequestParam Double tolerance) {

        Node node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) {
            return new ResponseEntity<>("Node does not exist", HttpStatus.BAD_REQUEST);
        }

        /** A node must have atleast three direct children. Inheritance children are not allowed.
         * Thus we will throw an error if a node has less than 3 nodes.
         * */
        if (node.getChildren().size() < 3) {
            return new ResponseEntity("Node must have at least 3 Child Node", HttpStatus.BAD_REQUEST);
        } else {
            if (tolerance >= 0.01 && tolerance <= 1) {
                inconsistencyTolerance = tolerance;
            }
            double[][] analyzedArray = pairwiseService.analyze(node, nodeId);
            return new ResponseEntity<>(analyzedArray, HttpStatus.ACCEPTED);
        }

    }

    /**
     * This API will take the input matrix which means it will accept only upper triangle values from UI.
     * Based on each value update from upper triangle matrix simultaneously it will update the lower triangle matrix.
     * If the passed number is 0 it means it will update each value in upper and lower triangle.
     * <p>
     * Once all the value updated a final matrix with the answer is created when the value is passed as 1
     */
    @CrossOrigin
    @ApiOperation(value = "API will produce the final Inconsistent Matrix", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Request Accepted and response Sent"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "File cannot be processed as it contain VIRUS")})
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<Object> pairwiseUpdate(@RequestBody double[][] inputArray, @RequestParam int num) {
        int rowCount = inputArray.length;
        int colCount = inputArray[0].length;

        /** Since it has to be a square matrix if row and column count is not matched then we will throw an error.
         * */
        if (rowCount != colCount || rowCount < 3 || colCount < 3) {
            return new ResponseEntity<>("Should be a square Matrix", HttpStatus.BAD_REQUEST);
        }
        double[][] resultArray = new double[rowCount][colCount];
        try {
            resultArray = pairwiseService.updateAfterFinalize(inputArray, num);
            array = resultArray;
        } catch (NumberFormatException exception) {
            return new ResponseEntity<>("Invalid Input, Please try Numbers greater than zero", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(resultArray, HttpStatus.ACCEPTED);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @CrossOrigin
    @ApiOperation(value = "Download the final Pairwise Matrix Result", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "CSV file sent Successfully"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "File cannot be processed as it contain VIRUS")})
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportToCSV(HttpServletResponse response) throws IOException {
        /** Since we want file in CSV so setting the response as text/csv
         * */
        response.setContentType("text/csv");
        /** This dateformat will be used to create a unique file name.
         * */
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=pairwise_file_" + currentDateTime + ".csv";
        response.setHeader(headerKey, headerValue);

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        /** Creating new File Object with the above header value.
         * */
        File file = new File(headerValue);
        /** If file does not exist then create a new file
         * */
        if (!file.isFile()) {
            file.createNewFile();
        }

        /** Creating a new CSV write to write the data on File
         * */
        CSVWriter csvWriter = new CSVWriter(new FileWriter(file));

        int rowCount = array.length;

        /** Writing data into the file.
         * */
        for (int i = 0; i < rowCount; i++) {
            int columnCount = array[i].length;
            String[] values = new String[columnCount];
            for (int j = 0; j < columnCount; j++) {
                values[j] = array[i][j] + "";
            }
            csvWriter.writeNext(values);
        }
        /** Flushing the data and closing the csvWriter.
         * */
        csvWriter.flush();
        csvWriter.close();

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));


        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }

    @CrossOrigin
    @ApiOperation(value = "File Upload and Virus Scan Functionality", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "File Upload is Successful"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "File cannot be processed as it contain VIRUS")})
    @PostMapping("/upload")
    public ResponseEntity<Object> singleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        String uploadsDir = "/uploads/";
        String realPathToUploads = Paths.get(".").normalize().toAbsolutePath().toFile().toString() + uploadsDir;
        if (!new File(realPathToUploads).exists()) {
            new File(realPathToUploads).mkdir();
        }
        String orgName = file.getOriginalFilename();
        String filePath = realPathToUploads + orgName;
        File dest = new File(filePath);

        try {
            file.transferTo(dest);
            VirusScanningResponse virusScanningResponse = virusScanning(dest.getAbsolutePath());
            if (virusScanningResponse.getCleanResult() == false) {
                dest.delete();
                return new ResponseEntity<>("File cannot be processed as it contain VIRUS", HttpStatus.BAD_GATEWAY);
            }
            FileInputStream fis = new FileInputStream(filePath);
            DataInputStream myInput = new DataInputStream(fis);
            String thisLine;
            List<String[]> lines = new ArrayList<String[]>();
            while ((thisLine = myInput.readLine()) != null) {
                thisLine = thisLine.replace("\"\"", "");
                lines.add(thisLine.split(","));
            }

            // convert our list to a String array.
            String[][] stringArray = new String[lines.size()][0];
            lines.toArray(stringArray);

            int rowCount = stringArray.length;
            int colCount = stringArray[0].length;

            double[][] doubleArray = new double[rowCount][colCount];
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    doubleArray[row][col] = Double.parseDouble(stringArray[row][col].replaceAll("\"", ""));
                }
            }
            fis.close();
            myInput.close();

            deleteAllNodeFromDatabase();

            insertNodeFileUpload(rowCount);

            dest.delete();
            dest.deleteOnExit();
            inconsistencyTolerance = 0.33;
            finalResult = pairwiseService.updateAfterFinalize(doubleArray, 0);
            array = finalResult;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>(finalResult, HttpStatus.ACCEPTED);
    }

    @Transactional
    public void deleteAllNodeFromDatabase() {
        if (nodeRepository.findAll().size() > 0) {
            nodeRepository.findAll().stream().forEach(element -> nodeRepository.delete(element));
        }
    }

    private void insertNodeFileUpload(int rowCount) {

        if (nodeRepository.findAll().isEmpty()) {
            insertFirstThreeNode();
            int size = nodeRepository.findAll().size();
            for (int i = 0; i < rowCount - (size -1); i++) {
                incrementNodeIfCountMoreThanThree();
            }
        }
    }

    private void incrementNodeIfCountMoreThanThree() {
        Node root = nodeRepository.findByNodeName("Root");
        Node tempNode = new Node(UUID.randomUUID().toString(), String.valueOf(root.getId()), 1, null);
        pairwiseService.addNode(tempNode);
    }

    private void insertFirstThreeNode() {
        Node rootNode = new Node("Root", null, 100, null);
        pairwiseService.addNode(rootNode);
        Node nodeA = new Node("A", String.valueOf(rootNode.getId()), 1, null);
        pairwiseService.addNode(nodeA);
        Node nodeB = new Node("B", String.valueOf(rootNode.getId()), 1, null);
        pairwiseService.addNode(nodeB);
        Node nodeC = new Node("C", String.valueOf(rootNode.getId()), 1, null);
        pairwiseService.addNode(nodeC);
    }

    private VirusScanningResponse virusScanning(String absolutePath) {
        String apikey = null;
        if (apiSelection() == 0) {
            apikey = "df2198a2-7291-4161-961b-31c679598052";
        } else {
            apikey = "b84e5517-ec04-4ad2-9d1c-0de32831e4a8";
        }
        String endpointUrl = "https://api.cloudmersive.com/virus/scan/file";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        FileSystemResource fileSystemResource = new FileSystemResource(new File(absolutePath));
        map.add("inputFile", fileSystemResource);
        map.add("Apikey", apikey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Apikey", apikey);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
        return restServiceClient.invokePaloAltoService(headers, HttpMethod.POST, null, VirusScanningResponse.class, endpointUrl, requestEntity);
    }

    public int apiSelection() {
        return (int) Math.round(Math.random());
    }

    @CrossOrigin
    @RequestMapping(value = "/nodes", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    List getAllNodes() {
        List<Node> nodeList = nodeRepository.findAll();
        return nodeList;
    }

    @CrossOrigin
    @RequestMapping(value = "/tree", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    List<NodeModel> getNodeForTree() {
        return pairwiseService.getTreeNode();
    }

    /**
     * This will download the sample example for the user.
     */
    @CrossOrigin
    @ApiOperation(value = "Will download the sample example for the user", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Request accepted and sample file sent to frontend"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "Should be a square Matrix")})
    @GetMapping("/example-download")
    public ResponseEntity<ByteArrayResource> exampleDownload(HttpServletResponse response) throws IOException {
        File file = Paths.get("example.csv").normalize().toAbsolutePath().toFile();

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + file.getName();
        response.setHeader(headerKey, headerValue);
        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }

    @CrossOrigin
    @ApiOperation(value = "Finding all the Max Inconsistency greater than Tolerance", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Request accepted and processed by the Server"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "Should be a square Matrix")})
    @RequestMapping(value = "/max-inconsistency", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<Object> maxInconsistency(@RequestBody double[][] inputArray) {
        int rowCount = inputArray.length;
        int colCount = inputArray[0].length;

        /** Since it has to be a square matrix if row and column count is not matched then we will throw an error.
         * */
        if (rowCount != colCount || rowCount < 3 || colCount < 3) {
            return new ResponseEntity<>("Should be a square Matrix", HttpStatus.BAD_REQUEST);
        }
        ArrayList<Triad> allInconsistencyValuesAndTriad = pairwiseService.getAllInconsistencyValues(inputArray, rowCount);
        return new ResponseEntity<>(allInconsistencyValuesAndTriad, HttpStatus.ACCEPTED);
    }


    @CrossOrigin
    @RequestMapping(value = "/reduce-inconsistency-2", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<Object> reduceInconsistency(@RequestBody double[][] inputArray) {
        ArrayList<Triad> allInconsistencyValuesAndTriad = pairwiseService.reduceInconsistency2(inputArray);
        return new ResponseEntity<>(allInconsistencyValuesAndTriad, HttpStatus.ACCEPTED);
    }


    @CrossOrigin
    @ApiOperation(value = "Reduce the inconsistency of the Pairwise Matrix", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Request accepted and processed by the Server"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"), @ApiResponse(code = 400, message = "Should be a square Matrix")})
    @RequestMapping(value = "/reduce-inconsistency", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<Object> computeInconsistency(@RequestBody double[][] inputArray) {
        int rowCount = inputArray.length;
        int colCount = inputArray[0].length;
        /** Since it has to be a square matrix if row and column count is not matched then we will throw an error.
         * */
        if (rowCount != colCount || rowCount < 3 || colCount < 3) {
            return new ResponseEntity<>("Should be a square Matrix", HttpStatus.BAD_REQUEST);
        }
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < colCount; col++) {
                if (row < col) {
                    inputArray[row][col] = round(inputArray[row][col], 3);
                }
                if (row == col) {
                    inputArray[row][col] = 1;
                }
                if (row > col) {
                    inputArray[row][col] = 1;
                }
            }
        }
        if (rowCount >= 4) {
            return reduceInconsistency(inputArray);
        } else {
            ArrayList<Triad> allInconsistencyValuesAndTriad = pairwiseService.getTriads(inputArray);
            return new ResponseEntity<>(allInconsistencyValuesAndTriad, HttpStatus.ACCEPTED);
        }
    }

    @CrossOrigin
    @ApiOperation(value = "Reset the whole Pairwise application", response = ResponseEntity.class)
    @ApiResponses(value = {@ApiResponse(code = 202, message = "Request accepted and processed by the Server"), @ApiResponse(code = 401, message = "You are not authorized to view the resource"), @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"), @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")})
    @RequestMapping(value = "/reset", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    ResponseEntity<Object> reset() {
        resetExceptRoot();
        inconsistencyTolerance = 0.33;
        return new ResponseEntity<>("Success", HttpStatus.ACCEPTED);
    }

    /**
     * This method will reset the Matrix and delete all the entries from the database. It is a
     * It is transaction thus needs to be committed once changes are done, thus @Transactional annotation is used.
     */
    @Transactional
    public void resetExceptRoot() {
        List<Node> repositoryAll = nodeRepository.findAll();
        if (!repositoryAll.isEmpty()) {
            for (Node node : repositoryAll) {
                if (!node.getNodeName().equalsIgnoreCase("Root")) {
                    Node parentNode = nodeRepository.findById(Long.parseLong(node.getParentNodeId())).orElse(null);
                    if (parentNode != null) {
                        parentNode.getChildren().remove(node);
                        nodeRepository.delete(node);
                        nodeRepository.save(parentNode);
                    }
                }
            }
        }
    }
}

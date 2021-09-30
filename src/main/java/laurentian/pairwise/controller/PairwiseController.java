package laurentian.pairwise.controller;

import com.opencsv.CSVWriter;
import laurentian.pairwise.repository.NodeRepository;
import laurentian.pairwise.request.Node;
import laurentian.pairwise.service.PairwiseService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
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

@RestController
@RequestMapping(value = "${app.version.v1}")
public class PairwiseController {

    private NodeRepository nodeRepository;
    private PairwiseService pairwiseService;
    private static double[][] array;
    private static double[][] finalResult;

    public PairwiseController(NodeRepository nodeRepository, PairwiseService pairwiseService) {
        this.nodeRepository = nodeRepository;
        this.pairwiseService = pairwiseService;
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
        List<Node> allNodes = pairwiseService.addNode(node);
        return new ResponseEntity<>(allNodes, HttpStatus.CREATED);
    }

    /**
     * This is to delete the node and it's children
     */
    @RequestMapping(value = "/pairwise", method = RequestMethod.DELETE)
    public @ResponseBody
    ResponseEntity<Object> pairwiseDeleteNode(@RequestParam Long nodeId) {
        List<Node> allNodes = pairwiseService.deleteNode(nodeId);
        return new ResponseEntity<>(allNodes, HttpStatus.ACCEPTED);
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
    @RequestMapping(value = "/analyze", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<Object> pairwiseAnalyze(@RequestBody Node node) {


        /** A node must have atleast three direct children. Inheritance children are not allowed.
         * Thus we will throw an error if a node has less than 3 nodes.
         * */
        if (node.getChildren().size() < 3) {
            return new ResponseEntity("Node must have at least 3 Child Node", HttpStatus.BAD_REQUEST);
        } else {
            double[][] analyzedArray = pairwiseService.analyze(node);
            return new ResponseEntity<>(analyzedArray, HttpStatus.ACCEPTED);
        }

    }

    @RequestMapping(value = "/update", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<Object> pairwiseUpdate(@RequestBody double[][] inputArray) {
        int rowCount = inputArray.length;
        int colCount = inputArray[0].length;

        /** Since it has to be a square matrix if row and column count is not matched then we will throw an error.
         * */
        if (rowCount != colCount || rowCount < 3 || colCount < 3) {
            return new ResponseEntity<>("Should be a square Matrix", HttpStatus.BAD_REQUEST);
        }

        double[][] resultArray = pairwiseService.updateAfterFinalize(inputArray);
        array = resultArray;
        return new ResponseEntity<>(resultArray, HttpStatus.ACCEPTED);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportToCSV(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
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

        File file = new File(headerValue);
        if (!file.isFile()) {
            file.createNewFile();
        }

        CSVWriter csvWriter = new CSVWriter(new FileWriter(file));

        int rowCount = array.length;

        for (int i = 0; i < rowCount; i++) {
            int columnCount = array[i].length;
            String[] values = new String[columnCount];
            for (int j = 0; j < columnCount; j++) {
                values[j] = array[i][j] + "";
            }
            csvWriter.writeNext(values);
        }

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

    @PostMapping("/upload")
    public ResponseEntity<Object> singleFileUpload(@RequestParam("file") MultipartFile file,
                                                   RedirectAttributes redirectAttributes) {

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
            finalResult = pairwiseService.updateAfterFinalize(doubleArray);
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

        if (rowCount == 3 && nodeRepository.findAll().isEmpty()) {
            insertFirstThreeNode();
        }
        if (rowCount == 4) {
            if (nodeRepository.findAll().isEmpty()) {
                insertFirstThreeNode();
            }
            if (nodeRepository.findAll().size() == 4) {
                for (int i = 0; i < 2; i++) {
                    incrementNodeIfCountMoreThanThree();
                }
            }
        }
        if (rowCount == 5) {
            if (nodeRepository.findAll().isEmpty()) {
                insertFirstThreeNode();
            }
            if (nodeRepository.findAll().size() == 4) {
                for (int i = 0; i < 5; i++) {
                    incrementNodeIfCountMoreThanThree();
                }
            }
        }
        if (rowCount == 6) {
            if (nodeRepository.findAll().isEmpty()) {
                insertFirstThreeNode();
            }
            if (nodeRepository.findAll().size() == 4) {
                for (int i = 0; i < 9; i++) {
                    incrementNodeIfCountMoreThanThree();
                }
            }
        }
        if (rowCount == 7) {
            if (nodeRepository.findAll().isEmpty()) {
                insertFirstThreeNode();
            }
            if (nodeRepository.findAll().size() == 4) {
                for (int i = 0; i < 14; i++) {
                    incrementNodeIfCountMoreThanThree();
                }
            }
        }
        if (rowCount == 8) {
            if (nodeRepository.findAll().isEmpty()) {
                insertFirstThreeNode();
            }
            if (nodeRepository.findAll().size() == 4) {
                for (int i = 0; i < 20; i++) {
                    incrementNodeIfCountMoreThanThree();
                }
            }
        }
        if (rowCount == 9) {
            if (nodeRepository.findAll().isEmpty()) {
                insertFirstThreeNode();
            }
            if (nodeRepository.findAll().size() == 4) {
                for (int i = 0; i < 27; i++) {
                    incrementNodeIfCountMoreThanThree();
                }
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
}

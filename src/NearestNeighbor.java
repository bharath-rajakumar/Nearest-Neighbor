import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

public class NearestNeighbor {

    public static String[][] referenceTable;
    public static String[][] tempTable;
    public static int rowSize;
    public static int colSize;
    public static void main(String[] args) {
        // Step : Read the two input files
        String file1Contents = readFile("input/crossValidation1.txt");
        String file2Contents = readFile("input/data1.txt");

        // Step : Extract information by tokenizing each line
        String[] file1Lines = file1Contents.split("[\\r\\n]+");
        String[] parameters = file1Lines[0].split(" "); // First line of cross validation file
        int kFold = Integer.parseInt(parameters[0]); // Number k of k-fold
        int m = Integer.parseInt(parameters[1]); // Number of examples
        int t = Integer.parseInt(parameters[2]); // Number of permutations
        Double[][] errorTable = new Double[t][5]; // To store the error for each permutation and for k Value
        Double[] totalErrorTable = new Double[5]; // To store the mean error
        Double[] variance = new Double[5];
        Double[] sigma = new Double[5];

        // Step : Build the table from file2
        referenceTable = buildReferenceTable(file2Contents,m);

        // Step : Create folds for a given permutation
        for (int p = 1; p <= t; p++) {
            String[] permutation = file1Lines[p].split(" ");
            ArrayList<ArrayList<Integer>> folds = createKFolds(permutation, kFold);
            for(ArrayList<Integer> list : folds) {
                System.out.println(list);
            }
            System.out.println("Permutation No " + p);

            for (int k = 1; k <= 5; k++ ) {// Step increment k of the k-NN classifier
                // For each value of k, the error, standard deviation sigma has to be calculated
                System.out.println("k Value " + k);
                double totalError = 0.0;
                int noOfMisClassified = 0; // number of elements that were classified wrong

                // Step : Keep one fold for testing and the remaining folds for training
                for (int i = 0; i < folds.size(); i++) {
                    ArrayList<Integer> testingFold = new ArrayList<Integer>();
                    ArrayList<Integer> trainingFold = new ArrayList<Integer>();
                    for (int j = 0; j < folds.size(); j++) {
                        if(j == i) {
                            testingFold = folds.get(j);
                        } else {
                            for(Integer a : folds.get(j)) {
                                trainingFold.add(a);
                            }
                        }
                    }

                    System.out.println("Testing fold = " + testingFold.toString() + " Training Fold = " + trainingFold.toString());

                    // Step : Perform k-NN classification
                    for(Integer a : testingFold) {
                        String kNNClassfication = knnClassifier(k, a, trainingFold);
                        System.out.println(a + " is classified as " + kNNClassfication + " based on k-NN classification");
                        String originalClassification = referenceTable[a][3]; // original category as mentioned in file2
                        System.out.println(a + " is originally classified as " + originalClassification);
                        System.out.println();
                        // Step : Find the error in classification by using cross validation
                        if (!(originalClassification.equals(kNNClassfication))) {
                            noOfMisClassified = noOfMisClassified + 1;
                        }
                    }
                }
                totalError = (double)noOfMisClassified/m;
                errorTable[p-1][k-1] = totalError;
                System.out.println("No of Errors = "+ noOfMisClassified +", Total Error = " + totalError);
            }
        }

        for(int i = 0; i < t; i++) {
            for(int j = 0; j < 5; j++) {
                System.out.print(errorTable[i][j] + " ");
            }
            System.out.println("");
        }

        // Step : Find the mean error for each k value
        for(int j = 0; j < 5; j++) {
            totalErrorTable[j] = 0.0;
            for(int i = 0; i < t; i++) {
                totalErrorTable[j] = totalErrorTable[j] + errorTable[i][j];
            }
            totalErrorTable[j] = totalErrorTable[j]/t;
            System.out.print(totalErrorTable[j] + " ");
        }
        System.out.println();

        // Step : Find the variance and standard deviation for each k value
        for(int j = 0; j < 5; j++) {
            variance[j] = 0.0;
            for(int i = 0; i < t; i++) {
                variance[j] = variance[j] + Math.pow(errorTable[i][j] - totalErrorTable[j], 2);
            }
            variance[j] = variance[j]/(t - 1);
            sigma[j] = Math.sqrt(variance[j]);
            System.out.println("k-NN Value = " + (j + 1) + " Variance = " + variance[j] + " Std. Deviation = " + sigma[j]);
        }

        // Step : Take any one of the permutation to form the training fold
        String[] permutation = file1Lines[1].split(" ");
        ArrayList<Integer> classifierFold = new ArrayList<Integer>();
        for(String a: permutation) {
            classifierFold.add(Integer.parseInt(a));
        }

        // Step : Classify the unclassified elements present in the temp table for all k-Values
    }

    public static String knnClassifier(Integer k, Integer a, ArrayList<Integer> trainingFold) {
        // find the euclidean distance of each point from point A
        Point[] dist = euclideanDistance(a, trainingFold);

        // Create a priority queue that will store only k shortest Point based on its euclidean distance
        PriorityQueue<Point> kNearestPoints = new PriorityQueue<Point>(k, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {
                if(p1.dist > p2.dist) {
                    return - 1;
                }
                if(p1.dist < p2.dist) {
                    return 1;
                }
                return 0;
            }
        });

        for(Point p : dist) {
            if(kNearestPoints.size() == k) {
                kNearestPoints.add(p);
                kNearestPoints.poll();
            } else {
                kNearestPoints.add(p);
            }
        }

        // Find no of positive and negative category
        int pcount = 0;
        int ncount = 0;
        for (Point p : kNearestPoints) {
            System.out.println("("+p.x+", "+p.y+"), "+p.getDistance()+", " + p.category);
            if(p.category.equals("+")) {
                pcount = pcount + 1;
            }

            if(p.category.equals("-")) {
                ncount = ncount + 1;
            }
        }

        // Classify the point based on number of positives and negatives count
        if (pcount > ncount) {
            return "+";
        } else {
            return "-";
        }
    }

    // Finds the euclidean distance from point A to all points present in the ArrayList
    public static Point[] euclideanDistance(Integer a, ArrayList<Integer> trainingFold) {
        Point[] dist = new Point[trainingFold.size()];
        int x1 = Integer.parseInt(referenceTable[a][1]);
        int y1 = Integer.parseInt(referenceTable[a][2]);
        Point one = new Point(x1, y1);
        one.dist = 0.0;
        one.category = referenceTable[a][3];
        int i = 0;
        for(Integer point : trainingFold) {
            int x2 = Integer.parseInt(referenceTable[point][1]);
            int y2 = Integer.parseInt(referenceTable[point][2]);
            Point two = new Point(x2, y2);
            two.dist = Math.pow((Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)), 0.5);
            two.category = referenceTable[point][3];
            dist[i] = two;
            i++;
        }
        return dist;
    }

    // Builds the reference table and the temporary table
    public static String[][] buildReferenceTable(String file2Contents, int m) {
        String[][] table = new String[m][4];
        String[] fileLines = file2Contents.split("[\\r\\n]+");
        String[] line1 = fileLines[0].split(" ");
        rowSize = Integer.parseInt(line1[0]);
        colSize = Integer.parseInt(line1[1]);
        tempTable = new String[rowSize][colSize];
        String[] lines = file2Contents.split("[\\r\\n]+");
        int rowCount = 0;
        for(String currentLine: lines) {
            String[] elements = currentLine.split(" ");
            if(elements.length == colSize) {
                for(int i = 0; i < elements.length; i++) {
                    tempTable[rowCount][i] = elements[i];
                }
                rowCount = rowCount + 1;
            }
        }

        int example = 0;
        for(int i = 0; i < rowSize; i++) {
            for(int j = 0; j < colSize; j++) {
                if(!(tempTable[i][j].equals(".")) && example < m) {
                    // add + and - symbols to the table matrix
                    table[example][0] = Integer.toString(example);
                    table[example][1] = Integer.toString(j);
                    table[example][2] = Integer.toString(i);
                    table[example][3] = tempTable[i][j];
                    example = example + 1;
                }
            }
        }

        for(int i = 0; i < m; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.print(table[i][j] + " ");
            }
            System.out.println();
        }
        return table;
    }

    // Create an ArrayList with K ArrayLists present inside it
    public static ArrayList<ArrayList<Integer>> createKFolds(String[] permutation1, int kFold) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        int n = permutation1.length;
        int elementCount = n/kFold;    // number of elements a list can hold except for the last list
        int kCount = 0;
        int i = 0;
        while(kCount <= kFold && i < n) {
            // create new arrayList
            int count = 0;
            ArrayList<Integer> temp = new ArrayList<Integer>();
            while (count < elementCount) {
                temp.add(Integer.parseInt(permutation1[i]));
                count = count + 1;
                i = i + 1;
            }

            if (kCount == kFold - 1) {
                temp.add(Integer.parseInt(permutation1[i]));
                i = i + 1;
            }
            // add the list to result
            result.add(temp);
            kCount = kCount + 1;
        }
        return result;
    }

    // Reads the input file and return its contents as a String
    public static String readFile(String filename) {
        String contents = "";
        int count = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            String line = null;
            try {
                while((line = br.readLine())!= null) {
                    if(count == 0) {
                        contents = contents + line;
                        count = count + 1;
                    } else {
                        contents = contents + "\n" + line;
                        count = count + 1;
                    }
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return contents;
    }
}

// Custom Object to store each example as a Point
class Point {
    int x = 0;
    int y = 0;
    double dist = 0.0;
    String category = "";

    Point(int a, int b) {
        this.x = a;
        this.y = b;
    }

    public int getY() {
        return this.y;
    }

    public int getX() {
        return this.y;
    }

    public double getDistance() {
        return this.dist;
    }
}

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import javafx.util.Pair;

/**
 * @author Alexander J. Heffernan
 * @version 15/05/2024
 */
public class EdmondsKarp {
    private static Map<String,Edge> edges; 
    private static ArrayList<Pair<ArrayList<String>, Integer>> augmentationPaths = null;
    private static Map<String, Pair<Integer, Integer>> originalEdgeData;

    /**
     * Computes the residual graph based on the given original graph.
     * Residual graph includes both original edges and reverse edges.
     * 
     * @param graph - The original graph to compute the residual graph from.
     */
    public static void computeResidualGraph(Graph graph){
        edges = new HashMap<>();
        originalEdgeData = new HashMap<>();        
        int id = 0;

        for (Edge edge : graph.getOriginalEdges()) {
            edges.put(id + "", edge); // Add original edge to the map
            originalEdgeData.put(id + "", new Pair<>(edge.capacity(), edge.flow())); // Store original edge data
            edge.fromCity().addEdgeId(id + ""); // Add edge ID to the fromCity's list
            id++;
            
            // Create and add reverse edge
            Edge reverseEdge = new Edge(edge.toCity(), edge.fromCity(), 0, 0);
            edges.put(id + "", reverseEdge);
            reverseEdge.fromCity().addEdgeId(id + "");
            id++;
        }
    }
    
    /**
     * Calculates the maximum flow in the given graph from a source city to a target city using Edmonds-Karp algorithm.
     * This method computes the residual graph, performs breadth-first search (BFS) to find augmentation paths,
     * updates the residual graph based on the found augmentation paths, and finally resets the edge data.
     * 
     * @param graph - The graph to calculate maximum flows in.
     * @param from - The source city.
     * @param to - The target city.
     * @return - A list of augmentation paths along with their corresponding flows.
     */
    public static ArrayList<Pair<ArrayList<String>, Integer>> calcMaxflows(Graph graph, City from, City to) {
        computeResidualGraph(graph);
        augmentationPaths = new ArrayList<>();
        
        // Repeat until no more augmentation paths can be found
        while (true) {
            // Find an augmentation path using BFS
            Pair<ArrayList<String>, Integer> augmentationPath = bfs(graph, from, to);
            
            // If no more augmentation paths can be found, exit the loop
            if (augmentationPath.getKey() == null)
                break;
                
            augmentationPaths.add(augmentationPath);
            updateResidualGraph(augmentationPath);
        }
        
        resetEdgeData();
        return augmentationPaths;
    }

    /**
     * Performs a breadth-first search (BFS) to find an augmentation path from a source city to a target city in the graph.
     * 
     * @param graph - The graph to perform BFS on.
     * @param source - The source city.
     * @param target - The target city.
     * @return - A pair containing the augmentation path and the bottleneck flow of the path.
     */
    public static Pair<ArrayList<String>, Integer>  bfs(Graph graph, City source, City target) {
        Queue<City> queue = new LinkedList<>();
        HashMap<String, String> backPointer = new HashMap<>();
        ArrayList<String> augmentationPath = new ArrayList<>();
        
        queue.offer(source);
        
        // Perform BFS until queue is empty
        while (!queue.isEmpty()) {
            City current = queue.poll();
            
            // Iterate through the edge IDs of the current city
            for (String edgeId : current.getEdgeIds()) {
                Edge edge = getEdge(edgeId);
                
                // Check if the edge leads to a city other than the source, has not been visited before, and has non-zero capacity
                if (!edge.toCity().equals(source) && backPointer.get(edge.toCity().getId()) == null && edge.capacity() != 0) {                
                    backPointer.put(edge.toCity().getId(), edgeId);
                    
                    // If the target city is reached, reconstruct the augmentation path and return it
                    if (backPointer.get(target.getId()) != null) {
                        return pathRecreation(augmentationPath, backPointer, target);
                    }
                    
                    queue.add(edge.toCity());
                }
            }
        }
        
        // If no augmentation path is found, return null
        return new Pair(null, 0);
    }
    
    /**
     * Updates the residual graph based on the given augmentation path and its corresponding flow.
     * 
     * @param augmentationPath - The augmentation path (list of edge IDs) along which flow is being augmented.
     */
    private static void updateResidualGraph(Pair<ArrayList<String>, Integer> augmentationPath) {
        ArrayList<String> path = augmentationPath.getKey();
        Integer pathFlow = augmentationPath.getValue();
        
        // Iterate through the edges in the augmentation path
        for (String edgeId : path) {
            Edge edge = getEdge(edgeId);
            int edgeIdInt = Integer.valueOf(edgeId);
            
            // Update the flow and capacity of the edge
            edge.setFlow(edge.flow() + pathFlow);
            edge.setCapacity(edge.capacity() - pathFlow);
            
            // Determine if the edge is a reverse edge
            boolean isReverseEdge = edgeIdInt % 2 != 0;
            int reverseId = isReverseEdge ? edgeIdInt - 1 : edgeIdInt + 1;
                
            Edge reverse = getEdge(reverseId + "");
            
            // Update the capacity of the reverse edge
            reverse.setCapacity(reverse.capacity() + pathFlow);
        }
    }
    
    /**
     * Reconstructs the augmentation path and calculates the bottleneck flow along the path.
     * 
     * @param augmentationPath - The list of edge IDs representing the augmentation path
     * @param backPointers - A map containg back pointers to reconstruct the path.
     * @param target - The target city.
     * @return - A pair containing the reconstructed augmentation path and the bottleneck flow along the path.
     */
    public static Pair<ArrayList<String>, Integer> pathRecreation(ArrayList<String> augmentationPath, HashMap<String, String> backPointers, City target) {
        String pathId = backPointers.get(target.getId());
        int bottleneck = Integer.MAX_VALUE;
        
        // Reconstruct the augmentation pathy and calculate the bottleneck flow
        while (pathId != null) {
            augmentationPath.add(pathId);
            // Update the bottleneck value if necessary
            if (getEdge(pathId).capacity() < bottleneck)
                bottleneck = getEdge(pathId).capacity();
            
            pathId = backPointers.get(getEdge(pathId).fromCity().getId());
        }
        
        // Reverse the path to go from source to target
        Collections.reverse(augmentationPath);
        return new Pair<ArrayList<String>, Integer>(augmentationPath, bottleneck);
    }

    /**
     * Resets the edge data (capacity and flow) of all edges in the graph to their original vaules.
     */
    public static void resetEdgeData() {
        // Iterate through the original edge data map
        for (Map.Entry<String, Pair<Integer, Integer>> entry : originalEdgeData.entrySet()) {
            String edgeId = entry.getKey();
            Pair<Integer, Integer> originalData = entry.getValue();
            Edge edge = getEdge(edgeId);

            // Reset the capacity and flow of the edge to its original values
            edge.setCapacity(originalData.getKey());
            edge.setFlow(originalData.getValue());
        }
    }

    public static Edge getEdge(String id){
        return edges.get(id);
    }
}

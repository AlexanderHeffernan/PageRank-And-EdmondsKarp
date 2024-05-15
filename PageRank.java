import javafx.util.Pair;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
* @author Alexander J. Heffernan
* @version 15/05/2024
*/
public class PageRank {
    private static double dampingFactor = .85;
    private static int iter = 10;
    private static Map<String, Double> pageRanks = null;
    private static Map<String, String> mostImportantNeighbours = null;
    
    /**
     * Build the fromLinks and toLinks for each node in the graph.
     * 
     * @param graph - The graph for which to compute the links.
     */
    public static void computeLinks(Graph graph){
        // Iterate through each edge on the graph
        for (Edge edge : graph.getOriginalEdges()) {
            // Get relevant nodes
            Gnode toNode = edge.toNode();
            Gnode fromNode = edge.fromNode();
            
            // Link the two nodes
            toNode.addFromLinks(fromNode);
            fromNode.addToLinks(toNode);
        }
    }
    
    /**
     * Compute the rank of all nodes in the network and display them.
     * 
     * @param graph - The graph for which to compute the PageRank.
     */
    public static void computePageRank(Graph graph){
        int numNodes = graph.getNodes().size();
        
        // Assign initial PageRank value to all nodes
        pageRanks = new HashMap<>();
        for (Gnode node : graph.getNodes().values()) {
            pageRanks.put(node.getId(), 1.0 / numNodes);
        }
        
        int count = 1; // Iteration counter
        
        do {
            double noOutLinkShare = 0.0;
            
            // Computer PageRank contribution for nodes with no outlinks
            for (Gnode node : graph.getNodes().values()) {
                if (node.getToLinks().isEmpty()) {
                    noOutLinkShare += dampingFactor * (pageRanks.get(node.getId()) / numNodes);
                }
            }
            
            // Upate PageRank values for all nodes
            Map<String, Double> newPageRanks = new HashMap<>();
            for (Gnode node : graph.getNodes().values()) {
                double nodeRank = noOutLinkShare + (1.0 - dampingFactor) / numNodes;
                double neighboursShare = 0.0;
                
                // Compute PageRank contribution from neighbours
                for (Gnode backNeighbour : node.getFromLinks()) {
                    int outEdgesCount = backNeighbour.getToLinks().size();
                    neighboursShare += pageRanks.get(backNeighbour.getId()) / outEdgesCount;
                }
                
                // Update PageRank for the current node
                double newNodeRank = nodeRank + dampingFactor * neighboursShare;
                newPageRanks.put(node.getId(), newNodeRank);
            }
            
            // Update PageRank values with the newly computed values
            pageRanks = newPageRanks;
            
            count++;
        } while(count <= iter);
    }
    
    /**
     * Computes the most important neighbour for each node in the graph based on PageRank.
     * 
     * @param graph - The graph for which to compute the most important neighbour.
     */
    public static void computeMostImpneighbour(Graph graph) {
        mostImportantNeighbours = new HashMap<>();
        for (Gnode node : graph.getNodes().values()) {
            Gnode mostImportantNeighbour = null;
            double maxRankDecrease = Double.NEGATIVE_INFINITY;
            
            // Create a copy of the node's in-neighbours set
            Set<Gnode> fromLinksCopy = new HashSet<>(node.getFromLinks());
            
            // Iterate over the node's in-neighbours
            for (Gnode neighbour : fromLinksCopy) {
                // Temporarily remove the link from the current node to the neighbour and recompute PageRank
                node.removeFromLinks(neighbour);
                neighbour.removeToLinks(node);
                computePageRank(graph);
    
                // Get the new PageRank for the current node
                double newRank = getPageRanks().get(node.getId());
    
                // Restore the link the recompute the PageRank
                node.addFromLinks(neighbour);
                neighbour.addToLinks(node);
                computePageRank(graph);
            
                double rankDecrease = getPageRanks().get(node.getId()) - newRank;
                
                // Update the most important neighbour if necessary
                if (rankDecrease > maxRankDecrease) {
                    maxRankDecrease = rankDecrease;
                    mostImportantNeighbour = neighbour;
                }
            }
    
            // If node has most important neighbour, put to map with it, else put to map with null
            if (mostImportantNeighbour != null)
                mostImportantNeighbours.put(node.getName(), mostImportantNeighbour.getName());
            else 
                mostImportantNeighbours.put(node.getName(), "null");
        }
    }

    
    public static int getIteration() {
        return iter;
    }
    
    public static Map<String, Double> getPageRanks() { 
        return Collections.unmodifiableMap(pageRanks); 
    }
    
    public static Map<String, String> getMostImportantNeighbours() {
        return Collections.unmodifiableMap(mostImportantNeighbours);
    }
    
}

package peterkim.wikilucene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import py4j.GatewayServer;

public class EntryPoint {
	
	private Question worker;
	
	public EntryPoint(String dataset, String luceneFolderPath, String luceneSentFolderPath, int topNArticle, int topNSent, String articleSimilarity, String sentSimilarity, int context_a, int context_b, boolean calculateMetrics) {
//		worker = new Question(luceneFolderPath, luceneSentFolderPath, topN, useBM25, showRankResults, threshold);
		worker = new Question(dataset, luceneFolderPath, luceneSentFolderPath, topNArticle, topNSent, articleSimilarity, sentSimilarity, context_a, context_b, calculateMetrics);
	}
	
	public HashMap<String, List<List<String>>> processQueries(List<String> queries) throws Exception {
		HashMap<String, List<List<String>>> r = worker.questionToRelevantSents(queries);
		return r;
	}
	
	public void updateParams(HashMap<String, String> config) {
		worker.updateParams(config);
	}
	
	public static void main(String[] args) {
		String dataset = args[0];
		String luceneFolderPath = args[1];
		String luceneSentFolderPath = args[2];
		int topNArticle = Integer.parseInt(args[3]);
		int topNSent = Integer.parseInt(args[4]);
		String articleSimilarity = args[5];
		String sentSimilarity = args[6];
		int context_a = Integer.parseInt(args[7]);
		int context_b = Integer.parseInt(args[8]);
		boolean calculateMetrics = args[9].equals("t");
		
//		String dataset = "quqsart"
//		String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindex";
//		String luceneSentFolderPath = "/Users/Peter/Documents/wikiluceneindexsent";
//		int topNArticle = 2;
//		int topNSent = 3;
//		String articleSimilarity = "tf25";
//		String sentSimilarity = "simple";
//		int context_a = 2;
//		int context_b = 1;
//		boolean calculateMetrics = false;
		
		GatewayServer gatewayServer = new GatewayServer(new EntryPoint(dataset, luceneFolderPath, luceneSentFolderPath, topNArticle, topNSent, articleSimilarity, sentSimilarity, context_a, context_b, calculateMetrics));
        gatewayServer.start();
        System.out.println("Gateway server started");
//        gatewayServer.shutdown();
	}
}

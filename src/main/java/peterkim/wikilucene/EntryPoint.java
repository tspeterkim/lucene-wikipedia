package peterkim.wikilucene;

import java.util.ArrayList;
import py4j.GatewayServer;

public class EntryPoint {
	
	private Question worker;
	
	public EntryPoint(String luceneFolderPath, String luceneParaFolderPath, int topN, boolean useBM25, double threshold) {
		worker = new Question(luceneFolderPath, luceneParaFolderPath, topN, useBM25, threshold);
	}
	
	public ArrayList<ArrayList<String>> processQueries(ArrayList<String> queries) throws Exception {
		ArrayList<ArrayList<String>> r = worker.questionToRelevantPgs(queries);
		worker.showRankStats();
		return r;
	}
	
	public static void main(String[] args) {
//		String luceneFolderPath = args[0];
//		String luceneParaFolderPath = args[1];
//		int topN = Integer.parseInt(args[2]);;
//		boolean useBM25 = (args[3].equals("t"));
//		double threshold = Double.parseDouble(args[4]);
		
		String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindex";
		String luceneParaFolderPath = "/Users/Peter/Documents/wikiluceneindexpara";
		int topN = 2;
		boolean useBM25 = true;
		double threshold = 0.9;
		
		GatewayServer gatewayServer = new GatewayServer(new EntryPoint(luceneFolderPath, luceneParaFolderPath, topN, useBM25, threshold));
        gatewayServer.start();
        System.out.println("Gateway server started");
//        gatewayServer.shutdown();
	}
}

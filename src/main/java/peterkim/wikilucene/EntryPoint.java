package peterkim.wikilucene;

import java.util.ArrayList;
import py4j.GatewayServer;

public class EntryPoint {
	
	private Question worker;
	
	public EntryPoint(String luceneFolderPath, String luceneParaFolderPath, int topN, boolean useBM25) {
		worker = new Question(luceneFolderPath, luceneParaFolderPath, topN, useBM25);
	}
	
	public ArrayList<ArrayList<String>> processQueries(ArrayList<String> queries) throws Exception {
		ArrayList<ArrayList<String>> r = worker.questionToRelevantPgs(queries);
		worker.showRankStats();
		return r;
	}
	
	public static void main(String[] args) {
		String luceneFolderPath = args[0];
		String luceneParaFolderPath = args[1];
		int topN = Integer.parseInt(args[2]);;
		boolean useBM25 = (args[3].equals("t"));
		
//		String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindex";
//		String luceneParaFolderPath = "/Users/Peter/Documents/wikiluceneindexpara";
//		int topN = 2;
//		boolean useBM25 = true;
		
		GatewayServer gatewayServer = new GatewayServer(new EntryPoint(luceneFolderPath, luceneParaFolderPath, topN, useBM25));
        gatewayServer.start();
        System.out.println("Gateway server started");
//        gatewayServer.shutdown();
	}
}

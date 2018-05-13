package peterkim.wikilucene;

import java.util.HashMap;
import java.util.List;
import py4j.GatewayServer;

public class EntryPoint {
	
	private Question worker;
	
	public EntryPoint() {
		worker = new Question();
	}
	
	public HashMap<String, List<List<String>>> processQueries(List<String> queries) throws Exception {
		return worker.questionToRelevantSents(queries);
	}
	
	public HashMap<String, List<String>> processQueriesForQuasartShort(List<String> queries) throws Exception {
		return worker.questionToRelevantSentsForQuasartShort(queries);
	}
	
	public static void main(String[] args) {
		GatewayServer gatewayServer = new GatewayServer(new EntryPoint());
        gatewayServer.start();
        System.out.println("Gateway server to lucene started!");
	}
}

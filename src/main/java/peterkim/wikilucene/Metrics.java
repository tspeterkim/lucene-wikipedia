package peterkim.wikilucene;

import java.util.List;

import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.document.Document;


public class Metrics {
	public static double getAVP(List<String> idealResult, List<Document> result) {
		QualityStats qs = new QualityStats(result.size(), 1000); // TODO: anticipated maximal number of relevant hits?
		for (int i = 0; i < result.size(); i++) {
			System.out.println(i);
			qs.addResult(i, idealResult.contains(result.get(i).get("title")), 1000);
		}
		return qs.getAvp();
	}
	
	public static double getMRR(List<String> idealResult, List<Document> result) {
		QualityStats qs = new QualityStats(result.size(), 1000); // TODO: anticipated maximal number of relevant hits?
		for (int i = 0; i < result.size(); i++) {
			qs.addResult(i, idealResult.contains(result.get(i).get("title")), 1000);
		}
		return qs.getMRR();
	}
	/**
	 * Compute the normalized discounted cumulative gain (NDCG) of a list of ranked items.
	 *
	 * @return the NDCG for the given data
	 */	
	public static double calculateNDCG(int x, List<String> idealResult, List<Document> result) {
		double idcg = calculateIDCG(x);
		if (idcg == 0)
			return 0;

		double dcg = 0;
		for (int i = 0; i < Math.min(x, result.size()); i++) {
			int rel = 1;

			if (!idealResult.contains(result.get(i).get("title")))
				rel = 0;

			dcg += (Math.pow(2, rel) - 1.0)  * (Math.log(2) / Math.log(i + 2));
		}
		return dcg / idcg;
	}

	/**
	 * Calculates the iDCG
	 * 
	 * @param n size of the expected resource list
	 * @return iDCG
	 */
	private static double calculateIDCG(int x) {
		double idcg = 0;
		for (int i = 0; i < x; i++)
			idcg += (Math.log(2) / Math.log(i + 2));
		return idcg;
	}

}
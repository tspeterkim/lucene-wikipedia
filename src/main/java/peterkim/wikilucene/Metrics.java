package peterkim.wikilucene;

import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.document.Document;

public class Metrics {
	
	public static boolean isContained(LevensteinDistance LD, double threshold, String goldPara, List<String> result) {
		for (int i = 0; i < result.size(); i++) {
			if (LD.getDistance(goldPara, result.get(i)) >= threshold) {
				return true;
			}
		}
		return false;
	}
	
	public static double getRR(List<String> idealResult, List<Document> result) {
		double rr = 0.0;
		for (int i = 1; i <= result.size(); i++) {
			if (idealResult.contains(result.get(i-1).get("title"))) {
				rr = 1.0 / i;
				break;
			}
		}
		return rr;
	}
	
	public static double getRR(int min, List<Integer> result) {
		double rr = 0.0;
		for (int i = 1; i <= result.size(); i++) {
			if (min == result.get(i-1)) {
				rr = 1.0 / i;
				break;
			}
		}
		return rr;
	}
	
	public static double getParaRR(LevensteinDistance LD, double threshold, String goldPara, List<String> result) {
		double rr = 0.0;
		for (int i = 1; i <= result.size(); i++) {
			if (LD.getDistance(goldPara, result.get(i-1)) >= threshold) {
				rr = 1.0 / i;
				break;
			}
		}
		return rr;
	}

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

	private static double calculateIDCG(int x) {
		double idcg = 0;
		for (int i = 0; i < x; i++)
			idcg += (Math.log(2) / Math.log(i + 2));
		return idcg;
	}

}
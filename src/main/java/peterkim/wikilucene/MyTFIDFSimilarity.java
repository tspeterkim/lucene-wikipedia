package peterkim.wikilucene;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class MyTFIDFSimilarity extends SimilarityBase {
   @Override
   protected float score(BasicStats stats, float termFreq, float docLength) {
	   // TFIDF's TF + BM25's IDF
       float relevance = (float) (Math.log(termFreq + 1.0) * Math.log((stats.getNumberOfDocuments() - stats.getDocFreq() + 0.5) / (stats.getDocFreq() + 0.5)));
       return relevance;
   }

	@Override
	public String toString() {
		return "My TFIDF";
	}
}

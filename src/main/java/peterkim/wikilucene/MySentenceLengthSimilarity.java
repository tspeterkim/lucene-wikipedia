package peterkim.wikilucene;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class MySentenceLengthSimilarity extends SimilarityBase {
	@Override
   protected float score(BasicStats stats, float termFreq, float docLength) {
	   return (float) 1.0 / docLength;
	   
   }

	@Override
	public String toString() {
		return "My Sentence Length Similarity";
	}
}

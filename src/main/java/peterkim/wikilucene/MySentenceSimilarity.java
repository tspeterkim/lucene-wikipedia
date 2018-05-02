package peterkim.wikilucene;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class MySentenceSimilarity extends SimilarityBase {
	   @Override
	   protected float score(BasicStats stats, float termFreq, float docLength) {
		   return (float) 1.0;
	   }

		@Override
		public String toString() {
			return "My Sentence";
		}
}	

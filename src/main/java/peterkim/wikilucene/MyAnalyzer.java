package peterkim.wikilucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class MyAnalyzer {
	private static Analyzer analyzer = new Analyzer() {
		@Override
		protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
			StandardTokenizer tokenizer = new StandardTokenizer(reader);
			TokenStream ts = new StopFilter(tokenizer, StandardAnalyzer.STOP_WORDS_SET);
			int flags = WordDelimiterFilter.SPLIT_ON_NUMERICS | WordDelimiterFilter.SPLIT_ON_CASE_CHANGE
					| WordDelimiterFilter.GENERATE_NUMBER_PARTS | WordDelimiterFilter.GENERATE_WORD_PARTS;
			ts = new WordDelimiterFilter(ts, flags, null);
			ts = new LowerCaseFilter(ts);
			ts = new PorterStemFilter(ts);
			return new TokenStreamComponents(tokenizer, ts);
			////		StandardTokenizer tokenizer = new StandardTokenizer(reader);
			//		NGramTokenizer tokenizer = new NGramTokenizer(reader, 1, 2);
			//		TokenStream ts = new StopFilter(tokenizer, StandardAnalyzer.STOP_WORDS_SET);
			////		int flags = WordDelimiterFilter.SPLIT_ON_NUMERICS | WordDelimiterFilter.SPLIT_ON_CASE_CHANGE
			////				| WordDelimiterFilter.GENERATE_NUMBER_PARTS | WordDelimiterFilter.GENERATE_WORD_PARTS;
			////		ts = new WordDelimiterFilter(ts, flags, null);
			//		ts = new LowerCaseFilter(ts);
			//		ts = new PorterStemFilter(ts);
			////		ts = new NGramTokenFilter(ts, 1, 2);
			//		return new TokenStreamComponents(tokenizer, ts);
		}
	};
	public static Analyzer getAnalyzer() {
		return analyzer;
	}
}

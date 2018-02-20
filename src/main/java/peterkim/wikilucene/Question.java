package peterkim.wikilucene;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Question {

	private boolean useBM25;
	private int topN;

	private Analyzer analyzer;
	private IndexSearcher searcher;
	private IndexSearcher searcherPara;
	private IndexReader luceneReader;
	private IndexReader luceneReaderPara;
		
	private int totalNumQuery = 0;
	private int badSearchCount = 0;
	private double totalRR = 0.0;
	private double totalNDCG = 0.0;
	private ArrayList<Double> reciRanks = new ArrayList<Double>();

	public Question(String luceneFolderPath, String luceneParaFolderPath, int topN, boolean useBM25) {
		this.topN = topN;
		this.useBM25 = useBM25;

		try {
			analyzer = new Analyzer() {
				@Override
				protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
					StandardTokenizer tokenzier = new StandardTokenizer(reader);
					TokenStream ts = new StopFilter(tokenzier, StandardAnalyzer.STOP_WORDS_SET);
					int flags = WordDelimiterFilter.SPLIT_ON_NUMERICS | WordDelimiterFilter.SPLIT_ON_CASE_CHANGE
							| WordDelimiterFilter.GENERATE_NUMBER_PARTS | WordDelimiterFilter.GENERATE_WORD_PARTS;
					ts = new WordDelimiterFilter(ts, flags, null);
					ts = new LowerCaseFilter(ts);
					ts = new PorterStemFilter(ts);

					return new TokenStreamComponents(tokenzier, ts);
				}
			};

			luceneReader = DirectoryReader.open(FSDirectory.open(new File(luceneFolderPath)));
			luceneReaderPara = DirectoryReader.open(FSDirectory.open(new File(luceneParaFolderPath)));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public ArrayList<ArrayList<String>> questionToRelevantPgs(ArrayList<String> queries) throws Exception {
		
		ArrayList<ArrayList<String>> finalResults = new ArrayList<ArrayList<String>>();
		
		try {
			searcher = new IndexSearcher(luceneReader);
			
			if (this.useBM25)
				searcher.setSimilarity(new BM25Similarity());
			else
				searcher.setSimilarity(new DefaultSimilarity()); // default is TFIDF
			
			for (String q : queries) {
				List<Document> result = new ArrayList<Document>();

				StringTokenizer tk = new StringTokenizer(q, "|");
				String id = tk.nextToken();
				String question = escapeSymbols(tk.nextToken());
				String goldArticle = tk.nextToken();

				System.out.println("Query id=" + id + " => " + question + ", Gold Article: " + goldArticle);

				String qstring = createQueryString(question);
				QueryParser parser = new QueryParser("text", analyzer);
				Query query = parser.parse("title:(" + QueryParser.escape(qstring) + ") OR text:(" + QueryParser.escape(qstring) + ")");
				
				TopScoreDocCollector collector = TopScoreDocCollector.create(this.topN, true);
				searcher.search(query, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;

				System.out.println("Found " + hits.length + " articles.");
				for (int i = 0; i < hits.length; i++) {
					int docId = hits[i].doc;
					Document d = searcher.doc(docId);
//					String title = d.get("title");
//					double score = hits[i].score;
//					System.out.println((i + 1) + ". ("+docId+") title: " + title + ", score: " + score);
					result.add(d);
				}
				
				// calculate search quality metrics
				double ndcg = Metrics.calculateNDCG(5, Arrays.asList(goldArticle), result);
				double rr = Metrics.getRR(Arrays.asList(goldArticle), result);
				if (rr == 0.0) {
					badSearchCount++;
				}
				reciRanks.add(rr);
				System.out.println("NDCG@" + Math.min(5, result.size()) + ": " + ndcg + ", Reciprocal Rank (RR): " + rr);
				
				totalNumQuery++;
				totalNDCG += ndcg;
				totalRR += rr;
				
				System.out.println("Searching paragraphs...");
				
				searcherPara = new IndexSearcher(luceneReaderPara);
				searcherPara.setSimilarity(new DefaultSimilarity());
				
				QueryParser parserPara = new QueryParser("paragraph", analyzer);
				
				String qp = "(";
				for (Document d : result) {
					qp += "title:(" + QueryParser.escape(d.get("title"))+ ") OR ";
				}
				qp = qp.substring(0, qp.length() - 4); // delete the last OR clause
				qp += ") AND paragraph:(" + QueryParser.escape(qstring) + ")";
//				System.out.println(qp);
				Query queryPara = parserPara.parse(qp);
				
				TopScoreDocCollector collectorPara = TopScoreDocCollector.create(this.topN, true);
				searcherPara.search(queryPara, collectorPara);
				ScoreDoc[] hitsPara = collectorPara.topDocs().scoreDocs;

				System.out.println("Found " + hitsPara.length + " paragraphs.");
				ArrayList<String> pgs = new ArrayList<String>();
				for (int i = 0; i < hitsPara.length; i++) {
					int docId = hitsPara[i].doc;
					Document d = searcherPara.doc(docId);
//					String title = d.get("title");
					pgs.add(d.get("paragraph"));
//					double score = hitsPara[i].score;
//					System.out.println((i + 1) + ". title: " + title + ", pg: " + pg + ", score: " + score);
//					result.add(d);
//					System.out.println(text);
				}
				finalResults.add(pgs);
				
				System.out.println("------------------");
			}
			
		} finally {
//			if (luceneReader != null)
//				luceneReader.close();
//			
//			if (luceneReaderPara != null)
//				luceneReaderPara.close();
		}
		
		return finalResults;
	}

	public void showRankStats() {
		System.out.println("Avg. NDCG for " + totalNumQuery + " queries = " + (totalNDCG / totalNumQuery));
		System.out.println("MRR for " + totalNumQuery + " queries = " + (totalRR / totalNumQuery));
		System.out.println("Min RR = " + Collections.min(reciRanks) + ", Max RR = " + Collections.max(reciRanks));
		System.out.println("Bad Search Count = " + badSearchCount);
	}
	
	private String createQueryString(String qstring) { // TODO: create better query? e.g. where is UCLA? -> UCLA
		return qstring;
	}

	private String escapeSymbols(String origin) {
		// escape + - && || ! ( ) { } [ ] ^ " ~ * ? : \
		return origin.replace("(", " ").replace(")", " ").replace("+", " ")
				.replace("-", " ").replace("&", " ").replace("|", " ")
				.replace("{", " ").replace("}", " ").replace("[", " ")
				.replace("]", " ").replace("^", " ").replace("\"", " ")
				.replace("~", " ").replace("*", " ").replace("?", " ")
				.replace(":", " ").replace("\\", " ").replace("/", " ");
	}

	public static void main(String[] args) throws Exception {
//		if (args.length != 4) {
//			// "java -cp lucene-wikipedia-0.0.1-jar-with-dependencies.jar markpeng.wiki.QuestionToWiki /home/uitox/wiki/lucene-wiki-index validation_set.tsv lucene_top10_nolengthnorm_or_submit.csv"
//			System.err
//					.println("Usage: java -cp lucene-wikipedia-0.0.1-jar-with-dependencies.jar "
//							+ "markpeng.wiki.QuestionToWiki <path of lucene index folder> "
//							+ " <input file> " + "<output file> <topN>");
//			System.exit(-1);
//		}

		String luceneFolderPath = args[0];
		String luceneParaFolderPath = args[1];
		String inputPath = args[2];
		String outputPath = args[3];
		int topN = Integer.parseInt(args[4]);
		boolean useBM25 = (args[5].equals("t")); // t for true
		
//		String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindex";
//		String luceneParaFolderPath = "/Users/Peter/Documents/wikiluceneindexpara";
//		String inputPath = "/Users/Peter/Documents/wikiluceneinput/input.txt";
//		String outputPath = "/Users/Peter/Documents/wikiluceneoutput/output.txt";
//		int topN = 2;
//		boolean useBM25 = true;

//		Question worker = new Question(luceneFolderPath, luceneParaFolderPath, topN, useBM25);
//		worker.questionToRelevantPgs();
//		worker.docsToRelevantParagraphs(topN, false); // use tfidf for paragraph search
//		worker.showRankStats();
//		NDCG.calculateNDCG(worker.questionToRelevantDocs(topN));
//		List<String> gold = Arrays.asList("Warsaw");
//		List<String> res1 = Arrays.asList("Warsaw","Poland","Germany");
//		List<String> res2 = Arrays.asList("Warsaw");
//		System.out.println(Metrics.calculateNDCG(gold, res1));
	}

}

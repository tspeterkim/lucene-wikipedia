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
import org.apache.lucene.search.spell.LevensteinDistance;

public class Question {

	private boolean useBM25;
	private int topN;


	private LevensteinDistance LD;
	private Analyzer analyzer;
	private IndexSearcher searcher;
	private IndexSearcher searcherPara;
	private IndexReader luceneReader;
	private IndexReader luceneReaderPara;
		
	private int totalNumQuery = 0;
	private int badSearchCount = 0;
	private int badParaSearchCount = 0;
	private double p1ParaCount = 0.0;
	private double p5ArticleCount = 0.0;
	private double p10ArticleCount = 0.0;
	private double p20ArticleCount = 0.0;
	private double p50ArticleCount = 0.0;
	private double p5AnswerCount = 0.0;
	private double p10AnswerCount = 0.0;
	private double p20AnswerCount = 0.0;
	private double p50AnswerCount = 0.0;
	private double p2Count = 0.0;
	private double totalRR = 0.0;
	private double totalParaRR = 0.0;
	private double totalNDCG = 0.0;
	private double threshold;
	private ArrayList<Double> reciRanks = new ArrayList<Double>();

	public Question(String luceneFolderPath, String luceneParaFolderPath, int topN, boolean useBM25, double threshold) {
		this.topN = topN;
		this.useBM25 = useBM25;
		this.threshold = threshold;
		LD = new LevensteinDistance();
		
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
	
	private ArrayList<String> processQuestion(String q) throws IOException, ParseException {
		
		List<Document> result = new ArrayList<Document>();

		StringTokenizer tk = new StringTokenizer(q, "|");
		String id = tk.nextToken();
		String question = escapeSymbols(tk.nextToken());
		String goldArticle = tk.nextToken();
		String goldParagraph = tk.nextToken();
		String goldAnswer = tk.nextToken();
		
		System.out.println("Query id=" + id + " => " + question + ", Gold Article: " + goldArticle + ", Gold Answer: " + goldAnswer);

		String qstring = createQueryString(question);
		QueryParser parser = new QueryParser("text", analyzer);
		Query query = parser.parse("title:(" + QueryParser.escape(qstring) + ") OR text:(" + QueryParser.escape(qstring) + ")");
//		Query query = parser.parse(QueryParser.escape(qstring));
		
		TopScoreDocCollector collector = TopScoreDocCollector.create(this.topN, true);
		searcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		System.out.println("Found " + hits.length + " articles.");
		for (int i = 0; i < hits.length; i++) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
//			String title = d.get("title");
//			double score = hits[i].score;
//			System.out.println((i + 1) + ". ("+docId+") title: " + title + ", score: " + score);
			result.add(d);
		}
		
		for (int i = 0; i < Math.min(50, result.size()); i++) {
			if (result.get(i).get("text").contains(goldAnswer)) {
				if (i <= 4) {
					p5AnswerCount += 1.0;
					p10AnswerCount += 1.0;
					p20AnswerCount += 1.0;
					p50AnswerCount += 1.0;
				} else if (i <= 9) {
					p10AnswerCount += 1.0;
					p20AnswerCount += 1.0;
					p50AnswerCount += 1.0;
				} else if (i <= 19) {
					p20AnswerCount += 1.0;
					p50AnswerCount += 1.0;
				} else if (i <= 49) {
					p50AnswerCount += 1.0;
				}
				
				break;
			}
		}
		
//		List<String> pgsFromTop5 = new ArrayList<String>();
//		for (int i = 0; i < Math.min(5, result.size()); i++) {
//			pgsFromTop5.addAll(Arrays.asList((result.get(i).get("text").split("\n\n"))));
//		}
		
		// calculate search quality metrics
//		p1ParaCount += Metrics.isContained(LD, this.threshold, goldParagraph, pgsFromTop5) ? 1.0 : 0.0;
		
		for (int i = 0; i < Math.min(50, result.size()); i++) {
			if (result.get(i).get("title").equals(goldArticle)) {
				if (i <= 4) {
					p5ArticleCount += 1.0;
					p10ArticleCount += 1.0;
					p20ArticleCount += 1.0;
					p50ArticleCount += 1.0;
				} else if (i <= 9) {
					p10ArticleCount += 1.0;
					p20ArticleCount += 1.0;
					p50ArticleCount += 1.0;
				} else if (i <= 19) {
					p20ArticleCount += 1.0;
					p50ArticleCount += 1.0;
				} else if (i <= 49) {
					p50ArticleCount += 1.0;
				}
				
				break;
			}
		}
		
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
		
//		System.out.println("Searching paragraphs...");
//		
//		searcherPara = new IndexSearcher(luceneReaderPara);
//		searcherPara.setSimilarity(new DefaultSimilarity());
//		
//		QueryParser parserPara = new QueryParser("paragraph", analyzer);
//		
//		String qp = "(";
//		for (Document d : result) {
//			// there are article titles "AND gate" and "OR gate" in which case lucene confuses it as a boolean operator
//			if (d.get("title").contains("AND") || d.get("title").contains("OR")) 
//				qp += "title:'" + QueryParser.escape(d.get("title")) + "' OR ";
//			else
//				qp += "title:(" + QueryParser.escape(d.get("title")) + ") OR ";
//		}
//		qp = qp.substring(0, qp.length() - 4); // delete the last OR clause
//		qp += ") AND paragraph:(" + QueryParser.escape(qstring) + ")";
////		System.out.println(qp);
//		Query queryPara = parserPara.parse(qp);
//		
//		TopScoreDocCollector collectorPara = TopScoreDocCollector.create(this.topN, true);
//		searcherPara.search(queryPara, collectorPara);
//		ScoreDoc[] hitsPara = collectorPara.topDocs().scoreDocs;
//
//		System.out.println("Found " + hitsPara.length + " paragraphs.");
//		List<String> pgs = new ArrayList<String>();
//		for (int i = 0; i < hitsPara.length; i++) {
//			int docId = hitsPara[i].doc;
//			Document d = searcherPara.doc(docId);
////			String title = d.get("title");
//			pgs.add(d.get("paragraph"));
////			double score = hitsPara[i].score;
////			System.out.println((i + 1) + ". title: " + title + ", pg: " + pg + ", score: " + score);
////			result.add(d);
////			System.out.println(text);
//		}
//		
//		if (this.topN > pgsFromTop5.size()) {
//			List<String> newpgs = pgs.subList(0, pgsFromTop5.size());
//			p2Count += Metrics.isContained(LD, this.threshold, goldParagraph, newpgs) ? 1 : 0;
//		} else {
//			p2Count += Metrics.isContained(LD, this.threshold, goldParagraph, pgs) ? 1 : 0;
//		}
//		
//		double pararr = Metrics.getParaRR(LD, this.threshold, goldParagraph, pgs);
//		if (pararr == 0.0)
//			badParaSearchCount++;
//		totalParaRR += pararr;
//				
//		
		System.out.println("------------------");
		List<String> pgs = new ArrayList<String>();
		return (ArrayList<String>) pgs;
		
	}
	
	public void questionToRelevantPgs(String inputPath) throws Exception {
		BufferedReader inputReader = null;
		try {
			searcher = new IndexSearcher(luceneReader);
			
			if (this.useBM25)
				searcher.setSimilarity(new BM25Similarity());
			else
				searcher.setSimilarity(new DefaultSimilarity()); // default is TFIDF
			
			inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));
			
			String q;
			while ((q = inputReader.readLine()) != null) {
				processQuestion(q);
			}
			
		} finally {
			if (luceneReader != null)
				luceneReader.close();
			
			if (luceneReaderPara != null)
				luceneReaderPara.close();
			
			if (inputReader != null)
				inputReader.close();
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
				ArrayList<String> curResult = processQuestion(q);
				finalResults.add(curResult);
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
//		System.out.println("BM25: % of questions for which the answer paragraph appears in top 5 articles = " + (p1ParaCount / totalNumQuery));
		System.out.println("Use BM25 is " + this.useBM25);
		System.out.println("% of questions for which the gold article appears in top 5 articles = " + (p5ArticleCount / totalNumQuery));
		System.out.println("% of questions for which the gold article appears in top 10 articles = " + (p10ArticleCount / totalNumQuery));
		System.out.println("% of questions for which the gold article appears in top 20 articles = " + (p20ArticleCount / totalNumQuery));
		System.out.println("% of questions for which the gold article appears in top 50 articles = " + (p50ArticleCount / totalNumQuery));
		System.out.println("% of questions for which the gold answer appears in top 5 articles = " + (p5AnswerCount / totalNumQuery));
		System.out.println("% of questions for which the gold answer appears in top 10 articles = " + (p10AnswerCount / totalNumQuery));
		System.out.println("% of questions for which the gold answer appears in top 20 articles = " + (p20AnswerCount / totalNumQuery));
		System.out.println("% of questions for which the gold answer appears in top 50 articles = " + (p50AnswerCount / totalNumQuery));
//		System.out.println("TFIDF: % of questions for which the answer paragraph appears = " + (p1Count / totalNumQuery));
		System.out.println("Avg. NDCG for " + totalNumQuery + " queries = " + (totalNDCG / totalNumQuery));
		System.out.println("Article MRR for " + totalNumQuery + " queries = " + (totalRR / totalNumQuery));
//		System.out.println("Article Min RR = " + Collections.min(reciRanks) + ", Max RR = " + Collections.max(reciRanks));
		System.out.println("Article Bad Search Count = " + badSearchCount);
		System.out.println("Para MRR for " + totalNumQuery + " queries = " + (totalParaRR / totalNumQuery));
		System.out.println("Para Bad Search Count = " + badParaSearchCount);
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
		double threshold = Double.parseDouble(args[6]);
		
//		String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindex";
//		String luceneParaFolderPath = "/Users/Peter/Documents/wikiluceneindexpara";
//		String inputPath = "/Users/Peter/Documents/wikiluceneinput/input.txt";
//		String outputPath = "/Users/Peter/Documents/wikiluceneoutput/output.txt";
//		int topN = 2;
//		boolean useBM25 = true;
//		double threshold = 1.0;

		Question worker = new Question(luceneFolderPath, luceneParaFolderPath, topN, useBM25, threshold);
		worker.questionToRelevantPgs(inputPath);
		worker.showRankStats();

	}

}

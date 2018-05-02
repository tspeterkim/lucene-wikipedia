package peterkim.wikilucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;

import com.google.gson.Gson;

public class Question {
	class DocDouble implements Comparable<DocDouble> {
		public DocDouble(Document d, double r) {
			this.d = d;
			this.r = r;
		}
		public Document d;
		public double r;
		
		@Override
		public int compareTo(DocDouble o) {
			if (this.r > o.r)
				return 1;
			else if (this.r < o.r)
				return -1;
			return 0;
		}
	}
	
	private String dataset;
	
	private HashMap<String, List<List<String>>> finalResults = new HashMap<String, List<List<String>>>();
	
	private int topNArticle;
	private int topNSent;
	
	private List<Integer> as = new ArrayList<Integer>();
	private List<Integer> bs = new ArrayList<Integer>();
	private List<Integer> minAs = new ArrayList<Integer>();
	private List<Integer> minBs = new ArrayList<Integer>();
	private int context_a;
	private int context_b;
	
	private boolean calculateMetrics;

	private Analyzer analyzer;
	private IndexSearcher searcherBody;
	private IndexSearcher searcherTitle;
	private IndexSearcher searcherSent1;
	private IndexSearcher searcherSent2;
	private IndexSearcher searcherDist;
	private IndexReader luceneReader;
	private IndexReader luceneReaderSent;
		
	private int totalNumQuery = 0;
	private int badSearchCount = 0;
	private int badParaSearchCount = 0;
	private double p1ParaCount = 0.0;
	private double p5ArticleCount = 0.0;
	private double p10ArticleCount = 0.0;
	private double p20ArticleCount = 0.0;
	private double p50ArticleCount = 0.0;
	private double p1AnswerCount = 0.0;
	private double p3AnswerCount = 0.0;
	private double p5AnswerCount = 0.0;
	private double p10AnswerCount = 0.0;
	private double p20AnswerCount = 0.0;
	private double p50AnswerCount = 0.0;
	private double p2Count = 0.0;
	private double totalRR = 0.0;
	private double totalParaRR = 0.0;
	private double totalNDCG = 0.0;
	private ArrayList<Double> rrAs = new ArrayList<Double>();
	private ArrayList<Double> rrBs = new ArrayList<Double>();
	private ArrayList<Double> reciRanks = new ArrayList<Double>();

	public Question(String dataset, String luceneFolderPath, String luceneSentFolderPath, int topNArticle, int topNSent, String articleSimilarity, String sentSimilarity, int context_a, int context_b, boolean calculateMetrics) {
		this.dataset = dataset;
		this.topNArticle = topNArticle;
		this.topNSent = topNSent;
		this.calculateMetrics = calculateMetrics;
		this.context_a = context_a;
		this.context_b = context_b;
		this.calculateMetrics = calculateMetrics;
		analyzer = MyAnalyzer.getAnalyzer();
		
		try {
			luceneReader = DirectoryReader.open(FSDirectory.open(new File(luceneFolderPath)));
			luceneReaderSent = DirectoryReader.open(FSDirectory.open(new File(luceneSentFolderPath)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		searcherBody = new IndexSearcher(luceneReader);
		searcherTitle = new IndexSearcher(luceneReader);
		searcherSent1 = new IndexSearcher(luceneReaderSent);
		searcherSent2 = new IndexSearcher(luceneReaderSent);
		searcherDist = new IndexSearcher(luceneReaderSent);
		
		setSimilarities(articleSimilarity, sentSimilarity);
	}
	
	private void setSimilarities(String articleSim, String sentSim) {
		switch (articleSim) {
		case "tf25":
			searcherBody.setSimilarity(new MyTFIDFSimilarity());
			searcherTitle.setSimilarity(new MyTFIDFSimilarity());
			break;
		case "tfidf":
			searcherBody.setSimilarity(new DefaultSimilarity());
			searcherTitle.setSimilarity(new DefaultSimilarity());
			break;
		case "bm25":
			searcherBody.setSimilarity(new BM25Similarity());
			searcherTitle.setSimilarity(new BM25Similarity());
			break;
		default: // tfidf
			searcherBody.setSimilarity(new DefaultSimilarity());
			searcherTitle.setSimilarity(new DefaultSimilarity());
			break;
		}
		
		switch (sentSim) {
		case "simple":
			searcherSent1.setSimilarity(new MySentenceSimilarity());
			break;
		case "tf25":
			searcherSent1.setSimilarity(new MyTFIDFSimilarity());
			break;
		case "tfidf":
			searcherSent1.setSimilarity(new DefaultSimilarity());
			break;
		case "bm25":
			searcherSent1.setSimilarity(new BM25Similarity());
			break;
		default: // tfidf
			searcherSent1.setSimilarity(new DefaultSimilarity());
			break;
		}
	}
	
	
	public void updateParams(HashMap<String, String> config) {
		if (config.containsKey("topNArticle"))
			this.topNArticle = Integer.parseInt(config.get("topNArticle"));
		if (config.containsKey("topNSent"))
			this.topNSent = Integer.parseInt(config.get("topNSent"));
		if (config.containsKey("calculateMetrics"))
			this.calculateMetrics = config.get("calculateMetrics").equals("t");
		if (config.containsKey("context_a"))	
			this.context_a = Integer.parseInt(config.get("context_a"));
		if (config.containsKey("context_b"))
			this.context_b = Integer.parseInt(config.get("context_b"));
		if (config.containsKey("articleSimilarity") && config.containsKey("sentSimilarity"))
			setSimilarities(config.get("articleSimilarity"), config.get("sentSimilarity"));
	}
	
	
	private boolean stringContainsItemFromList(String inputStr, String[] items) {
	    for(int i = 0; i < items.length; i++) 
	        if (inputStr.contains(items[i]))
	            return true;
	    return false;
	}
	
	private Document[] mergeResults(ScoreDoc[] tr, ScoreDoc[] br) throws IOException {
		
		HashMap<String, DocDouble> m = new HashMap<String, DocDouble>();
		for (int i = 0; i < tr.length; i++) {
			m.put(searcherTitle.doc(tr[i].doc).get("id"), new DocDouble(searcherTitle.doc(tr[i].doc), 1.0 / (i + 1)));
		}
		
		for (int i = 0; i < br.length; i++) {
			if (m.containsKey(searcherBody.doc(br[i].doc).get("id")))
				m.put(searcherBody.doc(br[i].doc).get("id"), new DocDouble(searcherBody.doc(br[i].doc), (1.0 / (i + 1)) + m.get(searcherBody.doc(br[i].doc).get("id")).r));
			else
				m.put(searcherBody.doc(br[i].doc).get("id"), new DocDouble(searcherBody.doc(br[i].doc), 1.0 / (i + 1)));
		}
		
		List<DocDouble> ddlist = new ArrayList<DocDouble>();
		for (Entry<String, DocDouble> e : m.entrySet()) {
		    DocDouble dd = e.getValue();
		    ddlist.add(dd);
		}
		
		Collections.sort(ddlist, Collections.reverseOrder());
		
		Document[] result = new Document[ddlist.size()];
		for (int i = 0; i < ddlist.size(); i++) {
			result[i] = ddlist.get(i).d;
		}
		return result;
	}
	
	private ArrayList<String> processQuestion(String q) throws IOException, ParseException, InvalidTokenOffsetsException {
		List<Document> result = new ArrayList<Document>();
		
		String id = "-1";
		String question = "na";
		String goldAnswers = "na";
		
		if (dataset.equals("squad")) {
			StringTokenizer tk = new StringTokenizer(q, "|");
			id = tk.nextToken();
			question = tk.nextToken();
			String goldArticle = tk.nextToken();
			String goldParagraph = tk.nextToken();
			goldAnswers = tk.nextToken();
		} else if (dataset.equals("quasart")) {
			Gson gson = new Gson();
			QuasartQuery qq = gson.fromJson(q, QuasartQuery.class);
			id = qq.uid;
			question = qq.question;
			goldAnswers = qq.answer;
		} else if (dataset.equals("triviaqa")) {
			StringTokenizer tk = new StringTokenizer(q, "|");
			id = tk.nextToken();
			question = tk.nextToken();
			goldAnswers = tk.nextToken();
		}
		
		System.out.println("Query id=" + id + " => " + question + ", Gold Answer(s): " + goldAnswers);

		QueryParser parserBody = new QueryParser("text", analyzer);
		Query queryBody = parserBody.parse(QueryParser.escape(question));		
		TopDocs docsBody = searcherBody.search(queryBody, this.topNArticle);
		ScoreDoc[] hitsBody = docsBody.scoreDocs;
		System.out.println("* Found " + hitsBody.length + " articles based on body.");
		for (int k = 0; k < hitsBody.length; k++) {
			Document dd = searcherBody.doc(hitsBody[k].doc);
			System.out.println(dd.get("title"));
		}
		
		QueryParser parserTitle = new QueryParser("title", analyzer);
		Query queryTitle = parserTitle.parse(QueryParser.escape(question));
		TopDocs docsTitle = searcherBody.search(queryTitle, this.topNArticle);
		ScoreDoc[] hitsTitle = docsTitle.scoreDocs;
		System.out.println("* Found " + hitsTitle.length + " articles based on title.");
		for (int k = 0; k < hitsTitle.length; k++) {
			Document dd = searcherTitle.doc(hitsTitle[k].doc);
			System.out.println(dd.get("title"));
		}
        
		List<List<String>> finalSentences = new ArrayList<List<String>>();
		Document[] hits = mergeResults(hitsTitle, hitsBody);
//		ScoreDoc[] hits = hitsTitle;
		System.out.println("* Merged to " + hits.length + " articles.");
		for (int i = 0; i < hits.length; i++) {
//			int docId = hits[i].doc;
//			Document d = searcherTitle.doc(docId);
			Document d = hits[i];
			result.add(d);
			
//			String articleId = d.get("id");
			String title = d.get("title");
//			String text = d.get("text");
//			double score = hits[i].score;
//			double score = 0.0;
			
//			System.out.println((i + 1) + ". ("+docId+") title: " + title + ", score: " + score);
			System.out.println((i + 1) + ". " + title);
			
			/*
			
			
//			/* Sentence querying, commented it out for metrics
			QueryParser parserSent = new QueryParser("sentence", analyzer);
			
			// Distance query to determine avg. context_a and context_b
			String dq = "articleID:" + articleId + " AND (";
			for (String answer : goldAnswers.split("@@@")) {
				dq += "sentence:\"" + QueryParser.escape(answer) + "\" OR ";
			}
			dq = dq.substring(0, dq.length() - 4) + ")";
			
			TopScoreDocCollector collectorDist = TopScoreDocCollector.create(10, true); // TODO: how many should I return?
	    		searcherDist.search(parserSent.parse(dq), collectorDist);
	    		ScoreDoc[] hitsDist = collectorDist.topDocs().scoreDocs;
			System.out.println("found " + hitsDist.length + " sentences containing an answer");
//			for (int k = 0; k < hitsDist.length; k++) {
//				Document dd = searcherDist.doc(hitsDist[k].doc);
//				System.out.println(dd.get("sentID") + " : " + dd.get("sentence"));
//			}
			if (hitsDist.length == 0)
				badSearchCount++;
			
			// Sentence Index Query #1 - retrieve most relevant sentences in top articles given the (same) question			
			BooleanQuery bq1 = new BooleanQuery();
			bq1.add(parserSent.parse("articleID:"+articleId), BooleanClause.Occur.MUST);
			bq1.add(parserSent.parse(QueryParser.escape(question)), BooleanClause.Occur.MUST);
//        		String qs1 = "articleID:"+articleId+" AND sentence:(" + QueryParser.escape(question) + ")";
//        		Query querySent1 = parserSent.parse(qs1);
        		
        		TopScoreDocCollector collectorSent1 = TopScoreDocCollector.create(this.topNSent, true);
        		searcherSent1.search(bq1, collectorSent1);
        		ScoreDoc[] hitsSent1 = collectorSent1.topDocs().scoreDocs;
        		System.out.println("found " + hitsSent1.length + " sentences related to the question");
//        		for (int j = 0; j < hitsSent1.length; j++) {
//    				Document ds1 = searcherSent1.doc(hitsSent1[j].doc);
//    				System.out.println(ds1.get("sentID") + " : " + ds1.get("sentence"));
//    			}
        		
        		// Calculate avg. context size
        		List<Integer> curMinAs = new ArrayList<Integer>();
        		List<Integer> curMinBs = new ArrayList<Integer>();
        		for (int j = 0; j < hitsSent1.length; j++) {
        			Document ds1 = searcherSent1.doc(hitsSent1[j].doc);
        			int curID = Integer.parseInt(ds1.get("sentID"));
        			List<Integer> aList = new ArrayList<Integer>();
        			List<Integer> bList = new ArrayList<Integer>();
        			for (int k = 0; k < hitsDist.length; k++) {
        				Document dd = searcherDist.doc(hitsDist[k].doc);
        				int sID = Integer.parseInt(dd.get("sentID"));
        				if (curID > sID) {
        					aList.add(curID - sID);
        				} else if (curID < sID) {
        					bList.add(sID - curID);
        				} else { // hit sentence contained a gold answer
        					aList.add(0);
        					bList.add(0);
        				}
        			}
        			
        			if (!aList.isEmpty()) {
        				int minA = Collections.min(aList);
        				minAs.add(minA);
        				curMinAs.add(minA);
        			}
        			
        			if (!bList.isEmpty()) {
        				int minB = Collections.min(bList);
        				minBs.add(minB);
        				curMinBs.add(minB);
        			}

        		}
        		
        		double rrA = -1;
        		double rrB = -1;
        		if (!curMinAs.isEmpty()) {
        			rrA = Metrics.getRR(Collections.min(curMinAs), curMinAs);
        			rrAs.add(rrA);
        		
        		}
        		if (!curMinBs.isEmpty()) {
        			rrB = Metrics.getRR(Collections.min(curMinBs), curMinBs);
        			rrBs.add(rrB);
        		}
        		
        		System.out.println("A Reciprocal Rank: " + rrA + ", B Reciprocal Rank: " + rrB);
//        		Collections.sort(curMinAs);
        		
        		/*
    			for (int j = 0; j < hitsSent1.length; j++) {
    				Document ds1 = searcherSent1.doc(hitsSent1[j].doc);
//	    				String curSentence = ds1.get("sentence");
    				int sentId = Integer.parseInt(ds1.get("sentID"));
//	    				System.out.println("--> " + sentId + ": " + curSentence);
    				
    				// Sentence Index Query #2 - retrieve context of top sentences i.e. prior a sentences + subsequent b sentences
    				Query articleQuery = parserSent.parse("articleID:"+articleId);
    				Query contextQuery = NumericRangeQuery.newIntRange("sentID", sentId - context_a, sentId + context_b, true, true);
    				
    				BooleanQuery bq = new BooleanQuery();
    				bq.add(articleQuery, BooleanClause.Occur.MUST);
    				bq.add(contextQuery, BooleanClause.Occur.MUST);
    				
	        		TopScoreDocCollector collectorSent2 = TopScoreDocCollector.create(context_a + context_b + 1, true);
	        		searcherSent2.search(bq, collectorSent2);
	        		ScoreDoc[] hitsSent2 = collectorSent2.topDocs().scoreDocs;
	        		
	        		List<String> sents = new ArrayList<String>();
	    			for (int k = 0; k < hitsSent2.length; k++) { 
	    				Document ds2 = searcherSent2.doc(hitsSent2[k].doc);
	    				sents.add(ds2.get("sentence"));
//	    				System.out.println(ds2.get("articleID") + ", " + ds2.get("sentID") + ": " + ds2.get("sentence"));
	    			}
//	    			System.out.println("------");
	    			finalSentences.add(sents);
			}
			*/
		}
		
		finalResults.put(id, finalSentences);
		
		if (calculateMetrics) {
			if (dataset.equals("squad")) {
				for (int i = 0; i < Math.min(10, result.size()); i++) {
//					String normalized_text = Normalizer.normalize(result.get(i).get("text"), Normalizer.Form.NFD);
					if (stringContainsItemFromList(result.get(i).get("text"), goldAnswers.split("@@@"))) {
//						if (i <= 4) {
//							p5AnswerCount += 1.0;
//							p10AnswerCount += 1.0;
//							p20AnswerCount += 1.0;
//							p50AnswerCount += 1.0;
//						} else if (i <= 9) {
//							p10AnswerCount += 1.0;
//							p20AnswerCount += 1.0;
//							p50AnswerCount += 1.0;
//						} else if (i <= 19) {
//							p20AnswerCount += 1.0;
//							p50AnswerCount += 1.0;
//						} else if (i <= 49) {
//							p50AnswerCount += 1.0;
//						}
						if (i <= 0) {
							p1AnswerCount += 1.0;
							p3AnswerCount += 1.0;
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 2) {
							p3AnswerCount += 1.0;
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 4) {
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 9) {
							p10AnswerCount += 1.0;
						}
						
						break;
					}
				}
			} else if (dataset.equals("quasart")) {
				for (int i = 0; i < Math.min(10, result.size()); i++) {
					if (result.get(i).get("text").contains(goldAnswers)) {
						if (i <= 0) {
							p1AnswerCount += 1.0;
							p3AnswerCount += 1.0;
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 2) {
							p3AnswerCount += 1.0;
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 4) {
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 9) {
							p10AnswerCount += 1.0;
						}
						
						break;
					}
				}
			} else if (dataset.equals("triviaqa")) {
				for (int i = 0; i < Math.min(10, result.size()); i++) {
//					String normalized_text = Normalizer.normalize(result.get(i).get("text"), Normalizer.Form.NFD);
					if (stringContainsItemFromList(result.get(i).get("text").toLowerCase(), goldAnswers.toLowerCase().split("@@@"))) {
						if (i <= 0) {
							p1AnswerCount += 1.0;
							p3AnswerCount += 1.0;
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 2) {
							p3AnswerCount += 1.0;
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 4) {
							p5AnswerCount += 1.0;
							p10AnswerCount += 1.0;
						} else if (i <= 9) {
							p10AnswerCount += 1.0;
						}
						
						break;
					}
				}
			}
			
		}
		
		
		
//		System.out.println(finalSentences);
		/*
		// For Squad
		for (int i = 0; i < Math.min(50, result.size()); i++) {
//			String normalized_text = Normalizer.normalize(result.get(i).get("text"), Normalizer.Form.NFD);
			if (stringContainsItemFromList(result.get(i).get("text"), goldAnswers.split("@@@"))) {
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
		*/
		totalNumQuery++;
		System.out.println("------------------");
		List<String> pgs = new ArrayList<String>();
		return (ArrayList<String>) pgs;
		
	}
	
	public void questionToRelevantSents(String inputPath) throws Exception {
		
		if (dataset.equals("triviaqa")) {
			String raw = new String(Files.readAllBytes(Paths.get(inputPath)));
			Gson gson = new Gson();
			TriviaQueryList tql = gson.fromJson(raw, TriviaQueryList.class);
			
			for (peterkim.wikilucene.TriviaQueryList.TriviaQuery tq : tql.Data) {
				processQuestion(tq.QuestionId + "|" + tq.Question + "|" + tq.Answer.getAnswers());
			}
			return;
		}
		
		
		BufferedReader inputReader = null;
		try {
			
			inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));
			
			String q;
			while ((q = inputReader.readLine()) != null) {
				processQuestion(q);
			}
			
		} finally {
			if (luceneReader != null)
				luceneReader.close();
			
			if (luceneReaderSent != null)
				luceneReaderSent.close();
			
			if (inputReader != null)
				inputReader.close();
		}
		
	}

	public HashMap<String, List<List<String>>> questionToRelevantSents(List<String> queries) throws Exception {
		try {
			for (String q : queries) {
				processQuestion(q);
			}
		} finally {
			// Can't close Indexreaders here because there are multiple calls to questionToRelevantSents
//			if (luceneReader != null)
//				luceneReader.close();
//			
//			if (luceneReaderSent != null)
//				luceneReaderSent.close();
		}
		return finalResults;
	}

	public void showAvgContextDistance() {
		Collections.sort(minAs);
		Collections.sort(minBs);
		
		if (minAs.size() % 2 == 0) {
			System.out.println("median a = " + (minAs.get(minAs.size()/2) + minAs.get(minAs.size()/2 - 1)) / 2.0);
		} else {
			System.out.println("median a = " + minAs.get(minAs.size()/2));
		}
		
		if (minBs.size() % 2 == 0) {
			System.out.println("median b = " + (minBs.get(minBs.size()/2) + minBs.get(minBs.size()/2 - 1)) / 2.0);
		} else {
			System.out.println("median b = " + minBs.get(minBs.size()/2));
		}
		
		System.out.println("min max a = " + minAs.get(0) + ", " + minAs.get(minAs.size()-1));
		System.out.println("min max b = " + minBs.get(0) + ", " + minBs.get(minBs.size()-1));
		
		int aSum = 0;
		for (int a : minAs) {
			aSum += a;
		}
		
		int bSum = 0;
		for (int b : minBs) {
			bSum += b;
		}
		
		System.out.println("avg a = " + (aSum / (double) minAs.size()));
		System.out.println("avg b = " + (bSum / (double) minBs.size()));
		
//		System.out.println(minAs);
//		System.out.println(minBs);
		double totalrrA = 0.0;
		double totalrrB = 0.0;
		for (double ra : rrAs)
			totalrrA += ra;
		for (double rb : rrBs)
			totalrrB += rb;
		
		System.out.println("MRR A = " + (totalrrA / rrAs.size()));
		System.out.println("MRR B = " + (totalrrB / rrBs.size()));
		
		System.out.println(badSearchCount + " articles had 0 sentences containing an answer!");
	}
	
	public void showPatK() {
//		System.out.println("BM25: % of questions for which the answer paragraph appears in top 5 articles = " + (p1ParaCount / totalNumQuery));
//		System.out.println("Use BM25 is " + this.useBM25);
//		System.out.println("% of questions for which the gold article appears in top 5 articles = " + (p5ArticleCount / totalNumQuery));
//		System.out.println("% of questions for which the gold article appears in top 10 articles = " + (p10ArticleCount / totalNumQuery));
//		System.out.println("% of questions for which the gold article appears in top 20 articles = " + (p20ArticleCount / totalNumQuery));
//		System.out.println("% of questions for which the gold article appears in top 50 articles = " + (p50ArticleCount / totalNumQuery));
		System.out.println("p@1 = " + (p1AnswerCount / totalNumQuery));
		System.out.println("p@3 = " + (p3AnswerCount / totalNumQuery));
		System.out.println("p@5 = " + (p5AnswerCount / totalNumQuery));
		System.out.println("p@10 = " + (p10AnswerCount / totalNumQuery));
//		System.out.println("TFIDF: % of questions for which the answer paragraph appears = " + (p1Count / totalNumQuery));
//		System.out.println("Avg. NDCG for " + totalNumQuery + " queries = " + (totalNDCG / totalNumQuery));
//		System.out.println("Article MRR for " + totalNumQuery + " queries = " + (totalRR / totalNumQuery));
//		System.out.println("Article Min RR = " + Collections.min(reciRanks) + ", Max RR = " + Collections.max(reciRanks));
//		System.out.println("Article Bad Search Count = " + badSearchCount);
//		System.out.println("Para MRR for " + totalNumQuery + " queries = " + (totalParaRR / totalNumQuery));
//		System.out.println("Para Bad Search Count = " + badParaSearchCount);
	}
	
	public static void main(String[] args) throws Exception {
		String dataset = args[0];
		String inputPath = args[1];
		String luceneFolderPath = args[2];
		String luceneSentFolderPath = args[3];
		int topNArticle = Integer.parseInt(args[4]);
		int topNSent = Integer.parseInt(args[5]);
		String articleSimilarity = args[6];
		String sentSimilarity = args[7];
		int context_a = Integer.parseInt(args[8]);
		int context_b = Integer.parseInt(args[9]);
		boolean calculateMetrics = args[10].equals("t");
		
//		String dataset = "squad";
//		String inputPath = "/Users/Peter/Documents/wikiluceneinput/input.txt";
//		String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindex";
//		String luceneSentFolderPath = "/Users/Peter/Documents/wikiluceneindexsent";
//		int topNArticle = 10;
//		int topNSent = 5;
//		String articleSimilarity = "tf25";
//		String sentSimilarity = "simple";
//		int context_a = 2;
//		int context_b = 1;
//		boolean calculateMetrics = true;
		
//		Question worker = new Question(luceneFolderPath, luceneSentFolderPath, topN, useBM25, showRankResults, threshold);
		Question worker = new Question(dataset, luceneFolderPath, luceneSentFolderPath, topNArticle, topNSent, articleSimilarity, sentSimilarity, context_a, context_b, calculateMetrics);
		worker.questionToRelevantSents(inputPath);
		if (calculateMetrics)
			worker.showPatK();
//		worker.showAvgContextDistance();
		
	}

}

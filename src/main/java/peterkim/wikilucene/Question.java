package peterkim.wikilucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;

public class Question {
	
	private HashMap<String, IndexReader> articleReader = new HashMap<String, IndexReader>();
	private HashMap<String, IndexReader> sentenceReader = new HashMap<String, IndexReader>();
	
	private Analyzer analyzer;
		
	public Question() {
		analyzer = MyAnalyzer.getAnalyzer();
		
		// Start all dataset specific indexes
		String[] datasets = {"squad", "triviaqa", "searchqa", "quasart"};
		for (String ds : datasets) {
			try {
				articleReader.put(ds, DirectoryReader.open(FSDirectory.open(new File("/if5/wua4nw/open_domain_qa/data/" + ds + "_index_article"))));
				sentenceReader.put(ds, DirectoryReader.open(FSDirectory.open(new File("/if5/wua4nw/open_domain_qa/data/" + ds + "_index_sentence"))));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// quasart_short article index TODO: will most likely change later
		try {
			articleReader.put("quasart_short", DirectoryReader.open(FSDirectory.open(new File("/if5/wua4nw/open_domain_qa/data/quasartshort_index_article"))));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setSimilarities(String articleBodySim, String sentSim, IndexSearcher searcherBody, IndexSearcher searcherSent1) {
		switch (articleBodySim) {
		case "simple":
			searcherBody.setSimilarity(new MySentenceSimilarity());
			break;
		case "simplelen":
			searcherBody.setSimilarity(new MySentenceLengthSimilarity());
			break;
		case "tf25":
			searcherBody.setSimilarity(new MyTFIDFSimilarity());
			break;
		case "tfidf":
			searcherBody.setSimilarity(new DefaultSimilarity());
			break;
		case "bm25":
			searcherBody.setSimilarity(new BM25Similarity());
			break;
		default: // tfidf
			searcherBody.setSimilarity(new DefaultSimilarity());
			break;
		}
		
		switch (sentSim) {
		case "simple":
			searcherSent1.setSimilarity(new MySentenceSimilarity());
			break;
		case "simplelen":
			searcherSent1.setSimilarity(new MySentenceLengthSimilarity());
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
	
	
	private List<String> processQuestionForQuasartShort(String question, String dataset, int topNArticle, String articleSim, int topNSentence, String sentSim, int a, int b) throws IOException, ParseException, InvalidTokenOffsetsException {
		
		IndexSearcher searcherBody = new IndexSearcher(articleReader.get("quasart_short"));
		IndexSearcher searcherSent1 = new IndexSearcher(sentenceReader.get(dataset));
		
		setSimilarities(articleSim, sentSim, searcherBody, searcherSent1);
				
		QueryParser parserBody = new QueryParser("text", analyzer);
		Query queryBody = parserBody.parse(QueryParser.escape(question));		
		TopDocs docsBody = searcherBody.search(queryBody, topNArticle);
		ScoreDoc[] hitsBody = docsBody.scoreDocs;

		List<String> r = new ArrayList<String>();
		for (int k = 0; k < hitsBody.length; k++) {
			Document dd = searcherBody.doc(hitsBody[k].doc);
			r.add(dd.get("text"));
		}
		
		return r;
	}
	
	private List<List<String>> processQuestion(String question, String dataset, int topNArticle, String articleSim, int topNSentence, String sentSim, int a, int b) throws IOException, ParseException, InvalidTokenOffsetsException {

		IndexSearcher searcherBody = new IndexSearcher(articleReader.get(dataset));
		IndexSearcher searcherSent1 = new IndexSearcher(sentenceReader.get(dataset));
		IndexSearcher searcherSent2 = new IndexSearcher(sentenceReader.get(dataset));
		
		setSimilarities(articleSim, sentSim, searcherBody, searcherSent1);
		
		QueryParser parserBody = new QueryParser("text", analyzer);
		Query queryBody = parserBody.parse(QueryParser.escape(question));		
		TopDocs docsBody = searcherBody.search(queryBody, topNArticle);
		ScoreDoc[] hitsBody = docsBody.scoreDocs;
		
		BooleanQuery allbq = new BooleanQuery();
		allbq.setMinimumNumberShouldMatch(1);
		QueryParser parserSent = new QueryParser("sentence", analyzer);
		allbq.add(parserSent.parse(QueryParser.escape(question)), BooleanClause.Occur.MUST);
		
		for (int i = 0; i < hitsBody.length; i++) {
			int docId = hitsBody[i].doc;
			Document d = searcherBody.doc(docId);
	
			String articleId = d.get("id");			
			allbq.add(new TermQuery(new Term("articleID", articleId)), BooleanClause.Occur.SHOULD);
		}
		
		TopScoreDocCollector collectorSent1 = TopScoreDocCollector.create(topNSentence, true);
		searcherSent1.search(allbq, collectorSent1);
		ScoreDoc[] hitsSent1 = collectorSent1.topDocs().scoreDocs;
		
		List<List<String>> finalSentences = new ArrayList<List<String>>();
		for (int j = 0; j < hitsSent1.length; j++) {
			Document ds1 = searcherSent1.doc((hitsSent1[j].doc));
			int sentId = Integer.parseInt(ds1.get("sentID"));
			String articleId = ds1.get("articleID");
    			
			// Sentence Index Query #2 - retrieve context of top sentences i.e. prior a sentences + subsequent b sentences
			Query articleQuery = parserSent.parse("articleID:"+articleId);
			Query contextQuery = NumericRangeQuery.newIntRange("sentID", sentId - a, sentId + b, true, true);
			
			BooleanQuery bq = new BooleanQuery();
			bq.add(articleQuery, BooleanClause.Occur.MUST);
			bq.add(contextQuery, BooleanClause.Occur.MUST);
			
        		TopScoreDocCollector collectorSent2 = TopScoreDocCollector.create(a + b + 1, true);
        		searcherSent2.search(bq, collectorSent2);
        		ScoreDoc[] hitsSent2 = collectorSent2.topDocs().scoreDocs;
        		
        		List<String> sents = new ArrayList<String>();
    			for (int k = 0; k < hitsSent2.length; k++) { 
    				Document ds2 = searcherSent2.doc(hitsSent2[k].doc);
    				sents.add(ds2.get("sentence"));
    			}
    			finalSentences.add(sents);
		}
		return finalSentences;
	}
	
	public void questionToRelevantSents(String inputPath) throws Exception {
		BufferedReader inputReader = null;

		try {
			inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));
			
			String q;
			while ((q = inputReader.readLine()) != null) {
				StringTokenizer tk = new StringTokenizer(q, "|");
				String id = tk.nextToken();
				String question = tk.nextToken();
				String dataset = tk.nextToken();
				int topNArticle = Integer.parseInt(tk.nextToken());
				String articleSimilarity = tk.nextToken();
				int topNSentence = Integer.parseInt(tk.nextToken());
				String sentSimilarity = tk.nextToken();
				int a = Integer.parseInt(tk.nextToken());
				int b = Integer.parseInt(tk.nextToken());
				
				System.out.println(processQuestion(question, dataset, topNArticle, articleSimilarity, topNSentence, sentSimilarity, a, b));
			}
			
		} finally {
			if (inputReader != null)
				inputReader.close();
		}
		
	}
	
	
	public HashMap<String, List<String>> questionToRelevantSentsForQuasartShort(List<String> queries) throws Exception {
		HashMap<String, List<String>> finalResultsForQuasart = new HashMap<String, List<String>>();
		for (String q : queries) {
			StringTokenizer tk = new StringTokenizer(q, "|");
			String id = tk.nextToken();
			String question = tk.nextToken();
			String dataset = tk.nextToken();
			int topNArticle = Integer.parseInt(tk.nextToken());
			String articleSimilarity = tk.nextToken();
			int topNSentence = Integer.parseInt(tk.nextToken());
			String sentSimilarity = tk.nextToken();
			int a = Integer.parseInt(tk.nextToken());
			int b = Integer.parseInt(tk.nextToken());
			
			finalResultsForQuasart.put(id, processQuestionForQuasartShort(question, dataset, topNArticle, articleSimilarity, topNSentence, sentSimilarity, a, b));
		}
		return finalResultsForQuasart;
	}

	
	public HashMap<String, List<List<String>>> questionToRelevantSents(List<String> queries) throws Exception {
		HashMap<String, List<List<String>>> finalResults = new HashMap<String, List<List<String>>>();
		for (String q : queries) {
			StringTokenizer tk = new StringTokenizer(q, "|");
			String id = tk.nextToken();
			String question = tk.nextToken();
			String dataset = tk.nextToken();
			int topNArticle = Integer.parseInt(tk.nextToken());
			String articleSimilarity = tk.nextToken();
			int topNSentence = Integer.parseInt(tk.nextToken());
			String sentSimilarity = tk.nextToken();
			int a = Integer.parseInt(tk.nextToken());
			int b = Integer.parseInt(tk.nextToken());
			
			finalResults.put(id, processQuestion(question, dataset, topNArticle, articleSimilarity, topNSentence, sentSimilarity, a, b));
		}
		return finalResults;
	}

	
	public static void main(String[] args) throws Exception {
		String inputPath = args[0];		
		Question worker = new Question();
		worker.questionToRelevantSents(inputPath);
	}

}

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
import java.util.Arrays;
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
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Question {

	private String inputPath;
	private String outputPath;

	private Analyzer analyzer;
	private IndexSearcher searcher;
	private IndexReader luceneReader;

	public Question(String luceneFolderPath, String inputPath, String outputPath) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;

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

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Document> questionToRelevantDocs(int topN) throws Exception {
		BufferedReader inputReader = null;
		BufferedWriter outputWriter = null;
		
		List<Document> result = null;
		
		try {
			searcher = new IndexSearcher(luceneReader);
			searcher.setSimilarity(new BM25Similarity());
			inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));

			File outputFileTest = new File(outputPath);
			outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath, false)));
			outputWriter.write("id|docs");
			outputWriter.newLine();
			outputWriter.flush();

			String aLine;
			inputReader.readLine(); // skip first line

			while ((aLine = inputReader.readLine()) != null) {
				StringTokenizer tk = new StringTokenizer(aLine, "|");
				String id = tk.nextToken();
				String question = escapeSymbols(tk.nextToken());

				System.out.println("Query id=" + id + " => " + question);

				String qstring = createQueryString(question);
				QueryParser parser = new QueryParser("text", analyzer);
				Query query = parser.parse("title:(" + qstring + ") OR text:(" + qstring + ")");
				
				TopScoreDocCollector collector = TopScoreDocCollector.create(topN, true);
				searcher.search(query, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;

				System.out.println("Found " + hits.length + " articles");
				for (int i = 0; i < hits.length; i++) {
					int docId = hits[i].doc;
					Document d = searcher.doc(docId);
					String title = d.get("title");
					String text = d.get("text");
					double score = hits[i].score;
					System.out.println((i + 1) + ". ("+docId+") title: " + title + ", score: " + score);
					result.add(d);
//					System.out.println(text);
				}
				System.out.print("\n\n");
			}
		} finally {
			if (luceneReader != null)
				luceneReader.close();

			if (inputReader != null)
				inputReader.close();

			if (outputWriter != null) {
				outputWriter.flush();
				outputWriter.close();
			}
		}
		return result;
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

//		String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindex";
//		String inputPath = "/Users/Peter/Documents/wikiluceneinput/input.txt";
//		String outputPath = "/Users/Peter/Documents/wikiluceneoutput/output.txt";
//		
//		int topN = 2;
//
//		Question worker = new Question(luceneFolderPath, inputPath, outputPath);
		
//		NDCG.calculateNDCG(worker.questionToRelevantDocs(topN));
		List<String> gold = Arrays.asList("Warsaw");
		List<String> res1 = Arrays.asList("Warsaw","Poland","Germany");
		List<String> res2 = Arrays.asList("Warsaw");
//		System.out.println(Metrics.calculateNDCG(gold, res1));
	}

}

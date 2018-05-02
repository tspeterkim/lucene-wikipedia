package peterkim.wikilucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.google.gson.Gson;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
//import org.apache.lucene.analysis.ngram.;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

public class Index {
	private String dataset;
	private int quasartIndex = 0;
	private int triviaIndex = 0;
	private int searchqaIndex = 0;
	private IndexWriter indexWriter = null;
	private Gson gson = null;
	private HashMap<String, Boolean> targetMap = new HashMap<String, Boolean>();
	private int totalIndexedItems = 0;

	public Index(String luceneFolderPath, String filterPath, String dataset) throws IOException {
		this.dataset = dataset;
		Directory indexDir = FSDirectory.open(new File(luceneFolderPath));		
		indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(Version.LATEST, MyAnalyzer.getAnalyzer()));
		
		gson = new Gson();
		
		// Initialize filter titles
		BufferedReader inputReader = new BufferedReader(new FileReader(new File(filterPath)));
		
		// Build target article map
		String aLine;
		while ((aLine = inputReader.readLine()) != null) {
			targetMap.put(aLine, false);
		}
		
		if (!targetMap.isEmpty())
			System.out.println("Filtering articles...");
		
		inputReader.close();

	}
	
	public List<String> splitSentenceStanford(String text) {
		List<String> r = new ArrayList<String>();
		DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(text));
		for (List<HasWord> sentence : dp) {
			String s = "";
			for (HasWord hw : sentence) {
				s += hw.toString() + " ";
			}
			r.add(s.substring(0,s.length()-1));
		}
		return r;
	}
	
	public List<String> splitSentence(String text) throws FileNotFoundException {
		InputStream modelIn = new FileInputStream("/if5/wua4nw/open_domain_qa/data/en-sent.bin"); // switch to this on server
//		InputStream modelIn = new FileInputStream("/Users/Peter/Downloads/en-sent.bin");
		SentenceModel model = null;
		try {
			model = new SentenceModel(modelIn);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {}
			}
		}
		SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
		String sentences[] = sentenceDetector.sentDetect(text);
		List<String> sents = Arrays.asList(sentences);
		return sents;
	}
	
	public void indexSentence(int articleId, int sentId, String sent) throws IOException {
		System.out.println("Indexing ID: " + articleId + ", Sentence #: " + sentId);
		System.out.println(sent);
		System.out.println("======");
		Document doc = new Document();
		doc.add(new StringField("articleID", Integer.toString(articleId), Field.Store.YES));
		doc.add(new IntField("sentID", sentId, Field.Store.YES));
		doc.add(new TextField("sentence", sent, Field.Store.YES));
		indexWriter.addDocument(doc);
	}
	
	public void processSentence(String filePath) throws IOException {
		if (dataset.equals("triviaqa")) {
			String text = new String(Files.readAllBytes(Paths.get(filePath)));
			int i = 0;
			for (String sent : splitSentence(text)) {
				indexSentence(triviaIndex, i, sent);
				i++;
			}
			totalIndexedItems++;
			System.out.println("Indexed " + triviaIndex + ". " + totalIndexedItems + " items indexed.");
			triviaIndex++;
			return;
		}
		
		BufferedReader inputReader = new BufferedReader(new FileReader(new File(filePath)));
		String aLine;
		while ((aLine = inputReader.readLine()) != null) {
			List<String> sents = null;
			int id = -1;
			String title = "na";
			if (dataset.equals("squad")) { // Wikipedia
				WikiArticle article = gson.fromJson(aLine, WikiArticle.class);
//				sents = article.splitSentence();
				sents = splitSentenceStanford(article.text);
				id = article.id;
				title = article.title;
			} else if (dataset.equals("quasart")) {
				QuasartArticle qart = gson.fromJson(aLine, QuasartArticle.class);
				sents = qart.getSentences();
				id = quasartIndex;
				title = Integer.toString(quasartIndex);
				quasartIndex++;
			} else if (dataset.equals("searchqa")) {
				StringTokenizer tk = new StringTokenizer(aLine, "|||");
				String rawtext = tk.nextToken();
				String question = tk.nextToken(); // No need for this here
				String answer = tk.nextToken();
				id = searchqaIndex;
				title = answer;
				sents = getSearchQAsents(rawtext);
				searchqaIndex++;
			}
			
			if (targetMap.isEmpty()) { // no filtering i.e. index everything
				int i = 0;
				for (String sent : sents) {
					indexSentence(id, i, sent);
					i++;
				}
				totalIndexedItems++;
				System.out.println("Indexed " + id + ". " + totalIndexedItems + " items indexed.");
				continue;
			}
			
			if (targetMap.containsKey(title)) {
				int i = 0;
				for (String sent : sents) {
					indexSentence(id, i, sent);
					i++;
				}
				targetMap.put(title, true); // indicate article has been indexed
			}
		}
		
		inputReader.close();
	}
	
//	public void indexParagraph(int id, String title, String text) throws IOException {
//		System.out.println("Indexing " + title + " (text length: " + text.length() + ") ...");
//		
//		Document doc = new Document();
//		doc.add(new IntField("id", id, Field.Store.YES));
//		doc.add(new TextField("title", title, Field.Store.YES));
//		doc.add(new TextField("paragraph", text, Field.Store.YES));
//		indexWriter.addDocument(doc);
//	}
//	
//	
//	public void processParagraph(String filePath) throws IOException {
//		BufferedReader inputReader = new BufferedReader(new FileReader(new File(filePath)));
//		
//		String aLine;
//		while ((aLine = inputReader.readLine()) != null) {
//			WikiArticle article = gson.fromJson(aLine, WikiArticle.class);
//			if (targetMap.isEmpty()) { // no filtering i.e. index everything
//				for (String pg : article.splitParagraph()) {
////					System.out.println(pg);
//					indexParagraph(article.id, article.title, pg);
//				}
//				continue;
//			}
//			
//			if (targetMap.containsKey(article.title)) {
//				for (String pg : article.splitParagraph()) {
////					System.out.println(pg);
//					indexParagraph(article.id, article.title, pg);
//				}
////				indexArticle(article.id, article.title, article.text);
//				targetMap.put(article.title, true); // indicate article has been indexed
//			}
//		}
//		
//		inputReader.close();
//	}
	
	private String getSearchQAtext(String raw) {
		String fullText = "";
		org.jsoup.nodes.Document document = Jsoup.parse(raw);
		Elements elements = document.getAllElements();
		for (Element element : elements) {
			Tag tag = element.tag();
			if (tag.getName().equalsIgnoreCase("s")) {
//		        System.out.println(element.text());
				fullText += element.text() + ". ";
			}
		}
		return fullText;
	}
	
	private List<String> getSearchQAsents(String raw) {
		List<String> r = new ArrayList<String>();
		org.jsoup.nodes.Document document = Jsoup.parse(raw);
		Elements elements = document.getAllElements();
		for (Element element : elements) {
		    Tag tag = element.tag();
		    if (tag.getName().equalsIgnoreCase("s")) {
		    		r.add(element.text());
		    }
		}
		return r;
	}
	
	public void indexArticle(int id, String title, String text) throws IOException {
		System.out.println("Indexing " + title + " (text length: " + text.length() + ") ...");
//		System.out.println(text);
		totalIndexedItems++;
		Document doc = new Document();
		doc.add(new IntField("id", id, Field.Store.YES));
		doc.add(new TextField("title", title, Field.Store.YES));
		doc.add(new TextField("text", text, Field.Store.YES));
		indexWriter.addDocument(doc);
	}
	
	public void processArticle(String filePath) throws IOException {
		if (dataset.equals("triviaqa")) {
			File f = new File(filePath);
			int flen = f.getName().length();
			String title = f.getName().substring(0, flen-4); // remove .txt from filename
			String text = new String(Files.readAllBytes(Paths.get(filePath)));
			indexArticle(triviaIndex, title, text);
			triviaIndex++;
			return;
		}
		
		BufferedReader inputReader = new BufferedReader(new FileReader(new File(filePath)));
		String aLine;
		while ((aLine = inputReader.readLine()) != null) {
			int id = -1;
			String title = "na";
			String text = "na";
			if (dataset.equals("squad")) { // Wikipedia
				WikiArticle wart = gson.fromJson(aLine, WikiArticle.class);
				id = wart.id;
				title = wart.title;
				text = wart.text;
			} else if (dataset.equals("quasart")) {
				QuasartArticle qart = gson.fromJson(aLine, QuasartArticle.class);
				for (List<String> qa : qart.contexts) {
					indexArticle(quasartIndex, Integer.toString(quasartIndex), qa.get(1));
					quasartIndex++;
				}
				continue;
//				id = quasartIndex;
//				title = Integer.toString(quasartIndex); // no title for quasart, so just use unique id
//				text = qart.getFullText();
				
			} else if (dataset.equals("searchqa")) {
				StringTokenizer tk = new StringTokenizer(aLine, "|||");
				String rawtext = tk.nextToken();
				String question = tk.nextToken(); // No need for this here
				String answer = tk.nextToken();
				id = searchqaIndex;
				title = answer;
				text = getSearchQAtext(rawtext);
//				System.out.println(text);
				searchqaIndex++;
			}
			
			if (targetMap.isEmpty()) { // no filtering i.e. index everything
				indexArticle(id, title, text); 
				continue;
			}
			
			if (targetMap.containsKey(title)) {
				indexArticle(id, title, text);
				targetMap.put(title, true); // indicate article has been indexed
			}
		}
		
		inputReader.close();
	}
	
	public void checkTargetFulfilled() {
		System.out.println("Indexed " + totalIndexedItems + " Items. Not indexed articles:");
		for (Map.Entry<String, Boolean> entry : targetMap.entrySet()) {
			if (!entry.getValue()) {
				System.out.println(entry.getKey());
			}
		}
	}
	

	public static void main(String[] args) throws IOException {
//		String filterPath = args[0];
//		String luceneFolderPath = args[1];
//		String extractedPath = args[2];
//		String dataset = args[3];
//		boolean indexSentence = (args[4].equals("t"));
		
		String filterPath = "/Users/Peter/Documents/input.txt";
		String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindexsent";
		String extractedPath = "/Users/Peter/Documents/wikiextractor/test/AA";
		String dataset = "squad"; // squad, quasart, triviaqa, searchqa
		boolean indexSentence = true;
		
		System.out.println("[ Indexing " + (indexSentence ? "sentences" : "articles") + " for " + dataset + " dataset ]");
		
		Index handler = new Index(luceneFolderPath, filterPath, dataset);
		Collection<File> extractedFiles = FileUtils.listFiles(new File(extractedPath), HiddenFileFilter.VISIBLE, TrueFileFilter.INSTANCE);
		
		for (File f : extractedFiles) {
			if (!indexSentence)
				handler.processArticle(f.getCanonicalPath());
			else
				handler.processSentence(f.getCanonicalPath());
		}
		
		handler.indexWriter.close();
		
		System.out.println("----------------------------------------");
		System.out.println("Indexing Successful!");
		
		handler.checkTargetFulfilled();
		
	}

}

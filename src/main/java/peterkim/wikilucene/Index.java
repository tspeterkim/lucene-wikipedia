package peterkim.wikilucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

//import info.bliki.wiki.dump.IArticleFilter;
//import info.bliki.wiki.dump.Siteinfo;
//import info.bliki.wiki.dump.WikiArticle;
//import info.bliki.wiki.dump.WikiXMLParser;

//import org.xml.sax.XMLReader;
//import org.xml.sax.Attributes;
//import org.xml.sax.InputSource;
//import org.xml.sax.helpers.XMLReaderFactory;
//import org.xml.sax.helpers.DefaultHandler;

public class Index {

	public IndexWriter indexWriter = null;
	public Gson gson = null;
	public HashMap<String, Boolean> targetMap = new HashMap<String, Boolean>();;

	public Index(String luceneFolderPath, String filterPath) throws IOException {
		
		Directory indexDir = FSDirectory.open(new File(luceneFolderPath));
		Analyzer analyzer = new Analyzer() {
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
		
		indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(Version.LATEST, analyzer));
		
		gson = new Gson();
		
		// Initialize filter titles
		BufferedReader inputReader = new BufferedReader(new FileReader(new File(filterPath)));
		
		// Build target article map
		String aLine;
		while ((aLine = inputReader.readLine()) != null) {
			targetMap.put(aLine, false);
		}
		
		inputReader.close();

	}

	public void indexArticle(int id, String title, String text) throws IOException {
		System.out.println("Indexing " + title + " (text length: " + text.length() + ") ...");
		
		Document doc = new Document();
		doc.add(new IntField("id", id, Field.Store.YES));
		doc.add(new TextField("title", title, Field.Store.YES));
		doc.add(new TextField("text", text, Field.Store.YES));
		indexWriter.addDocument(doc);
	}
	
	public void process(String filePath) throws IOException {
		BufferedReader inputReader = new BufferedReader(new FileReader(new File(filePath)));
		
		String aLine;
		while ((aLine = inputReader.readLine()) != null) {
			WikiArticle article = gson.fromJson(aLine, WikiArticle.class);
			if (targetMap.containsKey(article.title)) {
				indexArticle(article.id, article.title, article.text);
				targetMap.put(article.title, true); // indicate article has been indexed
			}
		}
		
		inputReader.close();
	}
	
	public void checkTargetFulfilled() {
		System.out.println("Not indexed articles:");
		for (Map.Entry<String, Boolean> entry : targetMap.entrySet()) {
			if (!entry.getValue()) {
				System.out.println(entry.getKey());
			}
		}
	}
	
	

	public static void main(String[] args) throws IOException {
		// java -cp lucene-wikipedia-0.0.1-jar-with-dependencies.jar
		// markpeng.wiki.WikipediaToLuceneIndex
		// enwiki-latest-pages-articles.xml.bz2 "lucene-wiki-index-keywords" "keywords.txt"
		
//		if (args.length != 3) {
//			System.err.println("Usage: java -cp lucene-wikipedia-0.0.1-jar-with-dependencies.jar "
//					+ "markpeng.wiki.WikipediaToLuceneIndex <path of XML bz2 file> " + "<path of lucene index folder> "
//					+ "<keywords to filter>");
//			System.exit(-1);
//		}

//		String bz2FilePath = "/Users/Peter/Documents/test.xml.bz2";
		
		String filterPath = args[0];
//		String filterPath = "/if5/wua4nw/open_domain_qa/data/squad_articles.txt";
		String luceneFolderPath = args[1];
//		String luceneFolderPath = "/if5/wua4nw/open_domain_qa/data/wikiluceneindex";
		String extractedPath = args[2];
		Index handler = new Index(luceneFolderPath, filterPath);
		
		Collection<File> extractedFiles = FileUtils.listFiles(new File(extractedPath), HiddenFileFilter.VISIBLE, TrueFileFilter.INSTANCE);
//		Collection<File> extractedFiles = FileUtils.listFiles(new File("/Users/Peter/Documents/wikiextractor/test"), HiddenFileFilter.VISIBLE, TrueFileFilter.INSTANCE);
		
		for (File f : extractedFiles) {
			handler.process(f.getCanonicalPath());
		}
		
		handler.indexWriter.close();
		
		System.out.println("----------------------------------------");
		System.out.println("Indexing Successful!");
		
		handler.checkTargetFulfilled();
		 
		//String extractedFilePath = "/Users/Peter/Documents/wikiextractor/test/AA/wiki_00";
		//String luceneFolderPath = "/Users/Peter/Documents/wikiluceneindex";

		//Index handler = new Index(luceneFolderPath);
		//handler.process(extractedFilePath);
		//handler.indexWriter.close();
		
//		try {
//			XMLReader xr = XMLReaderFactory.createXMLReader();
//			MyHandler myhandler = new MyHandler();
//			xr.setContentHandler(myhandler);
//			xr.setErrorHandler(myhandler);
//			FileReader r = new FileReader(extractedFilePath);
//		    xr.parse(new InputSource(r));
//		} catch (SAXException e) {
//			e.printStackTrace();
//		}
//		try {
//			WikiXMLParser wxp = new WikiXMLParser(bz2FilePath, handler);
//			wxp.parse();
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			handler.indexWriter.close();
//			System.out.println("----------------------------------------");
//			System.out.println("Indexing Successful!");
//		}
	}

}

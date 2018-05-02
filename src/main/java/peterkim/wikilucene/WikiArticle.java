package peterkim.wikilucene;

import java.util.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class WikiArticle {
	public int revid;
	public int id;
	public String url;
	public String title;
	public String text;
	
	private boolean isNonParagraph(String s) {
		if (s.length() == 1 || !s.contains("."))
			return true;
		return false;
	}
	
	public List<String> splitParagraph() {
		List<String> pgs = new ArrayList<String>(Arrays.asList(text.split("\n\n")));
		
		Iterator<String> it = pgs.iterator();
		while (it.hasNext()) {
		    if (isNonParagraph(it.next())) {
		        it.remove();
		    }
		}
		
		return pgs;
	}
	
	public List<String> splitSentence() throws FileNotFoundException {
		InputStream modelIn = new FileInputStream("/if5/wua4nw/open_domain_qa/data/en-sent.bin");
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
}
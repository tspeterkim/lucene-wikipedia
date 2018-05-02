package peterkim.wikilucene;

import java.util.*;

public class QuasartArticle {
	
	public List<List<String>> contexts;
	
	public String getFullText() {
		String fullText = "";
		for (List<String> qs : contexts) {
			fullText += qs.get(1) + " ";
		}
		return fullText.substring(0, fullText.length()-1); // delete last " "
	}
	
	public List<String> getSentences() {
		List<String> r = new ArrayList<String>();
		for (List<String> qs : contexts) {
			r.add(qs.get(1));
		}
		return r;
	}
}

# Lucene Multi-Sentence Context Retriever

This Lucene-based index for QA datasets retrieves a multi-sentence context given a query. It is the primary component in our open-domain QA pipeline that acts as the retriever of relevant texts for a reader to process and output an answer to the respective query.

## Indexing

For each dataset, we create two indices: 1) an article-level index that has the fields (article ID, article title, article text), 2) a sentence-level index that has the fields (article ID, sentence ID, sentence text). This second sentence index is created by splitting the article text into sentences (using OpenNLP sentence splitter) and labeling each with an incrementing sentence ID.

### SQuAD (Open)

As SQuAD is based on selected wikipedia paragraphs, we turn this into an open-domain setting by creating a Lucene index on top of the entire 02-jan-18 wikipedia dump. We used [WikiExtractor](https://github.com/attardi/wikiextractor) to extract and clean texts from the dump, and finally turn them into json files. Then, we apply our Lucene code to create the indices.

### TriviaQA

The raw dataset is split into two sources: web and wikipedia. For our open-domain purposes, we combine the two when we create our indices. Every file in the raw dataset is a different article, and the title of the article is the filename for the wikipedia source.

### SearchQA

Each line in the raw dataset contains the text, question, answer delimited by "|||". The text is composed of markups as such: "<S> I am a sentence </S>". Our Lucene code extracts the texts within each sentence markup and concatenates all these sentences to create an article text.

### QuasarT

Currently, we have created two different sets of indices for QuasarT-Short and QuasarT-Long. The former only has sentence-level information in the raw dataset so we have only created an article-level index where each article is actually a single sentence. Thus, no sentence-level index exists for QuasarT-Short. On the other hand, the Long version has been dealt similarly as the previous datasets, where we obtained the raw text in a json format and split them into sentences. There is no title information for each article.

### Usage

To create an index, you can run the following command:

`java -cp lucene.jar peterkim.wikilucene.Index <raw data path> <dataset> <create sentence index>`

* <raw data path> - Path where the raw data is stored
* <dataset> - the dataset and respective index to query. Must be one of the following "squad", "quasart", "searchqa", "triviaqa"
* <create sentence index> - Boolean argument indicated by "t" or "f" to create a sentence-level or article-level index, respectively.

E.g. `java -cp lucene.jar peterkim.wikilucene.Index ./squad_extracted squad f` -> creates an article-level index for the SQuAD dataset at the path ./squad_index_article

## Querying

### Gateway Server Setup

To start the gateway server that provides the python interface to our Java-based Lucene component, run the following command:

`java -cp lucene.jar peterkim.wikilucene.EntryPoint`

We use Py4J to provide the bridge and further information on the library can be found [here](https://www.py4j.org/faq.html)

### Usage

After starting the server successfully, you are ready to start calling the Lucene component. Simply import "lucene2ranker.py" and call the function "processQueries(qlist)". qlist is a list of queries structured as such "question unique id|query|dataset|topNArticles|articleSimilarityFunction|topNSentences|sentenceSimilarityFunction|contextA|contextB". E.g."0|what is nlp?|squad|10|tf25|5|tfidf|2|1"

* question unique id - This must be a unique identifier for each query
* query - the query text
* dataset - the dataset and respective index to query. Must be one of the following "squad", "quasart", "searchqa", "triviaqa"
* topNArticles - Retrieve this many top articles...
* articleSimilarityFunction - ...using this similarity function
* topNSentences - Retrieve this many top hit sentences among all the top previous articles...
* sentenceSimilarityFunction - ...using this similarity function
* contextA - Retrieve this many previous sentences for each hit sentence
* contextB - Retrieve this many subsequent sentences for each hit sentence

Full details on the function can be found inside "lucene2ranker.py"

### Similarity Functions

The following is the list of similarity functions you can provide for "articleSimilarityFunction" and "sentenceSimilarityFunction"

* tfidf - Lucene's default TFIDF scoring method
* bm25 - Lucene's BM25 method
* tf25 - Combines TFIDF's TF and BM25's IDF i.e. tfidf score = log(tf + 1) * log((N - Nt + 0.5) / (Nt + 0.5)) where tf, N, Nt are term frequency in document, number of documents, number of occurrences of term in all documents, respectively. See implementation [here](https://github.com/peterkim95/lucene-wikipedia/blob/master/src/main/java/peterkim/wikilucene/MyTFIDFSimilarity.java#L10)
* simple - every overlapping term returns a score of 1
* simplelen - extension of simple method but the score is divided by the document length i.e. shorter length documents are preferred

### QuasarT-Short

If you want to query the QuasarT-Short index, you can call "processQueriesForQuasartShort(qlist)". The format of qlist is identical to above. However, this will return a list of relevant sentences, as opposed to a list of multi-sentence contexts. For example, the query "0|what is nlp?|squad|10|tf25|5|tfidf|2|1" will return the top 10 sentences using TF25.

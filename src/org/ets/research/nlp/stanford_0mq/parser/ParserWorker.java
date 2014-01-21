package org.ets.research.nlp.stanford_0mq.parser;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.zeromq.ZMsg;
import org.zeromq.zguide.chapter4.majordomo.mdwrkapi;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;

public class ParserWorker extends mdwrkapi
{
    private LexicalizedParser parser;
    private TreePrint treePrinter;
    private TreebankLanguagePack tlp;
	
	public ParserWorker(String broker, boolean verbose) 
	{
		super(broker, "ParserWorker", verbose);
		parser = LexicalizedParser.loadModel(DefaultPaths.DEFAULT_PARSER_MODEL, new String[]{});
        tlp = new PennTreebankLanguagePack();
        treePrinter = new TreePrint("oneline", "", tlp);
		
		ZMsg reply = null;
		while (true)
		{
			ZMsg request = super.receive(reply);
			String receivedText = request.popString();
			reply = request;
			List<String> parseTrees = parseText(receivedText);
			for (String tree : parseTrees)
			{
				reply.add(tree);
			}
			//request.destroy();
		}
	}

	private List<String> parseText(String text) 
	{
        List<String> results = new ArrayList<String>();
        
        // assume no tokenization was done; use Stanford's default tokenizer
        DocumentPreprocessor preprocess = new DocumentPreprocessor(new StringReader(text));
        Iterator<List<HasWord>> foundSentences = preprocess.iterator();
        while (foundSentences.hasNext())
        {
        	Tree parseTree = parser.apply(foundSentences.next());
        	results.add(TreeObjectToString(parseTree));//, parseTree.score()));
        }
        
        return results;
	}
	
	private String TreeObjectToString(Tree tree)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		treePrinter.printTree(tree, pw);
		return sw.getBuffer().toString().trim();
	}
}
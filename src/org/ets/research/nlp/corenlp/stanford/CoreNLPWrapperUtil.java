package org.ets.research.nlp.corenlp.stanford;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.ParserAnnotatorUtils;
import edu.stanford.nlp.trees.EnglishGrammaticalStructureFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class CoreNLPWrapperUtil
{
    public static Annotation getAnnotationFromParseTrees(List<String> parseTrees)
    {
        List<CoreMap> sentences = new ArrayList<CoreMap>();
        List<String> allTokens = new ArrayList<String>();
        int tokenOffset = 0;
        for (String tree : parseTrees)
        {
            List<String> tokens = new ArrayList<String>();
            String[] firstSplit = tree.split("\\) ");
            for (String f : firstSplit)
            {
                String[] secondSplit = f.split("\\(");
                String[] tagAndToken = secondSplit[secondSplit.length-1].trim().replaceAll("\\)+$", "").split(" ");
                tokens.add(tagAndToken[1]);
            }
            allTokens.addAll(tokens);
            String[] tokensArr = new String[tokens.size()];
            tokens.toArray(tokensArr);
            List<CoreLabel> sentenceTokens = Sentence.toCoreLabelList(tokensArr);
            String originalText = Sentence.listToString(tokens);

            CoreMap sentence = new Annotation(originalText);
            sentence.set(CharacterOffsetBeginAnnotation.class, 0);
            sentence.set(CharacterOffsetEndAnnotation.class, sentenceTokens.get(sentenceTokens.size() - 1).get(TextAnnotation.class).length());
            sentence.set(CoreAnnotations.TokensAnnotation.class, sentenceTokens);
            sentence.set(CoreAnnotations.TokenBeginAnnotation.class, tokenOffset);
            tokenOffset += sentenceTokens.size();
            sentence.set(CoreAnnotations.TokenEndAnnotation.class, tokenOffset);
            ParserAnnotatorUtils.fillInParseAnnotations(false, true, new EnglishGrammaticalStructureFactory(),
                    sentence, Tree.valueOf(tree), GrammaticalStructure.Extras.NONE);

            sentences.add(sentence);
        }

        Annotation allSentences = new Annotation(Sentence.listToString(allTokens));
        allSentences.set(CoreAnnotations.SentencesAnnotation.class,
                adjustCharacterOffsets(sentences, true));

        return allSentences;
    }

    // Call this once for the first sentence worth of tokens,
    // Call this again for each additional sentence worth of tokens,
    // passing in that Annotation that was just returned from that first call.
    // Otherwise pass in null as the second argument.
    public static Annotation getAnnotationFromTokens(List<String> tokens, Annotation existingAnnotation)
    {
        List<CoreMap> sentences = new ArrayList<CoreMap>();
        Annotation allSentences;

        String[] tokensArr = new String[tokens.size()];
        tokens.toArray(tokensArr);
        List<CoreLabel> sentenceTokens = Sentence.toCoreLabelList(tokensArr);
        String originalText = Sentence.listToString(tokens);

        CoreMap sentence = new Annotation(originalText);
        sentence.set(CharacterOffsetBeginAnnotation.class, 0);
        sentence.set(CharacterOffsetEndAnnotation.class,
                sentenceTokens.get(sentenceTokens.size() - 1).get(TextAnnotation.class).length());
        sentence.set(CoreAnnotations.TokensAnnotation.class, sentenceTokens);
        sentence.set(CoreAnnotations.TokenBeginAnnotation.class, 0);
        sentence.set(CoreAnnotations.TokenEndAnnotation.class, sentenceTokens.size());

        sentences.add(sentence);

        if (existingAnnotation != null)
        {
            sentences.addAll(existingAnnotation.get(CoreAnnotations.SentencesAnnotation.class));
            allSentences = existingAnnotation.copy();
            allSentences.set(CoreAnnotations.SentencesAnnotation.class,
                    adjustCharacterOffsets(sentences, true));
        }
        else
        {
            allSentences = new Annotation(Sentence.listToString(tokens));
            allSentences.set(CoreAnnotations.SentencesAnnotation.class,
                    adjustCharacterOffsets(sentences, true));
        }

        return allSentences;
    }

    public static List<CoreMap> adjustCharacterOffsets(List<CoreMap> sentences, boolean setOriginalText)
    {
        List<CoreMap> sentencesCopy = sentences;

        for (CoreMap sentence : sentencesCopy)
        {
            List<CoreLabel> sentenceTokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            int characterCount = 0;
            for (int i = 0; i < sentenceTokens.size(); i++)
            {
                CoreLabel token = sentenceTokens.get(i);
                if (setOriginalText)
                {
                    token.set(CoreAnnotations.OriginalTextAnnotation.class, token.get(CoreAnnotations.TextAnnotation.class) + " ");
                }
                int startCharacterCount = characterCount;
                int endCharacterCount = startCharacterCount + token.get(CoreAnnotations.OriginalTextAnnotation.class).length();
                token.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, startCharacterCount);
                token.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, endCharacterCount);
                sentenceTokens.set(i, token);
                characterCount = endCharacterCount;
            }
            sentence.set(CoreAnnotations.TokensAnnotation.class, sentenceTokens);
        }
        return sentencesCopy;
    }

    public static List<String> ParseTreeListObjectsToStrings(List<List<Object>> parseTrees)
    {
        List<String> trees = new ArrayList<String>();
        for (List<Object> treeAndScore : parseTrees)
        {
            trees.add((String) treeAndScore.get(0));
        }
        return trees;
    }

    public static String closeHTMLTags(String original)
    {
        String improved = original;
        Pattern pattern = Pattern.compile("(<COREF.+?</COREF>)");
        Matcher matches = pattern.matcher(original);
        while (matches.find())
        {
            String match = matches.group(0);
            int tagCount = match.split("COREF").length-1;
            if (tagCount % 2 != 0)
            {
                improved = improved.replace(match, match + "</COREF>");
            }

        }
        return improved;
    }

    /*    public static List<TaggedWord> getListOfTaggedWordsFromTaggedSentence(String taggedSentence, String divider)
    {
        String[] taggedTokens = taggedSentence.split(" ");
        TaggedWordFactory tf = new TaggedWordFactory(divider.charAt(0));
        List<TaggedWord> taggedWordList = new ArrayList<TaggedWord>();
        for (String taggedToken : taggedTokens)
        {
            taggedWordList.add((TaggedWord)tf.newLabelFromString(taggedToken));
        }
        return taggedWordList;
    }

    public static List<TaggedWord> convertTaggedTokensToTaggedWords(List<TaggedToken> taggedSentence)
    {
        List<TaggedWord> taggedSentenceWords = new ArrayList<TaggedWord>();
        for (TaggedToken tt : taggedSentence)
        {
            taggedSentenceWords.add(new TaggedWord(tt.token, tt.tag));
        }
        return taggedSentenceWords;
    }*/
}
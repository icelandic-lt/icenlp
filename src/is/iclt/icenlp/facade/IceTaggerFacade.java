/*
 * Copyright (C) 2009 Sverrir Sigmundarson
 *
 * This file is part of the IceNLP toolkit.
 * IceNLP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IceNLP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with IceNLP. If not,  see <http://www.gnu.org/licenses/>.
 *
 * Contact information:
 * Hrafn Loftsson, School of Computer Science, Reykjavik University.
 * hrafn@ru.is
 */
package is.iclt.icenlp.facade;

import is.iclt.icenlp.core.icemorphy.IceMorphy;
import is.iclt.icenlp.core.icetagger.IceTagger;
import is.iclt.icenlp.core.icetagger.IceTaggerLexicons;
import is.iclt.icenlp.core.lemmald.Lemmald;
import is.iclt.icenlp.core.tokenizer.*;
import is.iclt.icenlp.core.tritagger.TriTaggerLexicons;
import is.iclt.icenlp.core.tritagger.TriTagger;
import is.iclt.icenlp.core.utils.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Provides a simplified interface to IceTagger.
 * @author Sverrir Sigmundarson
 */
public class IceTaggerFacade
{
    private TriTagger triTagger = null;
    private IceTagger.HmmModelType modelType = IceTagger.HmmModelType.startend;
    private int sentenceStart = IceTagger.sentenceStartUpperCase;

    private boolean fullDisambiguation = true;
    private boolean initialAssignmentOnly = false;
    // Tokenizer variables
    private boolean strictTokenization = true;
    public String modelPath = "ngrams/models/";

    private Tokenizer tokenizer;
    private IceTagger tagger;
    private Segmentizer segmentizer;
    private Lexicon mapper;

    public IceTaggerFacade(IceTaggerLexicons iceLexicons, Lexicon tokenizerLexicon) throws IOException
    {
        segmentizer = new Segmentizer(tokenizerLexicon);
        this.tokenizer = new Tokenizer( Tokenizer.typeIceTokenTags,
                                        strictTokenization,
                                        tokenizerLexicon);
        this.tokenizer.findMultiWords( false );

        initIceTagger(iceLexicons);
    }

    public IceTaggerFacade(IceTaggerLexicons iceLexicons, Lexicon tokenizerLexicon,  int lineFormat) throws IOException
    {
        segmentizer = new Segmentizer(tokenizerLexicon, lineFormat);
        this.tokenizer = new Tokenizer( Tokenizer.typeIceTokenTags,
                                        strictTokenization,
                                        tokenizerLexicon);
        this.tokenizer.findMultiWords( false );
        initIceTagger(iceLexicons);

    }

    public IceTaggerFacade(IceTaggerLexicons iceLexicons, Lexicon tokenizerLexicon, Lexicon mapperLexicon, boolean preLoadlemmald) throws IOException
    {
        this(iceLexicons,tokenizerLexicon);
        this.mapper = mapperLexicon;
        if(preLoadlemmald)
            Lemmald.getInstance();
    }



    private void initIceTagger(IceTaggerLexicons iceLexicons) {

        IceMorphy morphoAnalyzer = new IceMorphy(
                iceLexicons.morphyLexicons.dict,
                iceLexicons.morphyLexicons.baseDict,
                iceLexicons.morphyLexicons.endingsBase,
                iceLexicons.morphyLexicons.endings,
                iceLexicons.morphyLexicons.endingsProper,
                iceLexicons.morphyLexicons.prefixes,
                iceLexicons.morphyLexicons.tagFrequency, null );
        tagger = new IceTagger(sentenceStart, null, morphoAnalyzer,
                iceLexicons.morphyLexicons.baseDict,
                iceLexicons.morphyLexicons.dict,
                iceLexicons.idioms,
                iceLexicons.verbPrep,
                iceLexicons.verbObj,
                iceLexicons.verbAdverb,
                initialAssignmentOnly,
                fullDisambiguation, triTagger,modelType);
                //fullDisambiguation, triTagger,false);
    }

    public void createTriTagger(TriTaggerLexicons triLexicons) throws IOException
    {
        if (triTagger == null)
        {
			triTagger = new TriTagger( sentenceStart, TriTagger.trigrams, triLexicons.ngrams, triLexicons.freqLexicon, null, null, null );
            tagger.setTriTagger(triTagger);
        }
    }

    // For backward compatability - rather use setModelType
    public void useTriTagger(boolean flag) throws IOException
    {
        //tagger.setStartWithHmmModel(flag);
        if (flag)
            tagger.setHmmModelType(IceTagger.HmmModelType.startend);
        else
            tagger.setHmmModelType(IceTagger.HmmModelType.none);
    }

    public void setModelType(IceTagger.HmmModelType mType) {
        tagger.setHmmModelType(mType);
    }

    // This method assumes that one sentence is passed (a Segmentizer is used by the caller)
    public StringBuffer tagSentence(String sentence) throws IOException
    {
        StringBuffer taggedStr = new StringBuffer(256);
        tokenizer.tokenize( sentence );
        if( tokenizer.tokens.size() > 0 )
        {
            tokenizer.splitAbbreviations();
            tagger.tagTokens( tokenizer.tokens );
            for( Object token : tokenizer.tokens )
            {
                IceTokenTags to = (IceTokenTags)token;
                taggedStr.append(to.lexeme);
                taggedStr.append(" ");
                taggedStr.append(to.getFirstTagStr());
                taggedStr.append(" ");
            }
        }
        return taggedStr;
    }

    public Sentences tag( String text ) throws IOException
    {
        Sentence sent=null;
        segmentizer.segmentize( text );

        Sentences sents = new Sentences();

        while( segmentizer.hasMoreSentences() )
        {                                                                   
            String sentenceStr = segmentizer.getNextSentence();

            if( !sentenceStr.equals( "" ) )
            {
                tokenizer.tokenize(sentenceStr);
                if( tokenizer.tokens.size() <= 0 )
                    continue;

                tokenizer.splitAbbreviations();
                tagger.tagTokens( tokenizer.tokens );

                sent = new Sentence(tokenizer.tokens);
                sents.add(sent);
            }
            //if (insertNewline)
            //    segments.add(newLineSegment);
        }

        return sents;
    }

    /**
     * Returns a IceNLP tagged string that has been mapped
     * to an equivalent Apertium form. 
     *
     * @param String object that contains the string that will
     * be tagged.
     * @return String object that contains the string apertium
     * tagged.
     */
    public String tagApertium(String text) throws IOException
    {       
        Lemmald myLemmald = Lemmald.getInstance();
        Sentences sentences = this.tag(text);
        String out = "";
        
        for(Sentence s : sentences.getSentences())
        {
            ArrayList<Token> tokens = s.getTokens();
            for(Token o : tokens)
            {
                IceTokenTags tok = (IceTokenTags)o;
                String lemma = myLemmald.lemmatize(tok.lexeme,tok.getFirstTagStr()).getLemma();
                String mappedTag = mapper.lookup(tok.getFirstTagStr(), false);
                
                if(mappedTag == null)
                    mappedTag = "<NOT MAPPED>" + ":" + tok.getFirstTagStr();
                
                // Special cases of mapping.
                if (mappedTag.matches(".*<vblex>.*")) {
                    if (lemma.equalsIgnoreCase("vera")) {mappedTag = mappedTag.replaceFirst("vblex","vbser");}
                    else if (lemma.equalsIgnoreCase("hafa")) {mappedTag = mappedTag.replaceFirst("vblex","vbhaver");}
                    else if (lemma.equalsIgnoreCase("geta")) {mappedTag = mappedTag.replaceFirst("vblex","vaux");}
                }
                out = out + "^"+lemma + mappedTag + "$ "; 
            }
        }
        return out;
    } 
}

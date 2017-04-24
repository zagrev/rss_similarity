/**
 *
 */
package com.minethurn.cs7210.rss_similarity;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;

import com.opencsv.CSVWriter;

/**
 *
 */
public class TitleWriter implements AutoCloseable
{
   /** words to throw away when comparing titles */
   private static String[] stopWordArray =
   {
         "a", "about", "above", "across", "after", "again", "against", "all", "almost", "alone", "along", "already",
         "also", "although", "always", "am", "among", "an", "and", "another", "any", "anybody", "anyone", "anything",
         "anywhere", "are", "area", "areas", "aren't", "around", "as", "ask", "asked", "asking", "asks", "at", "away",
         "b", "back", "backed", "backing", "backs", "be", "became", "because", "become", "becomes", "been", "before",
         "began", "behind", "being", "beings", "below", "best", "better", "between", "big", "both", "but", "by", "c",
         "came", "can", "cannot", "can't", "case", "cases", "certain", "certainly", "clear", "clearly", "come", "could",
         "couldn't", "d", "did", "didn't", "differ", "different", "differently", "do", "does", "doesn't", "doing",
         "done", "don't", "down", "downed", "downing", "downs", "during", "e", "each", "early", "either", "end",
         "ended", "ending", "ends", "enough", "even", "evenly", "ever", "every", "everybody", "everyone", "everything",
         "everywhere", "f", "face", "faces", "fact", "facts", "far", "felt", "few", "find", "finds", "first", "for",
         "four", "from", "full", "fully", "further", "furthered", "furthering", "furthers", "g", "gave", "general",
         "generally", "get", "gets", "give", "given", "gives", "go", "going", "good", "goods", "got", "great",
         "greater", "greatest", "group", "grouped", "grouping", "groups", "h", "had", "hadn't", "has", "hasn't", "have",
         "haven't", "having", "he", "he'd", "he'll", "her", "here", "here's", "hers", "herself", "he's", "high",
         "higher", "highest", "him", "himself", "his", "how", "however", "how's", "i", "i'd", "if", "i'll", "i'm",
         "important", "in", "interest", "interested", "interesting", "interests", "into", "is", "isn't", "it", "its",
         "it's", "itself", "i've", "j", "just", "k", "keep", "keeps", "kind", "knew", "know", "known", "knows", "l",
         "large", "largely", "last", "later", "latest", "least", "less", "let", "lets", "let's", "like", "likely",
         "long", "longer", "longest", "m", "made", "make", "making", "man", "many", "may", "me", "member", "members",
         "men", "might", "more", "most", "mostly", "mr", "mrs", "much", "must", "mustn't", "my", "myself", "n",
         "necessary", "need", "needed", "needing", "needs", "never", "new", "newer", "newest", "next", "no", "nobody",
         "non", "noone", "nor", "not", "nothing", "now", "nowhere", "number", "numbers", "o", "of", "off", "often",
         "old", "older", "oldest", "on", "once", "one", "only", "open", "opened", "opening", "opens", "or", "order",
         "ordered", "ordering", "orders", "other", "others", "ought", "our", "ours", "ourselves", "out", "over", "own",
         "p", "part", "parted", "parting", "parts", "per", "perhaps", "place", "places", "point", "pointed", "pointing",
         "points", "possible", "present", "presented", "presenting", "presents", "problem", "problems", "put", "puts",
         "q", "quite", "r", "rather", "really", "right", "room", "rooms", "s", "said", "same", "saw", "say", "says",
         "second", "seconds", "see", "seem", "seemed", "seeming", "seems", "sees", "several", "shall", "shan't", "she",
         "she'd", "she'll", "she's", "should", "shouldn't", "show", "showed", "showing", "shows", "side", "sides",
         "since", "small", "smaller", "smallest", "so", "some", "somebody", "someone", "something", "somewhere",
         "state", "states", "still", "such", "sure", "t", "take", "taken", "than", "that", "that's", "the", "their",
         "theirs", "them", "themselves", "then", "there", "therefore", "there's", "these", "they", "they'd", "they'll",
         "they're", "they've", "thing", "things", "think", "thinks", "this", "those", "though", "thought", "thoughts",
         "three", "through", "thus", "to", "today", "together", "too", "took", "toward", "turn", "turned", "turning",
         "turns", "two", "u", "under", "until", "up", "upon", "us", "use", "used", "uses", "v", "very", "w", "want",
         "wanted", "wanting", "wants", "was", "wasn't", "way", "ways", "we", "we'd", "well", "we'll", "wells", "went",
         "were", "we're", "weren't", "we've", "what", "what's", "when", "when's", "where", "where's", "whether",
         "which", "while", "who", "whole", "whom", "who's", "whose", "why", "why's", "will", "with", "within",
         "without", "won't", "work", "worked", "working", "works", "would", "wouldn't", "x", "y", "year", "years",
         "yes", "yet", "you", "you'd", "you'll", "young", "younger", "youngest", "your", "you're", "yours", "yourself",
         "yourselves", "you've", "z"
   };
   private static HashSet<String> stopWords = null;

   /** the hash of news feed that gave us the article */
   public static final int OUT_ID = 0;
   /** the news feed that gave us the article */
   public static final int OUT_URL = 1;
   /** the date the article was published */
   public static final int OUT_DATE = 2;
   /** the title of the article */
   public static final int OUT_TITLE = 3;
   /** the title of the article */
   public static final int OUT_TEXT = 3;
   /** number fo fields */
   public static final int OUT_FIELD_COUNT = 5;

   /**
    * Clean the given string by removing all the special characters and numbers. That is, leave only alphabetic
    * characters and whitespace.
    *
    * @param title
    * @return
    */
   private static String cleanString(final String title)
   {
      final StringBuffer buffer = new StringBuffer(title.length());
      for (final char c : title.toCharArray())
      {
         if (Character.isAlphabetic(c) || Character.isWhitespace(c))
         {
            buffer.append(Character.toLowerCase(c));
         }
      }
      return buffer.toString();
   }

   /** the location to receive the title output */
   private CSVWriter output;

   /**
    * Create a writer
    *
    * @param output
    *        the location to receive the output
    */
   public TitleWriter(final CSVWriter output)
   {
      this.output = output;
      synchronized (stopWordArray)
      {
         if (stopWords == null)
         {
            stopWords = new HashSet<>();
            for (final String word : stopWordArray)
            {
               stopWords.add(word);
            }
         } // if stopWords == null
      } // sync
   }

   /*
    * (non-Javadoc)
    * @see java.lang.AutoCloseable#close()
    */
   @Override
   public void close()
   {
      if (output != null)
      {
         try
         {
            output.close();
         }
         catch (final IOException e)
         {
            e.printStackTrace();
         }
      }
      output = null;
   }

   /**
    * @param id
    *        some unique id
    * @param url
    *        the IN_URL that contained the article (web site)
    * @param date
    *        the date the article was published
    * @param title
    *        the title of the article
    * @param description
    *        the description (content) of the article
    * @throws IOException
    *         if there is any input or output problem
    */
   public void write(final int id, final String url, final LocalDateTime date, final String title,
         final String description) throws IOException
   {
      // turn title into alphabetically sorted array of words. Preparing for similarity test
      final ArrayList<String> titleList = new ArrayList<>();

      // first, remove anything that's not alphabetic or whitespace
      final StringTokenizer tokenizer = new StringTokenizer(cleanString(title));

      // break into words
      while (tokenizer.hasMoreTokens())
      {
         // throw out stop words
         final String word = tokenizer.nextToken();
         if (!stopWords.contains(word))
         {
            titleList.add(word);
         }
      }

      // if no words left, ignore. Also, if no date, ignore
      if (titleList.size() > 0 && date != null)
      {
         final String[] finalList = titleList.toArray(new String[titleList.size()]);
         Arrays.sort(finalList);

         final long seconds = date.toEpochSecond(ZoneOffset.UTC);
         final String cleanTitle = String.join(" ", finalList);

         final String[] fields = new String[OUT_FIELD_COUNT];
         fields[OUT_ID] = Long.toString(id);
         fields[OUT_URL] = url;
         fields[OUT_DATE] = Long.toString(seconds);
         fields[OUT_TITLE] = cleanTitle;
         // fields[OUT_TEXT] = description;

         output.writeNext(fields);
      }
      else
         if (date != null)
         {
            System.err.println("title all stop words, ignoring (" + title + ")");
         }
      // if invalid date, error has already be reported
   }

}

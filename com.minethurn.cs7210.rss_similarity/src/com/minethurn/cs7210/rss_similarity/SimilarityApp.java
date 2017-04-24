/**
 *
 */
package com.minethurn.cs7210.rss_similarity;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

/**
 * Here we read the output of the BfMain. Each line looks like this:
 * <p>
 * IN_ID, IN_URL, IN_TIME, IN_TITLE
 * <p>
 * TIME is the date of the article, in epoch seconds <br/>
 * IN_URL is the node name <br/>
 * OUT_ID identifies the node <br/>
 * IN_TITLE is the stripped title of the article
 * <p>
 * In this routine we generate the Jacquard distance between the titles of every article and output:
 * <p>
 * ID1, ID2, title_dist, time_dist
 */
public class SimilarityApp
{
   /** the node id */
   public static final int IN_ID = 0;
   /** the source URL */
   public static final int IN_URL = 1;
   /** the date of the article, in epoch seconds */
   public static final int IN_TIME = 2;
   /** the stripped and ordered title, for calculating similarity */
   public static final int IN_TITLE = 3;
   /** the number of columns we expect to find in the input */
   public static final int IN_COUNT = 4;

   /** the number of columns we expect to find in the output */
   public static final int OUT_ID1 = 0;
   /** the number of columns we expect to find in the output */
   public static final int OUT_ID2 = 1;
   /** the number of columns we expect to find in the output */
   public static final int OUT_URL1 = 0;
   /** the number of columns we expect to find in the output */
   public static final int OUT_URL2 = 1;
   /** the number of columns we expect to find in the output */
   public static final int OUT_TITLE1 = 2;
   /** the number of columns we expect to find in the output */
   public static final int OUT_TITLE2 = 3;
   /** the number of columns we expect to find in the output */
   public static final int OUT_TITLE_DISTANCE = 4;
   /** the number of columns we expect to find in the output */
   public static final int OUT_TIME_DISTANCE = 5;
   /** the number of columns we expect to find in the output */
   public static final int OUT_START_TIME = 6;
   /** the number of columns we expect to find in the output */
   public static final int OUT_END_TIME = 7;
   /** the number of columns we expect to find in the output */
   public static final int OUT_COUNT = 8;

   /**
    * converts the title from a space separated string to a {@link HashSet}
    *
    * @param title
    *        the title to convert
    * @return the set of words in the title
    */
   static HashSet<String> convertTitleToSet(final String title)
   {
      final String[] words = title.split("[ \t\n\f]");
      final HashSet<String> set = new HashSet<>();
      for (final String w : words)
      {
         if (w.length() > 0)
         {
            set.add(w);
         }
      }
      return set;
   }

   /**
    * @param title1
    * @param title2
    * @return the cosine distance between the title vectors
    */
   static double getCosineDistance(final String title1, final String title2)
   {
      double distance = 0.0;
      final HashSet<String> t1Words = convertTitleToSet(title1);
      final HashSet<String> t2Words = convertTitleToSet(title2);
      final HashSet<String> allWords = new HashSet<>(t1Words);
      allWords.addAll(t2Words);

      // intersectWords.retainAll(t1Words);
      // intersectWords.retainAll(t2Words);

      final String[] wordArray = allWords.toArray(new String[allWords.size()]);
      Arrays.sort(wordArray);

      final long w1vector = getVector(title1, wordArray);
      final long w2vector = getVector(title2, wordArray);

      long maxValue = 0;
      for (@SuppressWarnings("unused")
      final String element : wordArray)
      {
         maxValue = (maxValue << 1) | 1;
      }

      final long numerator = w1vector * w2vector;
      final long w1denom = w1vector * w1vector;
      final long w2denom = w2vector * w2vector;
      final double denom = maxValue;

      distance = Math.sqrt(numerator) / denom;
      return distance;
   }

   /**
    * determine the Jaccard distance between the two
    *
    * @param title1
    *        the title of the first article
    * @param title2
    *        the title of the second article
    * @return the Jacquard distance
    */
   static double getJaccardDistance(final String title1, final String title2)
   {
      final HashSet<String> t1Words = convertTitleToSet(title1);
      final HashSet<String> t2Words = convertTitleToSet(title2);
      final HashSet<String> allWords = new HashSet<>();

      allWords.addAll(t1Words);
      allWords.addAll(t2Words);

      final HashSet<String> intersectWords = new HashSet<>(allWords);
      intersectWords.retainAll(t1Words);
      intersectWords.retainAll(t2Words);

      if (allWords.size() > 0)
      {
         final double distance = (1.0 * intersectWords.size()) / allWords.size();
         return distance;
      }
      return 0.0;
   }

   /**
    * turn the phrase into a word vector using the order given.
    *
    * @param phrase
    *        the words to turn into a vector
    * @param wordOrder
    *        the order of the words in the vector
    * @return a bit vector representing the words in the phrase
    */
   static long getVector(final String phrase, final String[] wordOrder)
   {
      long result = 0;

      for (int i = 0; i < wordOrder.length && i < 32; i++)
      {
         result = (result << 1);
         if (phrase.contains(wordOrder[i]))
         {
            result = result | 1;
         }
      }
      return result;
   }

   /**
    * @param name
    *        the website URL
    * @return a distinct human readable name for the website
    */
   public static String getWebsiteName(final String name)
   {
      String result = name;
      try
      {
         final URL url = new URL(name);
         result = url.getHost();
         if (result.equals("feeds.feedburner.com"))
         {
            result = url.getPath();
            final int index = result.indexOf("/", 1);
            if (index > 0)
            {
               result = result.substring(1, index);
            }
         }
         else
         {
            result = result.replace("www.", "");
            final String[] segments = result.split("\\.");
            final int size = segments.length;
            if (size > 1)
            {
               if (size > 2 && result.endsWith("co.uk"))
               {
                  result = segments[size - 3] + "." + segments[size - 2] + "." + segments[size - 1];
               }
               else
               {
                  result = segments[size - 2] + "." + segments[size - 1];
               }
            }

         }
      }
      catch (final MalformedURLException url)
      {
         System.err.println("Invalid URL " + name);
      }
      return result;
   }

   /**
    * @param args
    */
   public static void main(final String[] args)
   {

      // prepare to parse the command line
      final Options options = new Options();
      options.addOption(Option.builder("file").argName("filename").hasArg().desc("the input file name").build());
      options.addOption(Option.builder("out").argName("filename").hasArg().desc("the output file name").build());
      options.addOption(
            Option.builder("progressCount").argName("n").hasArg().desc("output a status ever n lines").build());
      options.addOption(Option.builder("similarity").argName("decimal").hasArg()
            .desc("How similar two titles must be to consider them the same. Defaults to 0.5 (which might be high)")
            .build());

      final CommandLineParser parser = new DefaultParser();
      CommandLine cmdLine;
      try
      {
         cmdLine = parser.parse(options, args);
      }
      catch (@SuppressWarnings("unused") final ParseException e)
      {
         final HelpFormatter formatter = new HelpFormatter();
         formatter.printHelp("bf", options);
         return;
      }

      // ok, get parsed results from command line
      final String inputFilename = cmdLine.getOptionValue("file", "input.csv");
      final String outputFilename = cmdLine.getOptionValue("out", "titles.csv");
      final String progressCountArg = cmdLine.getOptionValue("progressCount", "50000");
      final String similarityArg = cmdLine.getOptionValue("similarity", "0.5");
      int progressCount = 0;
      double similarity = 0.5;

      try
      {
         progressCount = Integer.parseInt(progressCountArg);
      }
      catch (@SuppressWarnings("unused") final NumberFormatException ignored)
      {
         System.out
               .println("Invalid number for progressCount (" + progressCountArg + "). No progress will be reported");
      }

      try
      {
         similarity = Double.parseDouble(similarityArg);
      }
      catch (@SuppressWarnings("unused") final NumberFormatException ignored)
      {
         System.err.println("Unable to convert the similarity to a double: " + similarityArg + ". Using "
               + Double.toString(similarity));
      }

      // ok, start the processing
      try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputFilename)));
            final CSVReader reader = new CSVReader(new FileReader(inputFilename)))
      {
         final SimilarityApp app = new SimilarityApp();
         app.createSimilarity(reader, writer, progressCount, similarity);
      }
      catch (final IOException e)
      {
         e.printStackTrace();
      }
   }

   private final ArrayList<String[]> previous = new ArrayList<>();
   private final NumberFormat integerFormat = NumberFormat.getIntegerInstance();

   private int writeCount;

   /**
    * @param reader
    *        the CSV to read
    * @param writer
    *        the CSV to write
    * @param progressCount
    *        how often to print progress messages. 0 means nothing.
    * @param similarity
    *        how similar two titles must be before they are written out as similar
    * @throws IOException
    *         if there is an issue with input or output
    */
   private void createSimilarity(final CSVReader reader, final CSVWriter writer, final int progressCount,
         final double similarity) throws IOException
   {
      String[] line;
      boolean invalidLine = false;
      int lineCount = 0;
      writeCount = 0;

      // final ExecutorService pool = new BlockingFixedThreadPoolExecutor(8, 1000);
      final ExecutorService pool = Executors.newFixedThreadPool(8);
      final ExecutorService writerPool = Executors.newSingleThreadExecutor();

      while ((line = reader.readNext()) != null)
      {
         // if the line before was invalid, check that this line starts with the IN_URL
         if (invalidLine && line.length > 0 && line[0].startsWith("http"))
         {
            invalidLine = false;
         }
         if (invalidLine == false)
         {
            // for each new line, compare its distance with every line we have already read
            if (line.length >= IN_COUNT)
            {
               // now compare the new line against every existing line
               for (final String[] prev : previous)
               {
                  // we create these so the closure is easy to compute
                  final String id1 = line[IN_ID];
                  final String id2 = prev[IN_ID];

                  final String title1 = line[IN_TITLE];
                  final String title2 = prev[IN_TITLE];

                  final String time1 = line[IN_TIME];
                  final String time2 = prev[IN_TIME];

                  final String url1 = line[IN_URL];
                  final String url2 = prev[IN_URL];

                  if (url1.equals(url2))
                  {
                     // don't self reference
                  }
                  else
                  {
                     // ok, calculate the results in a separate thread pool
                     pool.submit((Runnable) () ->
                     {
                        final double titleDistance = getJaccardDistance(title1, title2);
                        // final double cosineDist = getCosineDistance(title1, title2);
                        long timeDistance = 0;
                        long t1 = 0;
                        long t2 = 0;

                        try
                        {
                           t2 = Integer.parseInt(time2);
                           t1 = Integer.parseInt(time1);
                           timeDistance = (t1 > t2) ? (t1 - t2) : (t2 - t1);
                        }
                        catch (@SuppressWarnings("unused") final NumberFormatException e)
                        {
                           System.err.println("Invalid time:  " + time1 + ", " + time2);
                        }

                        // if the result is worthy, write it out (using a single output thread)
                        if (titleDistance > similarity)
                        {
                           // make sure the earlier article it url1 in the output
                           final String[] record = new String[OUT_COUNT];
                           if (t1 < t2)
                           {
                              record[OUT_ID1] = id1;
                              record[OUT_ID2] = id2;
                              record[OUT_URL1] = getWebsiteName(url1);
                              record[OUT_URL2] = getWebsiteName(url2);
                              record[OUT_TITLE1] = title1;
                              record[OUT_TITLE2] = title2;
                              record[OUT_TIME_DISTANCE] = Long.toString(timeDistance);
                              record[OUT_TITLE_DISTANCE] = Double.toString(titleDistance);
                              record[OUT_START_TIME] = Long.toString(t1);
                              record[OUT_END_TIME] = Long.toString(t2);
                           }
                           else // time2 is earlier
                           {
                              record[OUT_ID1] = id2;
                              record[OUT_ID2] = id1;
                              record[OUT_URL1] = getWebsiteName(url2);
                              record[OUT_URL2] = getWebsiteName(url1);
                              record[OUT_TITLE1] = title2;
                              record[OUT_TITLE2] = title1;
                              record[OUT_TIME_DISTANCE] = Long.toString(timeDistance);
                              record[OUT_TITLE_DISTANCE] = Double.toString(titleDistance);
                              record[OUT_START_TIME] = Long.toString(t2);
                              record[OUT_END_TIME] = Long.toString(t1);
                           }

                           writerPool.submit(() ->
                           {
                              // System.out.println("cosine = " + Double.toString(cosineDist));
                              writer.writeNext(record);
                              incrementWriteCount();
                           }); // end write
                        }
                     }); // end calculate
                  }
               } // end for loop

               // an add this line to the collection
               previous.add(line);
            }
            else // this line is definitely crap
            {
               invalidLine = true;
               if (line.length > 0)
               {
                  final int length = Math.min(20, line[0].length());
                  final String msg = line[0].substring(0, length);
                  System.err.println("Invalid line at line " + lineCount + ", line = " + msg);
               }
               else
               {
                  System.err.println("line is just crap");
               }
            } // this line is crap
         }
         if (lineCount > 0 && progressCount > 0 && lineCount % progressCount == 0)
         {
            System.out.println("Read " + integerFormat.format(lineCount) + " lines");
         }

         lineCount++;
      } // end while

      // shut down the thread pools
      pool.shutdown();
      System.out.println("Shutting down work pool");
      // now wait for all the threads to finish
      try
      {
         while (!pool.isTerminated())
         {
            System.out.println("Waiting for shutdown to complete");
            pool.awaitTermination(2, TimeUnit.MINUTES);
         }
      }
      catch (final InterruptedException e)
      {
         e.printStackTrace();
         pool.shutdownNow();
      }

      // don't shutdown the write pool until all the distances have been calculated.
      System.out.println("Shutting down writer pool");
      writerPool.shutdown();
      try
      {
         writerPool.awaitTermination(2, TimeUnit.MINUTES);
      }
      catch (final InterruptedException e)
      {
         e.printStackTrace();
         writerPool.shutdownNow();
      }

      if (progressCount > 0)
      {
         System.out.println(Instant.now().toString() + "  Read " + integerFormat.format(lineCount) + " lines");
         System.out.println(Instant.now().toString() + "  Wrote " + integerFormat.format(writeCount) + " lines");
      }
   }

   /**
    * this is for the write closures to count output lines.
    */
   private synchronized void incrementWriteCount()
   {
      writeCount++;
      // System.out.println("writeCount = " + writeCount);
   }
}

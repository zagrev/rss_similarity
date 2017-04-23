/**
 *
 */
package com.minethurn.cs7210.rss_similarity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * unit tests for similarity
 */
@SuppressWarnings("static-method")
public class TestSimilarity
{
   /**
    * test identical
    */
   @Test
   public void testCosineDistance1()
   {
      final String words = "bob ted carol alice";

      final double distance = SimilarityApp.getCosineDistance(words, words);
      assertEquals(1.0, distance, 0.01);
   }

   /**
    * test identical
    */
   @Test
   public void testDistance1()
   {
      final String words = "bob ted carol alice";

      final double distance = SimilarityApp.getJaccardDistance(words, words);
      assertEquals(1.0, distance, 0.01);
   }

   /**
    * test 3/4
    */
   @Test
   public void testDistance2()
   {
      final String words1 = "bob ted carol alice";
      final String words2 = "bob ted carol ";

      final double distance = SimilarityApp.getJaccardDistance(words1, words2);
      assertEquals(0.75, distance, 0.01);
   }

   /**
    * test 3/4
    */
   @Test
   public void testDistancenone()
   {
      final String words1 = "bob ted carol alice";
      final String words2 = "steve randy boba fett";

      final double distance = SimilarityApp.getJaccardDistance(words1, words2);
      assertEquals(0.0, distance, 0.01);
   }
}

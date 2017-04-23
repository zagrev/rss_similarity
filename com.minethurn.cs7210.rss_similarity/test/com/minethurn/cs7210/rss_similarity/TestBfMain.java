/**
 *
 */
package com.minethurn.cs7210.rss_similarity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDateTime;

import org.junit.Test;

/**
 * @author Steve Betts
 */
@SuppressWarnings("static-method")
public class TestBfMain
{

   /**
    * test some date formats
    */
   @Test
   public void testDateFormatMdy()
   {
      final String date1 = "Apr 22, 2017 20:22:13 EST";
      final LocalDateTime date = BfMain.getDate(date1);
      assertEquals("bob", date.toString());
   }

   /**
    * test some date formats
    */
   @Test
   public void testDateFormatRfc1123()
   {
      final String date1 = "Tue, 11 Apr 2017 11:22:13 GMT";
      final LocalDateTime date = BfMain.getDate(date1);
      assertNotNull("Failed to parse RFC 1123 date", date);
      assertEquals("2017-04-11T11:22:13", date.toString());
   }

   /**
    * test some date formats
    */
   @Test
   public void testDateFormatRfc1123Example()
   {
      final String date1 = "Tue, 3 Jun 2008 11:05:30 GMT";
      final LocalDateTime date = BfMain.getDate(date1);
      assertEquals("2008-06-03T11:05:30", date.toString());
   }
}

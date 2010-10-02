import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class PlayOrders extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="PlayOrders";

   public void init()
      {
      context = getServletContext();
      synchronized(context)
        {
        pool=(ConnectionPool)context.getAttribute("pool");
        if(pool==null)
          {
          String driverClassName = context.getInitParameter("driverClassName");
          String url = context.getInitParameter("url");
          String userName = context.getInitParameter("username");
          String password = context.getInitParameter("password");
          try
            {
            pool=new ConnectionPool(driverClassName,url,userName,password);
            }
          catch(Exception error)
            {
			Routines.writeToLog(servletName,"Unable to create connection pool : " + error,false,context);
            }
          context.setAttribute("pool",pool);
          }
        }
      }

   public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
      {
      doGet(request,response);
      }

   public void doGet(HttpServletRequest request,
                     HttpServletResponse response)
      {
      response.setContentType("text/html");
	  PrintWriter webPageOutput=null;
	  try
		{
		webPageOutput=response.getWriter();
		}
	  catch(IOException error)
		{
		Routines.writeToLog(servletName,"Error getting writer : " + error,false,context);
		}
      HttpSession session=request.getSession();
      String action=request.getParameter("action");
      String disabledText="";
      if("New Order".equals(action)||
         "Change Order".equals(action))
         {
         disabledText=" DISABLED";
         }
      else
        {
        session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
        }
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      if(Routines.loginCheck(true,request,response,database,context))
        {
        return;
        }
      Routines.WriteHTMLHead("Play Orders",//title
                             false,//showMenu
                             11,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      int orderNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Order".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PlayOrderNumber " +
                                       "FROM playorders");
          while(queryResult.next())
               {
               orderNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(orderNumber))))
                 {
                 changeCount++;
                 if(!changeRequested)
                   {
                   changeRequested=true;
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to find play order entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No order selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one order to change");
          disabledText="";
          action="";
          }
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/PlayOrders\" METHOD=\"POST\">");
      if("New Order".equals(action)||
         "Change Order".equals(action))
        {
        formScreen(action,
                   database,
                   session,
                   request,
                   webPageOutput);
        }
      else
        {
        viewScreen(action,
                   session,
                   database,
                   request,
                   response,
                   webPageOutput);
        }
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void viewScreen(String action,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      boolean updated=true;
      int orderNumber=0;
      if ("Store New Order".equals(action)||
          "Store Changed Order".equals(action)||
          "Delete Order".equals(action)||
          "Move Order Up".equals(action)||
          "Move Order Down".equals(action))
          {
          updated=updateEntry(action,
                              session,
                              request,
                              database);
          }
      if((String)session.getAttribute("message")!=null)
        {
        Routines.tableStart(false,webPageOutput);
        Routines.tableHeader("Messages",0,webPageOutput);
        Routines.tableDataStart(true,false,true,true,false,0,0,"scoresrow",webPageOutput);
        Routines.messageCheck(false,request,webPageOutput);
        Routines.tableDataEnd(true,false,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        webPageOutput.println(Routines.spaceLines(1));
        }
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Orders",2,webPageOutput);
      boolean ordersFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PlayOrderNumber,PlayOrderName " +
                                     "FROM playorders " +
                                     "ORDER BY Sequence ASC");
        orderNumber=0;
        String orderName="";
        while(queryResult.next())
             {
             if(!ordersFound)
               {
               ordersFound=true;
               }
             orderNumber=queryResult.getInt(1);
             orderName=queryResult.getString(2);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(orderNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + orderNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(orderName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve playorders : " + error,false,context);	
        }
      if(!ordersFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Orders found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Order\" NAME=\"action\">");
      if(ordersFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Order\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Order\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Order Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Order Down\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

      private void formScreen(String action,
                              Connection database,
                              HttpSession session,
                              HttpServletRequest request,
                              PrintWriter webPageOutput)
      {
      int orderNumber=0;
      String orderName="";
      Routines.tableStart(false,webPageOutput);
      if("Change Order".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PlayOrderNumber " +
                                       "FROM playorders " +
                                       "ORDER BY Sequence DESC");
          int tempOrderNumber=0;
          while(queryResult.next())
               {
               tempOrderNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempOrderNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT PlayOrderNumber,PlayOrderName " +
                                              "FROM playorders " +
                                              "WHERE PlayOrderNumber=" + tempOrderNumber);
                 if(queryResult.first())
                   {
                   orderNumber=queryResult.getInt(1);
                   orderName=queryResult.getString(2);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find order (" + tempOrderNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve playorder: " + error,false,context);	
            }
      Routines.tableHeader("Amend details of Order",2,webPageOutput);
      }
      if("New Order".equals(action))
        {
        Routines.tableHeader("Enter details of new Order",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"orderName\" SIZE=\"10\" MAXLENGTH=\"10\" VALUE=\"" + orderName + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Order".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Order\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Order\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"orderNumber\" VALUE=\"" + orderNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int orderNumber=Routines.safeParseInt(request.getParameter("orderNumber"));
      int sequence=0;
      String orderName=request.getParameter("orderName");
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM playorders " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(orderNumber==0)
          {
          //Get latest OrderNumber.
          orderNumber=1;
          queryResult=sql.executeQuery("SELECT PlayOrderNumber " +
                                       "FROM playorders " +
                                       "ORDER BY PlayOrderNumber DESC");
          if(queryResult.first())
            {
            orderNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve playorders : " + error,false,context);	
        }
      if("Move Order Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayOrderNumber " +
                                         "FROM playorders " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               orderNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(orderNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,PlayOrderName FROM playorders " +
                                                "WHERE PlayOrderNumber=" + orderNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the orders list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE playorders " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Order not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE playorders " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE PlayOrderNumber=" + orderNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Order not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move playorders : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No Orders selected");
          }
        success=true;
        }
      if("Move Order Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayOrderNumber " +
                                         "FROM playorders " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               orderNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(orderNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,PlayOrderName FROM playorders " +
                                                "WHERE PlayOrderNumber=" + orderNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the orders list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE playorders " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Order not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE playorders " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PlayOrderNumber=" + orderNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"playorders not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move playorders : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No orders selected");
          }
        success=true;
        }
      if("Store New Order".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO playorders (" +
                                    "PlayOrderNumber,Sequence,PlayOrderName,DateTimeStamp) " +
                                    "VALUES (" +
                                    orderNumber + "," +
                                    (sequence+1) + ",\"" +
                                    orderName + "\",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New order not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create playorders : " + error,false,context);
          }
        session.setAttribute("message",orderName + " order stored successfully");
        success=true;
        }
      if("Store Changed Order".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE playorders " +
                                    "SET PlayOrderName='" + orderName + "'," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE PlayOrderNumber=" + orderNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Order not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update playorders : " + error,false,context);	
          }
        session.setAttribute("message",orderName + " order changed successfully");
        success=true;
        }
      if("Delete Order".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayOrderNumber " +
                                         "FROM playorders");
          while(queryResult1.next())
               {
               orderNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(orderNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT SituationNumber " +
                                                "FROM defaultsituations " +
                                                "WHERE PlayOrderNumber=" + orderNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Order currently in use by defaultsituations entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT SituationNumber " +
                                                  "FROM situations " +
                                                  "WHERE PlayOrderNumber=" + orderNumber);
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Order currently in use by situations entries");
                     return false;
                     }
                   else
                     {
                     updates=sql2.executeUpdate("DELETE FROM playorders " +
                                                "WHERE PlayOrderNumber=" + orderNumber);
                     if(updates!=1)
                       {
					   Routines.writeToLog(servletName,"Order not deleted (" + orderNumber + ")",false,context);	
                       }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT PlayOrderNumber " +
                                         "FROM playorders " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          orderNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                orderNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE playorders " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE PlayOrderNumber=" + orderNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"playorder entry not reset (" + orderNumber + ")",false,context);	
                   }
                }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete playorders : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No order selected");
          }
        success=true;
        }
      return success;
      }
}
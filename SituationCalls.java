import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class SituationCalls extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="SituationCalls";

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
      int type=Routines.safeParseInt(request.getParameter("type"));
      String action=request.getParameter("action");
      String[] types={"Offense","Defense","Special Teams"};
      String disabledText="";
      if("New Call".equals(action)||
         "Change Call".equals(action))
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
      Routines.WriteHTMLHead("Situation Calls",//title
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
      int callNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Call".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SituationCallNumber " +
                                       "FROM situationcalls " +
                                       "WHERE Type=" + type);
          while(queryResult.next())
               {
               callNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(callNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find situationcalls entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No call selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one call to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      if ("Store New Call".equals(action)||
          "Store Changed Call".equals(action)||
          "Delete Call".equals(action)||
          "Move Call Up".equals(action)||
          "Move Call Down".equals(action))
          {
          updated=updateEntry(action,
                              type,
                              session,
                              request,
                              database);
          }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/SituationCalls\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(!updated)
        {
        disabledText="";
        }
      webPageOutput.println("<SELECT" + disabledText + " NAME=\"type\">");
      String selected="";
      for(int currentType=0;currentType<types.length;currentType++)
         {
         if(currentType==type)
           {
           selected=" SELECTED";
           }
         else
           {
           selected="";
           }
         webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentType + "\">" + types[currentType]);
         }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<INPUT" + disabledText + " TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
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
      if("New Call".equals(action)||
         "Change Call".equals(action))
        {
        formScreen(action,
                   type,
                   database,
                   session,
                   request,
                   webPageOutput);
        }
      else
        {
        viewScreen(action,
                   type,
                   updated,
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
                           int type,
                           boolean updated,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      String currentFormation="";
      int callNumber=0;
      Routines.tableStart(false,webPageOutput);
      String titleText="";
      if(type==0)
        {
        titleText="Offensive";
        }
      if(type==1)
        {
        titleText="Defensive";
        }
      if(type==2)
        {
        titleText="Special Teams";
        }
      Routines.tableHeader(titleText + " Situation Calls",2,webPageOutput);
      boolean callsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SituationCallNumber,SituationCallName " +
                                     "FROM situationcalls " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        callNumber=0;
        String callName="";
        while(queryResult.next())
             {
             if(!callsFound)
               {
               callsFound=true;
               }
             callNumber=queryResult.getInt(1);
             callName=queryResult.getString(2);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(callNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + callNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(callName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve situationcalls : " + error,false,context);	
        }
      if(!callsFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Calls found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Call\" NAME=\"action\">");
      if(callsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Call\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Call\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Call Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Call Down\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

      private void formScreen(String action,
                              int type,
                              Connection database,
                              HttpSession session,
                              HttpServletRequest request,
                              PrintWriter webPageOutput)
      {
      int callNumber=0;
      String callName="";
      Routines.tableStart(false,webPageOutput);
      String titleText="";
      if(type==0)
        {
        titleText="Offensive";
        }
      if(type==1)
        {
        titleText="Defensive";
        }
      if(type==2)
        {
        titleText="Special Teams";
        }
      if("Change Call".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SituationCallNumber " +
                                       "FROM situationcalls " +
                                       "WHERE Type=" + type + " " +
                                       "ORDER BY Sequence DESC");
          int tempCallNumber=0;
          while(queryResult.next())
               {
               tempCallNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempCallNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT SituationCallNumber,SituationCallName " +
                                              "FROM situationcalls " +
                                              "WHERE SituationCallNumber=" + tempCallNumber);
                 if(queryResult.first())
                   {
                   callNumber=queryResult.getInt(1);
                   callName=queryResult.getString(2);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find call (" + tempCallNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve situationcalls : " + error,false,context);	
            }
      Routines.tableHeader("Amend details of " + titleText + " call",2,webPageOutput);
      }
      if("New Call".equals(action))
        {
        Routines.tableHeader("Enter details of new " + titleText + " call",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"callName\" SIZE=\"20\" MAXLENGTH=\"20\" VALUE=\"" + callName + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Call".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Call\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Call\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"callNumber\" VALUE=\"" + callNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"type\" VALUE=\"" + type + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            int type,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int callNumber=Routines.safeParseInt(request.getParameter("callNumber"));
      int sequence=0;
      String callName=request.getParameter("callName");
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM situationcalls " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(callNumber==0)
          {
          //Get latest SituationCallNumber.
          callNumber=1;
          queryResult=sql.executeQuery("SELECT SituationCallNumber " +
                                       "FROM situationcalls " +
                                       "ORDER BY SituationCallNumber DESC");
          if(queryResult.first())
            {
            callNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve situationcalls : " + error,false,context);		
        }
      if("Move Call Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SituationCallNumber " +
                                         "FROM situationcalls " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               callNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(callNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,SituationCallName FROM situationcalls " +
                                                "WHERE SituationCallNumber=" + callNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the call list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE situationcalls " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Call not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE situationcalls " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE SituationCallNumber=" + callNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Call not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move situationcalls : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No calls selected");
          }
        success=true;
        }
      if("Move Call Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SituationCallNumber " +
                                         "FROM situationcalls " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               callNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(callNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,SituationCallName FROM situationcalls " +
                                                "WHERE SituationCallNumber=" + callNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the call list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE situationcalls " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Call not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE situationcalls " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE SituationCallNumber=" + callNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Call not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move situationcalls : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No calls selected");
          }
        success=true;
        }
      if("Store New Call".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO situationcalls (" +
                                    "SituationCallNumber,Type," +
                                    "Sequence,SituationCallName,DateTimeStamp) " +
                                    "VALUES (" +
                                    callNumber + "," +
                                    type + "," +
                                    (sequence+1) + ",\"" +
                                    callName + "\",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New situationcall not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create situationcall : " + error,false,context);	
          }
        session.setAttribute("message",callName + " call stored successfully");
        success=true;
        }
      if("Store Changed Call".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE situationcalls " +
                                    "SET SituationCallName='" + callName + "'," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE SituationCallNumber=" + callNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Call not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update situationcalls : " + error,false,context);	
          }
        session.setAttribute("message",callName + " call changed successfully");
        success=true;
        }
      if("Delete Call".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SituationCallNumber " +
                                         "FROM situationcalls " +
                                         "WHERE Type=" + type);
          while(queryResult1.next())
               {
               callNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(callNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT SituationNumber " +
                                                "FROM defaultsituations " +
                                                "WHERE SituationCallNumber1=" + callNumber + " " +
                                                "OR SituationCallNumber2=" + callNumber + " " +
                                                "OR SituationCallNumber3=" + callNumber + " " +
                                                "OR SituationCallNumber4=" + callNumber + " " +
                                                "OR SituationCallNumber5=" + callNumber + " " +
                                                "OR SituationCallNumber6=" + callNumber + " " +
                                                "OR SituationCallNumber7=" + callNumber + " " +
                                                "OR SituationCallNumber8=" + callNumber + " " +
                                                "OR SituationCallNumber9=" + callNumber + " " +
                                                "OR SituationCallNumber10=" + callNumber + " " +
                                                "OR SituationCallNumber11=" + callNumber + " " +
                                                "OR SituationCallNumber12=" + callNumber + " " +
                                                "OR SituationCallNumber13=" + callNumber + " " +
                                                "OR SituationCallNumber14=" + callNumber + " " +
                                                "OR SituationCallNumber15=" + callNumber + " " +
                                                "OR SituationCallNumber16=" + callNumber + " " +
                                                "OR SituationCallNumber17=" + callNumber + " " +
                                                "OR SituationCallNumber18=" + callNumber + " " +
                                                "OR SituationCallNumber19=" + callNumber + " " +
                                                "OR SituationCallNumber20=" + callNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Call currently in use by defaultsituations entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT SituationNumber " +
                                                  "FROM situations " +
                                                  "WHERE SituationCallNumber1=" + callNumber + " " +
                                                  "OR SituationCallNumber2=" + callNumber + " " +
                                                  "OR SituationCallNumber3=" + callNumber + " " +
                                                  "OR SituationCallNumber4=" + callNumber + " " +
                                                  "OR SituationCallNumber5=" + callNumber + " " +
                                                  "OR SituationCallNumber6=" + callNumber + " " +
                                                  "OR SituationCallNumber7=" + callNumber + " " +
                                                  "OR SituationCallNumber8=" + callNumber + " " +
                                                  "OR SituationCallNumber9=" + callNumber + " " +
                                                  "OR SituationCallNumber10=" + callNumber + " " +
                                                  "OR SituationCallNumber11=" + callNumber + " " +
                                                  "OR SituationCallNumber12=" + callNumber + " " +
                                                  "OR SituationCallNumber13=" + callNumber + " " +
                                                  "OR SituationCallNumber14=" + callNumber + " " +
                                                  "OR SituationCallNumber15=" + callNumber + " " +
                                                  "OR SituationCallNumber16=" + callNumber + " " +
                                                  "OR SituationCallNumber17=" + callNumber + " " +
                                                  "OR SituationCallNumber18=" + callNumber + " " +
                                                  "OR SituationCallNumber19=" + callNumber + " " +
                                                  "OR SituationCallNumber20=" + callNumber);
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Call currently in use by situations entries");
                     return false;
                     }
                 else
                     {
                     updates=sql2.executeUpdate("DELETE FROM situationcalls " +
                                                "WHERE SituationCallNumber=" + callNumber);
                       if(updates!=1)
                         {
						 Routines.writeToLog(servletName,"Call not deleted (" + callNumber + ")",false,context);	
                         }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT SituationCallNumber " +
                                         "FROM situationcalls " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          callNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                callNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE situationcalls " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE SituationCallNumber=" + callNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Situationcall entry not reset (" + callNumber + ")",false,context);	
                   }
                }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete situationcalls : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No calls selected");
          }
        success=true;
        }
      return success;
      }
}
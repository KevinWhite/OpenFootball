import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Plays extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Plays";

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
      if("New Play".equals(action)||
         "Change Play".equals(action))
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
      Routines.WriteHTMLHead("Plays",//title
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
      int playNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Play".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PlayNumber " +
                                       "FROM plays " +
                                       "WHERE Type=" + type);
          while(queryResult.next())
               {
               playNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(playNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find plays entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No play selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one play to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      if ("Store New Play".equals(action)||
          "Store Changed Play".equals(action)||
          "Delete Play".equals(action)||
          "Move Play Up".equals(action)||
          "Move Play Down".equals(action))
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
                             "/servlet/Plays\" METHOD=\"POST\">");
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
      if("New Play".equals(action)||
         "Change Play".equals(action))
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
      int playNumber=0;
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
      Routines.tableHeader(titleText + " Plays",2,webPageOutput);
      boolean playsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PlayNumber,PlayName,SubType " +
                                     "FROM plays " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        playNumber=0;
        String playName="";
        int subType=0;
        while(queryResult.next())
             {
             if(!playsFound)
               {
               playsFound=true;
               }
             playNumber=queryResult.getInt(1);
             playName=queryResult.getString(2);
             subType=queryResult.getInt(3);
             if(type==2)
               {
               if(subType==0)
                 {
                 playName+=" (Kick)";
                 }
               else
                 {
                 playName+=" (Return)";
                 }
               }
             else
               {
               if(subType==0)
                 {
                 playName+=" (Run)";
                 }
               else
                 {
                 playName+=" (Pass)";
                 }
               }
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(playNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + playNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(playName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve plays : " + error,false,context);		
        }
      if(!playsFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Plays found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Play\" NAME=\"action\">");
      if(playsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Play\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Play\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Play Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Play Down\" NAME=\"action\">");
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
      int playNumber=0;
      int subType=0;
      String playName="";
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
      if("Change Play".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PlayNumber " +
                                       "FROM plays " +
                                       "WHERE Type=" + type + " " +
                                       "ORDER BY Sequence DESC");
          int tempPlayNumber=0;
          while(queryResult.next())
               {
               tempPlayNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempPlayNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT PlayNumber,PlayName,SubType " +
                                              "FROM plays " +
                                              "WHERE PlayNumber=" + tempPlayNumber);
                 if(queryResult.first())
                   {
                   playNumber=queryResult.getInt(1);
                   playName=queryResult.getString(2);
                   subType=queryResult.getInt(3);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find play (" + tempPlayNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve play: " + error,false,context);	
            }
      Routines.tableHeader("Amend details of " + titleText + " play",2,webPageOutput);
      }
      if("New Play".equals(action))
        {
        Routines.tableHeader("Enter details of new " + titleText + " play",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"playName\" SIZE=\"30\" MAXLENGTH=\"30\" VALUE=\"" + playName + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Play Type");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"subType\">");
      if(type==2)
        {
        if(subType==0)
          {
          webPageOutput.println(" <OPTION SELECTED VALUE=\"0\">Kick");
          webPageOutput.println(" <OPTION VALUE=\"1\">Return");
          }
        else
          {
          webPageOutput.println(" <OPTION VALUE=\"0\">Kick");
          webPageOutput.println(" <OPTION SELECTED VALUE=\"1\">Return");
          }
        }
      else
        {
        if(subType==0)
          {
          webPageOutput.println(" <OPTION SELECTED VALUE=\"0\">Run");
          webPageOutput.println(" <OPTION VALUE=\"1\">Pass");
          }
        else
          {
          webPageOutput.println(" <OPTION VALUE=\"0\">Run");
          webPageOutput.println(" <OPTION SELECTED VALUE=\"1\">Pass");
          }
        }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Play".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Play\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Play\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"playNumber\" VALUE=\"" + playNumber + "\">");
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
      int playNumber=Routines.safeParseInt(request.getParameter("playNumber"));
      int subType=Routines.safeParseInt(request.getParameter("subType"));
      int sequence=0;
      String playName=request.getParameter("playName");
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM plays " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(playNumber==0)
          {
          //Get latest PlayNumber.
          playNumber=1;
          queryResult=sql.executeQuery("SELECT PlayNumber " +
                                       "FROM plays " +
                                       "ORDER BY PlayNumber DESC");
          if(queryResult.first())
            {
            playNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve plays : " + error,false,context);		
        }
      if("Move Play Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayNumber " +
                                         "FROM plays " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               playNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(playNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,PlayName FROM plays " +
                                                "WHERE PlayNumber=" + playNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the play list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE plays " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Play not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE plays " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE PlayNumber=" + playNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Play not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move plays : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No plays selected");
          }
        success=true;
        }
      if("Move Play Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayNumber " +
                                         "FROM plays " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               playNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(playNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,PlayName FROM plays " +
                                                "WHERE PlayNumber=" + playNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the play list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE plays " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Play not moved (prior), reason unknown",false,context);		
                   }
                 updates=sql1.executeUpdate("UPDATE plays " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PlayNumber=" + playNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Play not moved (current), reason unknown",false,context);		
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move plays : " + error,false,context);		
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No plays selected");
          }
        success=true;
        }
      if("Store New Play".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO plays (" +
                                    "PlayNumber,Type," +
                                    "Sequence,PlayName,SubType,DateTimeStamp) " +
                                    "VALUES (" +
                                    playNumber + "," +
                                    type + "," +
                                    (sequence+1) + ",\"" +
                                    playName + "\"," +
                                    subType + ",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New play not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create plays : " + error,false,context);	
          }
        session.setAttribute("message",playName + " play stored successfully");
        success=true;
        }
      if("Store Changed Play".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE plays " +
                                    "SET PlayName='" + playName + "'," +
                                    "SubType=" + subType + ",DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE PlayNumber=" + playNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Play not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update plays : " + error,false,context);	
          }
        session.setAttribute("message",playName + " play changed successfully");
        success=true;
        }
      if("Delete Play".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PlayNumber " +
                                         "FROM plays " +
                                         "WHERE Type=" + type);
          while(queryResult1.next())
               {
               playNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(playNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                "FROM defaultplaybook " +
                                                "WHERE PlayNumber=" + playNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Play currently in use by defaultplaybook entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                  "FROM playbook " +
                                                  "WHERE PlayNumber=" + playNumber);
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Play currently in use by playbook entries");
                     return false;
                     }
                 else
                     {
                     queryResult2=sql2.executeQuery("SELECT PlayByPlayNumber " +
                                                    "FROM playbyplay " +
                                                    "WHERE OffensivePlayNumber=" + playNumber + " " +
                                                    "OR DefensivePlayNumber=" + playNumber);
                     if(queryResult2.first())
                       {
                       session.setAttribute("message","Play currently in use by playbyplay entries");
                       return false;
                       }
                     else
                       {
                       updates=sql2.executeUpdate("DELETE FROM plays " +
                                                  "WHERE PlayNumber=" + playNumber);
                       if(updates!=1)
                         {
						 Routines.writeToLog(servletName,"Play not deleted (" + playNumber + ")",false,context);	
                         }
                       }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT PlayNumber " +
                                         "FROM plays " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          playNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                playNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE plays " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE PlayNumber=" + playNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Play entry not reset (" + playNumber + ")",false,context);	
                   }
                }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete plays : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No plays selected");
          }
        success=true;
        }
      return success;
      }
}
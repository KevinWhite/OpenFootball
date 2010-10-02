import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class MyDefensiveGamePlan extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="MyDefensiveGamePlan";

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
      boolean formScreen=false;
      String currentSituation="";
      if("New Play".equals(action)||
         "Change Play".equals(action))
         {
         formScreen=true;
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
      if(Routines.loginCheck(false,request,response,database,context))
        {
        return;
        }
      Routines.WriteHTMLHead("My Defensive GamePlan",//title
                             false,//showMenu
                             2,//menuHighLight
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
      String[] myTeamData = new String[2];
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      myTeamData = Routines.getMyTeamName(Routines.safeParseInt((String)session.getAttribute("coachNumber")),
                                          league,
                                          database,
                                          context);
      String[] colourDetails = Routines.getTeamColours(Routines.safeParseInt(myTeamData[1]),database,context);
      String shirtColour  = colourDetails[1];
      String numberColour = colourDetails[2];
      Routines.titleHeader("head2",true,webPageOutput);
      webPageOutput.println(myTeamData[0] + " (My Defensive GamePlan)");
      Routines.titleTrailer(true,webPageOutput);
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/GamePlan.gif\"" +
                            " WIDTH='90' HEIGHT='75' ALT='My Defensive GamePlan'>");
      webPageOutput.println("</CENTER>");
      int situationNumber=Routines.safeParseInt(request.getParameter("situationNumber"));
      int setNumber=Routines.safeParseInt(request.getParameter("setNumber"));
      int snapCount=Routines.safeParseInt(request.getParameter("snapCount"));
      int formPlay=Routines.safeParseInt(request.getParameter("FormPlay"));
      int formationNumber=Routines.safeParseInt(request.getParameter("formation" + formPlay));
      int playNumber=Routines.safeParseInt(request.getParameter("play" + formPlay));
      int playBookNumber=Routines.safeParseInt(request.getParameter("playBookNumber"));
      int teamNumber=Routines.safeParseInt((String)session.getAttribute("team"));
      boolean updated=true;
      String disabledText="";
      String selectText="";
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse;
         if(situationNumber==0)
           {
           queryResponse=sql.executeQuery("SELECT SituationNumber " +
                                          "FROM situations " +
                                          "WHERE Defense=1 " +
                                          "AND TeamNumber=" + teamNumber + " " +
                                          "ORDER BY SituationNumber ASC");
           if(queryResponse.first())
             {
             situationNumber=queryResponse.getInt(1);
             }
           else
             {
			 Routines.writeToLog(servletName,"No situations found",false,context);	
             }
           }
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Unable to retrieve situations : " + error,false,context);	
         }
      if ("Store New Play".equals(action)||
          "Store Changed Play".equals(action)||
          "Delete Play".equals(action)||
          "Move Play Up".equals(action)||
          "Move Play Down".equals(action))
          {
          updated=updateEntry(action,
                              session,
                              request,
                              database);
          }
      if("Change Play".equals(action))
        {
        boolean changeRequested=false;
        updated=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PlayBookNumber " +
                                       "FROM playbook " +
                                       "WHERE SituationNumber=" + situationNumber);
          while(queryResult.next())
               {
               playBookNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(playBookNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find playbook entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No play selected");
          formScreen=false;
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one play to change");
          formScreen=false;
          action="";
          }
        }
      if(formScreen)
        {
        disabledText="DISABLED ";
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
      webPageOutput.println("<FORM ACTION=\"http://" +
                            request.getServerName() +
                            ":" +
                            request.getServerPort() +
                            request.getContextPath() +
                            "/servlet/MyDefensiveGamePlan\" METHOD=\"POST\">");
      Routines.myTableStart(false,webPageOutput);
      Routines.myTableHeader("Situation",1,webPageOutput);
      Routines.myTableColumnHeaders("<TD ALIGN=\"CENTER\"><FONT CLASS=\"opt2\">Select the situation you wish to see the plays for, then click 'View'</FONT></TD>",webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,1,"scoresrow",webPageOutput);
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SituationNumber,SituationName " +
                                     "FROM situations " +
                                     "WHERE Defense=1 " +
                                     "AND TeamNumber=" + teamNumber + " " +
                                     "ORDER BY Sequence ASC");
        webPageOutput.println("<SELECT NAME=\"situationNumber\" " + disabledText + ">");
        int tempSituationNumber=0;
        String situationName="";
        String selected="";
        while(queryResult.next())
           {
           tempSituationNumber=queryResult.getInt(1);
           situationName=queryResult.getString(2);
           if(tempSituationNumber==situationNumber)
              {
              selected="SELECTED ";
              currentSituation=situationName;
              }
           else
              {
              selected="";
              }
           webPageOutput.println(" " +
                                 "<OPTION " +
                                 selected +
                                 "VALUE=\"" +
                                 tempSituationNumber +
                                 "\">" +
                                 situationName);
           }
         }
       catch(SQLException error)
         {
	     Routines.writeToLog(servletName,"Unable to retrieve situations : " + error,false,context);	
         }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\" " + disabledText + ">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.myTableStart(false,webPageOutput);
      Routines.myTableHeader("PlayBook Entries for " + currentSituation + " Situation",4,webPageOutput);
      Routines.myTableColumnHeaders("<TD><FONT CLASS=\"opt2\">Sel</FONT></TD><TD><FONT CLASS=\"opt2\">Set</FONT></TD><TD><FONT CLASS=\"opt2\">Play</FONT></TD>",webPageOutput);
      boolean playBooksFound=false;
      try
        {
        Statement sql1=database.createStatement();
        Statement sql2=database.createStatement();
        ResultSet queryResult1;
        ResultSet queryResult2;
        queryResult1=sql1.executeQuery("SELECT PlayBookNumber,SetName,FormationName,PlayName,playbook.SetNumber,playbook.FormationNumber,playbook.PlayNumber " +
                                       "FROM playbook,defaultsets,defaultformations,defaultplays " +
                                       "WHERE playbook.Defense=1 " +
                                       "AND playbook.SituationNumber = " + situationNumber + " " +
                                       "AND playbook.SetNumber=defaultsets.SetNumber " +
                                       "AND playbook.FormationNumber=defaultformations.FormationNumber " +
                                       "AND playbook.PlayNumber=defaultplays.PlayNumber " +
                                       "ORDER BY playbook.Sequence ASC");
        int tempPlayBookNumber=0;
        String setName="";
        String formationName="";
        String playName="";
        setNumber=0;
        while(queryResult1.next())
             {
             if(!playBooksFound)
               {
               playBooksFound=true;
               }
             tempPlayBookNumber=queryResult1.getInt(1);
             setName=queryResult1.getString(2);
             formationName=queryResult1.getString(3);
             playName=queryResult1.getString(4);
             setNumber=queryResult1.getInt(5);
             formationNumber=queryResult1.getInt(6);
             playNumber=queryResult1.getInt(7);
             boolean selected=false;
             String param="";
             selectText="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(tempPlayBookNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 selectText="CHECKED";
                 }
               }
             if(selected&&"Change Play".equals(action))
               {
               formLine(false,
                        tempPlayBookNumber,
                        setNumber,
                        formationNumber,
                        playNumber,
                        database,
                        webPageOutput);
               }
             else
               {
               if(formScreen)
                 {
                 disabledText="DISABLED ";
                 }
               else
                 {
                 disabledText="";
                 }
               Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
               webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                                   tempPlayBookNumber +
                                   "\" VALUE=\"true\" " +
                                   selectText +
                                   " " +
                                   disabledText +
                                   ">");
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(true,false,false,false,false,20,0,"scoresrow",webPageOutput);
               webPageOutput.println(setName);
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(true,false,false,false,false,45,0,"scoresrow",webPageOutput);
               webPageOutput.println(formationName + " formation, " + playName);
               Routines.tableDataEnd(false,false,false,webPageOutput);
               }
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve playbooks : " + error,false,context);	
        }
      if("New Play".equals(action))
        {
        if(!playBooksFound)
          {
          playBooksFound=true;
          Routines.tableDataStart(true,false,true,true,false,5,0,"scoresrow",webPageOutput);
          webPageOutput.println("Sel");
          Routines.tableDataEnd(true,false,false,webPageOutput);
          Routines.tableDataStart(true,false,true,false,false,20,0,"scoresrow",webPageOutput);
          webPageOutput.println("Set");
          Routines.tableDataEnd(true,false,false,webPageOutput);
          Routines.tableDataStart(true,false,true,false,false,45,0,"scoresrow",webPageOutput);
          webPageOutput.println("Play");
          Routines.tableDataEnd(true,false,false,webPageOutput);
          }
        formLine(true,
                 playBookNumber,
                 0,
                 0,
                 0,
                 database,
                 webPageOutput);
        }
      if(formScreen)
        {
        webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"situationNumber\" VALUE=\"" + situationNumber + "\">");
        }
      if(!playBooksFound&&!formScreen)
        {
        Routines.tableDataStart(true,true,false,true,false,0,4,"scoresrow",webPageOutput);
        webPageOutput.println("No Playbook entries found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",4,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,4,"scoresrow",webPageOutput);
      if(formScreen)
        {
        if("New Play".equals(action))
          {
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Play\" NAME=\"action\">");
          }
        else
          {
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Play\" NAME=\"action\">");
          }
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Play\" NAME=\"action\">");
        if(playBooksFound)
          {
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Play\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Play\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Play Up\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Play Down\" NAME=\"action\">");
          }
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"team\" VALUE=\"" + teamNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + league + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"situation\" VALUE=\"" + situationNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int playBookNumber=Routines.safeParseInt(request.getParameter("playBookNumber"));
      int situationNumber=Routines.safeParseInt(request.getParameter("situationNumber"));
      int setNumber=Routines.safeParseInt(request.getParameter("setNumber"));
      int formPlay=Routines.safeParseInt(request.getParameter("FormPlay"));
      int formationNumber=Routines.safeParseInt(request.getParameter("formation" + formPlay));
      int playNumber=Routines.safeParseInt(request.getParameter("play" + formPlay));
      int teamNumber=Routines.safeParseInt((String)session.getAttribute("teamNumber"));
      int sequence=0;
      int offense=0;
      int defense=1;
      int specialTeams=0;
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM playbook " +
                                     "WHERE SituationNumber=" + situationNumber + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(playBookNumber==0)
          {
          //Get latest PlayBookNumber.
          playBookNumber=1;
          queryResult=sql.executeQuery("SELECT PlayBookNumber " +
                                       "FROM playbook " +
                                       "ORDER BY PlayBookNumber DESC");
          if(queryResult.first())
            {
            playBookNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve playbook : " + error,false,context);	
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
          queryResult1=sql1.executeQuery("SELECT PlayBookNumber " +
                                         "FROM playbook " +
                                         "WHERE SituationNumber=" + situationNumber + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               playBookNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(playBookNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence FROM playbook " +
                                                "WHERE PlayBookNumber=" + playBookNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message","PlayBook entry is already at the top of the play list.");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE playbook " +
                                            "SET Sequence=(Sequence+1) " +
                                            "WHERE SituationNumber=" + situationNumber + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Playbook entry not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE playbook " +
                                            "SET Sequence=(Sequence-1) " +
                                            "WHERE PlayBookNumber=" + playBookNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Playbook entry not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
	      Routines.writeToLog(servletName,"Unable to move playbook : " + error,false,context);	
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
          queryResult1=sql1.executeQuery("SELECT PlayBookNumber " +
                                         "FROM playbook " +
                                         "WHERE SituationNumber=" + situationNumber + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               playBookNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(playBookNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence FROM playbook " +
                                                "WHERE PlayBookNumber=" + playBookNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message","Playbook entry is already at the bottom of the play list.");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE playbook " +
                                            "SET Sequence=(Sequence-1) " +
                                            "WHERE SituationNumber=" + situationNumber + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Playbook entry not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE playbook " +
                                            "SET Sequence=(Sequence+1) " +
                                            "WHERE PlayBookNumber=" + playBookNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Playbook entry not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move playbook entry : " + error,false,context);	
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
          updates=sql.executeUpdate("INSERT INTO playbook (" +
                                    "PlayBookNumber,TeamNumber,SituationNumber," +
                                    "Offense,Defense,SpecialTeams," +
                                    "Sequence,SetNumber," +
                                    "FormationNumber,PlayNumber) " +
                                    "VALUES (" +
                                    playBookNumber + "," +
                                    teamNumber + "," +
                                    situationNumber + "," +
                                    offense + "," +
                                    defense + "," +
                                    specialTeams + "," +
                                    (sequence+1) + "," +
                                    setNumber + "," +
                                    formationNumber + "," +
                                    playNumber + ")");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New playbook not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create playbook entry : " + error,false,context);	
          }
        session.setAttribute("message","Playbook entry created successfully");
        success=true;
        }
      if("Store Changed Play".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE playbook " +
                                    "SET SetNumber=" + setNumber +
                                    ",FormationNumber=" + formationNumber +
                                    ",PlayNumber=" + playNumber + " " +
                                    "WHERE PlayBookNumber=" + playBookNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Playbook entry not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update playbook entry : " + error,false,context);	
          }
        session.setAttribute("message","Playbook entry updated successfully");
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
          ResultSet queryResult;
          queryResult=sql1.executeQuery("SELECT PlayBookNumber " +
                                        "FROM playbook " +
                                        "WHERE SituationNumber=" + situationNumber);
          while(queryResult.next())
               {
               playBookNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(playBookNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 updates=sql2.executeUpdate("DELETE FROM playbook " +
                                            "WHERE PlayBookNumber=" + playBookNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Playbook entry not deleted (" + playBookNumber + ")",false,context);	
                   }
                 }
               }
          queryResult=sql1.executeQuery("SELECT PlayBookNumber " +
                                        "FROM playbook " +
                                        "WHERE SituationNumber=" + situationNumber + " " +
                                        "ORDER BY Sequence ASC");
          int newSequence=0;
          playBookNumber=0;
          while(queryResult.next())
                {
                newSequence++;
                playBookNumber=queryResult.getInt(1);
                updates=sql2.executeUpdate("UPDATE playbook " +
                                           "SET Sequence=" + newSequence + " " +
                                           "WHERE PlayBookNumber=" + playBookNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Playbook entry not reset (" + playBookNumber + ")",false,context);	
                   }
                }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete playbook entry : " + error,false,context);	
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

   private void formLine(boolean create,
                         int playBookNumber,
                         int setNumber,
                         int formationNumber,
                         int playNumber,
                         Connection database,
                         PrintWriter webPageOutput)
      {
      String disabledText="";
      String selectText="";
      disabledText="DISABLED ";
      if(!create)
        {
        selectText="CHECKED ";
        }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
        webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                            playBookNumber +
                            "\" VALUE=\"true\" " +
                            selectText +
                            " " +
                            disabledText +
                            ">");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,20,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"setNumber\">");
        queryResult=sql.executeQuery("SELECT SetNumber,SetName " +
                                     "FROM defaultsets " +
                                     "ORDER BY SetNumber ASC");
        int tempSetNumber=0;
        String setName="";
        while(queryResult.next())
             {
             tempSetNumber=queryResult.getInt(1);
             setName=queryResult.getString(2);
             if(setNumber==tempSetNumber)
               {
               selectText="SELECTED ";
               }
             else
               {
               selectText="";
               }
             webPageOutput.println(" " +
                                   "<OPTION " +
                                   selectText +
                                   "VALUE=\"" +
                                   tempSetNumber +
                                   "\">" +
                                   setName);
             }
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,45,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"FormPlay\">");
        queryResult=sql.executeQuery("SELECT defaultformations.FormationNumber,FormationName,PlayNumber,PlayName " +
                                     "FROM defaultformations,defaultplays " +
                                     "WHERE defaultformations.FormationNumber=defaultplays.FormationNumber " +
                                     "AND defaultformations.Defense=1 " +
                                     "ORDER BY defaultformations.FormationNumber ASC, defaultplays.PlayNumber ASC");
        int tempFormationNumber=0;
        int tempPlayNumber=0;
        String formationName="";
        String playName="";
        int selection=0;
        while(queryResult.next())
             {
             selection++;
             tempFormationNumber=queryResult.getInt(1);
             formationName=queryResult.getString(2);
             tempPlayNumber=queryResult.getInt(3);
             playName=queryResult.getString(4);
             if(formationNumber==tempFormationNumber&&
                playNumber==tempPlayNumber)
                {
                selectText="SELECTED ";
                }
             else
                {
                selectText="";
                }
             webPageOutput.println(" " +
                                   "<OPTION " +
                                   selectText +
                                   "VALUE=\"" +
                                   selection +
                                   "\">" +
                                   formationName + " formation, " + playName);
             }
        selection=0;
        queryResult.beforeFirst();
        while(queryResult.next())
             {
             selection++;
             tempFormationNumber=queryResult.getInt(1);
             tempPlayNumber=queryResult.getInt(3);
             webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"formation" + selection + "\" VALUE=\"" + tempFormationNumber + "\">");
             webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"play" + selection + "\" VALUE=\"" + tempPlayNumber + "\">");
             }
        Routines.tableDataEnd(false,false,false,webPageOutput);
        webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"playBookNumber\" VALUE=\"" + playBookNumber + "\">");
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to find playbook entries : " + error,false,context);	
        }
      }
}